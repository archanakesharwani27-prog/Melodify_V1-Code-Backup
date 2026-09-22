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
import kotlinx.coroutines.flow.asStateFlow

object AudioEffectsManager {
    private const val TAG = "AudioEffectsManager"
    private const val PREFS_NAME = "audio_effects_prefs"
    private const val PRIORITY = 1000 // High priority to take precedence over OEM effects

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0
    private var prefs: SharedPreferences? = null

    // Reactive UI States
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow("Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    // 5 Band frequencies in Hz (e.g. 60, 230, 910, 3600, 14000)
    private val _bandCenterFreqs = MutableStateFlow(listOf(60, 230, 910, 3600, 14000))
    val bandCenterFreqs: StateFlow<List<Int>> = _bandCenterFreqs.asStateFlow()

    // Band levels in millibels (-1500 to +1500 mB, where 100mB = 1dB)
    private val _bandLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 to 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 to 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _loudnessGain = MutableStateFlow(0) // 0 to 800 mB (+8 dB)
    val loudnessGain: StateFlow<Int> = _loudnessGain.asStateFlow()

    var minBandLevel: Int = -1500
        private set
    var maxBandLevel: Int = 1500
        private set

    val AVAILABLE_PRESETS = listOf(
        "Flat",
        "Bass Boost",
        "Deep Bass",
        "Rock",
        "Pop",
        "DJ / Dance",
        "Vocal Clarity",
        "3D Surrounding Sound",
        "Dolby Atmos / Studio",
        "Custom"
    )

    fun init(context: Context, audioSessionId: Int) {
        val targetSession = if (audioSessionId > 0 && audioSessionId != android.media.AudioManager.ERROR) {
            audioSessionId
        } else {
            0
        }

        if (prefs == null) {
            try {
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                loadSavedSettings()
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "Error reading audio_effects_prefs", t)
            }
        }

        // If already initialized for this exact session and equalizer is active, just reapply
        if (currentSessionId == targetSession && equalizer != null) {
            applyCurrentStateToHardware()
            return
        }

        currentSessionId = targetSession
        release()

        // 1. Equalizer initialization (try target session, fallback to 0; try high priority then normal priority)
        equalizer = tryInitEffect("Equalizer-Pri1000") {
            Equalizer(PRIORITY, targetSession)
        } ?: tryInitEffect("Equalizer-Pri0") {
            Equalizer(0, targetSession)
        } ?: tryInitEffect("Equalizer-FallbackSession0") {
            if (targetSession != 0) Equalizer(0, 0) else null
        }

        equalizer?.let { eq ->
            try {
                val range = eq.bandLevelRange
                if (range != null && range.size >= 2) {
                    minBandLevel = range[0].toInt()
                    maxBandLevel = range[1].toInt()
                }

                val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
                val freqs = mutableListOf<Int>()
                val levels = mutableListOf<Int>()

                for (i in 0 until numBands) {
                    val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000
                    freqs.add(centerFreqHz)
                    val savedLevel = if (i < _bandLevels.value.size && _bandLevels.value[i] != 0) {
                        _bandLevels.value[i]
                    } else {
                        prefs?.getInt("eq_band_$i", 0) ?: 0
                    }
                    levels.add(savedLevel)
                }

                _bandCenterFreqs.value = freqs
                _bandLevels.value = levels
                eq.enabled = _isEnabled.value
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Error configuring equalizer properties", t)
            }
        }

        // 2. BassBoost initialization
        bassBoost = tryInitEffect("BassBoost-Pri1000") {
            BassBoost(PRIORITY, targetSession)
        } ?: tryInitEffect("BassBoost-Pri0") {
            BassBoost(0, targetSession)
        } ?: tryInitEffect("BassBoost-FallbackSession0") {
            if (targetSession != 0) BassBoost(0, 0) else null
        }
        bassBoost?.let { bb ->
            try {
                bb.enabled = _isEnabled.value && _bassBoostStrength.value > 0
            } catch (t: Throwable) {}
        }

        // 3. Virtualizer initialization
        virtualizer = tryInitEffect("Virtualizer-Pri1000") {
            Virtualizer(PRIORITY, targetSession)
        } ?: tryInitEffect("Virtualizer-Pri0") {
            Virtualizer(0, targetSession)
        } ?: tryInitEffect("Virtualizer-FallbackSession0") {
            if (targetSession != 0) Virtualizer(0, 0) else null
        }
        virtualizer?.let { vz ->
            try {
                vz.enabled = _isEnabled.value && _virtualizerStrength.value > 0
            } catch (t: Throwable) {}
        }

        // 4. LoudnessEnhancer (API 19+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loudnessEnhancer = tryInitEffect("LoudnessEnhancer") {
                LoudnessEnhancer(targetSession)
            } ?: tryInitEffect("LoudnessEnhancer-Fallback0") {
                if (targetSession != 0) LoudnessEnhancer(0) else null
            }
            loudnessEnhancer?.let { le ->
                try {
                    le.enabled = _isEnabled.value && _loudnessGain.value > 0
                } catch (t: Throwable) {}
            }
        }

        val savedPreset = _currentPreset.value
        if (savedPreset.isNotEmpty() && savedPreset != "Custom") {
            applyPreset(savedPreset)
        } else {
            applyCurrentStateToHardware()
        }

        android.util.Log.i(TAG, "Initialized successfully for session $targetSession (eq=${equalizer != null}, bb=${bassBoost != null}, vz=${virtualizer != null}, le=${loudnessEnhancer != null}, preset=$savedPreset)")
    }

    private fun <T> tryInitEffect(name: String, initBlock: () -> T?): T? {
        return try {
            initBlock()
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "Failed to init $name: ${t.message}")
            null
        }
    }

    private fun loadSavedSettings() {
        val p = prefs ?: return
        _isEnabled.value = p.getBoolean("effects_enabled", true)
        _currentPreset.value = p.getString("selected_preset", "Flat") ?: "Flat"
        _bassBoostStrength.value = p.getInt("bass_boost_strength", 0)
        _virtualizerStrength.value = p.getInt("virtualizer_strength", 0)
        _loudnessGain.value = p.getInt("loudness_gain", 0)

        val levels = mutableListOf<Int>()
        for (i in 0 until 5) {
            levels.add(p.getInt("eq_band_$i", 0))
        }
        _bandLevels.value = levels
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs?.edit()?.putBoolean("effects_enabled", enabled)?.apply()
        applyCurrentStateToHardware()
    }

    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val clamped = levelMilliBels.coerceIn(minBandLevel, maxBandLevel)
        val current = _bandLevels.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = clamped
            _bandLevels.value = current
            prefs?.edit()?.putInt("eq_band_$bandIndex", clamped)?.apply()
        }

        _currentPreset.value = "Custom"
        prefs?.edit()?.putString("selected_preset", "Custom")?.apply()

        if (_isEnabled.value) {
            try {
                equalizer?.let { eq ->
                    if (!eq.enabled) eq.enabled = true
                    eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
                }
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Error setting band $bandIndex level", t)
            }
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        prefs?.edit()?.putInt("bass_boost_strength", clamped)?.apply()

        if (_isEnabled.value) {
            try {
                bassBoost?.let { bb ->
                    if (bb.strengthSupported) {
                        bb.setStrength(clamped.toShort())
                    }
                    bb.enabled = clamped > 0
                }
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Error setting bass boost", t)
            }
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        prefs?.edit()?.putInt("virtualizer_strength", clamped)?.apply()

        if (_isEnabled.value) {
            try {
                virtualizer?.let { vz ->
                    if (vz.strengthSupported) {
                        vz.setStrength(clamped.toShort())
                    }
                    vz.enabled = clamped > 0
                }
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Error setting virtualizer", t)
            }
        }
    }

    fun setLoudness(gainmB: Int) {
        val clamped = gainmB.coerceIn(0, 800)
        _loudnessGain.value = clamped
        prefs?.edit()?.putInt("loudness_gain", clamped)?.apply()

        if (_isEnabled.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                loudnessEnhancer?.let { le ->
                    if (clamped > 0) {
                        le.setTargetGain(clamped)
                        le.enabled = true
                    } else {
                        le.setTargetGain(0)
                        le.enabled = false
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.w(TAG, "Error setting loudness enhancer", t)
            }
        }
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        prefs?.edit()?.putString("selected_preset", presetName)?.apply()

        when (presetName) {
            "Flat" -> {
                setPresetBands(0, 0, 0, 0, 0)
                setBassBoost(0)
                setVirtualizer(0)
                setLoudness(0)
            }
            "Bass Boost" -> {
                // Strong warm bass curve (+7dB, +4dB, +1dB, 0dB, -1dB)
                setPresetBands(700, 450, 100, 0, -100)
                setBassBoost(750)
                setVirtualizer(150)
                setLoudness(0)
            }
            "Deep Bass" -> {
                // Subwoofer punch (+10dB, +7dB, +2dB, 0dB, -2dB) + BassBoost 950
                setPresetBands(1000, 700, 200, 0, -200)
                setBassBoost(950)
                setVirtualizer(200)
                setLoudness(150)
            }
            "Rock" -> {
                // V-shaped dynamic punch (+6dB bass, -2dB mids, +6dB highs)
                setPresetBands(600, 300, -200, 400, 650)
                setBassBoost(500)
                setVirtualizer(300)
                setLoudness(0)
            }
            "Pop" -> {
                // Crisp vocals & lively highs (-1dB bass, +3dB mids, +5dB upper mids, +2dB highs)
                setPresetBands(-100, 250, 500, 350, 200)
                setBassBoost(300)
                setVirtualizer(250)
                setLoudness(0)
            }
            "DJ / Dance" -> {
                // Club mix: boosted sub-bass, tight punch, sizzling highs, loudness boost
                setPresetBands(800, 500, 0, 400, 700)
                setBassBoost(700)
                setVirtualizer(400)
                setLoudness(250)
            }
            "Vocal Clarity" -> {
                // Speech & lyric emphasis: cut low rumbles, boost vocal presence
                setPresetBands(-400, -100, 600, 500, 100)
                setBassBoost(0)
                setVirtualizer(200)
                setLoudness(100)
            }
            "3D Surrounding Sound" -> {
                // Spatial field widening with virtualizer at maximum
                setPresetBands(300, 150, 250, 450, 600)
                setBassBoost(400)
                setVirtualizer(1000)
                setLoudness(150)
            }
            "Dolby Atmos / Studio" -> {
                // Studio reference mastering curve + dynamic gain boost
                setPresetBands(400, 250, 150, 350, 500)
                setBassBoost(550)
                setVirtualizer(700)
                setLoudness(400) // +4.0 dB master gain boost
            }
            "Custom" -> {
                // Leave bands as manually configured
            }
        }
    }

    private fun setPresetBands(vararg presetLevels: Int) {
        val count = presetLevels.size
        val newLevels = _bandLevels.value.toMutableList()
        for (i in 0 until count) {
            val level = presetLevels[i].coerceIn(minBandLevel, maxBandLevel)
            if (i < newLevels.size) {
                newLevels[i] = level
            } else {
                newLevels.add(level)
            }
            prefs?.edit()?.putInt("eq_band_$i", level)?.apply()
        }
        _bandLevels.value = newLevels

        if (_isEnabled.value) {
            equalizer?.let { eq ->
                try {
                    if (!eq.enabled) eq.enabled = true
                    for (i in 0 until eq.numberOfBands.toInt()) {
                        val lvl = if (i < newLevels.size) newLevels[i] else 0
                        eq.setBandLevel(i.toShort(), lvl.coerceIn(minBandLevel, maxBandLevel).toShort())
                    }
                } catch (t: Throwable) {
                    android.util.Log.w(TAG, "Error applying preset bands", t)
                }
            }
        }
    }

    fun resetToFlat() {
        applyPreset("Flat")
    }

    private fun applyCurrentStateToHardware() {
        val enabled = _isEnabled.value

        try {
            equalizer?.let { eq ->
                eq.enabled = enabled
                if (enabled) {
                    val levels = _bandLevels.value
                    for (i in 0 until eq.numberOfBands.toInt()) {
                        val level = if (i < levels.size) levels[i] else 0
                        eq.setBandLevel(i.toShort(), level.coerceIn(minBandLevel, maxBandLevel).toShort())
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "Error applying equalizer state", t)
        }

        try {
            bassBoost?.let { bb ->
                bb.enabled = enabled && _bassBoostStrength.value > 0
                if (enabled && bb.strengthSupported) {
                    bb.setStrength(_bassBoostStrength.value.toShort())
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "Error applying bass boost state", t)
        }

        try {
            virtualizer?.let { vz ->
                vz.enabled = enabled && _virtualizerStrength.value > 0
                if (enabled && vz.strengthSupported) {
                    vz.setStrength(_virtualizerStrength.value.toShort())
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "Error applying virtualizer state", t)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer?.let { le ->
                    val gain = _loudnessGain.value
                    if (enabled && gain > 0) {
                        le.setTargetGain(gain)
                        le.enabled = true
                    } else {
                        le.setTargetGain(0)
                        le.enabled = false
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "Error applying loudness enhancer state", t)
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
