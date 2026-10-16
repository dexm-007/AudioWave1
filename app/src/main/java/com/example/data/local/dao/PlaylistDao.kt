package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistTrackCrossRef
import com.example.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracksToPlaylist(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.orderIndex ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCountForPlaylist(playlistId: Long): Int

    @Query("UPDATE playlists SET trackCount = :count WHERE id = :playlistId")
    suspend fun updatePlaylistTrackCount(playlistId: Long, count: Int)

    @Transaction
    suspend fun addTrackAndUpdateCount(playlistId: Long, trackId: Long, orderIndex: Int) {
        insertTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, orderIndex))
        val count = getTrackCountForPlaylist(playlistId)
        updatePlaylistTrackCount(playlistId, count)
    }

    @Transaction
    suspend fun addTracksAndUpdateCount(playlistId: Long, trackIds: List<Long>) {
        val currentCount = getTrackCountForPlaylist(playlistId)
        val crossRefs = trackIds.mapIndexed { idx, tId ->
            PlaylistTrackCrossRef(playlistId, tId, currentCount + idx)
        }
        insertTracksToPlaylist(crossRefs)
        val newCount = getTrackCountForPlaylist(playlistId)
        updatePlaylistTrackCount(playlistId, newCount)
    }

    @Transaction
    suspend fun removeTrackAndUpdateCount(playlistId: Long, trackId: Long) {
        removeTrackFromPlaylist(playlistId, trackId)
        val count = getTrackCountForPlaylist(playlistId)
        updatePlaylistTrackCount(playlistId, count)
    }
}
