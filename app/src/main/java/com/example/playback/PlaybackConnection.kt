package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.domain.model.PlaybackState
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackConnection(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Dedicated flow for high-frequency position updates to avoid full-screen recompositions
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Track>>(emptyList())
    val currentQueue: StateFlow<List<Track>> = _currentQueue.asStateFlow()

    private var originalQueue = listOf<Track>()
    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            setupPlayerListener()
            updatePlaybackState()
            if (controller?.isPlaying == true) {
                startProgressPoller()
            }
        }, context.mainExecutor)
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                if (isPlaying) {
                    startProgressPoller()
                } else {
                    stopProgressPoller()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playbackState.value = _playbackState.value.copy(repeatMode = mode)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.value = _playbackState.value.copy(isShuffleEnabled = shuffleModeEnabled)
            }
        })
    }

    private fun startProgressPoller() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.let {
                        if (it == C.TIME_UNSET) _playbackState.value.currentTrack?.durationMs ?: 0L
                        else it.coerceAtLeast(0L)
                    }
                    val service = PlaybackService.instance
                    val sleepRem = service?.sleepTimerSec?.value
                    val isEndOfTrack = service?.isEndOfTrackSleepTimer?.value ?: false

                    _currentPositionMs.value = pos
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        sleepTimerRemainingSec = sleepRem,
                        isSleepTimerEndOfTrack = isEndOfTrack,
                        isPlaying = true
                    )
                } else {
                    break
                }
                delay(500L) // Polls only while actively playing, 2x/sec
            }
        }
    }

    private fun stopProgressPoller() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlaybackState() {
        val ctrl = controller ?: return
        val item = ctrl.currentMediaItem
        val currentTrack = item?.let { mediaItemToTrack(it) }
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        val rawDur = ctrl.duration
        val dur = if (rawDur == C.TIME_UNSET || rawDur <= 0L) {
            currentTrack?.durationMs ?: 0L
        } else {
            rawDur
        }

        _currentPositionMs.value = pos
        _playbackState.value = _playbackState.value.copy(
            currentTrack = currentTrack,
            isPlaying = ctrl.isPlaying,
            currentPositionMs = pos,
            durationMs = dur
        )
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val ctrl = controller ?: return
        if (tracks.isEmpty()) return

        originalQueue = tracks
        _currentQueue.value = tracks

        val mediaItems = tracks.map { trackToMediaItem(it) }
        ctrl.setMediaItems(mediaItems, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun playNext(tracks: List<Track>) {
        val ctrl = controller ?: return
        if (tracks.isEmpty()) return

        val currentIndex = ctrl.currentMediaItemIndex
        val targetIndex = if (currentIndex >= 0 && currentIndex < _currentQueue.value.size) {
            currentIndex + 1
        } else {
            _currentQueue.value.size
        }

        val updatedQueue = _currentQueue.value.toMutableList()
        updatedQueue.addAll(targetIndex, tracks)
        _currentQueue.value = updatedQueue

        val mediaItems = tracks.map { trackToMediaItem(it) }
        ctrl.addMediaItems(targetIndex, mediaItems)
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playTracks(queue, index)
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            ctrl.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        controller?.seekTo(clamped)
        _currentPositionMs.value = clamped
        _playbackState.value = _playbackState.value.copy(currentPositionMs = clamped)
    }

    fun skipNext() {
        controller?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
    }

    fun skipPrevious() {
        controller?.let {
            if (it.currentPosition > 3000L || !it.hasPreviousMediaItem()) {
                seekTo(0L)
            } else {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        val ctrl = controller ?: return
        val playerMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        ctrl.repeatMode = playerMode
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
    }

    fun cycleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(next)
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffleEnabled
        _playbackState.value = _playbackState.value.copy(isShuffleEnabled = newShuffle)

        val ctrl = controller ?: return
        val currentTrack = _playbackState.value.currentTrack

        if (newShuffle) {
            val queue = _currentQueue.value.toMutableList()
            if (currentTrack != null) {
                queue.removeIf { it.id == currentTrack.id }
                queue.shuffle()
                queue.add(0, currentTrack)
            } else {
                queue.shuffle()
            }
            _currentQueue.value = queue
            val mediaItems = queue.map { trackToMediaItem(it) }
            ctrl.setMediaItems(mediaItems, 0, ctrl.currentPosition)
        } else {
            val restoreQueue = originalQueue
            _currentQueue.value = restoreQueue
            val restoreIndex = currentTrack?.let { ct -> restoreQueue.indexOfFirst { it.id == ct.id } } ?: 0
            val mediaItems = restoreQueue.map { trackToMediaItem(it) }
            ctrl.setMediaItems(mediaItems, restoreIndex.coerceAtLeast(0), ctrl.currentPosition)
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        controller?.playbackParameters = PlaybackParameters(clamped)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
    }

    fun setSleepTimer(minutes: Int) {
        PlaybackService.instance?.setSleepTimer(minutes)
    }

    fun setSleepTimerEndOfTrack() {
        PlaybackService.instance?.setSleepTimerEndOfTrack()
    }

    fun cancelSleepTimer() {
        PlaybackService.instance?.cancelSleepTimer()
        _playbackState.value = _playbackState.value.copy(
            sleepTimerRemainingSec = null,
            isSleepTimerEndOfTrack = false
        )
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val ctrl = controller ?: return
        val list = _currentQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _currentQueue.value = list
            ctrl.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun removeQueueItem(index: Int) {
        val ctrl = controller ?: return
        val list = _currentQueue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _currentQueue.value = list
            ctrl.removeMediaItem(index)
        }
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setExtras(Bundle().apply {
                putLong("id", track.id)
                putString("path", track.path)
                putString("lyrics", track.lyrics)
                putInt("bitrate", track.bitrate)
                putString("genre", track.genre)
                putLong("albumId", track.albumId)
                putLong("durationMs", track.durationMs)
            })

        // Only attach MediaStore albumart URI for real MediaStore album IDs (500L is demo placeholder)
        if (track.albumId > 0L && track.albumId != 500L) {
            metadataBuilder.setArtworkUri(
                Uri.parse("content://media/external/audio/albumart/${track.albumId}")
            )
        }

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun mediaItemToTrack(mediaItem: MediaItem): Track {
        val meta = mediaItem.mediaMetadata
        val extras = meta.extras
        val id = extras?.getLong("id") ?: mediaItem.mediaId.toLongOrNull() ?: 0L
        val path = extras?.getString("path") ?: ""
        val lyrics = extras?.getString("lyrics")
        val bitrate = extras?.getInt("bitrate", 320) ?: 320
        val genre = extras?.getString("genre") ?: "Audio"
        val albumId = extras?.getLong("albumId") ?: 0L
        val durationMs = extras?.getLong("durationMs") ?: 0L

        return Track(
            id = id,
            uri = mediaItem.requestMetadata.mediaUri?.toString() ?: "",
            path = path,
            title = meta.title?.toString() ?: "Unknown Title",
            artist = meta.artist?.toString() ?: "Unknown Artist",
            album = meta.albumTitle?.toString() ?: "Unknown Album",
            albumId = albumId,
            durationMs = durationMs,
            bitrate = bitrate,
            genre = genre,
            lyrics = lyrics
        )
    }

    fun release() {
        stopProgressPoller()
        scope.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controller = null
    }
}