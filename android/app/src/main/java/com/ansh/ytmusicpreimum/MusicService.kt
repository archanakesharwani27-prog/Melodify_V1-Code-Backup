package com.ansh.ytmusicpreimum

import android.content.Intent
import android.content.Context
import kotlinx.coroutines.*
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var audioVisualizer: Visualizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var crossfadeDurationMs = 0L // 0 means disabled

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        crossfadeDurationMs = sharedPrefs.getLong("crossfade_duration", 0L)

        exoPlayer = ExoPlayer.Builder(this)
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
                initEqualizer(audioSessionId)
                // initVisualizer(audioSessionId) // Disabled for now
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
                    // audioVisualizer?.enabled = true
                } else {
                    // audioVisualizer?.enabled = false
                }
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
                    "SET_EQ_BAND" -> {
                        val band = args.getInt("band")
                        val level = args.getShort("level")
                        try {
                            if (equalizer?.enabled != true) equalizer?.enabled = true
                            equalizer?.setBandLevel(band.toShort(), level)
                        } catch (e: Exception) {
                            android.util.Log.e("MusicService", "Error setting EQ band", e)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "GET_EQ_INFO" -> {
                        val resultBundle = Bundle()
                        equalizer?.let {
                            resultBundle.putInt("numBands", it.numberOfBands.toInt())
                            val centerFreqs = IntArray(it.numberOfBands.toInt()) { i -> it.getCenterFreq(i.toShort()) }
                            resultBundle.putIntArray("centerFreqs", centerFreqs)
                            val range = it.bandLevelRange
                            resultBundle.putShortArray("levelRange", range)
                        }
                        bassBoost?.let {
                            resultBundle.putShort("bassBoostStrength", it.roundedStrength)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                    }
                    "SET_BASS_BOOST" -> {
                        val strength = args.getShort("strength")
                        bassBoost?.setStrength(strength)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "SET_VIRTUALIZER" -> {
                        val strength = args.getShort("strength")
                        virtualizer?.setStrength(strength)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
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
            while (isActive && exoPlayer.isPlaying) {
                delay(3000)
                val sharedPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                val tier = sharedPrefs.getInt("user_tier", 0)
                
                if (tier < 2) { // Non-PRO users
                    if (!isAppInForeground()) {
                        exoPlayer.pause()
                        android.util.Log.d("MusicService", "Pausing playback: Background play restricted for non-PRO users.")
                        // In a real app, you'd show a notification or toast.
                    }
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

    private fun initEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            
            bassBoost = BassBoost(0, audioSessionId)
            bassBoost?.enabled = true
            
            virtualizer = Virtualizer(0, audioSessionId)
            virtualizer?.enabled = true
            
            // RESET TO FLAT
            val numBands = equalizer?.numberOfBands ?: 0
            for (i in 0 until numBands) {
                equalizer?.setBandLevel(i.toShort(), 0)
            }
            bassBoost?.setStrength(0)
            virtualizer?.setStrength(0)
            
            android.util.Log.d("MusicService", "Audio Effects initialized for session: $audioSessionId")
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Failed to init Audio Effects", e)
        }
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

    override fun onDestroy() {
        serviceScope.cancel()
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
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
        player?.pause()
        stopSelf()
    }
}
