package com.ansh.ytmusicpreimum

import android.content.Intent
import android.content.Context
import kotlinx.coroutines.*
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow

object VisualizerData {
    val fftData = MutableStateFlow(ByteArray(0))
    val waveformData = MutableStateFlow(ByteArray(0))
}

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var audioVisualizer: Visualizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var crossfadeDurationMs = 0L // 0 means disabled

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        crossfadeDurationMs = sharedPrefs.getLong("crossfade_duration", 0L)

        // Force disable Audio Offload to ensure effects work on all devices (if any were used)
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)

        exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle Audio Focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    initVisualizer(audioSessionId)
                    
                    // Notify system about our audio session
                    val intent = Intent(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    sendBroadcast(intent)
                } else {
                    android.util.Log.w("MusicDiagnostics", "onAudioSessionIdChanged: Invalid session ID $audioSessionId")
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    if (crossfadeDurationMs > 0) {
                        fadeIn(crossfadeDurationMs)
                    } else {
                        exoPlayer.volume = 1.0f
                    }
                }
                savePlaybackState()
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                savePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    savePlaybackState()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startBackgroundCheck()
                }
                audioVisualizer?.enabled = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicService", "Playback error encountered: ${error.errorCodeName} (${error.errorCode})", error)
                val currentItem = exoPlayer.currentMediaItem ?: return
                val videoId = currentItem.mediaId

                // If error is network or stale/expired stream URL, attempt silent recovery
                if (!videoId.startsWith("content://") && !videoId.startsWith("file://") && videoId.isNotEmpty()) {
                    android.util.Log.i("MusicService", "Attempting silent stream renewal for videoId: $videoId")
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            YouTubeExtractor.invalidateCache(videoId)
                            val quality = currentItem.mediaMetadata.extras?.getString("quality") ?: "Medium"
                            val freshUrl = YouTubeExtractor.getAudioUrl(videoId, quality)

                            if (freshUrl != null && isActive) {
                                withContext(Dispatchers.Main) {
                                    val currentPos = exoPlayer.currentPosition
                                    val renewedItem = currentItem.buildUpon().setUri(freshUrl).build()
                                    exoPlayer.setMediaItem(renewedItem, currentPos)
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                    android.util.Log.i("MusicService", "Silent stream renewal succeeded for $videoId")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MusicService", "Failed stream renewal for $videoId", e)
                        }
                    }
                }
            }
        })

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            @OptIn(UnstableApi::class)
            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long
            ) {
            }
            
            @OptIn(UnstableApi::class)
            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {
            }
        })

        startPositionMonitor()

        val callback = object : MediaSession.Callback {
            @OptIn(UnstableApi::class)
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .build()
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .addAllCommands()
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build()
            }

            @OptIn(UnstableApi::class)
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val settableFuture = com.google.common.util.concurrent.SettableFuture.create<MutableList<MediaItem>>()
                
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val updatedItems = mediaItems.map { item ->
                            if (item.requestMetadata.mediaUri != null || item.localConfiguration?.uri != null || (item.mediaId.startsWith("http") && item.localConfiguration?.uri == null && item.requestMetadata.mediaUri == null)) {
                                item
                            } else {
                                val videoId = item.mediaId
                                val quality = item.mediaMetadata.extras?.getString("quality") ?: "Medium"
                                android.util.Log.d("MusicService", "Resolving URI for $videoId with quality $quality")
                                
                                val url = withTimeoutOrNull(15000) {
                                    YouTubeExtractor.getAudioUrl(videoId, quality)
                                }
                                if (url != null) {
                                    android.util.Log.d("MusicService", "Successfully resolved URI for $videoId")
                                    item.buildUpon().setUri(url).build()
                                } else {
                                    android.util.Log.e("MusicService", "Failed to resolve URI for $videoId (Timeout or Error)")
                                    item
                                }
                            }
                        }.toMutableList()
                        settableFuture.set(updatedItems)
                    } catch (e: Throwable) {
                        android.util.Log.e("MusicService", "Error in onAddMediaItems", e)
                        settableFuture.set(mediaItems)
                    }
                }
                
                return settableFuture
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    "SET_CROSSFADE" -> {
                        val duration = args.getLong("duration")
                        crossfadeDurationMs = duration
                        getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                            .putLong("crossfade_duration", duration).apply()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(callback)
            .build()
    }

    private fun savePlaybackState() {
        val currentItem = exoPlayer.currentMediaItem ?: return
        val sharedPrefs = getSharedPreferences("playback_state", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("last_media_id", currentItem.mediaId)
            putLong("last_position", exoPlayer.currentPosition)
            putBoolean("shuffle_enabled", exoPlayer.shuffleModeEnabled)
            putInt("repeat_mode", exoPlayer.repeatMode)
            // Save basic metadata for fast restore in UI before service resolves URL
            putString("last_title", currentItem.mediaMetadata.title?.toString())
            putString("last_artist", currentItem.mediaMetadata.artist?.toString())
            putString("last_artwork", currentItem.mediaMetadata.artworkUri?.toString())
            apply()
        }
    }

    private fun initVisualizer(audioSessionId: Int) {
        try {
            audioVisualizer?.release()
            audioVisualizer = Visualizer(audioSessionId).apply {
                // Use a smaller capture size for performance
                captureSize = 512
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let { VisualizerData.waveformData.value = it.copyOf() }
                    }

                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        fft?.let { VisualizerData.fftData.value = it.copyOf() }
                    }
                }, Visualizer.getMaxCaptureRate() / 4, true, true) // Reduced update frequency
                enabled = exoPlayer.isPlaying
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Failed to init Visualizer", e)
        }
    }


    private var backgroundCheckJob: Job? = null
    private fun startBackgroundCheck() {
        backgroundCheckJob?.cancel()
        backgroundCheckJob = serviceScope.launch {
            val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
            while (isActive && exoPlayer.isPlaying) {
                delay(5000) // 5 seconds for battery efficiency
                val isLoggedIn = authPrefs.getBoolean("is_logged_in", false)
                val userEmail = authPrefs.getString("user_email", "") ?: ""
                val isGuest = userEmail == "guest@melodify.com" || userEmail.isEmpty()
                
                if ((!isLoggedIn || isGuest) && !isAppInForeground() && exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    authPrefs.edit().putBoolean("guest_pause_triggered", true).apply()
                }
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    private fun startPositionMonitor() {
        serviceScope.launch {
            while (isActive) {
                delay(500)
                if (exoPlayer.isPlaying && crossfadeDurationMs > 0) {
                    val duration = exoPlayer.duration
                    val position = exoPlayer.currentPosition
                    if (duration > 0 && (duration - position) <= crossfadeDurationMs) { 
                        fadeOut(crossfadeDurationMs)
                        // Note: Media3 will automatically transition to the next item
                        // when the current one ends. The fadeIn will be triggered in onMediaItemTransition.
                        delay(crossfadeDurationMs + 500)
                    }
                }
            }
        }
    }

    private fun fadeIn(durationMs: Long) {
        if (durationMs <= 0) {
            exoPlayer.volume = 1.0f
            return
        }
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 20
            val stepDuration = durationMs / steps
            for (i in 0..steps) {
                val vol = i.toFloat() / steps
                exoPlayer.volume = vol
                delay(stepDuration)
            }
            exoPlayer.volume = 1.0f
        }
    }

    private fun fadeOut(durationMs: Long, onEnd: () -> Unit = {}) {
        if (durationMs <= 0) {
            exoPlayer.volume = 0f
            onEnd()
            return
        }
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 20
            val stepDuration = durationMs / steps
            for (i in steps downTo 0) {
                val vol = i.toFloat() / steps
                exoPlayer.volume = vol
                delay(stepDuration)
            }
            exoPlayer.volume = 0f
            onEnd()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        serviceScope.cancel()
        
        // Notify system session closing
        val sessionId = exoPlayer.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            val intent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
            sendBroadcast(intent)
        }

        audioVisualizer?.release()
        audioVisualizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // If not playing or idle, terminate service cleanly.
        // If actively playing in foreground, retain playback.
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) {
            player?.pause()
            stopSelf()
        }
    }
}
