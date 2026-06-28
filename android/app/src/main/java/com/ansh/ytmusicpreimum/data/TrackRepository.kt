package com.ansh.ytmusicpreimum.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class TrackRepository(private val trackDao: TrackDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val likedTracks: Flow<List<TrackEntity>> = trackDao.getLikedTracks()
    val history: Flow<List<HistoryEntity>> = trackDao.getHistory()

    suspend fun insert(track: TrackEntity) {
        trackDao.insertTrack(track)
        syncTrackToCloud(track)
    }

    suspend fun addToHistory(track: TrackEntity) {
        val historyItem = HistoryEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl
        )
        trackDao.insertHistory(historyItem)
        syncHistoryToCloud(historyItem)
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
        val userId = auth.currentUser?.uid ?: return
        try {
            if (track.isLiked) {
                firestore.collection("users").document(userId)
                    .collection("liked_tracks").document(track.id.replace("/", "_"))
                    .set(track).await()
            } else {
                firestore.collection("users").document(userId)
                    .collection("liked_tracks").document(track.id.replace("/", "_"))
                    .delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncHistoryToCloud(history: HistoryEntity) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("history").document(history.trackId.replace("/", "_"))
                .set(history).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWithCloud() {
        val userId = auth.currentUser?.uid ?: return
        try {
            // 1. Sync Liked Tracks
            val likedSnapshot = firestore.collection("users").document(userId)
                .collection("liked_tracks").get().await()
            val cloudLiked = likedSnapshot.toObjects(TrackEntity::class.java)
            for (track in cloudLiked) {
                trackDao.insertTrack(track)
            }

            // 2. Sync History
            val historySnapshot = firestore.collection("users").document(userId)
                .collection("history").limit(50).get().await()
            val cloudHistory = historySnapshot.toObjects(HistoryEntity::class.java)
            for (item in cloudHistory) {
                trackDao.insertHistory(item)
            }

            // 3. Sync Playlists (Expanding sync logic)
            syncPlaylistsFromCloud(userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncPlaylistsFromCloud(userId: String) {
        try {
            val playlistsSnapshot = firestore.collection("users").document(userId)
                .collection("playlists").get().await()
            val cloudPlaylists = playlistsSnapshot.toObjects(PlaylistEntity::class.java)
            for (playlist in cloudPlaylists) {
                trackDao.insertPlaylist(playlist)
                // Sync tracks inside each playlist
                val tracksSnapshot = firestore.collection("users").document(userId)
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
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("playlists").document(playlist.id.toString())
            .set(playlist).await()
    }

    suspend fun syncTrackToPlaylistInCloud(playlistId: Long, trackId: String) {
        val userId = auth.currentUser?.uid ?: return
        val ref = PlaylistTrackCrossRef(playlistId, trackId)
        firestore.collection("users").document(userId)
            .collection("playlists").document(playlistId.toString())
            .collection("tracks").document(trackId.replace("/", "_"))
            .set(ref).await()
    }

    suspend fun clearLocalData() {
        trackDao.clearTracks()
        trackDao.clearPlaylists()
        trackDao.clearPlaylistCrossRefs()
        trackDao.clearHistory()
    }
}
