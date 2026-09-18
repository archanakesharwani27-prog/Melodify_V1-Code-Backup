package com.ansh.ytmusicpreimum.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class TrackRepository(private val trackDao: TrackDao) {
    companion object {
        var currentSyncUserId: String? = null
    }

    private val firestore: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (t: Throwable) { null }
    private val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (t: Throwable) { null }

    private fun getActiveUserId(): String? {
        val a = auth ?: return currentSyncUserId
        return a.currentUser?.uid 
            ?: a.currentUser?.email?.replace(".", "_")
            ?: currentSyncUserId
    }

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val likedTracks: Flow<List<TrackEntity>> = trackDao.getLikedTracks()
    val history: Flow<List<HistoryEntity>> = trackDao.getHistory()
    val recentSearches: Flow<List<String>> = trackDao.getRecentSearches()

    fun searchLocalTracks(query: String): Flow<List<TrackEntity>> = trackDao.searchLocalTracks(query)

    suspend fun getPlaylistTrackCount(playlistId: Long): Int = trackDao.getPlaylistTrackCount(playlistId)

    suspend fun insert(track: TrackEntity) {
        trackDao.insertTrack(track)
        syncTrackToCloud(track)
    }

    suspend fun addToHistory(track: TrackEntity) {
        val historyItem = HistoryEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            timestamp = System.currentTimeMillis() // Update timestamp to bring to top
        )
        trackDao.insertHistory(historyItem)
        syncHistoryToCloud(historyItem)
    }

    suspend fun enrichTracks(tracks: List<TrackEntity>): List<TrackEntity> {
        return tracks.map { track ->
            val dbTrack = trackDao.getTrackById(track.id)
            if (dbTrack != null) {
                track.copy(
                    isDownloaded = dbTrack.isDownloaded,
                    isLiked = dbTrack.isLiked
                )
            } else {
                track
            }
        }
    }

    suspend fun addToSearchHistory(query: String) {
        if (query.isBlank()) return
        val item = SearchHistoryEntity(query = query, timestamp = System.currentTimeMillis())
        trackDao.insertSearch(item)
        syncSearchHistoryToCloud(item)
    }

    suspend fun deleteSearch(query: String) {
        trackDao.deleteSearch(query)
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            fs.collection("users").document(userId)
                .collection("search_history").document(query.replace("/", "_"))
                .delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearSearchHistory() {
        trackDao.clearSearchHistory()
        // Optionally clear cloud too, but usually we just keep it or clear on demand
    }

    suspend fun toggleLike(trackId: String, title: String, artist: String, thumbnailUrl: String) {
        val existing = trackDao.getTrackById(trackId)
        val updatedTrack = if (existing != null) {
            existing.copy(isLiked = !existing.isLiked)
        } else {
            TrackEntity(trackId, title, artist, thumbnailUrl, isLiked = true)
        }
        
        trackDao.insertTrack(updatedTrack)
        syncTrackToCloud(updatedTrack)
    }

    private suspend fun syncTrackToCloud(track: TrackEntity) {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            if (track.isLiked) {
                fs.collection("users").document(userId)
                    .collection("liked_tracks").document(track.id.replace("/", "_"))
                    .set(track).await()
            } else {
                fs.collection("users").document(userId)
                    .collection("liked_tracks").document(track.id.replace("/", "_"))
                    .delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncHistoryToCloud(history: HistoryEntity) {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            fs.collection("users").document(userId)
                .collection("history").document(history.trackId.replace("/", "_"))
                .set(history).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncSearchHistoryToCloud(search: SearchHistoryEntity) {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            fs.collection("users").document(userId)
                .collection("search_history").document(search.query.replace("/", "_"))
                .set(search).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWithCloud() {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            // 1. Sync Liked Tracks
            val likedSnapshot = fs.collection("users").document(userId)
                .collection("liked_tracks").get().await()
            val cloudLiked = likedSnapshot.toObjects(TrackEntity::class.java)
            for (track in cloudLiked) {
                trackDao.insertTrack(track)
            }

            // 2. Sync History
            val historySnapshot = fs.collection("users").document(userId)
                .collection("history").get().await()
            val cloudHistory = historySnapshot.toObjects(HistoryEntity::class.java)
            for (item in cloudHistory) {
                trackDao.insertHistory(item)
            }

            // 3. Sync Search History
            val searchSnapshot = fs.collection("users").document(userId)
                .collection("search_history").get().await()
            val cloudSearch = searchSnapshot.toObjects(SearchHistoryEntity::class.java)
            for (item in cloudSearch) {
                trackDao.insertSearch(item)
            }

            // 4. Sync Playlists
            syncPlaylistsFromCloud(userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncPlaylistsFromCloud(userId: String) {
        val fs = firestore ?: return
        try {
            val playlistsSnapshot = fs.collection("users").document(userId)
                .collection("playlists").get().await()
            val cloudPlaylists = playlistsSnapshot.toObjects(PlaylistEntity::class.java)
            for (playlist in cloudPlaylists) {
                trackDao.insertPlaylist(playlist)
                // Sync tracks inside each playlist
                val tracksSnapshot = fs.collection("users").document(userId)
                    .collection("playlists").document(playlist.id.toString())
                    .collection("tracks").get().await()
                val cloudRefs = tracksSnapshot.toObjects(PlaylistTrackCrossRef::class.java)
                for (ref in cloudRefs) {
                    trackDao.insertTrackToPlaylist(ref)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncPlaylistToCloud(playlist: PlaylistEntity) {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            fs.collection("users").document(userId)
                .collection("playlists").document(playlist.id.toString())
                .set(playlist).await()
        } catch (e: Exception) {
            android.util.Log.e("TrackRepository", "Error syncing playlist to cloud", e)
        }
    }

    suspend fun syncTrackToPlaylistInCloud(playlistId: Long, trackId: String) {
        val userId = getActiveUserId() ?: return
        val fs = firestore ?: return
        try {
            val ref = PlaylistTrackCrossRef(playlistId, trackId)
            fs.collection("users").document(userId)
                .collection("playlists").document(playlistId.toString())
                .collection("tracks").document(trackId.replace("/", "_"))
                .set(ref).await()
        } catch (e: Exception) {
            android.util.Log.e("TrackRepository", "Error syncing playlist track to cloud", e)
        }
    }

    suspend fun clearLocalData() {
        trackDao.clearTracks()
        trackDao.clearPlaylists()
        trackDao.clearPlaylistCrossRefs()
        trackDao.clearHistory()
    }
}
