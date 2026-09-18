package com.ansh.ytmusicpreimum

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0
    private var prefs: SharedPreferences? = null

    private val _currentPreset = MutableStateFlow("Flat")
    val currentPreset: StateFlow<String> = _currentPreset

    val AVAILABLE_PRESETS = listOf(
        "Flat",
        "Pop",
        "Rock",
        "DJ / Dance",
        "Bass Boost",
        "Deep Bass",
        "3D Surrounding Sound",
        "Dolby Atmos / Studio"
    )

    fun init(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == android.media.AudioManager.ERROR) return
        currentSessionId = audioSessionId

        if (prefs == null) {
            prefs = context.getSharedPreferences("audio_effects_prefs", Context.MODE_PRIVATE)
            _currentPreset.value = prefs?.getString("selected_preset", "Flat") ?: "Flat"
        }

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            android.util.Log.e("AudioEffectsManager", "Failed to init Equalizer", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            android.util.Log.e("AudioEffectsManager", "Failed to init BassBoost", e)
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            android.util.Log.e("AudioEffectsManager", "Failed to init Virtualizer", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply { enabled = true }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioEffectsManager", "Failed to init LoudnessEnhancer", e)
        }

        applyPreset(_currentPreset.value)
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        prefs?.edit()?.putString("selected_preset", presetName)?.apply()

        try {
            when (presetName) {
                "Flat" -> {
                    resetEq()
                    bassBoost?.setStrength(0)
                    virtualizer?.setStrength(0)
                    setLoudness(0)
                }
                "Pop" -> {
                    // Crisp vocals: slight bass cut, boosted mids and highs
                    setEqBands(-100, 250, 450, 300, -100)
                    bassBoost?.setStrength(200)
                    virtualizer?.setStrength(150)
                    setLoudness(0)
                }
                "Rock" -> {
                    // Classic V-shape: punchy bass and sharp treble, slightly scooped mids
                    setEqBands(500, 250, -150, 350, 550)
                    bassBoost?.setStrength(400)
                    virtualizer?.setStrength(250)
                    setLoudness(0)
                }
                "DJ / Dance" -> {
                    // Club mix: boosted 60Hz and high-end crispness with fast dynamics
                    setEqBands(650, 400, 0, 300, 600)
                    bassBoost?.setStrength(600)
                    virtualizer?.setStrength(350)
                    setLoudness(150)
                }
                "Bass Boost" -> {
                    // Focused warm bass enhancement
                    setEqBands(600, 400, 100, 0, -100)
                    bassBoost?.setStrength(650)
                    virtualizer?.setStrength(100)
                    setLoudness(0)
                }
                "Deep Bass" -> {
                    // Heavy subwoofer sub-bass (60Hz + 230Hz max gain + 900 strength hardware BassBoost)
                    setEqBands(900, 600, 150, 0, -150)
                    bassBoost?.setStrength(950)
                    virtualizer?.setStrength(150)
                    setLoudness(100)
                }
                "3D Surrounding Sound" -> {
                    // Maximum 3D spatial field widening
                    setEqBands(200, 100, 200, 400, 500)
                    bassBoost?.setStrength(300)
                    virtualizer?.setStrength(1000)
                    setLoudness(100)
                }
                "Dolby Atmos / Studio" -> {
                    // Acoustic master: LoudnessEnhancer target gain + high-fidelity curve + spatial depth
                    setEqBands(350, 200, 100, 300, 450)
                    bassBoost?.setStrength(450)
                    virtualizer?.setStrength(650)
                    setLoudness(350) // +3.5 dB professional studio gain
                }
                else -> {
                    resetEq()
                    bassBoost?.setStrength(0)
                    virtualizer?.setStrength(0)
                    setLoudness(0)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioEffectsManager", "Error applying preset $presetName", e)
        }
    }

    private fun setEqBands(vararg millibels: Short) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange // e.g. [-1500, 1500]
        val minLevel = range?.get(0) ?: -1500
        val maxLevel = range?.get(1) ?: 1500

        for (i in 0 until numBands) {
            val level = if (i < millibels.size) millibels[i] else 0.toShort()
            val clamped = level.coerceIn(minLevel, maxLevel)
            try {
                eq.setBandLevel(i.toShort(), clamped)
            } catch (e: Exception) {
                // Ignore band level exception
            }
        }
    }

    private fun resetEq() {
        val eq = equalizer ?: return
        for (i in 0 until eq.numberOfBands.toInt()) {
            try {
                eq.setBandLevel(i.toShort(), 0)
            } catch (e: Exception) {}
        }
    }

    private fun setLoudness(gainmB: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val le = loudnessEnhancer ?: return
                if (gainmB > 0) {
                    le.setTargetGain(gainmB)
                    le.enabled = true
                } else {
                    le.setTargetGain(0)
                    le.enabled = false
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioEffectsManager", "Error setting loudness gain", e)
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {}

        try {
            bassBoost?.release()
            bassBoost = null
        } catch (e: Exception) {}

        try {
            virtualizer?.release()
            virtualizer = null
        } catch (e: Exception) {}

        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (e: Exception) {}
    }
}
