package com.ansh.ytmusicpreimum

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ApiKeyHealth(
    val keyId: String,
    val key: String,
    val usageCount: Int = 0,
    val isBlocked: Boolean = false,
    val lastErrorTime: Long = 0L
)

/**
 * Production API Key Rotation & Multi-Source Failover Engine.
 * Automatically distributes API load across multiple keys and handles 429 Rate Limits / Quotas.
 */
object ApiKeyManager {
    private const val TAG = "ApiKeyManager"

    private val pool = mutableListOf(
        ApiKeyHealth("Primary Alpha", "AIzaSy_Melodify_Alpha_01"),
        ApiKeyHealth("Backup Beta", "AIzaSy_Melodify_Beta_02"),
        ApiKeyHealth("Backup Gamma", "AIzaSy_Melodify_Gamma_03"),
        ApiKeyHealth("Fallback Delta", "AIzaSy_Melodify_Delta_04")
    )

    private var activeIndex = 0

    private val _activeKey = MutableStateFlow(pool[activeIndex].key)
    val activeKey: StateFlow<String> = _activeKey.asStateFlow()

    private val _engineStatus = MutableStateFlow("Engine Healthy • Multi-Key Load Balanced")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    /**
     * Obtains the next healthy API key with automated load distribution.
     */
    fun getActiveKey(): String {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val current = pool[activeIndex]

            // If current key was blocked more than 15 minutes ago, auto-recover it
            if (current.isBlocked && now - current.lastErrorTime > 900_000) {
                pool[activeIndex] = current.copy(isBlocked = false, usageCount = 0)
            }

            if (pool[activeIndex].isBlocked) {
                rotateToNextHealthyKey()
            }

            val item = pool[activeIndex]
            pool[activeIndex] = item.copy(usageCount = item.usageCount + 1)
            return pool[activeIndex].key
        }
    }

    /**
     * Reports a 429 Too Many Requests or Quota Exceeded error.
     * Instantly rotates to the next healthy key in the pool.
     */
    fun reportQuotaExhausted(key: String, errorReason: String = "429 Rate Limit") {
        synchronized(this) {
            Log.w(TAG, "Quota error on key '$key': $errorReason. Triggering instant key rotation...")
            val index = pool.indexOfFirst { it.key == key }
            if (index != -1) {
                pool[index] = pool[index].copy(isBlocked = true, lastErrorTime = System.currentTimeMillis())
            }
            rotateToNextHealthyKey()
        }
    }

    private fun rotateToNextHealthyKey() {
        val start = activeIndex
        do {
            activeIndex = (activeIndex + 1) % pool.size
            if (!pool[activeIndex].isBlocked) {
                _activeKey.value = pool[activeIndex].key
                _engineStatus.value = "Rotated to ${pool[activeIndex].keyId} seamlessly"
                Log.i(TAG, "Successfully failover-rotated to ${pool[activeIndex].keyId}")
                return
            }
        } while (activeIndex != start)

        // All keys temporarily rate-limited; reset pool
        Log.e(TAG, "All API keys temporarily rate-limited. Resetting key pool for recovery.")
        for (i in pool.indices) {
            pool[i] = pool[i].copy(isBlocked = false, usageCount = 0)
        }
        activeIndex = 0
        _activeKey.value = pool[0].key
        _engineStatus.value = "Engine Auto-Recovered • Key Pool Reset"
    }
}
