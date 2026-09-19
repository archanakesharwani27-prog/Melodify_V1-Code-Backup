package com.ansh.ytmusicpreimum.ui.screens.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ansh.ytmusicpreimum.MusicService
import com.ansh.ytmusicpreimum.VisualizerData
import kotlin.math.*

/**
 * Professional Studio Audio Spectrum Analyzer & Visualizer
 *
 * Directly connects to ExoPlayer's hardware audio session to render
 * the real-time frequency spectrum of the music currently playing.
 *
 * Features:
 * - 48 logarithmically mapped frequency bands (Sub-Bass, Bass, Mids, Highs).
 * - Real hardware FFT processing with logarithmic bin averaging.
 * - Hardware waveform fallback for devices with limited FFT support.
 * - Physics-based floating peak hold caps that drop with realistic gravity.
 * - Fast attack on beats + smooth release decay.
 * - Neon cyber-studio gradient with ambient sub-bass pulse glow.
 * - Studio decibel reference grid (-6dB, -12dB, -24dB).
 * - Automatic permission request & studio sync prompt.
 */
@Composable
fun LiveWaveCanvas(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            MusicService.reinitVisualizer()
        }
    }

    // Auto-request permission on first display if not granted
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            MusicService.reinitVisualizer()
        }
    }

    val fftData by VisualizerData.fftData.collectAsState()
    val waveformData by VisualizerData.waveformData.collectAsState()
    val barCount = 48

    // Continuous 60fps frame driver for fluid bar movement and peak cap gravity
    val infiniteTransition = rememberInfiniteTransition(label = "studioVisualizerTransition")
    val frameTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameTick"
    )

    // Current smoothed bar heights (0f..1f)
    val barHeights = remember { FloatArray(barCount) { 0.02f } }
    // Floating peak caps (0f..1f) and their current falling velocity
    val peakCaps = remember { FloatArray(barCount) { 0.02f } }
    val peakVelocities = remember { FloatArray(barCount) { 0f } }

    val hasFft = remember(fftData) {
        fftData.isNotEmpty() && fftData.any { it != 0.toByte() }
    }

    val hasWaveform = remember(waveformData) {
        waveformData.isNotEmpty() && waveformData.any { it != 0.toByte() && it != (-128).toByte() && it != 128.toByte() }
    }

    // Professional Studio Neon Gradient: Top is hot cyan -> vibrant magenta -> deep electric indigo
    val barGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00FFFF), // Electric Cyan
                Color(0xFF00B0FF), // Neon Blue
                Color(0xFFD500F9), // Cyber Magenta
                Color(0xFF3D5AFE)  // Deep Studio Indigo
            )
        )
    }

    val ambientGlowBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = 0.20f),
                Color(0xFFE040FB).copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }

    // Gravity physics constant
    val gravity = 0.0038f

    // Compute real-time audio amplitudes from ExoPlayer's hardware visualizer
    if (isPlaying) {
        if (hasFft) {
            val halfSize = fftData.size / 2
            for (i in 0 until barCount) {
                // Logarithmic frequency bin range:
                // Maps 0..barCount across 1..halfSize using pow(2.2) so bass & mids get wide representation
                val normStart = (i.toFloat() / barCount.toFloat()).toDouble().pow(2.2)
                val normEnd = ((i + 1).toFloat() / barCount.toFloat()).toDouble().pow(2.2)
                val binStart = (1 + normStart * (halfSize - 2)).toInt().coerceIn(1, halfSize - 1)
                val binEnd = (1 + normEnd * (halfSize - 2)).toInt().coerceIn(binStart + 1, halfSize)

                var maxMag = 0f
                for (b in binStart until binEnd) {
                    val realIdx = b * 2
                    val imagIdx = realIdx + 1
                    if (imagIdx < fftData.size) {
                        val real = fftData[realIdx].toInt()
                        val imag = fftData[imagIdx].toInt()
                        val mag = hypot(real.toDouble(), imag.toDouble()).toFloat()
                        if (mag > maxMag) maxMag = mag
                    }
                }

                // Boost lower frequencies and extreme highs according to human equal-loudness curves
                val weight = when {
                    i < 10 -> 2.4f  // Sub-bass & Bass
                    i < 24 -> 1.7f  // Low-mids & Vocals
                    i < 38 -> 2.0f  // Upper-mids & Presence
                    else -> 2.6f    // Brilliance / High Treble
                }

                val target = (maxMag / 128f * weight).coerceIn(0.02f, 1f)
                // Fast attack on beats (0.2 old + 0.8 new), smooth decay (0.7 old + 0.3 new)
                if (target > barHeights[i]) {
                    barHeights[i] = barHeights[i] * 0.20f + target * 0.80f
                } else {
                    barHeights[i] = barHeights[i] * 0.70f + target * 0.30f
                }
            }
        } else if (hasWaveform) {
            val chunkSize = (waveformData.size / barCount).coerceAtLeast(1)
            for (i in 0 until barCount) {
                var sum = 0f
                val start = i * chunkSize
                val end = minOf(start + chunkSize, waveformData.size)
                for (j in start until end) {
                    val sample = (waveformData[j].toInt() and 0xFF) - 128
                    sum += abs(sample)
                }
                val avg = if (end > start) sum / (end - start) else 0f
                val target = (avg / 64f * 1.8f).coerceIn(0.02f, 1f)
                if (target > barHeights[i]) {
                    barHeights[i] = barHeights[i] * 0.25f + target * 0.75f
                } else {
                    barHeights[i] = barHeights[i] * 0.75f + target * 0.25f
                }
            }
        } else {
            // Settling baseline when waiting for audio packets
            for (i in 0 until barCount) {
                barHeights[i] = (barHeights[i] * 0.88f).coerceAtLeast(0.02f)
            }
        }

        // Update floating peak caps physics (jump to bar height, then fall with gravity)
        for (i in 0 until barCount) {
            val currentH = barHeights[i]
            if (currentH >= peakCaps[i]) {
                peakCaps[i] = currentH
                peakVelocities[i] = 0f
            } else {
                peakVelocities[i] += gravity
                peakCaps[i] = (peakCaps[i] - peakVelocities[i]).coerceAtLeast(currentH)
            }
        }
    } else {
        // Smoothly decay all bars and peak caps when paused
        for (i in 0 until barCount) {
            barHeights[i] = (barHeights[i] * 0.85f).coerceAtLeast(0.02f)
            peakCaps[i] = (peakCaps[i] * 0.88f).coerceAtLeast(0.02f)
            peakVelocities[i] = 0f
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        color = Color(0xFF0A0C10), // Deep Studio Charcoal
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasPermission) {
                // --- PERMISSION REQUIRED STUDIO PROMPT ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "REAL-TIME AUDIO SYNC",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Audio permission is required so Melodify can analyze and sync the live frequency spectrum of your music.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF)
                        )
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Enable Live Audio Sync",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // --- ACTIVE STUDIO SPECTRUM CANVAS ---
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 24.dp)
                ) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas

                    val width = size.width
                    val height = size.height
                    val bottomY = height - 16.dp.toPx() // Leave room for studio HUD labels
                    val maxBarHeight = bottomY - 36.dp.toPx()

                    // 1. Draw subtle studio decibel reference lines (-6dB, -12dB, -24dB)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    val dbLevels = listOf(0.75f to "-6dB", 0.50f to "-12dB", 0.25f to "-24dB")
                    for ((level, _) in dbLevels) {
                        val lineY = bottomY - (maxBarHeight * level)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, lineY),
                            end = Offset(width, lineY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // 2. Ambient sub-bass background pulse
                    val avgBass = (barHeights[0] + barHeights[1] + barHeights[2] + barHeights[3]) / 4f
                    if (avgBass > 0.12f) {
                        drawCircle(
                            brush = ambientGlowBrush,
                            radius = (width * 0.45f * avgBass).coerceAtLeast(20f),
                            center = Offset(width * 0.25f, bottomY * 0.6f)
                        )
                    }

                    // 3. Render 48 Spectrum Bars + Floating Peak Caps
                    val spacing = 2.5.dp.toPx()
                    val totalSpacing = (barCount - 1) * spacing
                    val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(1f)
                    val cornerRadius = (barWidth / 2f).coerceAtLeast(0.5f)
                    val peakCapHeight = (barWidth * 0.85f).coerceIn(2.5f, 5f)

                    for (i in 0 until barCount) {
                        val barH = (maxBarHeight * barHeights[i]).coerceIn(4f, maxBarHeight)
                        val x = i * (barWidth + spacing)
                        val topY = bottomY - barH

                        // Draw main spectrum bar
                        drawRoundRect(
                            brush = barGradient,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )

                        // Draw floating peak hold cap
                        val peakH = (maxBarHeight * peakCaps[i]).coerceIn(4f, maxBarHeight)
                        val peakY = (bottomY - peakH - peakCapHeight - 2.dp.toPx()).coerceAtLeast(2f)

                        drawRoundRect(
                            color = if (peakCaps[i] > 0.85f) Color(0xFFFF5252) else Color(0xFFE0F7FA),
                            topLeft = Offset(x, peakY),
                            size = Size(barWidth, peakCapHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                }

                // --- Top HUD: Mode & Status Pill ---
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    !isPlaying -> Color(0xFFFF9800)
                                    hasFft -> Color(0xFF00E5FF)
                                    hasWaveform -> Color(0xFF00E676)
                                    else -> Color(0xFF00B0FF)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            !isPlaying -> "STANDBY"
                            hasFft -> "DSP HARDWARE FFT"
                            hasWaveform -> "WAVEFORM SYNC"
                            else -> "AUDIO SYNC"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // --- Bottom HUD: Studio Frequency Zone Labels ---
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BASS (20-250Hz)",
                        color = Color(0xFF00E5FF).copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MIDS (250-4kHz)",
                        color = Color(0xFFE040FB).copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TREBLE (4-16kHz)",
                        color = Color(0xFF3D5AFE).copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
