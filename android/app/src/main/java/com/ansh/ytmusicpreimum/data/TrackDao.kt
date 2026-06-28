package com.ansh.ytmusicpreimum.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLiked = 1")
    fun getLikedTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    // Playlist Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithTracks(playlistId: Long): Flow<PlaylistWithTracks>

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    // History Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM tracks")
    suspend fun clearTracks()

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("DELETE FROM playlist_track_cross_ref")
    suspend fun clearPlaylistCrossRefs()
}
