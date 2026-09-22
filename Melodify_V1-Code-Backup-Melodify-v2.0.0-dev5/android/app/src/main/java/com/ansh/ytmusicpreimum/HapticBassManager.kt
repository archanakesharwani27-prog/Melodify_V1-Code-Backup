package com.ansh.ytmusicpreimum

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

object HapticBassManager {
    private const val TAG = "HapticBassManager"
    private const val PREFS_NAME = "haptic_bass_prefs"

    private var vibrator: Vibrator? = null
    private var prefs: SharedPreferences? = null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _intensity = MutableStateFlow(0.7f) // 0.1f to 1.0f
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    private var lastBeatTimestamp = 0L
    private const val MIN_BEAT_INTERVAL_MS = 140L // Limits to ~430 BPM max to avoid constant buzzing
    private var averageBassEnergy = 20.0f

    fun init(context: Context) {
        if (prefs == null) {
            try {
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _isEnabled.value = prefs?.getBoolean("haptic_bass_enabled", false) ?: false
                _intensity.value = prefs?.getFloat("haptic_bass_intensity", 0.7f) ?: 0.7f
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Failed to load haptic bass preferences", t)
            }
        }

        if (vibrator == null) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Failed to obtain system Vibrator", t)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs?.edit()?.putBoolean("haptic_bass_enabled", enabled)?.apply()
    }

    fun setIntensity(value: Float) {
        val clamped = value.coerceIn(0.1f, 1.0f)
        _intensity.value = clamped
        prefs?.edit()?.putFloat("haptic_bass_intensity", clamped)?.apply()
    }

    fun processFft(fft: ByteArray) {
        if (!_isEnabled.value || vibrator == null || !vibrator!!.hasVibrator()) return
        if (fft.size < 8) return

        val now = System.currentTimeMillis()
        if (now - lastBeatTimestamp < MIN_BEAT_INTERVAL_MS) return

        // Bins 0, 1, 2 represent lowest frequencies (20Hz - 120Hz = sub-bass and kick drums)
        var currentBassEnergy = 0.0f
        var binCount = 0

        for (i in 2 until 8 step 2) {
            val r = fft[i].toFloat()
            val im = fft[i + 1].toFloat()
            val mag = hypot(r, im)
            currentBassEnergy += mag
            binCount++
        }

        if (binCount > 0) {
            currentBassEnergy /= binCount
        }

        // Dynamic thresholding: detect sudden amplitude spikes (kick drop)
        val threshold = averageBassEnergy * 1.55f
        if (currentBassEnergy > threshold && currentBassEnergy > 15f) {
            lastBeatTimestamp = now
            triggerHapticPulse()
        }

        // Exponential moving average to track changing song dynamics
        averageBassEnergy = 0.92f * averageBassEnergy + 0.08f * currentBassEnergy
    }

    private fun triggerHapticPulse() {
        val vib = vibrator ?: return
        try {
            val amp = (_intensity.value * 255).toInt().coerceIn(1, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Sharp, punchy 25ms acoustic kick pulse
                val effect = VibrationEffect.createOneShot(25L, amp)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(25L)
            }
        } catch (t: Throwable) {
            // Ignore minor vibration dispatch exceptions
        }
    }
}
