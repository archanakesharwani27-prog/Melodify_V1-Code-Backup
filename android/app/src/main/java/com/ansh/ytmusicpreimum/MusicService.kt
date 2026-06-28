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
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var crossfadeEnabled = true 

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                initEqualizer(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    fadeIn(2000)
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startBackgroundCheck()
                }
            }
        })

        startPositionMonitor()

        val callback = object : MediaSession.Callback {
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
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(callback)
            .build()
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
                if (exoPlayer.isPlaying && crossfadeEnabled) {
                    val duration = exoPlayer.duration
                    val position = exoPlayer.currentPosition
                    if (duration > 0 && (duration - position) <= 3500) { 
                        fadeOut(3000)
                        delay(4000)
                    }
                }
            }
        }
    }

    private fun fadeIn(durationMs: Long) {
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
