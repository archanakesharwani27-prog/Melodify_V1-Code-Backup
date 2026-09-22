package com.ansh.ytmusicpreimum

import android.content.Intent
import android.content.Context
import android.os.PowerManager
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import android.os.Bundle
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.media3.session.DefaultMediaNotificationProvider
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

    companion object {
        var instance: MusicService? = null
            private set

        fun reinitVisualizer() {
            instance?.let { service ->
                val sessionId = service.exoPlayer.audioSessionId
                service.initVisualizer(sessionId)
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var audioVisualizer: Visualizer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var crossfadeDurationMs = 0L // 0 means disabled
    private var pauseTimestamp = 0L

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        HapticBassManager.init(applicationContext)
        AmbientSoundscapeManager.init(applicationContext)

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

        initAudioEffects(exoPlayer.audioSessionId)

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                initAudioEffects(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                initAudioEffects()
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
                if (playbackState == Player.STATE_READY) {
                    initAudioEffects()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateLocks(isPlaying)
                if (isPlaying) {
                    initAudioEffects()
                    // Smart Rewind: if paused for >12s, rewind 3s to restore context
                    if (pauseTimestamp > 0L) {
                        val elapsed = System.currentTimeMillis() - pauseTimestamp
                        if (elapsed > 12_000L && exoPlayer.currentPosition > 4_000L) {
                            val target = (exoPlayer.currentPosition - 3_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                        }
                        pauseTimestamp = 0L
                    }
                } else {
                    pauseTimestamp = System.currentTimeMillis()
                }
                try {
                    if (audioVisualizer == null && isPlaying) {
                        initVisualizer(exoPlayer.audioSessionId)
                    } else {
                        audioVisualizer?.enabled = isPlaying
                    }
                } catch (t: Throwable) {
                    android.util.Log.w("MusicService", "Failed to toggle visualizer state", t)
                }
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

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
                    true
                } else {
                    super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean {
                return true
            }

            override fun hasPreviousMediaItem(): Boolean {
                return true
            }

            override fun seekToNext() {
                if (super.hasNextMediaItem()) {
                    super.seekToNext()
                } else {
                    val intent = Intent("${packageName}.ACTION_SKIP_NEXT")
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                }
            }

            override fun seekToPrevious() {
                if (super.hasPreviousMediaItem()) {
                    super.seekToPrevious()
                } else {
                    val intent = Intent("${packageName}.ACTION_SKIP_PREV")
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                }
            }
        }

        val channelId = "melodify_playback_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Melodify Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Media playback controls and status"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(callback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(channelId)
                .build()
        )
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

    fun initAudioEffects(audioSessionId: Int = 0) {
        val targetSession = if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
            audioSessionId
        } else if (::exoPlayer.isInitialized && exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId != 0) {
            exoPlayer.audioSessionId
        } else {
            0
        }

        try {
            AudioEffectsManager.init(applicationContext, targetSession)
            if (targetSession != 0) {
                initVisualizer(targetSession)
                try {
                    val intent = Intent(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, targetSession)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    sendBroadcast(intent)
                } catch (t: Throwable) {
                    android.util.Log.w("MusicService", "Failed to broadcast audio effect session", t)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MusicService", "Failed to init AudioEffectsManager", t)
        }
    }

    fun initVisualizer(audioSessionId: Int) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("MusicService", "RECORD_AUDIO permission not granted, hardware visualizer bypassed")
            return
        }

        try {
            audioVisualizer?.let { old ->
                try { old.enabled = false } catch (t: Throwable) {}
                try { old.release() } catch (t: Throwable) {}
            }
            audioVisualizer = null

            val targetSession = if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) audioSessionId else 0
            val v = try {
                Visualizer(targetSession)
            } catch (e: Exception) {
                if (targetSession != 0) {
                    Visualizer(0)
                } else {
                    throw e
                }
            }
            val range = Visualizer.getCaptureSizeRange()
            if (range != null && range.size >= 2) {
                val targetSize = 512.coerceIn(range[0], range[1])
                v.captureSize = targetSize
            }
            val maxRate = Visualizer.getMaxCaptureRate()
            val rate = if (maxRate > 0) maxRate.coerceAtMost(20000) else 20000

            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    waveform?.let { VisualizerData.waveformData.value = it.copyOf() }
                }

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    fft?.let {
                        VisualizerData.fftData.value = it.copyOf()
                        HapticBassManager.processFft(it)
                    }
                }
            }, rate, true, true)

            try {
                v.enabled = exoPlayer.isPlaying
            } catch (t: Throwable) {}

            audioVisualizer = v
            android.util.Log.i("MusicService", "Visualizer initialized successfully for session $targetSession at rate $rate mHz")
        } catch (t: Throwable) {
            android.util.Log.e("MusicService", "Failed to init Visualizer safely", t)
            audioVisualizer = null
        }
    }


    private fun updateLocks(isPlaying: Boolean) {
        try {
            if (isPlaying) {
                if (wakeLock == null) {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Melodify:PlaybackWakeLock")?.apply {
                        setReferenceCounted(false)
                    }
                }
                wakeLock?.acquire(60 * 60 * 1000L) // 60 min safety timeout

                if (wifiLock == null) {
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Melodify:PlaybackWifiLock")?.apply {
                        setReferenceCounted(false)
                    }
                }
                wifiLock?.acquire()
            } else {
                wakeLock?.let { if (it.isHeld) it.release() }
                wifiLock?.let { if (it.isHeld) it.release() }
            }
        } catch (t: Throwable) {
            android.util.Log.w("MusicService", "Failed to update wake/wifi locks", t)
        }
    }

    private fun startPositionMonitor() {
        serviceScope.launch {
            while (isActive) {
                delay(200)
                if (exoPlayer.isPlaying) {
                    val position = exoPlayer.currentPosition
                    val loopTarget = ABLooperManager.checkLoop(position)
                    if (loopTarget != null) {
                        exoPlayer.seekTo(loopTarget)
                    }

                    if (crossfadeDurationMs > 0) {
                        val duration = exoPlayer.duration
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
        try {
            val sessionId = exoPlayer.audioSessionId
            if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                val intent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                }
                sendBroadcast(intent)
            }
        } catch (t: Throwable) {}

        try {
            audioVisualizer?.enabled = false
            audioVisualizer?.release()
        } catch (t: Throwable) {}
        audioVisualizer = null
        AudioEffectsManager.release()
        AmbientSoundscapeManager.stop()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (t: Throwable) {}
        wakeLock = null
        wifiLock = null
        if (instance == this) {
            instance = null
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
