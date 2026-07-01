package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.PlaylistEntity
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.data.TrackRepository
import com.ansh.ytmusicpreimum.data.PlaylistTrackCrossRef
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    val repository: TrackRepository
    val likedTracks: StateFlow<List<TrackEntity>>
    val downloadedTracks: StateFlow<List<TrackEntity>>
    val playlists: StateFlow<List<PlaylistEntity>>
    val playbackHistory: StateFlow<List<com.ansh.ytmusicpreimum.data.HistoryEntity>>
    
    val recentlyAdded: StateFlow<List<TrackEntity>>
    val recentlyPlayed: StateFlow<List<TrackEntity>>
    val topTracks: StateFlow<List<TrackEntity>>

    private val _localTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val localTracks: StateFlow<List<TrackEntity>> = _localTracks.asStateFlow()

    private val trackDao = AppDatabase.getDatabase(application).trackDao()
    private val auth = FirebaseAuth.getInstance()

    init {
        repository = TrackRepository(trackDao)
        likedTracks = repository.likedTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        downloadedTracks = trackDao.getDownloadedTracks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        playlists = trackDao.getAllPlaylists().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        playbackHistory = repository.history.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recentlyAdded = trackDao.getRecentlyAdded().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        recentlyPlayed = trackDao.getRecentlyPlayedTracks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        topTracks = trackDao.getTopTracks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        syncWithCloud()
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun loadLocalTracks() {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) {
                val list = mutableListOf<TrackEntity>()
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID)
                getApplication<Application>().contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val albumArtUri = ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albumIdCol))
                        list.add(TrackEntity(id = contentUri.toString(), title = cursor.getString(titleCol), artist = cursor.getString(artistCol), thumbnailUrl = albumArtUri.toString()))
                    }
                }
                list
            }
            _localTracks.value = tracks
        }
    }

    fun toggleLike(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleLike(track.id, track.title, track.artist, track.thumbnailUrl)
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            val playlist = PlaylistEntity(name = name, description = description)
            val id = trackDao.insertPlaylist(playlist)
            repository.syncPlaylistToCloud(playlist.copy(id = id))
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: TrackEntity) {
        viewModelScope.launch {
            trackDao.insertTrack(track)
            // Get current tracks to determine position
            val currentTracks = withContext(Dispatchers.IO) {
                // We need a way to get the count or the next position
                // For now, just add at the end
                0 // placeholder, should ideally be currentTracks.size
            }
            trackDao.insertTrackToPlaylist(PlaylistTrackCrossRef(playlistId, track.id, position = 0)) // simplistic
            repository.syncTrackToPlaylistInCloud(playlistId, track.id)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            trackDao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        viewModelScope.launch {
            val updated = playlist.copy(name = newName)
            trackDao.insertPlaylist(updated)
            repository.syncPlaylistToCloud(updated)
        }
    }

    fun reorderPlaylist(playlistId: Long, trackId: String, newPosition: Int) {
        viewModelScope.launch {
            trackDao.updatePlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId, newPosition))
        }
    }

    fun recordPlayback(trackId: String) {
        viewModelScope.launch {
            trackDao.incrementPlayCount(trackId)
        }
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> {
        return trackDao.getTracksForPlaylist(playlistId)
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            trackDao.deletePlaylist(playlist)
        }
    }
}
