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
        if (audioSessionId <= 0 || audioSessionId == android.media.AudioManager.ERROR) return
        if (currentSessionId == audioSessionId && equalizer != null) return

        currentSessionId = audioSessionId

        if (prefs == null) {
            try {
                prefs = context.getSharedPreferences("audio_effects_prefs", Context.MODE_PRIVATE)
                _currentPreset.value = prefs?.getString("selected_preset", "Flat") ?: "Flat"
            } catch (t: Throwable) {
                android.util.Log.e("AudioEffectsManager", "Error reading audio_effects_prefs", t)
            }
        }

        release()

        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            equalizer = eq
        } catch (t: Throwable) {
            android.util.Log.e("AudioEffectsManager", "Failed to init Equalizer for session $audioSessionId", t)
            equalizer = null
        }

        try {
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = true
            bassBoost = bb
        } catch (t: Throwable) {
            android.util.Log.e("AudioEffectsManager", "Failed to init BassBoost for session $audioSessionId", t)
            bassBoost = null
        }

        try {
            val vz = Virtualizer(0, audioSessionId)
            vz.enabled = true
            virtualizer = vz
        } catch (t: Throwable) {
            android.util.Log.e("AudioEffectsManager", "Failed to init Virtualizer for session $audioSessionId", t)
            virtualizer = null
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val le = LoudnessEnhancer(audioSessionId)
                le.enabled = true
                loudnessEnhancer = le
            }
        } catch (t: Throwable) {
            android.util.Log.e("AudioEffectsManager", "Failed to init LoudnessEnhancer for session $audioSessionId", t)
            loudnessEnhancer = null
        }

        applyPreset(_currentPreset.value)
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        try {
            prefs?.edit()?.putString("selected_preset", presetName)?.apply()
        } catch (t: Throwable) {}

        try {
            when (presetName) {
                "Flat" -> {
                    resetEq()
                    setBassBoost(0)
                    setVirtualizer(0)
                    setLoudness(0)
                }
                "Pop" -> {
                    // Crisp vocals: slight bass cut, boosted mids and highs
                    setEqBands(-100, 250, 450, 300, -100)
                    setBassBoost(200)
                    setVirtualizer(150)
                    setLoudness(0)
                }
                "Rock" -> {
                    // Classic V-shape: punchy bass and sharp treble, slightly scooped mids
                    setEqBands(500, 250, -150, 350, 550)
                    setBassBoost(400)
                    setVirtualizer(250)
                    setLoudness(0)
                }
                "DJ / Dance" -> {
                    // Club mix: boosted 60Hz and high-end crispness with fast dynamics
                    setEqBands(650, 400, 0, 300, 600)
                    setBassBoost(600)
                    setVirtualizer(350)
                    setLoudness(150)
                }
                "Bass Boost" -> {
                    // Focused warm bass enhancement
                    setEqBands(600, 400, 100, 0, -100)
                    setBassBoost(650)
                    setVirtualizer(100)
                    setLoudness(0)
                }
                "Deep Bass" -> {
                    // Heavy subwoofer sub-bass (60Hz + 230Hz max gain + 900 strength hardware BassBoost)
                    setEqBands(900, 600, 150, 0, -150)
                    setBassBoost(950)
                    setVirtualizer(150)
                    setLoudness(100)
                }
                "3D Surrounding Sound" -> {
                    // Maximum 3D spatial field widening
                    setEqBands(200, 100, 200, 400, 500)
                    setBassBoost(300)
                    setVirtualizer(1000)
                    setLoudness(100)
                }
                "Dolby Atmos / Studio" -> {
                    // Acoustic master: LoudnessEnhancer target gain + high-fidelity curve + spatial depth
                    setEqBands(350, 200, 100, 300, 450)
                    setBassBoost(450)
                    setVirtualizer(650)
                    setLoudness(350) // +3.5 dB professional studio gain
                }
                else -> {
                    resetEq()
                    setBassBoost(0)
                    setVirtualizer(0)
                    setLoudness(0)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("AudioEffectsManager", "Error applying preset $presetName", t)
        }
    }

    private fun setEqBands(vararg millibels: Short) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range?.get(0) ?: -1500
            val maxLevel = range?.get(1) ?: 1500

            for (i in 0 until numBands) {
                val level = if (i < millibels.size) millibels[i] else 0.toShort()
                val clamped = level.coerceIn(minLevel, maxLevel)
                try {
                    eq.setBandLevel(i.toShort(), clamped)
                } catch (t: Throwable) {}
            }
        } catch (t: Throwable) {
            android.util.Log.w("AudioEffectsManager", "Error in setEqBands", t)
        }
    }

    private fun resetEq() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                try {
                    eq.setBandLevel(i.toShort(), 0)
                } catch (t: Throwable) {}
            }
        } catch (t: Throwable) {
            android.util.Log.w("AudioEffectsManager", "Error in resetEq", t)
        }
    }

    private fun setBassBoost(strength: Short) {
        val bb = bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(strength)
            }
            bb.enabled = (strength > 0)
        } catch (t: Throwable) {
            android.util.Log.w("AudioEffectsManager", "Error setting bass boost", t)
        }
    }

    private fun setVirtualizer(strength: Short) {
        val vz = virtualizer ?: return
        try {
            if (vz.strengthSupported) {
                vz.setStrength(strength)
            }
            vz.enabled = (strength > 0)
        } catch (t: Throwable) {
            android.util.Log.w("AudioEffectsManager", "Error setting virtualizer", t)
        }
    }

    private fun setLoudness(gainmB: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val le = loudnessEnhancer ?: return
            try {
                if (gainmB > 0) {
                    le.setTargetGain(gainmB)
                    le.enabled = true
                } else {
                    le.setTargetGain(0)
                    le.enabled = false
                }
            } catch (t: Throwable) {
                android.util.Log.w("AudioEffectsManager", "Error setting loudness gain", t)
            }
        }
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (t: Throwable) {}
        equalizer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (t: Throwable) {}
        bassBoost = null

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (t: Throwable) {}
        virtualizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (t: Throwable) {}
        loudnessEnhancer = null
    }
}
