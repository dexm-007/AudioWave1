package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.TrackEntity
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.FolderItem
import com.example.domain.model.LrcLine
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.domain.model.TrackSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()

    val allTracks: Flow<List<Track>> = trackDao.getAllTracks().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    val mostPlayedTracks: Flow<List<Track>> = trackDao.getMostPlayedTracks().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    val allAlbums: Flow<List<Album>> = allTracks.map { tracks ->
        tracks.groupBy { it.album }
            .map { (albumTitle, trackList) ->
                val first = trackList.first()
                Album(
                    id = first.albumId,
                    title = albumTitle,
                    artist = first.artist,
                    trackCount = trackList.size,
                    year = first.year
                )
            }.sortedBy { it.title.lowercase() }
    }.flowOn(Dispatchers.IO)

    val allArtists: Flow<List<Artist>> = allTracks.map { tracks ->
        tracks.groupBy { it.artist }
            .map { (artistName, trackList) ->
                val albumCount = trackList.map { it.album }.distinct().size
                Artist(
                    name = artistName,
                    trackCount = trackList.size,
                    albumCount = albumCount
                )
            }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.IO)

    fun getSortedTracks(order: TrackSortOrder): Flow<List<Track>> {
        val flow = when (order) {
            TrackSortOrder.RECENTLY_ADDED -> trackDao.getTracksSortedByDate()
            TrackSortOrder.TITLE_AZ -> trackDao.getTracksSortedByTitle()
            TrackSortOrder.ARTIST -> trackDao.getTracksSortedByArtist()
            TrackSortOrder.DURATION -> trackDao.getTracksSortedByDuration()
            TrackSortOrder.MOST_PLAYED -> trackDao.getTracksSortedByPlayCount()
        }
        return flow.map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> =
        playlistDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)

    suspend fun getTrackById(id: Long): Track? = withContext(Dispatchers.IO) {
        trackDao.getTrackById(id)?.toDomain()
    }

    suspend fun initializeLibraryIfEmpty() = withContext(Dispatchers.IO) {
        val count = trackDao.getTrackCount()
        if (count == 0) {
            seedBundledDemoTracks()
            scanMediaStore()
        }
    }

    suspend fun seedBundledDemoTracks() = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo_tracks").apply { mkdirs() }

        val demoSeeds = listOf(
            Triple("neon_horizon", "Neon Horizon", "Audiowave Synthwave"),
            Triple("midnight_breeze", "Midnight Breeze", "Audiowave Lo-Fi"),
            Triple("cyber_pulse", "Cyber Pulse", "Audiowave Electro")
        )

        val entities = mutableListOf<TrackEntity>()

        demoSeeds.forEachIndexed { index, (filePrefix, title, artist) ->
            val wavFileName = "$filePrefix.wav"
            val lrcFileName = "$filePrefix.lrc"
            val targetWav = File(demoDir, wavFileName)

            try {
                if (!targetWav.exists()) {
                    context.assets.open("demo_tracks/$wavFileName").use { input ->
                        targetWav.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var lyricsContent: String? = null
            try {
                context.assets.open("demo_tracks/$lrcFileName").use { input ->
                    lyricsContent = input.bufferedReader().readText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val durationMs = when (filePrefix) {
                "neon_horizon" -> 28000L
                "midnight_breeze" -> 32000L
                else -> 24000L
            }

            val entity = TrackEntity(
                id = 1000L + index,
                uri = Uri.fromFile(targetWav).toString(),
                path = targetWav.absolutePath,
                title = title,
                artist = artist,
                album = "Audiowave Demo Sessions",
                albumId = 500L,
                durationMs = durationMs,
                bitrate = 1411,
                genre = if (filePrefix == "neon_horizon") "Synthwave" else if (filePrefix == "midnight_breeze") "Lo-Fi" else "Electronic",
                trackNumber = index + 1,
                year = 2026,
                isFavorite = (index == 0),
                playCount = when (index) {
                    0 -> 14
                    1 -> 9
                    else -> 4
                },
                dateAdded = System.currentTimeMillis() - (index * 86400000L),
                lyrics = lyricsContent
            )
            entities.add(entity)
        }

        trackDao.insertTracks(entities)

        // Seed a default playlist
        val defaultPlaylistId = playlistDao.insertPlaylist(
            PlaylistEntity(name = "Audiowave Favorites", trackCount = entities.size)
        )
        entities.forEachIndexed { idx, t ->
            playlistDao.insertTrackToPlaylist(
                com.example.data.local.entity.PlaylistTrackCrossRef(defaultPlaylistId, t.id, idx)
            )
        }
    }

    suspend fun scanMediaStore() = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val trackNum = cursor.getInt(trackCol)
                    val year = cursor.getInt(yearCol)
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val dateAddedSec = cursor.getLong(dateAddedCol)

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    if (duration > 5000) {
                        tracks.add(
                            TrackEntity(
                                id = id,
                                uri = contentUri.toString(),
                                path = dataPath,
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = albumId,
                                durationMs = duration,
                                bitrate = 320,
                                genre = "Audio",
                                trackNumber = trackNum,
                                year = if (year > 0) year else 2026,
                                isFavorite = false,
                                playCount = 0,
                                dateAdded = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            if (tracks.isNotEmpty()) {
                trackDao.insertTracks(tracks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleFavorite(trackId: Long, current: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(trackId, !current)
    }

    suspend fun incrementPlayCount(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun deleteTracks(ids: List<Long>) = withContext(Dispatchers.IO) {
        trackDao.deleteTracksByIds(ids)
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) = withContext(Dispatchers.IO) {
        playlistDao.addTracksAndUpdateCount(playlistId, trackIds)
    }

    suspend fun updateTrackMetadata(
        trackId: Long,
        title: String,
        artist: String,
        album: String,
        year: Int,
        trackNumber: Int
    ) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(trackId) ?: return@withContext
        val updated = existing.copy(
            title = title.trim(),
            artist = artist.trim(),
            album = album.trim(),
            year = year,
            trackNumber = trackNumber
        )
        trackDao.updateTrack(updated)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val count = playlistDao.getTrackCountForPlaylist(playlistId)
        playlistDao.addTrackAndUpdateCount(playlistId, trackId, count)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackAndUpdateCount(playlistId, trackId)
    }

    fun parseLrc(lrcText: String?): List<LrcLine> {
        if (lrcText.isNullOrBlank()) return emptyList()
        val lines = mutableListOf<LrcLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")
        lrcText.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3]
                val ms = when (msPart.length) {
                    1 -> msPart.toLong() * 100
                    2 -> msPart.toLong() * 10
                    3 -> msPart.toLong()
                    else -> 0L
                }
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LrcLine(totalMs, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    fun exportToM3u(playlistName: String, tracks: List<Track>, outputFile: File): Boolean {
        return try {
            outputFile.bufferedWriter().use { writer ->
                writer.write("#EXTM3U\n")
                tracks.forEach { track ->
                    writer.write("#EXTINF:${track.durationMs / 1000},${track.artist} - ${track.title}\n")
                    writer.write("${track.path}\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun listFolderContents(directoryPath: String): List<FolderItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val items = mutableListOf<FolderItem>()
        val files = dir.listFiles() ?: return@withContext emptyList()

        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                val subCount = file.listFiles { f ->
                    f.isDirectory || isAudioFile(f.name)
                }?.size ?: 0
                items.add(
                    FolderItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = true,
                        trackCount = subCount
                    )
                )
            } else if (isAudioFile(file.name)) {
                items.add(
                    FolderItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        track = Track(
                            id = file.absolutePath.hashCode().toLong(),
                            uri = Uri.fromFile(file).toString(),
                            path = file.absolutePath,
                            title = file.nameWithoutExtension,
                            artist = "Local File",
                            album = dir.name,
                            durationMs = 0L,
                            genre = "Folder Audio"
                        )
                    )
                )
            }
        }

        items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                lower.endsWith(".flac") || lower.endsWith(".m4a") ||
                lower.endsWith(".ogg") || lower.endsWith(".aac")
    }

    private fun TrackEntity.toDomain() = Track(
        id = id,
        uri = uri,
        path = path,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        bitrate = bitrate,
        genre = genre,
        trackNumber = trackNumber,
        year = year,
        isFavorite = isFavorite,
        playCount = playCount,
        dateAdded = dateAdded,
        lyrics = lyrics
    )

    private fun PlaylistEntity.toDomain() = Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        trackCount = trackCount,
        isSystem = isSystem
    )
}
