package com.ansh.ytmusicpreimum

import android.content.Intent
import android.content.Context
import kotlinx.coroutines.*
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.LoudnessEnhancer
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
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioVisualizer: Visualizer? = null

    // Effect availability flags
    private var isBassBoostSupported = false
    private var isVirtualizerSupported = false
    private var isLoudnessEnhancerSupported = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var crossfadeDurationMs = 0L // 0 means disabled

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        crossfadeDurationMs = sharedPrefs.getLong("crossfade_duration", 0L)

        // Force disable Audio Offload to ensure Equalizer works on all devices
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
                    initEqualizer(audioSessionId)
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
                    "SET_EQ_BAND" -> {
                        val band = args.getInt("band")
                        val level = args.getShort("level")
                        android.util.Log.d("MusicDiagnostics", "Command received: SET_EQ_BAND $band to $level")
                        try {
                            equalizer?.let {
                                if (!it.enabled) it.enabled = true
                                it.setBandLevel(band.toShort(), level)
                                val verified = it.getBandLevel(band.toShort())
                                android.util.Log.d("MusicDiagnostics", "Equalizer updated: Band $band set to $level. Verification read: $verified. hasControl: ${it.hasControl()}")
                                saveEqState()
                            } ?: android.util.Log.w("MusicDiagnostics", "SET_EQ_BAND ignored: Equalizer object is null")
                        } catch (e: Exception) {
                            android.util.Log.e("MusicDiagnostics", "Error setting EQ band", e)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "APPLY_EXTREME_TEST" -> {
                        android.util.Log.d("MusicDiagnostics", "Command received: APPLY_EXTREME_TEST")
                        equalizer?.let { eq ->
                            try {
                                eq.enabled = true
                                val range = eq.bandLevelRange
                                val min = range[0]
                                val max = range[1]
                                val bands = eq.numberOfBands.toInt()
                                // Step 5: Band1=MIN, Band2=MIN, Band3=MAX, Band4=MIN, Band5=MAX
                                if (bands >= 5) {
                                    eq.setBandLevel(0, min)
                                    eq.setBandLevel(1, min)
                                    eq.setBandLevel(2, max)
                                    eq.setBandLevel(3, min)
                                    eq.setBandLevel(4, max)
                                } else {
                                    for (i in 0 until bands) {
                                        eq.setBandLevel(i.toShort(), if (i % 2 == 0) min else max)
                                    }
                                }
                                android.util.Log.d("MusicDiagnostics", "Extreme Test applied. hasControl: ${eq.hasControl()}")
                                saveEqState()
                            } catch (e: Exception) {
                                android.util.Log.e("MusicDiagnostics", "Failed to apply extreme test", e)
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "GET_EQ_INFO" -> {
                        val resultBundle = Bundle()
                        val eqEnabled = getSharedPreferences("equalizer_state", Context.MODE_PRIVATE).getBoolean("is_enabled", true)
                        resultBundle.putBoolean("isEnabled", eqEnabled)
                        equalizer?.let {
                            resultBundle.putInt("numBands", it.numberOfBands.toInt())
                            val centerFreqs = IntArray(it.numberOfBands.toInt()) { i -> it.getCenterFreq(i.toShort()) }
                            resultBundle.putIntArray("centerFreqs", centerFreqs)
                            val range = it.bandLevelRange
                            resultBundle.putShortArray("levelRange", range)
                            val levels = ShortArray(it.numberOfBands.toInt()) { i -> it.getBandLevel(i.toShort()) }
                            resultBundle.putShortArray("currentLevels", levels)
                            android.util.Log.d("MusicDiagnostics", "Reporting EQ Info: bands=${it.numberOfBands}, hasControl=${it.hasControl()}")
                        }
                        resultBundle.putBoolean("isBassBoostSupported", isBassBoostSupported)
                        if (isBassBoostSupported) {
                            bassBoost?.let {
                                resultBundle.putShort("bassBoostStrength", it.roundedStrength)
                                android.util.Log.d("MusicDiagnostics", "Reporting BassBoost: strength=${it.roundedStrength}")
                            }
                        }
                        resultBundle.putBoolean("isVirtualizerSupported", isVirtualizerSupported)
                        if (isVirtualizerSupported) {
                            virtualizer?.let {
                                resultBundle.putShort("virtualizerStrength", it.roundedStrength)
                                android.util.Log.d("MusicDiagnostics", "Reporting Virtualizer: strength=${it.roundedStrength}")
                            }
                        }
                        resultBundle.putBoolean("isLoudnessEnhancerSupported", isLoudnessEnhancerSupported)
                        if (isLoudnessEnhancerSupported) {
                            loudnessEnhancer?.let {
                                resultBundle.putInt("loudnessGain", it.targetGain.toInt())
                                android.util.Log.d("MusicDiagnostics", "Reporting Loudness: gain=${it.targetGain}")
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                    }
                    "SET_BASS_BOOST" -> {
                        val strength = args.getShort("strength")
                        android.util.Log.d("MusicDiagnostics", "Command received: SET_BASS_BOOST $strength")
                        bassBoost?.setStrength(strength)
                        android.util.Log.d("MusicDiagnostics", "BassBoost updated: strength=${bassBoost?.roundedStrength}, hasControl=${bassBoost?.hasControl()}")
                        saveEqState()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "SET_VIRTUALIZER" -> {
                        val strength = args.getShort("strength")
                        android.util.Log.d("MusicDiagnostics", "Command received: SET_VIRTUALIZER $strength")
                        virtualizer?.setStrength(strength)
                        android.util.Log.d("MusicDiagnostics", "Virtualizer updated: strength=${virtualizer?.roundedStrength}, hasControl=${virtualizer?.hasControl()}")
                        saveEqState()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "SET_LOUDNESS" -> {
                        val gain = args.getInt("gain")
                        android.util.Log.d("MusicDiagnostics", "Command received: SET_LOUDNESS $gain")
                        loudnessEnhancer?.setTargetGain(gain)
                        android.util.Log.d("MusicDiagnostics", "LoudnessEnhancer updated: gain=${loudnessEnhancer?.targetGain}")
                        saveEqState()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "SET_CROSSFADE" -> {
                        val duration = args.getLong("duration")
                        crossfadeDurationMs = duration
                        getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                            .putLong("crossfade_duration", duration).apply()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "SET_EQ_ENABLED" -> {
                        val enabled = args.getBoolean("enabled")
                        android.util.Log.d("MusicDiagnostics", "Command received: SET_EQ_ENABLED $enabled")
                        equalizer?.enabled = enabled
                        bassBoost?.enabled = enabled
                        virtualizer?.enabled = enabled
                        loudnessEnhancer?.enabled = enabled
                        getSharedPreferences("equalizer_state", Context.MODE_PRIVATE).edit()
                            .putBoolean("is_enabled", enabled).apply()
                        android.util.Log.d("MusicDiagnostics", "All effects enabled state updated to $enabled. Equalizer hasControl: ${equalizer?.hasControl()}")
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

    private fun saveEqState() {
        val sharedPrefs = getSharedPreferences("equalizer_state", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt()
                putInt("num_bands", numBands)
                for (i in 0 until numBands) {
                    putInt("band_$i", eq.getBandLevel(i.toShort()).toInt())
                }
            }
            bassBoost?.let { putInt("bass_boost", it.roundedStrength.toInt()) }
            virtualizer?.let { putInt("virtualizer", it.roundedStrength.toInt()) }
            loudnessEnhancer?.let { putInt("loudness", it.targetGain.toInt()) }
            apply()
        }
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

    @OptIn(UnstableApi::class)
    private fun initEqualizer(audioSessionId: Int) {
        android.util.Log.d("MusicDiagnostics", "initEqualizer: Attempting to initialize effects for session $audioSessionId")
        android.util.Log.d("MusicDiagnostics", "ExoPlayer AudioSessionId according to player: ${exoPlayer.audioSessionId}")
        
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()

            // Reset availability flags
            isBassBoostSupported = false
            isVirtualizerSupported = false
            isLoudnessEnhancerSupported = false
            
            // Priority 0 is default and usually enough. 
            // We use the specific sessionId from onAudioSessionIdChanged
            equalizer = Equalizer(1000, audioSessionId).apply {
                enabled = true
            }
            
            android.util.Log.d("MusicDiagnostics", "--- STEP 1: Initial Effect Status ---")
            android.util.Log.d("MusicDiagnostics", "Equalizer Initialized: session=$audioSessionId, hasControl=${equalizer?.hasControl()}, bands=${equalizer?.numberOfBands}")
            
            try {
                bassBoost = BassBoost(1000, audioSessionId).apply {
                    enabled = true
                    isBassBoostSupported = true
                    android.util.Log.d("MusicDiagnostics", "BassBoost Initialized: hasControl=${hasControl()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicDiagnostics", "BassBoost initialization failed", e)
            }
            
            try {
                virtualizer = Virtualizer(1000, audioSessionId).apply {
                    enabled = true
                    isVirtualizerSupported = true
                    android.util.Log.d("MusicDiagnostics", "Virtualizer Initialized: hasControl=${hasControl()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicDiagnostics", "Virtualizer initialization failed", e)
            }

            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                    isLoudnessEnhancerSupported = true
                    android.util.Log.d("MusicDiagnostics", "LoudnessEnhancer Initialized")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicDiagnostics", "LoudnessEnhancer initialization failed", e)
            }
            
            // Restore settings
            val sharedPrefs = getSharedPreferences("equalizer_state", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("is_enabled", true)
            
            equalizer?.enabled = isEnabled
            bassBoost?.enabled = isEnabled
            virtualizer?.enabled = isEnabled
            loudnessEnhancer?.enabled = isEnabled

            val numBands = equalizer?.numberOfBands ?: 0
            for (i in 0 until numBands) {
                val level = sharedPrefs.getInt("band_$i", 0).toShort()
                try {
                    equalizer?.setBandLevel(i.toShort(), level)
                    android.util.Log.d("MusicDiagnostics", "Restored Band $i to $level. Verification: ${equalizer?.getBandLevel(i.toShort())}")
                } catch (e: Exception) {
                    android.util.Log.e("MusicDiagnostics", "Failed to set Band $i", e)
                }
            }
            
            try {
                if (isBassBoostSupported) {
                    val strength = sharedPrefs.getInt("bass_boost", 0).toShort()
                    bassBoost?.setStrength(strength)
                    android.util.Log.d("MusicDiagnostics", "BassBoost Strength Restored: $strength")
                }
                if (isVirtualizerSupported) {
                    val strength = sharedPrefs.getInt("virtualizer", 0).toShort()
                    virtualizer?.setStrength(strength)
                    android.util.Log.d("MusicDiagnostics", "Virtualizer Strength Restored: $strength")
                }
                if (isLoudnessEnhancerSupported) {
                    val gain = sharedPrefs.getInt("loudness", 0)
                    loudnessEnhancer?.setTargetGain(gain)
                    android.util.Log.d("MusicDiagnostics", "Loudness Gain Restored: $gain")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicDiagnostics", "Failed to set effect strengths", e)
            }
            
            android.util.Log.d("MusicDiagnostics", "Audio Effects initialization complete. Master Enabled: $isEnabled")
        } catch (e: Exception) {
            android.util.Log.e("MusicDiagnostics", "Critical failure in initEqualizer", e)
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

        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
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
