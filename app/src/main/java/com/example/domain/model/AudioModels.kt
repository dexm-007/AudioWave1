package com.example.domain.model

data class Track(
    val id: Long,
    val uri: String,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0L,
    val durationMs: Long,
    val bitrate: Int = 320,
    val genre: String = "Unknown",
    val trackNumber: Int = 1,
    val year: Int = 2026,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lyrics: String? = null
)

enum class TrackSortOrder(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    TITLE_AZ("Title (A-Z)"),
    ARTIST("Artist"),
    DURATION("Duration"),
    MOST_PLAYED("Most Played")
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val year: Int
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

data class Playlist(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0,
    val isSystem: Boolean = false
)

enum class RepeatMode {
    OFF, ONE, ALL
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSec: Int? = null,
    val isSleepTimerEndOfTrack: Boolean = false,
    val crossfadeSec: Int = 0,
    val isDucking: Boolean = false
)

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val currentLevelMb: Short,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0 to 1000
    val loudnessGainMb: Int = 0,
    val activePreset: String = "Flat"
)

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

data class FolderItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val trackCount: Int = 0,
    val track: Track? = null
)
