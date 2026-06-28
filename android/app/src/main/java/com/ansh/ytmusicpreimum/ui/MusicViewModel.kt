package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.ansh.ytmusicpreimum.MusicService
import com.ansh.ytmusicpreimum.YouTubeExtractor
import com.ansh.ytmusicpreimum.MelodifyDownloadManager
import com.ansh.ytmusicpreimum.MelodifyQueueManager
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import androidx.media3.session.SessionCommand
import android.os.Bundle

import androidx.media3.common.util.UnstableApi

data class EqBand(
    val index: Int,
    val centerFreq: Int,
    val level: Short
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val trackDao = database.trackDao()
    private val repository = com.ansh.ytmusicpreimum.data.TrackRepository(trackDao)

    private val downloadManager = MelodifyDownloadManager.getInstance(application)
    val downloadStates = downloadManager.downloadStates
    val queuedTracks = downloadManager.queuedTracks
    
    private val queueManager = MelodifyQueueManager()
    val currentQueue = queueManager.queue
    
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

    private val urlCache = mutableMapOf<String, String>()
    private var prefetchJob: kotlinx.coroutines.Job? = null

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics

    private val _eqBands = MutableStateFlow<List<EqBand>>(emptyList())
    val eqBands: StateFlow<List<EqBand>> = _eqBands

    private val _eqLevelRange = MutableStateFlow<Pair<Short, Short>>(Pair(-1500, 1500))
    val eqLevelRange: StateFlow<Pair<Short, Short>> = _eqLevelRange

    private val _bassBoostStrength = MutableStateFlow<Short>(0)
    val bassBoostStrength: StateFlow<Short> = _bassBoostStrength

    private val _virtualizerStrength = MutableStateFlow<Short>(0)
    val virtualizerStrength: StateFlow<Short> = _virtualizerStrength

    private val sharedPrefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _audioQuality = MutableStateFlow(sharedPrefs.getString("audio_quality", "Medium") ?: "Medium")
    val audioQuality: StateFlow<String> = _audioQuality

    init {
        startProgressUpdate()
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
        urlCache.clear() // Clear cache to fetch new quality next time
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
                    _volume.value = controller.volume
                    android.util.Log.d("MusicViewModel", "MediaController connected successfully. Session: ${controller.connectedToken}")
                    _isPlaying.value = controller.isPlaying
                    // Only update currentMediaItem from controller if we aren't already trying to play something
                    if (_currentMediaItem.value == null) {
                        _currentMediaItem.value = controller.currentMediaItem
                    }
                    _duration.value = controller.duration.coerceAtLeast(0L)
                    _isBuffering.value = controller.playbackState == Player.STATE_BUFFERING
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode
                    
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }
                        
                        @UnstableApi
                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                             fetchEqInfo()
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _currentMediaItem.value = mediaItem
                            _duration.value = controller.duration.coerceAtLeast(0L)
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _isBuffering.value = playbackState == Player.STATE_BUFFERING
                            if (playbackState == Player.STATE_READY) {
                                _duration.value = controller.duration.coerceAtLeast(0L)
                            }
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            _playbackError.value = "Playback Error: ${error.errorCodeName}"
                            _isBuffering.value = false
                            _isPlaying.value = false
                            android.util.Log.e("MusicViewModel", "Player error: ${error.message}", error)
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
                    _currentPosition.value = it.currentPosition
                    if (it.duration > 0) {
                        _duration.value = it.duration
                    }
                    // Sync essential state
                    _isPlaying.value = it.isPlaying
                    _isBuffering.value = it.playbackState == Player.STATE_BUFFERING
                    
                    if (_currentMediaItem.value?.mediaId != it.currentMediaItem?.mediaId) {
                        _currentMediaItem.value = it.currentMediaItem
                    }
                }
                delay(500)
            }
        }
    }

    fun playSong(videoId: String, title: String, artist: String, artworkUrl: String, otherTracks: List<TrackEntity> = emptyList(), userTier: Int = 0, canCacheLyrics: Boolean = false) {
        viewModelScope.launch {
            android.util.Log.d("MusicViewModel", "Preparing to play song: $title ($videoId), Tier: $userTier")
            _playbackError.value = null
            _isBuffering.value = true
            _lyrics.value = null
            isHistoryRecorded = false
            
            // Record History
            repository.addToHistory(TrackEntity(id = videoId, title = title, artist = artist, thumbnailUrl = artworkUrl))

            // For Free Users: Delay removed to make app feel faster as requested
            // if (userTier == 0 && _currentMediaItem.value != null) {
            //     delay(800) 
            // }

            // Enforce Quality based on Tier
            val finalQuality = if (userTier >= 2) _audioQuality.value else "Medium"
            
            // Update Queue Manager
            if (otherTracks.isNotEmpty()) {
                queueManager.setQueue(otherTracks, videoId)
            }
            
            // Set metadata immediately for UI
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(android.net.Uri.parse(artworkUrl))
                .build()
            
            val loadingMediaItem = MediaItem.Builder()
                .setMediaId(videoId)
                .setMediaMetadata(metadata)
                .build()
            
            _currentMediaItem.value = loadingMediaItem
            _isPlaying.value = true

            // Fetch lyrics if Pro or if Free user has lyrics cache left
            if (userTier >= 2 || canCacheLyrics) {
                fetchLyrics(title, artist)
            }

            try {
                var retryCount = 0
                while (_controller == null && retryCount < 10) {
                    delay(100) 
                    retryCount++
                }

                val currentController = _controller
                if (currentController == null) {
                    _playbackError.value = "Player not connected."
                    _isBuffering.value = false
                    _isPlaying.value = false
                    return@launch
                }

                var urlResult: String? = null
                val isLocal = videoId.startsWith("content://") || videoId.startsWith("file://")
                
                if (isLocal) {
                    urlResult = videoId
                } else {
                    // CHECK FOR MELODIFY OFFLINE DOWNLOAD FIRST
                    val dbTrack = withContext(Dispatchers.IO) {
                        trackDao.getTrackById(videoId)
                    }
                    
                    if (dbTrack?.isDownloaded == true && dbTrack.localFilePath != null) {
                        val file = java.io.File(dbTrack.localFilePath)
                        if (file.exists()) {
                            urlResult = dbTrack.localFilePath
                            android.util.Log.d("MusicViewModel", "Playing from Offline Storage: $title")
                        }
                    }

                    if (urlResult == null) {
                        urlResult = urlCache[videoId]
                        if (urlResult == null) {
                            urlResult = withTimeoutOrNull(8000) { // Reduced timeout
                                YouTubeExtractor.getAudioUrl(videoId, finalQuality)
                            }
                        }
                    }
                }
                
                if (urlResult != null) {
                    if (!isLocal) {
                        urlCache[videoId] = urlResult
                    }
                    val mainItem = MediaItem.Builder()
                        .setMediaId(videoId)
                        .setUri(urlResult)
                        .setMediaMetadata(metadata)
                        .build()
                    
                    currentController.setMediaItem(mainItem)
                    currentController.prepare()
                    currentController.play()
                    
                    _currentMediaItem.value = mainItem
                    _isPlaying.value = true
                    
                    if (!isLocal) {
                        prefetchNext()
                    }
                } else {
                    _playbackError.value = "Could not fetch stream."
                    _isBuffering.value = false
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error in playSong", e)
                _playbackError.value = "Error: ${e.message}"
                _isBuffering.value = false
                _isPlaying.value = false
            }
        }
    }

    private fun prefetchNext() {
        prefetchJob?.cancel()
        val queue = queueManager.queue.value
        val currentIndex = queueManager.getCurrentIndex()
        if (queue.isNotEmpty()) {
            prefetchJob = viewModelScope.launch {
                delay(800) // Reduced delay
                
                // Prefetch next 3 songs for smoother transition
                val nextIndices = listOf(currentIndex + 1, currentIndex + 2, currentIndex + 3)
                for (index in nextIndices) {
                    if (index < queue.size) {
                        val track = queue[index]
                        if (!urlCache.containsKey(track.id)) {
                            android.util.Log.d("MusicViewModel", "Prefetching: ${track.title}")
                            YouTubeExtractor.getAudioUrl(track.id)?.let { url ->
                                urlCache[track.id] = url
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
            if (controller.isPlaying) {
                controller.pause()
                _isPlaying.value = false
            } else {
                controller.play()
                _isPlaying.value = true
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
        val nextTrack = queueManager.getNextTrack()
        if (nextTrack != null) {
            playSong(nextTrack.id, nextTrack.title, nextTrack.artist, nextTrack.thumbnailUrl, userTier = userTier)
        } else {
             _controller?.seekToNext()
        }
    }

    fun skipPrevious(userTier: Int = 0) {
        val prevTrack = queueManager.getPreviousTrack()
        if (prevTrack != null) {
            playSong(prevTrack.id, prevTrack.title, prevTrack.artist, prevTrack.thumbnailUrl, userTier = userTier)
        } else {
            _controller?.seekToPrevious()
        }
    }

    fun playNext(track: TrackEntity) {
        queueManager.playNext(track)
    }

    fun addToQueue(track: TrackEntity) {
        queueManager.addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        queueManager.removeAt(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        queueManager.moveTrack(from, to)
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

    @UnstableApi
    fun fetchEqInfo() {
        _controller?.let { controller ->
            val command = SessionCommand("GET_EQ_INFO", Bundle.EMPTY)
            val future = controller.sendCustomCommand(command, Bundle.EMPTY)
            future.addListener({
                val result = future.get()
                val extras = result.extras
                if (result.resultCode == 0 && !extras.isEmpty) {
                    val numBands = extras.getInt("numBands")
                    val freqs = extras.getIntArray("centerFreqs")
                    val range = extras.getShortArray("levelRange")
                    val bass = extras.getShort("bassBoostStrength")
                    
                    if (freqs != null && range != null) {
                        _eqLevelRange.value = Pair(range[0], range[1])
                        val bands = List(numBands) { i ->
                            EqBand(i, freqs[i], 0) // Levels initially 0 (flat)
                        }
                        _eqBands.value = bands
                        _bassBoostStrength.value = bass
                        android.util.Log.d("MusicViewModel", "EQ Info fetched: $numBands bands, Bass: $bass")
                    }
                }
            }, ContextCompat.getMainExecutor(getApplication()))
        }
    }

    @UnstableApi
    fun setEqBandLevel(bandIndex: Int, level: Short) {
        val currentBands = _eqBands.value.toMutableList()
        if (bandIndex in currentBands.indices) {
            currentBands[bandIndex] = currentBands[bandIndex].copy(level = level)
            _eqBands.value = currentBands
            
            _controller?.let { controller ->
                val args = Bundle().apply {
                    putInt("band", bandIndex)
                    putShort("level", level)
                }
                controller.sendCustomCommand(SessionCommand("SET_EQ_BAND", Bundle.EMPTY), args)
            }
        }
    }

    @UnstableApi
    fun setBassBoost(strength: Short) {
        _bassBoostStrength.value = strength
        _controller?.let { controller ->
            val args = Bundle().apply {
                putShort("strength", strength)
            }
            controller.sendCustomCommand(SessionCommand("SET_BASS_BOOST", Bundle.EMPTY), args)
        }
    }

    @UnstableApi
    fun setVirtualizer(strength: Short, userTier: Int = 0) {
        _virtualizerStrength.value = strength
        _controller?.let { controller ->
            val args = Bundle().apply {
                putShort("strength", strength)
            }
            controller.sendCustomCommand(SessionCommand("SET_VIRTUALIZER", Bundle.EMPTY), args)
            
            // Freemium 8D Logic: Reset after 1 minute for Free users
            if (userTier == 0 && strength > 0) {
                viewModelScope.launch {
                    delay(60000) // 1 minute trial
                    if (_virtualizerStrength.value > 0) {
                        setVirtualizer(0)
                        _playbackError.value = "8D Trial Ended. Upgrade for unlimited use!"
                    }
                }
            }
        }
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

    fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
    }

    private fun fetchLyrics(title: String, artist: String) {
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            try {
                // Advanced cleaning: "Artist - Song (Official Video)" -> "Song"
                var cleanTitle = title
                if (cleanTitle.contains("-")) {
                    cleanTitle = cleanTitle.split("-").last()
                }
                cleanTitle = cleanTitle.replace(Regex("\\(.*?\\)"), "")
                    .replace(Regex("\\[.*?\\]"), "")
                    .replace("Official Video", "", ignoreCase = true)
                    .replace("Lyrics", "", ignoreCase = true)
                    .trim()

                val url = "https://lrclib.net/api/get?artist=${java.net.URLEncoder.encode(artist, "UTF-8")}&track=${java.net.URLEncoder.encode(cleanTitle, "UTF-8")}"
                
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(url).build()
                
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = org.json.JSONObject(body)
                                val lyricsText = if (json.has("plainLyrics")) {
                                    json.getString("plainLyrics")
                                } else if (json.has("lyrics")) {
                                    json.getString("lyrics")
                                } else {
                                    "No lyrics found."
                                }
                                _lyrics.value = lyricsText
                            }
                        } else {
                            _lyrics.value = "Lyrics not available for this track."
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error fetching lyrics", e)
                _lyrics.value = "Could not load lyrics."
            } finally {
                _isFetchingLyrics.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
