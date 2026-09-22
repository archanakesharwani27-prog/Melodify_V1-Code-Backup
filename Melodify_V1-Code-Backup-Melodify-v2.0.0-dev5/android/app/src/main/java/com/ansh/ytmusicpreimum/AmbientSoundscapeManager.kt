package com.ansh.ytmusicpreimum

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

object AmbientSoundscapeManager {
    private const val TAG = "AmbientSoundscape"
    private const val PREFS_NAME = "ambient_soundscape_prefs"
    private const val SAMPLE_RATE = 44100

    private val scope = CoroutineScope(Dispatchers.Default)
    private var synthJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var prefs: SharedPreferences? = null

    val AVAILABLE_SOUNDSCAPES = listOf("Off", "Rain 🌧️", "Fireplace 🔥", "Vinyl 📻", "Cafe ☕")

    private val _selectedSoundscape = MutableStateFlow("Off")
    val selectedSoundscape: StateFlow<String> = _selectedSoundscape.asStateFlow()

    private val _volume = MutableStateFlow(0.25f) // 0.05 to 0.6
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            try {
                prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _selectedSoundscape.value = prefs?.getString("selected_ambient", "Off") ?: "Off"
                _volume.value = prefs?.getFloat("ambient_volume", 0.25f) ?: 0.25f
            } catch (t: Throwable) {}
        }
    }

    fun selectSoundscape(name: String) {
        _selectedSoundscape.value = name
        prefs?.edit()?.putString("selected_ambient", name)?.apply()

        if (name == "Off") {
            stop()
        } else {
            startSynthesizer(name)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 0.6f)
        _volume.value = clamped
        prefs?.edit()?.putFloat("ambient_volume", clamped)?.apply()
        try {
            audioTrack?.setVolume(clamped)
        } catch (t: Throwable) {}
    }

    private fun startSynthesizer(mode: String) {
        stop()

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(_volume.value)
            track.play()
            audioTrack = track

            synthJob = scope.launch {
                val buffer = ShortArray(1024)
                val random = Random()

                // Filter states for Pink / Brown noise
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                var b3 = 0.0
                var b4 = 0.0
                var b5 = 0.0
                var b6 = 0.0
                var lastBrown = 0.0

                while (isActive) {
                    val currentMode = _selectedSoundscape.value
                    if (currentMode == "Off") break

                    for (i in buffer.indices) {
                        val white = random.nextDouble() * 2.0 - 1.0
                        var sample = 0.0

                        when (currentMode) {
                            "Rain 🌧️" -> {
                                // Pink noise (Paul Kellet filter) + random light droplet impacts
                                b0 = 0.99886 * b0 + white * 0.0555179
                                b1 = 0.99332 * b1 + white * 0.0750759
                                b2 = 0.96900 * b2 + white * 0.1538520
                                b3 = 0.86650 * b3 + white * 0.3104856
                                b4 = 0.55000 * b4 + white * 0.5329522
                                b5 = -0.7616 * b5 - white * 0.0168980
                                val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.09
                                b6 = white * 0.115926

                                val droplet = if (random.nextInt(3000) == 0) (random.nextDouble() * 0.4) else 0.0
                                sample = pink + droplet
                            }
                            "Fireplace 🔥" -> {
                                // Brown noise base + random timber crackle impulses
                                lastBrown = (lastBrown + (0.02 * white)) / 1.02
                                val rumble = lastBrown * 2.2
                                val crackle = if (random.nextInt(1200) == 0) {
                                    (random.nextDouble() * 2.0 - 1.0) * 0.7
                                } else 0.0
                                sample = rumble * 0.15 + crackle
                            }
                            "Vinyl 📻" -> {
                                // High-pass filtered hiss + subtle periodic dust clicks
                                val hiss = white * 0.04
                                val dustClick = if (random.nextInt(4500) == 0) {
                                    (random.nextDouble() * 0.5)
                                } else 0.0
                                sample = hiss + dustClick
                            }
                            "Cafe ☕" -> {
                                // Soft dual-band ambient murmur
                                lastBrown = (lastBrown + (0.04 * white)) / 1.04
                                sample = lastBrown * 0.18
                            }
                        }

                        val clampedSample = (sample.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                        buffer[i] = clampedSample
                    }

                    track.write(buffer, 0, buffer.size)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "Error running ambient synthesizer", t)
            stop()
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (t: Throwable) {}
        audioTrack = null
    }
}
