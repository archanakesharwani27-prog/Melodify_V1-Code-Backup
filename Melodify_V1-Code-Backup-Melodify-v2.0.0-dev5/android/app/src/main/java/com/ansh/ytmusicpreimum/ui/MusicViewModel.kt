package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.ansh.ytmusicpreimum.AudioEffectsManager
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import androidx.media3.session.SessionCommand
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

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

    private var userTier: Int = 0
    private var canCacheLyrics: Boolean = false

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

    private val _isCurrentTrackOffline = MutableStateFlow(false)
    val isCurrentTrackOffline: StateFlow<Boolean> = _isCurrentTrackOffline.asStateFlow()

    fun updateOfflineStatus(mediaId: String?) {
        if (mediaId.isNullOrEmpty()) {
            _isCurrentTrackOffline.value = false
            return
        }
        if (mediaId.startsWith("content://") || mediaId.startsWith("file://")) {
            _isCurrentTrackOffline.value = true
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbTrack = trackDao.getTrackById(mediaId)
                val isOffline = dbTrack?.isDownloaded == true && dbTrack.localFilePath != null && java.io.File(dbTrack.localFilePath).exists()
                _isCurrentTrackOffline.value = isOffline
            } catch (t: Throwable) {
                _isCurrentTrackOffline.value = false
            }
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncWithCloud()
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Failed to sync with cloud", e)
            }
        }
    }

    private var isPreparingManually = false
    private val urlCache = mutableMapOf<String, String>()
    private var prefetchJob: Job? = null
    private var predictivePrefetchJobs = mutableListOf<Job>()

    fun prefetchTracks(tracks: List<TrackEntity>) {
        // Cancel existing predictive prefetches
        predictivePrefetchJobs.forEach { it.cancel() }
        predictivePrefetchJobs.clear()

        // Prefetch ONLY first 2 tracks
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

    private val _videoQuality = MutableStateFlow(sharedPrefs.getString("video_quality", "720p") ?: "720p")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()

    private val _relatedTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val relatedTracks: StateFlow<List<TrackEntity>> = _relatedTracks.asStateFlow()

    private val _isLoadingRelated = MutableStateFlow(false)
    val isLoadingRelated: StateFlow<Boolean> = _isLoadingRelated.asStateFlow()

    private val _mediaControllerState = MutableStateFlow<MediaController?>(null)
    val mediaControllerState: StateFlow<MediaController?> = _mediaControllerState.asStateFlow()

    private val _crossfadeDuration = MutableStateFlow(sharedPrefs.getLong("crossfade_duration", 0L))
    val crossfadeDuration: StateFlow<Long> = _crossfadeDuration

    fun setVideoQuality(quality: String) {
        _videoQuality.value = quality
        sharedPrefs.edit().putString("video_quality", quality).apply()
        val currentId = _currentMediaItem.value?.mediaId ?: return
        if (_isVideoMode.value && !currentId.startsWith("content://") && !currentId.startsWith("file://")) {
            val ctrl = _controller ?: return
            val pos = ctrl.currentPosition
            val wasPlaying = ctrl.isPlaying
            viewModelScope.launch {
                _isBuffering.value = true
                val vUrl = YouTubeExtractor.getVideoUrl(currentId, quality)
                _videoUrl.value = vUrl
                if (vUrl != null && _isVideoMode.value) {
                    val currentItem = ctrl.currentMediaItem
                    if (currentItem != null) {
                        val videoItem = currentItem.buildUpon().setUri(vUrl).build()
                        ctrl.setMediaItem(videoItem, pos)
                        ctrl.prepare()
                        if (wasPlaying) ctrl.play()
                    }
                }
                _isBuffering.value = false
            }
        }
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        sharedPrefs.edit().putString("audio_quality", quality).apply()
        urlCache.clear()
        val currentId = _currentMediaItem.value?.mediaId ?: return
        if (!_isVideoMode.value && !currentId.startsWith("content://") && !currentId.startsWith("file://")) {
            val ctrl = _controller ?: return
            val pos = ctrl.currentPosition
            val wasPlaying = ctrl.isPlaying
            viewModelScope.launch {
                _isBuffering.value = true
                val aUrl = YouTubeExtractor.getAudioUrl(currentId, quality)
                if (aUrl != null && !_isVideoMode.value) {
                    val currentItem = ctrl.currentMediaItem
                    if (currentItem != null) {
                        val audioItem = currentItem.buildUpon().setUri(aUrl).build()
                        ctrl.setMediaItem(audioItem, pos)
                        ctrl.prepare()
                        if (wasPlaying) ctrl.play()
                    }
                }
                _isBuffering.value = false
            }
        }
    }

    fun setVideoMode(enabled: Boolean) {
        if (_isVideoMode.value == enabled) return
        _isVideoMode.value = enabled
        val currentId = _currentMediaItem.value?.mediaId ?: return
        if (currentId.startsWith("content://") || currentId.startsWith("file://")) return

        val ctrl = _controller ?: return
        val pos = ctrl.currentPosition
        val wasPlaying = ctrl.isPlaying

        viewModelScope.launch {
            if (enabled) {
                _isBuffering.value = true
                val vUrl = YouTubeExtractor.getVideoUrl(currentId, _videoQuality.value)
                _videoUrl.value = vUrl
                if (vUrl != null && _isVideoMode.value) {
                    val currentItem = ctrl.currentMediaItem
                    if (currentItem != null) {
                        val videoItem = currentItem.buildUpon().setUri(vUrl).build()
                        ctrl.setMediaItem(videoItem, pos)
                        ctrl.prepare()
                        if (wasPlaying) ctrl.play()
                    }
                }
                _isBuffering.value = false
            } else {
                _isBuffering.value = true
                _videoUrl.value = null
                val aUrl = YouTubeExtractor.getAudioUrl(currentId, _audioQuality.value)
                if (aUrl != null && !_isVideoMode.value) {
                    val currentItem = ctrl.currentMediaItem
                    if (currentItem != null) {
                        val audioItem = currentItem.buildUpon().setUri(aUrl).build()
                        ctrl.setMediaItem(audioItem, pos)
                        ctrl.prepare()
                        if (wasPlaying) ctrl.play()
                    }
                }
                _isBuffering.value = false
            }
        }
    }

    fun toggleVideoMode() {
        setVideoMode(!_isVideoMode.value)
    }

    fun loadRelatedTracks(videoId: String, artist: String, title: String) {
        if (videoId.startsWith("content://") || videoId.startsWith("file://")) {
            _relatedTracks.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoadingRelated.value = true
            try {
                val related = YouTubeExtractor.getRelatedTracks(videoId, artist, title)
                val enriched = repository.enrichTracks(related)
                _relatedTracks.value = enriched
                
                val currentQ = queueManager.queue.value
                if (currentQ.size <= 1 && enriched.isNotEmpty()) {
                    val currentTrack = currentQ.firstOrNull() ?: TrackEntity(
                        id = videoId,
                        title = title,
                        artist = artist,
                        thumbnailUrl = _currentMediaItem.value?.mediaMetadata?.artworkUri?.toString() ?: ""
                    )
                    queueManager.setQueue(listOf(currentTrack) + enriched, videoId)
                    prefetchNext()
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error loading related tracks", e)
            } finally {
                _isLoadingRelated.value = false
            }
        }
    }

    fun playRelatedTrack(track: TrackEntity) {
        val currentList = _relatedTracks.value
        playSong(
            videoId = track.id,
            title = track.title,
            artist = track.artist,
            artworkUrl = track.thumbnailUrl,
            otherTracks = currentList,
            userTier = this.userTier,
            canCacheLyrics = this.canCacheLyrics
        )
    }

    private val skipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "${context?.packageName}.ACTION_SKIP_NEXT",
                "com.melodify.melodifyapp.ACTION_SKIP_NEXT",
                "com.ansh.ytmusicpreimum.ACTION_SKIP_NEXT" -> skipNext(userTier)
                "${context?.packageName}.ACTION_SKIP_PREV",
                "com.melodify.melodifyapp.ACTION_SKIP_PREV",
                "com.ansh.ytmusicpreimum.ACTION_SKIP_PREV" -> skipPrevious(userTier)
            }
        }
    }

    init {
        restorePlaybackState()
        startProgressUpdate()
        
        try {
            val pkg = getApplication<Application>().packageName
            val filter = android.content.IntentFilter().apply {
                addAction("${pkg}.ACTION_SKIP_NEXT")
                addAction("${pkg}.ACTION_SKIP_PREV")
                addAction("com.melodify.melodifyapp.ACTION_SKIP_NEXT")
                addAction("com.melodify.melodifyapp.ACTION_SKIP_PREV")
                addAction("com.ansh.ytmusicpreimum.ACTION_SKIP_NEXT")
                addAction("com.ansh.ytmusicpreimum.ACTION_SKIP_PREV")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getApplication<Application>().registerReceiver(skipReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                getApplication<Application>().registerReceiver(skipReceiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicViewModel", "Failed to register skipReceiver", e)
        }

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
        try {
            val statePrefs = getApplication<Application>().getSharedPreferences("playback_state", Context.MODE_PRIVATE)
            statePrefs.edit().apply {
                putString("queue_json", queueManager.serialize())
                putInt("queue_index", queueManager.getCurrentIndex())
                apply()
            }
            
            // Save to Database for deep restoration safely
            viewModelScope.launch(Dispatchers.IO) {
                try {
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
                } catch (t: Throwable) {
                    android.util.Log.w("MusicViewModel", "Failed to persist queue to database", t)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w("MusicViewModel", "Failed to save queue state", t)
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


    fun setCrossfadeDuration(duration: Long) {
        _crossfadeDuration.value = duration
        sharedPrefs.edit().putLong("crossfade_duration", duration).apply()
    }

    private fun restorePlaybackState() {
        try {
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
                try {
                    queueManager.deserialize(it, queueIndex)
                } catch (t: Throwable) {
                    android.util.Log.w("MusicViewModel", "Failed to deserialize queue", t)
                }
            }

            val cleanTitle = YouTubeExtractor.cleanSongTitle(title)
            val cleanArtist = YouTubeExtractor.cleanArtistName(title, artist)

            val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(cleanTitle)
                .setArtist(cleanArtist)

            if (!artwork.isNullOrBlank()) {
                try {
                    metadataBuilder.setArtworkUri(android.net.Uri.parse(artwork))
                } catch (t: Throwable) {}
            }
            
            val restoredItem = MediaItem.Builder()
                .setMediaId(lastMediaId)
                .setMediaMetadata(metadataBuilder.build())
                .build()
            
            _currentMediaItem.value = restoredItem
            updateOfflineStatus(lastMediaId)
            _currentPosition.value = position
            _shuffleModeEnabled.value = shuffle
            _repeatMode.value = repeat
            
            // We don't auto-play on restore, just set the metadata so it shows in UI
            android.util.Log.d("MusicViewModel", "Restored playback state: $title")
        } catch (t: Throwable) {
            android.util.Log.e("MusicViewModel", "Failed to restore playback state safely", t)
        }
    }

    fun initController(context: Context) {
        if (controllerFuture != null) {
             android.util.Log.d("MusicViewModel", "initController: Future already exists")
             return
        }
        android.util.Log.d("MusicViewModel", "Initializing MediaController...")
        val sessionToken = SessionToken(context.applicationContext, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                if (controller != null) {
                    _controller = controller
                    _mediaControllerState.value = controller
                    _volume.value = controller.volume
                    android.util.Log.d("MusicViewModel", "MediaController connected successfully. Session: ${controller.connectedToken}")
                    
                    // Priority: 1. Controller's active item, 2. Restored item
                    if (controller.currentMediaItem != null) {
                        _currentMediaItem.value = controller.currentMediaItem
                        updateOfflineStatus(controller.currentMediaItem?.mediaId)
                        _isPlaying.value = controller.isPlaying
                        _duration.value = controller.duration.coerceAtLeast(0L)
                        _currentPosition.value = controller.currentPosition
                    } else {
                        // If controller is empty, we don't clear the restored state
                        // but we also don't play yet.
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
                        
                        @UnstableApi
                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                             android.util.Log.d("MusicDiagnostics", "MusicViewModel: Controller reported AudioSessionId: $audioSessionId")
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _playbackError.value = null
                            
                            val now = System.currentTimeMillis()
                            if (now - lastManualPlayTime > 2000) {
                                if (mediaItem != null) {
                                    _currentMediaItem.value = mediaItem
                                    updateOfflineStatus(mediaItem.mediaId)
                                }
                            }
                            _duration.value = controller.duration.coerceAtLeast(0L)
                            
                            // If transition happened due to skip/auto-next, update queue index
                            mediaItem?.let { item ->
                                val queue = queueManager.queue.value
                                val indexInQueue = queue.indexOfFirst { it.id == item.mediaId }
                                if (indexInQueue != -1) {
                                    if (indexInQueue != queueManager.getCurrentIndex()) {
                                        queueManager.setCurrentIndex(indexInQueue)
                                        // Update controller with even more upcoming items for continuous gapless
                                        updateControllerQueue(controller, indexInQueue)
                                    }
                                    
                                    // Fetch lyrics for the new track automatically on transition
                                    val track = queue[indexInQueue]
                                    if (userTier >= 2 || canCacheLyrics) {
                                        fetchLyrics(track.id, track.title, track.artist)
                                    }
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
                                val durSec = (controller.duration / 1000).toInt()
                                if (durSec > 0) {
                                    val currentTrack = _currentMediaItem.value
                                    val currentLyrics = _lyrics.value
                                    if (currentTrack != null && (!currentLyrics.isSynced || currentLyrics.lines.isEmpty())) {
                                        fetchLyrics(
                                            currentTrack.mediaId,
                                            currentTrack.mediaMetadata.title?.toString() ?: "",
                                            currentTrack.mediaMetadata.artist?.toString() ?: "",
                                            durSec
                                        )
                                    }
                                }
                            }
                            if (playbackState == Player.STATE_ENDED) {
                                skipNext()
                            }
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            android.util.Log.e("MusicViewModel", "Player error: ${error.message}", error)
                            val isNetworkOrStale = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

                            if (isNetworkOrStale) {
                                _playbackError.value = "Reconnecting audio stream..."
                            } else {
                                _playbackError.value = "Playback Error: ${error.errorCodeName}"
                                _isBuffering.value = false
                                _isPlaying.value = false
                            }
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
                    // If we are preparing manually, don't let the loop override the buffering/playing state
                    if (!isPreparingManually && (now - lastManualPlayTime > 2000)) {
                        _isPlaying.value = it.isPlaying
                        _isBuffering.value = it.playbackState == Player.STATE_BUFFERING
                        
                        it.currentMediaItem?.let { controllerItem ->
                            if (_currentMediaItem.value?.mediaId != controllerItem.mediaId) {
                                _currentMediaItem.value = controllerItem
                            }
                        }
                    }
                    
                    if (it.duration > 0) {
                        _duration.value = it.duration
                    }
                    _currentPosition.value = it.currentPosition
                }
                delay(if (_isPlaying.value) 100L else 500L)
            }
        }
    }

    fun playSong(videoId: String, title: String, artist: String, artworkUrl: String, otherTracks: List<TrackEntity> = emptyList(), userTier: Int = 0, canCacheLyrics: Boolean = false) {
        this.userTier = userTier
        this.canCacheLyrics = canCacheLyrics
        val tapTime = System.currentTimeMillis()
        android.util.Log.d("PlaybackProfile", "User Tap: $videoId at $tapTime")
        
        lastManualPlayTime = System.currentTimeMillis()
        isPreparingManually = true
        
        val cleanTitle = YouTubeExtractor.cleanSongTitle(title)
        val cleanArtist = YouTubeExtractor.cleanArtistName(title, artist)

        // Record History & Playback (Async)
        viewModelScope.launch(Dispatchers.IO) {
            repository.addToHistory(TrackEntity(id = videoId, title = cleanTitle, artist = cleanArtist, thumbnailUrl = artworkUrl))
            trackDao.incrementPlayCount(videoId)
        }

        // Enforce Quality based on Tier
        val finalQuality = if (userTier >= 2) _audioQuality.value else "Medium"
        
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(cleanTitle)
            .setArtist(cleanArtist)
            .setArtworkUri(android.net.Uri.parse(artworkUrl))
            .setExtras(Bundle().apply { putString("quality", finalQuality) })
            .build()
        
        val loadingMediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .build()
        
        // Set state immediately for UI responsiveness
        _currentMediaItem.value = loadingMediaItem
        _isPlaying.value = true
        _isBuffering.value = true
        _duration.value = 0L
        _currentPosition.value = 0L
        _lyrics.value = LyricsState()
        _playbackError.value = null
        isHistoryRecorded = false
        android.util.Log.d("PlaybackProfile", "Player UI Open: +${System.currentTimeMillis() - tapTime}ms")
        android.util.Log.d("PlaybackProfile", "Metadata Ready: +${System.currentTimeMillis() - tapTime}ms")

        viewModelScope.launch {
            // Update Queue Manager
            if (otherTracks.isNotEmpty()) {
                queueManager.setQueue(otherTracks, videoId)
            }

            // Fetch lyrics in background unconditionally for all users
            launch {
                fetchLyrics(videoId, title, artist)
            }

            // Fetch Spotify-style related tracks
            launch {
                loadRelatedTracks(videoId, artist, title)
            }

            // Pre-fetch video stream if user is in video mode
            if (_isVideoMode.value && !videoId.startsWith("content://") && !videoId.startsWith("file://")) {
                launch {
                    val vUrl = YouTubeExtractor.getVideoUrl(videoId, _videoQuality.value)
                    _videoUrl.value = vUrl
                }
            } else {
                _videoUrl.value = null
            }

            try {
                var retryCount = 0
                while (_controller == null && retryCount < 15) {
                    delay(50) 
                    retryCount++
                }

                val currentController = _controller
                if (currentController == null) {
                    _playbackError.value = "Player not connected."
                    _isBuffering.value = false
                    _isPlaying.value = false
                    isPreparingManually = false
                    return@launch
                }

                var urlResult: String? = null
                val isLocal = videoId.startsWith("content://") || videoId.startsWith("file://")
                
                if (isLocal) {
                    urlResult = videoId
                } else {
                    // CHECK FOR MELODIFY OFFLINE DOWNLOAD
                    val dbTrack = withContext(Dispatchers.IO) {
                        trackDao.getTrackById(videoId)
                    }
                    
                    if (dbTrack?.isDownloaded == true && dbTrack.localFilePath != null) {
                        val file = java.io.File(dbTrack.localFilePath)
                        if (file.exists()) {
                            urlResult = dbTrack.localFilePath
                        }
                    }
                }
                
                val mainItemBuilder = MediaItem.Builder()
                    .setMediaId(videoId)
                    .setMediaMetadata(metadata)
                
                if (urlResult != null) {
                    mainItemBuilder.setUri(urlResult)
                    android.util.Log.d("PlaybackProfile", "Cache Hit (Local/Downloaded): $videoId")
                } else {
                    // For remote streams, we let MusicService handle resolution to avoid blocking here,
                    // BUT if we have it in urlCache, we should set it NOW to save a roundtrip to Service.
                    urlCache[videoId]?.let {
                        mainItemBuilder.setUri(it)
                        android.util.Log.d("PlaybackProfile", "Cache Hit (Memory): $videoId")
                    } ?: android.util.Log.d("PlaybackProfile", "Cache Miss: $videoId. Let Service resolve.")
                }
                
                val mainItem = mainItemBuilder.build()
                
                android.util.Log.d("PlaybackProfile", "MediaItem Ready: +${System.currentTimeMillis() - tapTime}ms")
                
                // Optimized: If it's already playing this item, don't reset
                if (currentController.currentMediaItem?.mediaId == videoId && currentController.playbackState != Player.STATE_IDLE) {
                    currentController.play()
                } else {
                    currentController.setMediaItem(mainItem)
                    android.util.Log.d("PlaybackProfile", "ExoPlayer Prepare Start: +${System.currentTimeMillis() - tapTime}ms")
                    currentController.prepare()
                    currentController.play()
                }
                updateOfflineStatus(videoId)
                
                if (!isLocal) {
                    prefetchNext()
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error in playSong", e)
                _playbackError.value = "Playback Error: ${e.message}"
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
                delay(1500) // Wait for current playback to stabilize
                
                // Prefetch ONLY the next track
                val nextIndex = currentIndex + 1
                if (nextIndex < queue.size) {
                    val track = queue[nextIndex]
                    if (!urlCache.containsKey(track.id)) {
                        android.util.Log.d("MusicViewModel", "Prefetching Next Only: ${track.title}")
                        YouTubeExtractor.getAudioUrl(track.id)?.let { url ->
                            urlCache[track.id] = url
                            
                            // Background Preparation: Add to ExoPlayer queue if possible
                            _controller?.let { controller ->
                                if (controller.mediaItemCount == 1) { // Only if nothing else is queued
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
        val controller = _controller
        if (controller != null) {
            lastManualPlayTime = System.currentTimeMillis() // Shield state from controller flicker
            if (controller.isPlaying) {
                controller.pause()
                _isPlaying.value = false
            } else {
                if (controller.mediaItemCount == 0 && _currentMediaItem.value != null) {
                    // Restored item case
                    val restoredItem = _currentMediaItem.value!!
                    val position = _currentPosition.value
                    playSong(
                        videoId = restoredItem.mediaId,
                        title = restoredItem.mediaMetadata.title?.toString() ?: "Unknown",
                        artist = restoredItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        artworkUrl = restoredItem.mediaMetadata.artworkUri?.toString() ?: ""
                    )
                    // Wait for prepare and seek? playSong already does prepare/play.
                    // I should probably add seek to playSong or handle it here.
                    viewModelScope.launch {
                        delay(500)
                        seekTo(position)
                    }
                } else {
                    controller.play()
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
        _controller?.let {
            android.util.Log.d("MusicViewModel", "seekTo: $position")
            it.seekTo(position)
        }
    }

    fun skipNext(userTier: Int = 0) {
        val controller = _controller ?: return
        val nextTrack = queueManager.getNextTrack()
        
        if (nextTrack != null) {
            // Check if controller already has this item as next
            val nextIndex = controller.nextMediaItemIndex
            if (nextIndex != C.INDEX_UNSET) {
                val nextItem = controller.getMediaItemAt(nextIndex)
                if (nextItem.mediaId == nextTrack.id) {
                    controller.seekToNext()
                    return
                }
            }
            playSong(nextTrack.id, nextTrack.title, nextTrack.artist, nextTrack.thumbnailUrl, userTier = userTier)
        } else {
             controller.seekToNext()
        }
    }

    fun skipPrevious(userTier: Int = 0) {
        val controller = _controller ?: return
        val prevTrack = queueManager.getPreviousTrack()
        
        if (prevTrack != null) {
            // Check if controller already has this item as previous
            val prevIndex = controller.previousMediaItemIndex
            if (prevIndex != C.INDEX_UNSET) {
                val prevItem = controller.getMediaItemAt(prevIndex)
                if (prevItem.mediaId == prevTrack.id) {
                    controller.seekToPrevious()
                    return
                }
            }
            playSong(prevTrack.id, prevTrack.title, prevTrack.artist, prevTrack.thumbnailUrl, userTier = userTier)
        } else {
            controller.seekToPrevious()
        }
    }

    fun playNext(track: TrackEntity) {
        queueManager.playNext(track)
    }

    fun addToQueue(track: TrackEntity) {
        queueManager.addToQueue(track)
    }

    fun insertIntoQueue(index: Int, track: TrackEntity) {
        queueManager.insertAt(index, track)
    }

    fun removeFromQueue(index: Int) {
        queueManager.removeAt(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        queueManager.moveTrack(from, to)
    }

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
            android.util.Log.d("MusicViewModel", "toggleShuffle: $nextMode")
        } ?: android.util.Log.e("MusicViewModel", "toggleShuffle: controller is null")
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
            android.util.Log.d("MusicViewModel", "toggleRepeatMode: $nextMode")
        } ?: android.util.Log.e("MusicViewModel", "toggleRepeatMode: controller is null")
    }

    fun downloadTrack(track: TrackEntity, quality: String = "High", userTier: Int = 0, networkType: Int = 0) {
        viewModelScope.launch {
            try {
                val url = withContext(Dispatchers.IO) {
                    YouTubeExtractor.getAudioUrl(track.id, quality)
                }
                if (url != null) {
                    downloadManager.downloadSong(track, url, quality, userTier, networkType)
                } else {
                    _playbackError.value = "Failed to get download URL"
                }
            } catch (e: Exception) {
                _playbackError.value = "Download error: ${e.message}"
            }
        }
    }

    fun downloadVideoTrack(track: TrackEntity, quality: String = "720p", userTier: Int = 0, networkType: Int = 0) {
        viewModelScope.launch {
            try {
                val url = withContext(Dispatchers.IO) {
                    YouTubeExtractor.getVideoUrl(track.id, quality)
                }
                if (url != null) {
                    downloadManager.downloadVideo(track, url, quality, userTier, networkType)
                } else {
                    _playbackError.value = "Failed to get video download URL"
                }
            } catch (e: Exception) {
                _playbackError.value = "Video download error: ${e.message}"
            }
        }
    }

    val currentAudioPreset: StateFlow<String> = AudioEffectsManager.currentPreset

    fun setAudioPreset(presetName: String) {
        AudioEffectsManager.applyPreset(presetName)
    }

    fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
    }

    fun fetchLyrics(videoId: String, title: String, artist: String, durationSeconds: Int = 0) {
        viewModelScope.launch {
            _lyrics.value = LyricsState(isLoading = true)
            _isFetchingLyrics.value = true
            try {
                val dur = if (durationSeconds > 0) durationSeconds else (_duration.value / 1000).toInt()
                val state = LyricsManager.getLyrics(videoId, title, artist, dur)
                _lyrics.value = state
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching lyrics", e)
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
        // Check if already in controller queue
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
        try {
            getApplication<Application>().unregisterReceiver(skipReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
