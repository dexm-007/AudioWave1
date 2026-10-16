package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MusicRepository
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.EqualizerState
import com.example.domain.model.FolderItem
import com.example.domain.model.LrcLine
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.model.TrackSortOrder
import com.example.playback.PlaybackConnection
import com.example.playback.PlaybackService
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("audiowave_prefs", Context.MODE_PRIVATE)
    private val repository = MusicRepository(application)
    val playbackConnection = PlaybackConnection(application)

    val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState
    val currentPositionMs: StateFlow<Long> = playbackConnection.currentPositionMs
    val currentQueue: StateFlow<List<Track>> = playbackConnection.currentQueue

    // Theme state
    private val _currentThemeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.EMERALD.name) ?: AppThemeMode.EMERALD.name)
        } catch (e: Exception) {
            AppThemeMode.EMERALD
        }
    )
    val currentThemeMode: StateFlow<AppThemeMode> = _currentThemeMode.asStateFlow()

    // Sort order (default: Recently Added)
    private val _currentSortOrder = MutableStateFlow(TrackSortOrder.RECENTLY_ADDED)
    val currentSortOrder: StateFlow<TrackSortOrder> = _currentSortOrder.asStateFlow()

    // Audio Engine Settings
    val crossfadeSec = MutableStateFlow(prefs.getInt("crossfade_sec", 0))
    val isGaplessEnabled = MutableStateFlow(prefs.getBoolean("gapless_enabled", true))

    // General Settings
    val pauseOnHeadsetDisconnect = MutableStateFlow(prefs.getBoolean("pause_on_headset_disconnect", true))
    val duckVolumeOnNotification = MutableStateFlow(prefs.getBoolean("duck_volume_on_notification", true))
    val keepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", false))

    // Multi-Select Mode
    val isMultiSelectActive = MutableStateFlow(false)
    val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())

    val allTracks: StateFlow<List<Track>> = _currentSortOrder.flatMapLatest { sortOrder ->
        repository.getSortedTracks(sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTrackIds: StateFlow<Set<Long>> = repository.favoriteTracks.map { list ->
        list.map { it.id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val mostPlayedTracks: StateFlow<List<Track>> = repository.mostPlayedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<Album>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<Artist>> = repository.allArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Only emit when the currentTrack object itself changes, completely ignoring position ticks!
    private val distinctCurrentTrack: Flow<Track?> = playbackState
        .map { it.currentTrack }
        .distinctUntilChanged { old, new -> old?.id == new?.id && old?.uri == new?.uri }

    // Live Current Track: Only recomputes when track ID, library list, or favorites change
    val liveCurrentTrack: StateFlow<Track?> = combine(
        distinctCurrentTrack,
        allTracks,
        favoriteTrackIds
    ) { currentTrack, trackList, favIds ->
        val currentId = currentTrack?.id
        if (currentId != null) {
            val baseTrack = trackList.find { it.id == currentId } ?: currentTrack
            baseTrack?.copy(isFavorite = favIds.contains(currentId))
        } else {
            null
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Search query & filtering offloaded to background Default dispatcher
    val searchQuery = MutableStateFlow("")
    val filteredTracks: StateFlow<List<Track>> = combine(allTracks, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lyrics
    private val _currentLyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val currentLyrics: StateFlow<List<LrcLine>> = _currentLyrics.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(0)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    // Folder navigation
    private val defaultRootPath = application.filesDir.absolutePath
    private val _currentFolderPath = MutableStateFlow(defaultRootPath)
    val currentFolderPath: StateFlow<String> = _currentFolderPath.asStateFlow()

    private val _folderItems = MutableStateFlow<List<FolderItem>>(emptyList())
    val folderItems: StateFlow<List<FolderItem>> = _folderItems.asStateFlow()

    // UI sheet & dialog visibility
    val isNowPlayingExpanded = MutableStateFlow(false)
    val showEqualizerDialog = MutableStateFlow(false)
    val showSleepTimerDialog = MutableStateFlow(false)
    val showQueueSheet = MutableStateFlow(false)
    val showSettingsSheet = MutableStateFlow(false)
    val showCreatePlaylistDialog = MutableStateFlow(false)
    val trackForPlaylistSelection = MutableStateFlow<Track?>(null)
    val trackForTagEditing = MutableStateFlow<Track?>(null)
    val selectedPlaylistDetail = MutableStateFlow<Playlist?>(null)
    val playlistDetailTracks = MutableStateFlow<List<Track>>(emptyList())

    // Delete confirmation for multi-select
    val showDeleteConfirmDialog = MutableStateFlow(false)

    // Equalizer state
    val equalizerState: StateFlow<EqualizerState> = PlaybackService.instance?.effectManager?.equalizerState
        ?: MutableStateFlow(
            EqualizerState(
                bands = listOf(
                    com.example.domain.model.EqualizerBand(0, 60, 0),
                    com.example.domain.model.EqualizerBand(1, 230, 0),
                    com.example.domain.model.EqualizerBand(2, 910, 0),
                    com.example.domain.model.EqualizerBand(3, 3600, 0),
                    com.example.domain.model.EqualizerBand(4, 14000, 0)
                )
            )
        )

    private var lastRecordedTrackId: Long? = null

    init {
        viewModelScope.launch {
            repository.initializeLibraryIfEmpty()
            loadFolder(defaultRootPath)
        }

        // Parse lyrics only when the track actually changes
        viewModelScope.launch(Dispatchers.Default) {
            distinctCurrentTrack.collect { track ->
                if (track != null && !track.lyrics.isNullOrBlank()) {
                    _currentLyrics.value = repository.parseLrc(track.lyrics)
                } else {
                    _currentLyrics.value = emptyList()
                }
                _activeLyricIndex.value = 0
            }
        }

        // Synchronize active lyric line and play count off the main thread
        viewModelScope.launch(Dispatchers.Default) {
            currentPositionMs.collect { pos ->
                val track = playbackState.value.currentTrack ?: return@collect
                val lyrics = _currentLyrics.value
                if (lyrics.isNotEmpty()) {
                    val activeIdx = lyrics.indexOfLast { it.timestampMs <= pos }.coerceAtLeast(0)
                    if (_activeLyricIndex.value != activeIdx) {
                        _activeLyricIndex.value = activeIdx
                    }
                }

                // Count as played if played for > 10 seconds and not already recorded this session
                if (pos > 10000L && lastRecordedTrackId != track.id) {
                    lastRecordedTrackId = track.id
                    repository.incrementPlayCount(track.id)
                }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _currentThemeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setSortOrder(order: TrackSortOrder) {
        _currentSortOrder.value = order
    }

    fun setCrossfade(seconds: Int) {
        crossfadeSec.value = seconds
        prefs.edit().putInt("crossfade_sec", seconds).apply()
    }

    fun setGapless(enabled: Boolean) {
        isGaplessEnabled.value = enabled
        prefs.edit().putBoolean("gapless_enabled", enabled).apply()
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        lastRecordedTrackId = null
        playbackConnection.playTrack(track, queue)
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            lastRecordedTrackId = null
        }
        playbackConnection.playTracks(tracks, startIndex)
    }

    fun togglePlayPause() = playbackConnection.togglePlayPause()

    fun seekTo(posMs: Long) = playbackConnection.seekTo(posMs)

    fun skipNext() {
        lastRecordedTrackId = null
        playbackConnection.skipNext()
    }

    fun skipPrevious() {
        lastRecordedTrackId = null
        playbackConnection.skipPrevious()
    }

    fun toggleShuffle() = playbackConnection.toggleShuffle()

    fun cycleRepeatMode() = playbackConnection.cycleRepeatMode()

    fun setSpeed(speed: Float) = playbackConnection.setSpeed(speed)

    fun setSleepTimer(minutes: Int) = playbackConnection.setSleepTimer(minutes)

    fun setSleepTimerEndOfTrack() = playbackConnection.setSleepTimerEndOfTrack()

    fun cancelSleepTimer() = playbackConnection.cancelSleepTimer()

    fun toggleFavorite(track: Track) {
        val isCurrentlyFav = favoriteTrackIds.value.contains(track.id)
        viewModelScope.launch {
            repository.toggleFavorite(track.id, isCurrentlyFav)
        }
    }

    fun setPauseOnHeadsetDisconnect(enabled: Boolean) {
        pauseOnHeadsetDisconnect.value = enabled
        prefs.edit().putBoolean("pause_on_headset_disconnect", enabled).apply()
    }

    fun setDuckVolumeOnNotification(enabled: Boolean) {
        duckVolumeOnNotification.value = enabled
        prefs.edit().putBoolean("duck_volume_on_notification", enabled).apply()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        keepScreenOn.value = enabled
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
    }

    // MULTI-SELECT ACTIONS
    fun toggleTrackSelection(trackId: Long) {
        val current = selectedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        selectedTrackIds.value = current
        if (current.isEmpty()) {
            isMultiSelectActive.value = false
        }
    }

    fun startMultiSelect(initialId: Long) {
        isMultiSelectActive.value = true
        selectedTrackIds.value = setOf(initialId)
    }

    fun selectAll() {
        selectedTrackIds.value = filteredTracks.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        isMultiSelectActive.value = false
    }

    fun playSelectedNext() {
        val selectedIds = selectedTrackIds.value
        val tracksToPlay = allTracks.value.filter { selectedIds.contains(it.id) }
        if (tracksToPlay.isNotEmpty()) {
            playbackConnection.playNext(tracksToPlay)
        }
        clearSelection()
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        val selectedIds = selectedTrackIds.value.toList()
        viewModelScope.launch {
            repository.addTracksToPlaylist(playlistId, selectedIds)
            clearSelection()
            trackForPlaylistSelection.value = null
        }
    }

    fun deleteSelectedTracks() {
        val selectedIds = selectedTrackIds.value.toList()
        viewModelScope.launch {
            repository.deleteTracks(selectedIds)
            clearSelection()
            showDeleteConfirmDialog.value = false
        }
    }

    fun setEqualizerBand(bandIndex: Short, levelMb: Short) {
        PlaybackService.instance?.effectManager?.setBandLevel(bandIndex, levelMb)
    }

    fun applyEqualizerPreset(presetName: String) {
        PlaybackService.instance?.effectManager?.applyPreset(presetName)
    }

    fun setBassBoost(strength: Int) {
        PlaybackService.instance?.effectManager?.setBassBoost(strength)
    }

    fun toggleEqualizer(enabled: Boolean) {
        PlaybackService.instance?.effectManager?.setEnabled(enabled)
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim())
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (selectedPlaylistDetail.value?.id == playlistId) {
                selectedPlaylistDetail.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            trackForPlaylistSelection.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    private var playlistCollectJob: kotlinx.coroutines.Job? = null

    fun openPlaylistDetail(playlist: Playlist) {
        selectedPlaylistDetail.value = playlist
        playlistCollectJob?.cancel()
        playlistCollectJob = viewModelScope.launch {
            if (playlist.id == -1L) {
                repository.favoriteTracks.collect {
                    playlistDetailTracks.value = it
                }
            } else if (playlist.id == -2L) {
                repository.mostPlayedTracks.collect {
                    playlistDetailTracks.value = it
                }
            } else {
                repository.getTracksForPlaylist(playlist.id).collect {
                    playlistDetailTracks.value = it
                }
            }
        }
    }

    fun closePlaylistDetail() {
        playlistCollectJob?.cancel()
        playlistCollectJob = null
        selectedPlaylistDetail.value = null
        playlistDetailTracks.value = emptyList()
    }

    fun reorderQueue(from: Int, to: Int) = playbackConnection.reorderQueue(from, to)

    fun removeQueueItem(index: Int) = playbackConnection.removeQueueItem(index)

    fun loadFolder(path: String) {
        viewModelScope.launch {
            _currentFolderPath.value = path
            _folderItems.value = repository.listFolderContents(path)
        }
    }

    fun navigateFolderUp() {
        val current = File(_currentFolderPath.value)
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            loadFolder(parent.absolutePath)
        }
    }

    fun playAllInFolder() {
        val tracks = _folderItems.value.mapNotNull { it.track }
        if (tracks.isNotEmpty()) {
            playTracks(tracks, 0)
        }
    }

    fun saveTrackTags(track: Track, title: String, artist: String, album: String, year: Int, trackNumber: Int) {
        viewModelScope.launch {
            repository.updateTrackMetadata(track.id, title, artist, album, year, trackNumber)
            trackForTagEditing.value = null
        }
    }

    fun exportPlaylistToM3u(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = playlistDetailTracks.value
            val exportDir = File(getApplication<Application>().filesDir, "playlists").apply { mkdirs() }
            val sanitized = playlist.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(exportDir, "$sanitized.m3u")
            repository.exportToM3u(playlist.name, tracks, targetFile)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.scanMediaStore()
        }
    }

    fun resetDemoTracks() {
        viewModelScope.launch {
            repository.seedBundledDemoTracks()
        }
    }

    override fun onCleared() {
        playbackConnection.release()
        super.onCleared()
    }
}