package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.ansh.ytmusicpreimum.MusicService
import com.ansh.ytmusicpreimum.YouTubeExtractor
import com.ansh.ytmusicpreimum.LyricsManager
import com.ansh.ytmusicpreimum.LyricsState
import com.ansh.ytmusicpreimum.MelodifyDownloadManager
import com.ansh.ytmusicpreimum.MelodifyQueueManager
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import androidx.media3.common.util.UnstableApi

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val trackDao = database.trackDao()
    private val repository = com.ansh.ytmusicpreimum.data.TrackRepository(trackDao)

    private val downloadManager = MelodifyDownloadManager.getInstance(application)
    val downloadStates = downloadManager.downloadStates
    val queuedTracks = downloadManager.queuedTracks
    
    private val queueManager = MelodifyQueueManager()
    val currentQueue = queueManager.queue
    val currentIndex = queueManager.currentIndex
    
    private var isHistoryRecorded = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var _controller: MediaController? = null
    private val controller: MediaController? get() = _controller

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    private var isPreparingManually = false
    private val urlCache = mutableMapOf<String, String>()
    private var prefetchJob: Job? = null
    private var predictivePrefetchJobs = mutableListOf<Job>()

    fun prefetchTracks(tracks: List<TrackEntity>) {
        predictivePrefetchJobs.forEach { it.cancel() }
        predictivePrefetchJobs.clear()

        tracks.take(2).forEach { track ->
            val job = viewModelScope.launch {
                YouTubeExtractor.getAudioUrl(track.id)?.let { url ->
                    urlCache[track.id] = url
                }
            }
            predictivePrefetchJobs.add(job)
        }
    }

    fun cancelPredictivePrefetch() {
        predictivePrefetchJobs.forEach { it.cancel() }
        predictivePrefetchJobs.clear()
    }

    private var lastManualPlayTime = 0L

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState())
    val lyrics: StateFlow<LyricsState> = _lyrics

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics

    private val sharedPrefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _audioQuality = MutableStateFlow(sharedPrefs.getString("audio_quality", "Medium") ?: "Medium")
    val audioQuality: StateFlow<String> = _audioQuality

    private val _crossfadeDuration = MutableStateFlow(sharedPrefs.getLong("crossfade_duration", 0L))
    val crossfadeDuration: StateFlow<Long> = _crossfadeDuration

    init {
        restorePlaybackState()
        startProgressUpdate()
        
        viewModelScope.launch {
            currentQueue.collect {
                saveQueueState()
            }
        }
        viewModelScope.launch {
            currentIndex.collect {
                saveQueueState()
            }
        }
    }

    private fun saveQueueState() {
        val statePrefs = getApplication<Application>().getSharedPreferences("playback_state", Context.MODE_PRIVATE)
        statePrefs.edit().apply {
            putString("queue_json", queueManager.serialize())
            putInt("queue_index", queueManager.getCurrentIndex())
            apply()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            trackDao.clearQueue()
            val queueEntities = queueManager.queue.value.mapIndexed { index, track ->
                com.ansh.ytmusicpreimum.data.QueueEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    thumbnailUrl = track.thumbnailUrl,
                    position = index
                )
            }
            trackDao.insertQueue(queueEntities)
        }
    }

    fun resetPlayer() {
        _controller?.let {
            it.stop()
            it.clearMediaItems()
        }
        _currentMediaItem.value = null
        _isPlaying.value = false
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        sharedPrefs.edit().putString("audio_quality", quality).apply()
        urlCache.clear() 
    }

    fun setCrossfadeDuration(duration: Long) {
        _crossfadeDuration.value = duration
        sharedPrefs.edit().putLong("crossfade_duration", duration).apply()
    }

    private fun restorePlaybackState() {
        val statePrefs = getApplication<Application>().getSharedPreferences("playback_state", Context.MODE_PRIVATE)
        val lastMediaId = statePrefs.getString("last_media_id", null) ?: return
        
        val title = statePrefs.getString("last_title", "Unknown") ?: "Unknown"
        val artist = statePrefs.getString("last_artist", "Unknown") ?: "Unknown"
        val artwork = statePrefs.getString("last_artwork", "") ?: ""
        val position = statePrefs.getLong("last_position", 0L)
        val shuffle = statePrefs.getBoolean("shuffle_enabled", false)
        val repeat = statePrefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)
        val queueJson = statePrefs.getString("queue_json", null)
        val queueIndex = statePrefs.getInt("queue_index", -1)

        queueJson?.let {
            queueManager.deserialize(it, queueIndex)
        }

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(android.net.Uri.parse(artwork))
            .build()
        
        val restoredItem = MediaItem.Builder()
            .setMediaId(lastMediaId)
            .setMediaMetadata(metadata)
            .build()
        
        _currentMediaItem.value = restoredItem
        _currentPosition.value = position
        _shuffleModeEnabled.value = shuffle
        _repeatMode.value = repeat
    }

    fun initController(context: Context) {
        if (controllerFuture != null) return
        
        val sessionToken = SessionToken(context.applicationContext, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                if (controller != null) {
                    _controller = controller
                    _volume.value = controller.volume
                    
                    if (controller.currentMediaItem != null) {
                        _currentMediaItem.value = controller.currentMediaItem
                        _isPlaying.value = controller.isPlaying
                        _duration.value = controller.duration.coerceAtLeast(0L)
                        _currentPosition.value = controller.currentPosition
                    }

                    _isBuffering.value = controller.playbackState == Player.STATE_BUFFERING
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode
                    
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) isPreparingManually = false
                            val now = System.currentTimeMillis()
                            if (now - lastManualPlayTime > 2000 || isPlaying) {
                                _isPlaying.value = isPlaying
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _playbackError.value = null
                            val now = System.currentTimeMillis()
                            if (now - lastManualPlayTime > 2000) {
                                if (mediaItem != null) {
                                    _currentMediaItem.value = mediaItem
                                }
                            }
                            _duration.value = controller.duration.coerceAtLeast(0L)
                            
                            mediaItem?.let { item ->
                                val queue = queueManager.queue.value
                                val indexInQueue = queue.indexOfFirst { it.id == item.mediaId }
                                if (indexInQueue != -1) {
                                    if (indexInQueue != queueManager.getCurrentIndex()) {
                                        queueManager.setCurrentIndex(indexInQueue)
                                        updateControllerQueue(controller, indexInQueue)
                                    }
                                    val track = queue[indexInQueue]
                                    fetchLyrics(track.id, track.title, track.artist)
                                }
                            }
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY) {
                                isPreparingManually = false
                            }
                            _isBuffering.value = playbackState == Player.STATE_BUFFERING
                            if (playbackState == Player.STATE_READY) {
                                _duration.value = controller.duration.coerceAtLeast(0L)
                            }
                            if (playbackState == Player.STATE_ENDED) {
                                skipNext()
                            }
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            _playbackError.value = "Playback Error: ${error.errorCodeName}"
                            _isBuffering.value = false
                            _isPlaying.value = false
                        }
                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                            _shuffleModeEnabled.value = shuffleModeEnabled
                        }
                        override fun onRepeatModeChanged(repeatMode: Int) {
                            _repeatMode.value = repeatMode
                        }
                        override fun onVolumeChanged(volume: Float) {
                            _volume.value = volume
                        }
                    })
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Failed to get MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                _controller?.let {
                    val now = System.currentTimeMillis()
                    if (!isPreparingManually && (now - lastManualPlayTime > 2000)) {
                        _isPlaying.value = it.isPlaying
                        _isBuffering.value = it.playbackState == Player.STATE_BUFFERING
                        
                        it.currentMediaItem?.let { controllerItem ->
                            if (_currentMediaItem.value?.mediaId != controllerItem.mediaId) {
                                _currentMediaItem.value = controllerItem
                            }
                        }
                    }
                    if (it.duration > 0) _duration.value = it.duration
                    _currentPosition.value = it.currentPosition
                }
                delay(500)
            }
        }
    }

    fun playSong(videoId: String, title: String, artist: String, artworkUrl: String, otherTracks: List<TrackEntity> = emptyList()) {
        val tapTime = System.currentTimeMillis()
        lastManualPlayTime = tapTime
        isPreparingManually = true
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.addToHistory(TrackEntity(id = videoId, title = title, artist = artist, thumbnailUrl = artworkUrl))
            trackDao.incrementPlayCount(videoId)
        }

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(android.net.Uri.parse(artworkUrl))
            .setExtras(Bundle().apply { putString("quality", _audioQuality.value) })
            .build()
        
        val loadingMediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .build()
        
        _currentMediaItem.value = loadingMediaItem
        _isPlaying.value = true
        _isBuffering.value = true
        _lyrics.value = LyricsState()
        _playbackError.value = null
        isHistoryRecorded = false

        viewModelScope.launch {
            if (otherTracks.isNotEmpty()) {
                queueManager.setQueue(otherTracks, videoId)
            }

            launch { fetchLyrics(videoId, title, artist) }

            try {
                var retryCount = 0
                while (_controller == null && retryCount < 15) {
                    delay(50) 
                    retryCount++
                }

                val currentController = _controller ?: return@launch

                var urlResult: String? = null
                val isLocal = videoId.startsWith("content://") || videoId.startsWith("file://")
                
                if (isLocal) {
                    urlResult = videoId
                } else {
                    val dbTrack = withContext(Dispatchers.IO) {
                        trackDao.getTrackById(videoId)
                    }
                    if (dbTrack?.isDownloaded == true && dbTrack.localFilePath != null) {
                        val file = java.io.File(dbTrack.localFilePath)
                        if (file.exists()) urlResult = dbTrack.localFilePath
                    }
                }
                
                val mainItemBuilder = MediaItem.Builder()
                    .setMediaId(videoId)
                    .setMediaMetadata(metadata)
                
                if (urlResult != null) {
                    mainItemBuilder.setUri(urlResult)
                } else {
                    urlCache[videoId]?.let {
                        mainItemBuilder.setUri(it)
                    }
                }
                
                val mainItem = mainItemBuilder.build()
                
                if (currentController.currentMediaItem?.mediaId == videoId && currentController.playbackState != Player.STATE_IDLE) {
                    currentController.play()
                } else {
                    currentController.setMediaItem(mainItem)
                    currentController.prepare()
                    currentController.play()
                }
                
                if (!isLocal) prefetchNext()
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error in playSong", e)
                _isBuffering.value = false
                _isPlaying.value = false
                isPreparingManually = false
            }
        }
    }

    private fun prefetchNext() {
        prefetchJob?.cancel()
        val queue = queueManager.queue.value
        val currentIndex = queueManager.getCurrentIndex()
        if (queue.isNotEmpty()) {
            prefetchJob = viewModelScope.launch {
                delay(1500)
                val nextIndex = currentIndex + 1
                if (nextIndex < queue.size) {
                    val track = queue[nextIndex]
                    if (!urlCache.containsKey(track.id)) {
                        YouTubeExtractor.getAudioUrl(track.id)?.let { url ->
                            urlCache[track.id] = url
                            _controller?.let { controller ->
                                if (controller.mediaItemCount == 1) {
                                    val nextItem = MediaItem.Builder()
                                        .setMediaId(track.id)
                                        .setUri(url)
                                        .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                                            .setTitle(track.title)
                                            .setArtist(track.artist)
                                            .setArtworkUri(android.net.Uri.parse(track.thumbnailUrl))
                                            .setExtras(Bundle().apply { putString("quality", _audioQuality.value) })
                                            .build())
                                        .build()
                                    controller.addMediaItem(nextItem)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    fun togglePlayPause() {
        _controller?.let {
            lastManualPlayTime = System.currentTimeMillis()
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                if (it.mediaItemCount == 0 && _currentMediaItem.value != null) {
                    val restoredItem = _currentMediaItem.value!!
                    val position = _currentPosition.value
                    playSong(
                        videoId = restoredItem.mediaId,
                        title = restoredItem.mediaMetadata.title?.toString() ?: "Unknown",
                        artist = restoredItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        artworkUrl = restoredItem.mediaMetadata.artworkUri?.toString() ?: ""
                    )
                    viewModelScope.launch {
                        delay(500)
                        seekTo(position)
                    }
                } else {
                    it.play()
                    _isPlaying.value = true
                }
            }
        }
    }

    fun setVolume(volume: Float) {
        _controller?.let {
            it.volume = volume
            _volume.value = volume
        }
    }

    fun seekTo(position: Long) {
        _controller?.let { it.seekTo(position) }
    }

    fun skipNext() {
        val controller = _controller ?: return
        val nextTrack = queueManager.getNextTrack()
        
        if (nextTrack != null) {
            val nextIndex = controller.nextMediaItemIndex
            if (nextIndex != C.INDEX_UNSET) {
                val nextItem = controller.getMediaItemAt(nextIndex)
                if (nextItem.mediaId == nextTrack.id) {
                    controller.seekToNext()
                    return
                }
            }
            playSong(nextTrack.id, nextTrack.title, nextTrack.artist, nextTrack.thumbnailUrl)
        } else {
             controller.seekToNext()
        }
    }

    fun skipPrevious() {
        val controller = _controller ?: return
        val prevTrack = queueManager.getPreviousTrack()
        
        if (prevTrack != null) {
            val prevIndex = controller.previousMediaItemIndex
            if (prevIndex != C.INDEX_UNSET) {
                val prevItem = controller.getMediaItemAt(prevIndex)
                if (prevItem.mediaId == prevTrack.id) {
                    controller.seekToPrevious()
                    return
                }
            }
            playSong(prevTrack.id, prevTrack.title, prevTrack.artist, prevTrack.thumbnailUrl)
        } else {
            controller.seekToPrevious()
        }
    }

    fun playNext(track: TrackEntity) { queueManager.playNext(track) }
    fun addToQueue(track: TrackEntity) { queueManager.addToQueue(track) }
    fun insertIntoQueue(index: Int, track: TrackEntity) { queueManager.insertAt(index, track) }
    fun removeFromQueue(index: Int) { queueManager.removeAt(index) }
    fun moveInQueue(from: Int, to: Int) { queueManager.moveTrack(from, to) }

    fun clearQueue() {
        queueManager.clearQueue()
        _controller?.let {
            it.stop()
            it.clearMediaItems()
        }
        _currentMediaItem.value = null
        _isPlaying.value = false
    }

    fun toggleShuffle() {
        _controller?.let {
            val nextMode = !it.shuffleModeEnabled
            it.shuffleModeEnabled = nextMode
            _shuffleModeEnabled.value = nextMode
        }
    }

    fun toggleRepeatMode() {
        _controller?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun downloadTrack(track: TrackEntity, quality: String = "High", networkType: Int = 0) {
        viewModelScope.launch {
            try {
                val url = withContext(Dispatchers.IO) {
                    YouTubeExtractor.getAudioUrl(track.id, quality)
                }
                if (url != null) {
                    downloadManager.downloadSong(track, url, quality, networkType)
                } else {
                    _playbackError.value = "Failed to get download URL"
                }
            } catch (e: Exception) {
                _playbackError.value = "Download error: ${e.message}"
            }
        }
    }

    fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
    }

    private fun fetchLyrics(videoId: String, title: String, artist: String) {
        viewModelScope.launch {
            _lyrics.value = LyricsState(isLoading = true)
            _isFetchingLyrics.value = true
            try {
                val state = LyricsManager.getLyrics(videoId, title, artist)
                _lyrics.value = state
            } catch (e: Exception) {
                _lyrics.value = LyricsState(error = "Could not load lyrics.")
            } finally {
                _isFetchingLyrics.value = false
            }
        }
    }

    private fun updateControllerQueue(controller: MediaController, currentIndex: Int) {
        val queue = queueManager.queue.value
        if (currentIndex >= queue.size - 1) return
        
        val nextTrack = queue[currentIndex + 1]
        var alreadyQueued = false
        for (i in 0 until controller.mediaItemCount) {
            if (controller.getMediaItemAt(i).mediaId == nextTrack.id) {
                alreadyQueued = true
                break
            }
        }
        
        if (!alreadyQueued) {
            val nextItem = MediaItem.Builder()
                .setMediaId(nextTrack.id)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(nextTrack.title)
                    .setArtist(nextTrack.artist)
                    .setArtworkUri(android.net.Uri.parse(nextTrack.thumbnailUrl))
                    .setExtras(Bundle().apply { putString("quality", _audioQuality.value) })
                    .build())
                .build()
            controller.addMediaItem(nextItem)
        }
    }
        override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
