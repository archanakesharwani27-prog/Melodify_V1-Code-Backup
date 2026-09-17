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

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedDate DESC")
    fun getRecentlyAdded(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayed > 0 ORDER BY lastPlayed DESC")
    fun getRecentlyPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getTopTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' LIMIT 25")
    fun searchLocalTracks(query: String): Flow<List<TrackEntity>>

    // Playlist Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Update
    suspend fun updatePlaylistTrackCrossRef(crossRef: PlaylistTrackCrossRef)

    @Transaction
    @Query("SELECT * FROM tracks INNER JOIN playlist_track_cross_ref ON tracks.id = playlist_track_cross_ref.trackId WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: Long): Int

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

    // Search History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("SELECT `query` FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<String>>

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM tracks")
    suspend fun clearTracks()

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    // Queue Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: List<QueueEntity>)

    @Query("SELECT * FROM queue ORDER BY position ASC")
    fun getQueue(): Flow<List<QueueEntity>>

    @Query("DELETE FROM queue")
    suspend fun clearQueue()

    @Query("DELETE FROM playlist_track_cross_ref")
    suspend fun clearPlaylistCrossRefs()

    // Stream Cache Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCache(cache: StreamCacheEntity)

    @Query("SELECT * FROM stream_cache WHERE videoId = :videoId AND quality = :quality LIMIT 1")
    suspend fun getStreamCache(videoId: String, quality: String): StreamCacheEntity?

    @Query("DELETE FROM stream_cache WHERE expiryTime < :currentTime")
    suspend fun clearExpiredCache(currentTime: Long)

    @Query("DELETE FROM stream_cache WHERE videoId = :videoId")
    suspend fun deleteStreamCache(videoId: String)

    // Lyrics Cache Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("SELECT * FROM lyrics_cache WHERE videoId = :videoId LIMIT 1")
    suspend fun getLyrics(videoId: String): LyricsEntity?

    @Query("DELETE FROM lyrics_cache WHERE expiryTime < :currentTime")
    suspend fun clearExpiredLyrics(currentTime: Long)

    // Equalizer Presets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity)

    @Query("SELECT * FROM equalizer_presets")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    @Query("SELECT * FROM equalizer_presets WHERE name = :name LIMIT 1")
    suspend fun getPresetByName(name: String): EqualizerPresetEntity?
}
