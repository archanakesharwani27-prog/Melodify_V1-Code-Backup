package com.ansh.ytmusicpreimum

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ABLooperManager {
    private val _pointA = MutableStateFlow<Long?>(null)
    val pointA: StateFlow<Long?> = _pointA.asStateFlow()

    private val _pointB = MutableStateFlow<Long?>(null)
    val pointB: StateFlow<Long?> = _pointB.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    fun setPointA(positionMs: Long) {
        _pointA.value = positionMs
        val b = _pointB.value
        if (b != null && b <= positionMs) {
            _pointB.value = null
            _isLooping.value = false
        } else if (b != null) {
            _isLooping.value = true
        }
    }

    fun setPointB(positionMs: Long) {
        val a = _pointA.value ?: 0L
        if (positionMs > a) {
            _pointB.value = positionMs
            _isLooping.value = true
        }
    }

    fun toggleLoop() {
        if (_pointA.value != null && _pointB.value != null) {
            _isLooping.value = !_isLooping.value
        }
    }

    fun clearLoop() {
        _pointA.value = null
        _pointB.value = null
        _isLooping.value = false
    }

    /**
     * Checks if current position reached or exceeded pointB.
     * Returns pointA to seek to, or null if no loop jump is needed.
     */
    fun checkLoop(currentPositionMs: Long): Long? {
        if (!_isLooping.value) return null
        val a = _pointA.value ?: return null
        val b = _pointB.value ?: return null

        return if (currentPositionMs >= b) {
            a
        } else {
            null
        }
    }
}
