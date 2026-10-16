package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import com.example.domain.model.EqualizerBand
import com.example.domain.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEffectManager {
    private val TAG = "AudioEffectManager"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // Predefined preset mappings in millibels (-1500 to +1500)
    private val presetMap = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Rock" to listOf(500, 300, -100, 400, 600),
        "Pop" to listOf(-100, 200, 500, 300, -200),
        "Jazz" to listOf(400, 200, -200, 200, 500),
        "Electronic" to listOf(600, 400, 0, 200, 500),
        "Vocal" to listOf(-300, 100, 600, 300, -200),
        "Bass Heavy" to listOf(800, 500, 100, -100, -300)
    )

    fun attachAudioSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        currentSessionId = sessionId
        release()

        try {
            val eq = Equalizer(0, sessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            equalizer = eq

            val bands = mutableListOf<EqualizerBand>()
            val numBands = eq.numberOfBands
            val minRange = eq.bandLevelRange[0]
            val maxRange = eq.bandLevelRange[1]

            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000 // Convert mHz to Hz
                val currentLevel = eq.getBandLevel(bandIndex)
                bands.add(
                    EqualizerBand(
                        bandIndex = bandIndex,
                        centerFreqHz = centerFreq,
                        currentLevelMb = currentLevel,
                        minLevelMb = minRange,
                        maxLevelMb = maxRange
                    )
                )
            }

            // Bass Boost
            val bb = BassBoost(0, sessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb

            // Loudness Enhancer (API 19+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    setTargetGain(_equalizerState.value.loudnessGainMb)
                }
            }

            _equalizerState.value = _equalizerState.value.copy(bands = bands)
        } catch (e: Exception) {
            Log.w(TAG, "Audio effect initialization fallback: ${e.message}")
            // Provide fallback simulated bands if hardware effect initialization fails in emulator
            if (_equalizerState.value.bands.isEmpty()) {
                val fallbackBands = listOf(
                    EqualizerBand(0, 60, 0),
                    EqualizerBand(1, 230, 0),
                    EqualizerBand(2, 910, 0),
                    EqualizerBand(3, 3600, 0),
                    EqualizerBand(4, 14000, 0)
                )
                _equalizerState.value = _equalizerState.value.copy(bands = fallbackBands)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle audio effects: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMb)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set band level: ${e.message}")
        }

        val updatedBands = _equalizerState.value.bands.map {
            if (it.bandIndex == bandIndex) it.copy(currentLevelMb = levelMb) else it
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            activePreset = "Custom"
        )
    }

    fun applyPreset(presetName: String) {
        val levels = presetMap[presetName] ?: return
        val currentBands = _equalizerState.value.bands

        val updatedBands = currentBands.mapIndexed { index, band ->
            val targetLevel = (levels.getOrNull(index) ?: 0).toShort()
            try {
                equalizer?.setBandLevel(band.bandIndex, targetLevel)
            } catch (e: Exception) {
                // Ignore if in mock or emulator
            }
            band.copy(currentLevelMb = targetLevel)
        }

        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            activePreset = presetName
        )
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = clamped)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set bass boost: ${e.message}")
        }
    }

    fun setLoudnessGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(loudnessGainMb = clamped)
        try {
            loudnessEnhancer?.setTargetGain(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set loudness gain: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            equalizer = null
            bassBoost = null
            loudnessEnhancer = null
        }
    }
}
