package com.ansh.ytmusicpreimum

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SleepTimerManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

    private val _isEndOfSong = MutableStateFlow(false)
    val isEndOfSong = _isEndOfSong.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalSecs = minutes * 60L
        _remainingSeconds.value = totalSecs
        _isActive.value = true
        _isEndOfSong.value = false

        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                val remaining = _remainingSeconds.value - 1
                _remainingSeconds.value = remaining

                // Gentle Volume Fade-Out: during the final 60 seconds, smoothly fade volume to 0
                if (remaining in 1..60) {
                    val fadeVolume = (remaining.toFloat() / 60f).coerceIn(0f, 1f)
                    MusicService.instance?.setVolume(fadeVolume)
                }

                if (remaining <= 0) {
                    // Time up: Pause playback, reset volume to 1.0f for next play session
                    MusicService.instance?.pausePlayback()
                    MusicService.instance?.setVolume(1.0f)
                    _isActive.value = false
                    _remainingSeconds.value = 0L
                    break
                }
            }
        }
    }

    fun startEndOfSongTimer() {
        cancelTimer()
        _isEndOfSong.value = true
        _isActive.value = true
        _remainingSeconds.value = 0L
    }

    fun onSongFinished() {
        if (_isEndOfSong.value) {
            MusicService.instance?.pausePlayback()
            MusicService.instance?.setVolume(1.0f)
            _isEndOfSong.value = false
            _isActive.value = false
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        MusicService.instance?.setVolume(1.0f)
        _remainingSeconds.value = 0L
        _isActive.value = false
        _isEndOfSong.value = false
    }

    fun formatRemainingTime(): String {
        if (_isEndOfSong.value) return "End of song"
        val totalSecs = _remainingSeconds.value
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
