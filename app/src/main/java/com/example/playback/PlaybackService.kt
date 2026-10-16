package com.example.playback

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private val TAG = "PlaybackService"
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    val effectManager = AudioEffectManager()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var sleepTimerJob: Job? = null
    private val _sleepTimerSec = MutableStateFlow<Int?>(null)
    val sleepTimerSec = _sleepTimerSec.asStateFlow()

    private val _isEndOfTrackSleepTimer = MutableStateFlow(false)
    val isEndOfTrackSleepTimer = _isEndOfTrackSleepTimer.asStateFlow()

    private var audioManager: AudioManager? = null
    private var noisyReceiverRegistered = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.volume = 0.3f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = 1.0f
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        effectManager.attachAudioSession(sessionId)
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    if (_isEndOfTrackSleepTimer.value) {
                        player.pause()
                        player.volume = 1.0f
                        _isEndOfTrackSleepTimer.value = false
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // If 'End of track' sleep timer is active, stop playback as soon as the current track completes
                if (_isEndOfTrackSleepTimer.value && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    player.pause()
                    player.volume = 1.0f
                    _isEndOfTrackSleepTimer.value = false
                }
            }
        })

        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val customCallback = object : MediaSession.Callback {
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SET_SPEED -> {
                        val speed = args.getFloat("speed", 1.0f)
                        player.playbackParameters = PlaybackParameters(speed)
                    }
                    ACTION_SET_SLEEP_TIMER -> {
                        val minutes = args.getInt("minutes", 0)
                        setSleepTimer(minutes)
                    }
                    ACTION_SET_SLEEP_TIMER_END_OF_TRACK -> {
                        setSleepTimerEndOfTrack()
                    }
                    ACTION_CANCEL_SLEEP_TIMER -> {
                        cancelSleepTimer()
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(customCallback)
            .build()

        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(noisyReceiver, filter)
            noisyReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register noisy receiver: ${e.message}")
        }

        instance = this
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _isEndOfTrackSleepTimer.value = false

        if (minutes <= 0) {
            _sleepTimerSec.value = null
            player.volume = 1.0f
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimerSec.value = totalSeconds

        sleepTimerJob = serviceScope.launch {
            var remaining = totalSeconds
            // Fade-out starts 30 seconds before timer expires (or half the time if timer is short)
            val fadeWindow = if (totalSeconds <= 30) totalSeconds else 30

            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerSec.value = remaining

                // Gradual volume fade-out effect
                if (remaining <= fadeWindow) {
                    val fadeRatio = (remaining.toFloat() / fadeWindow.toFloat()).coerceIn(0.0f, 1.0f)
                    player.volume = fadeRatio
                } else {
                    player.volume = 1.0f
                }
            }

            // Countdown finished: pause playback & restore volume baseline
            player.pause()
            player.volume = 1.0f
            _sleepTimerSec.value = null
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _sleepTimerSec.value = null
        _isEndOfTrackSleepTimer.value = true

        sleepTimerJob = serviceScope.launch {
            while (_isEndOfTrackSleepTimer.value && isActive) {
                delay(500L)
                val duration = player.duration
                val position = player.currentPosition

                if (duration > 0 && position >= 0) {
                    val remainingMs = duration - position

                    // In the final 15 seconds of the track, gradually fade out volume
                    if (remainingMs in 1L..15000L) {
                        val fadeRatio = (remainingMs / 15000f).coerceIn(0.05f, 1.0f)
                        player.volume = fadeRatio
                    } else if (remainingMs <= 300L) {
                        player.pause()
                        player.volume = 1.0f
                        _isEndOfTrackSleepTimer.value = false
                        break
                    } else {
                        player.volume = 1.0f
                    }
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSec.value = null
        _isEndOfTrackSleepTimer.value = false
        player.volume = 1.0f
    }

    override fun onDestroy() {
        instance = null
        if (noisyReceiverRegistered) {
            try {
                unregisterReceiver(noisyReceiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
        effectManager.release()
        sleepTimerJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        var instance: PlaybackService? = null
            private set

        const val ACTION_SET_SPEED = "com.example.audiowave.ACTION_SET_SPEED"
        const val ACTION_SET_SLEEP_TIMER = "com.example.audiowave.ACTION_SET_SLEEP_TIMER"
        const val ACTION_SET_SLEEP_TIMER_END_OF_TRACK = "com.example.audiowave.ACTION_SET_SLEEP_TIMER_END_OF_TRACK"
        const val ACTION_CANCEL_SLEEP_TIMER = "com.example.audiowave.ACTION_CANCEL_SLEEP_TIMER"
    }
}
