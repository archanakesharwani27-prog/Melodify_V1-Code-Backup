package com.ansh.ytmusicpreimum.ui.screens.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ansh.ytmusicpreimum.MusicService
import com.ansh.ytmusicpreimum.VisualizerData
import kotlin.math.*

enum class VisualizerTheme(val displayName: String, val icon: String) {
    NEON_BARS("Neon Bars", "📊"),
    FLUID_WAVE("Fluid Wave", "🌊"),
    PULSING_VINYL("Vinyl Disc", "💿"),
    PARTICLE_STARS("Sparks", "✨")
}

/**
 * Professional Studio Audio Spectrum Analyzer & Multi-Theme Visualizer
 */
@Composable
fun LiveWaveCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedTheme by remember { mutableStateOf(VisualizerTheme.NEON_BARS) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            MusicService.reinitVisualizer()
        }
    }

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

    // Continuous 60fps frame driver for fluid animations
    val infiniteTransition = rememberInfiniteTransition(label = "studioVisualizerTransition")
    val frameTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameTick"
    )

    // Current smoothed bar heights (0f..1f)
    val barHeights = remember { FloatArray(barCount) { 0.02f } }
    val peakCaps = remember { FloatArray(barCount) { 0.02f } }
    val peakVelocities = remember { FloatArray(barCount) { 0f } }

    val hasFft = remember(fftData) {
        fftData.isNotEmpty() && fftData.any { it != 0.toByte() }
    }

    val hasWaveform = remember(waveformData) {
        waveformData.isNotEmpty() && waveformData.any { it != 0.toByte() && it != (-128).toByte() && it != 128.toByte() }
    }

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
                Color(0xFF00E5FF).copy(alpha = 0.22f),
                Color(0xFFE040FB).copy(alpha = 0.09f),
                Color.Transparent
            )
        )
    }

    val gravity = 0.0038f

    // Process audio data
    if (isPlaying) {
        if (hasFft) {
            val halfSize = fftData.size / 2
            for (i in 0 until barCount) {
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

                val boost = when {
                    i < 8 -> 1.8f
                    i < 20 -> 1.4f
                    i > 40 -> 2.2f
                    else -> 1.0f
                }

                val normalized = ((maxMag * boost) / 100f).coerceIn(0.02f, 1f)
                val smoothFactor = if (normalized > barHeights[i]) 0.45f else 0.16f
                barHeights[i] = barHeights[i] + (normalized - barHeights[i]) * smoothFactor
            }
        } else if (hasWaveform) {
            val step = (waveformData.size / barCount).coerceAtLeast(1)
            for (i in 0 until barCount) {
                val idx = (i * step).coerceIn(0, waveformData.size - 1)
                val byteVal = waveformData[idx].toInt()
                val unsignedVal = if (byteVal < 0) byteVal + 256 else byteVal
                val amplitude = (abs(unsignedVal - 128) / 128f).coerceIn(0.02f, 1f)
                val smoothFactor = if (amplitude > barHeights[i]) 0.40f else 0.18f
                barHeights[i] = barHeights[i] + (amplitude - barHeights[i]) * smoothFactor
            }
        } else {
            val time = frameTick * 6.28318f
            for (i in 0 until barCount) {
                val synthetic = 0.22f + 0.28f * sin(time * 2f + i * 0.25f).absoluteValue + 0.15f * sin(time * 5f + i * 0.4f).absoluteValue
                barHeights[i] = barHeights[i] + (synthetic.coerceIn(0.04f, 0.9f) - barHeights[i]) * 0.22f
            }
        }

        // Peak caps physics
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
        color = Color(0xFF0A0C10),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasPermission) {
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
                        "Melodify visualizes the sound directly from your device DSP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable Live Spectrum", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                // Average bass for pulse effects
                val avgBass = (barHeights[0] + barHeights[1] + barHeights[2] + barHeights[3]) / 4f

                // --- CANVAS DRAWING ---
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 24.dp)
                ) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val width = size.width
                    val height = size.height
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val bottomY = height - 16.dp.toPx()
                    val maxBarHeight = bottomY - 48.dp.toPx()

                    when (selectedTheme) {
                        VisualizerTheme.NEON_BARS -> {
                            // Studio decibel reference lines
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                            val dbLevels = listOf(0.75f, 0.50f, 0.25f)
                            for (level in dbLevels) {
                                val lineY = bottomY - (maxBarHeight * level)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.08f),
                                    start = Offset(0f, lineY),
                                    end = Offset(width, lineY),
                                    strokeWidth = 1f,
                                    pathEffect = dashEffect
                                )
                            }

                            // Sub-bass ambient glow
                            if (avgBass > 0.12f) {
                                drawCircle(
                                    brush = ambientGlowBrush,
                                    radius = (width * 0.45f * avgBass).coerceAtLeast(20f),
                                    center = Offset(width * 0.25f, bottomY * 0.6f)
                                )
                            }

                            // 48 Bars + Peak Caps
                            val spacing = 2.5.dp.toPx()
                            val totalSpacing = (barCount - 1) * spacing
                            val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(1f)
                            val cornerRadius = (barWidth / 2f).coerceAtLeast(0.5f)
                            val peakCapHeight = (barWidth * 0.85f).coerceIn(2.5f, 5f)

                            for (i in 0 until barCount) {
                                val barH = (maxBarHeight * barHeights[i]).coerceIn(4f, maxBarHeight)
                                val x = i * (barWidth + spacing)
                                val topY = bottomY - barH

                                drawRoundRect(
                                    brush = barGradient,
                                    topLeft = Offset(x, topY),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )

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

                        VisualizerTheme.FLUID_WAVE -> {
                            // Multi-layer liquid flowing neon sine waves
                            val waveColors = listOf(
                                Color(0xFF00E5FF) to 0.4f,
                                Color(0xFFD500F9) to 0.7f,
                                Color(0xFF3D5AFE) to 1.0f
                            )

                            // Ambient glow behind wave
                            drawCircle(
                                brush = ambientGlowBrush,
                                radius = (width * 0.4f * (0.5f + avgBass)).coerceAtLeast(30f),
                                center = Offset(centerX, centerY)
                            )

                            waveColors.forEachIndexed { waveIdx, (color, phaseMul) ->
                                val path = Path()
                                val points = 60
                                val sliceW = width / points
                                val timePhase = frameTick * 6.28318f * phaseMul

                                for (p in 0..points) {
                                    val px = p * sliceW
                                    val barIdx = (p.toFloat() / points * (barCount - 1)).toInt().coerceIn(0, barCount - 1)
                                    val amp = (barHeights[barIdx] * 70.dp.toPx()).coerceAtLeast(6f)
                                    val py = centerY + sin(timePhase + (p * 0.18f)) * amp

                                    if (p == 0) path.moveTo(px, py)
                                    else path.lineTo(px, py)
                                }

                                drawPath(
                                    path = path,
                                    color = color.copy(alpha = 0.85f),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        VisualizerTheme.PULSING_VINYL -> {
                            // Rotating Vinyl Disc pulsating to bass kicks
                            val baseRadius = (min(width, height) * 0.32f)
                            val pulseRadius = baseRadius + (avgBass * 35.dp.toPx())

                            // Vinyl outer glow ring
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = (avgBass * 0.6f).coerceIn(0.1f, 0.7f)),
                                radius = pulseRadius + 16.dp.toPx(),
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 4.dp.toPx())
                            )

                            // Vinyl black body
                            drawCircle(
                                color = Color(0xFF14151C),
                                radius = pulseRadius,
                                center = Offset(centerX, centerY)
                            )

                            // Concentric groove rings
                            val grooveSteps = 5
                            for (g in 1..grooveSteps) {
                                val gRadius = pulseRadius * (0.35f + (g * 0.12f))
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = gRadius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }

                            // Rotating center label
                            rotate(degrees = frameTick * 360f, pivot = Offset(centerX, centerY)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFD500F9), Color(0xFF00B0FF)),
                                        center = Offset(centerX, centerY),
                                        radius = pulseRadius * 0.32f
                                    ),
                                    radius = pulseRadius * 0.30f,
                                    center = Offset(centerX, centerY)
                                )
                                drawCircle(
                                    color = Color(0xFF0A0C10),
                                    radius = 8.dp.toPx(),
                                    center = Offset(centerX, centerY)
                                )
                            }
                        }

                        VisualizerTheme.PARTICLE_STARS -> {
                            // 36-Ray starburst radiating outward with FFT audio reaction
                            val rays = 36
                            val angleStep = 360f / rays

                            for (r in 0 until rays) {
                                val angleRad = Math.toRadians((r * angleStep).toDouble())
                                val barIdx = (r.toFloat() / rays * (barCount - 1)).toInt().coerceIn(0, barCount - 1)
                                val rayMag = (barHeights[barIdx] * min(width, height) * 0.42f).coerceAtLeast(10f)

                                val innerRadius = 24.dp.toPx() + (avgBass * 12.dp.toPx())
                                val startX = centerX + (innerRadius * cos(angleRad)).toFloat()
                                val startY = centerY + (innerRadius * sin(angleRad)).toFloat()
                                val endX = centerX + ((innerRadius + rayMag) * cos(angleRad)).toFloat()
                                val endY = centerY + ((innerRadius + rayMag) * sin(angleRad)).toFloat()

                                val rayColor = when {
                                    r < 9 -> Color(0xFF00FFFF)
                                    r < 18 -> Color(0xFF00B0FF)
                                    r < 27 -> Color(0xFFD500F9)
                                    else -> Color(0xFFFF4081)
                                }

                                drawLine(
                                    color = rayColor.copy(alpha = 0.85f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Core pulsar
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent),
                                    center = Offset(centerX, centerY),
                                    radius = 28.dp.toPx() * (1f + avgBass)
                                ),
                                radius = 28.dp.toPx() * (1f + avgBass),
                                center = Offset(centerX, centerY)
                            )
                        }
                    }
                }

                // --- Top Controls: Theme Switcher Pills & Status ---
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Theme Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VisualizerTheme.values().forEach { theme ->
                            val isSelected = selectedTheme == theme
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTheme = theme }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${theme.icon} ${theme.displayName}",
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isPlaying -> Color(0xFFFF9800)
                                    hasFft -> Color(0xFF00E5FF)
                                    else -> Color(0xFF00E676)
                                }
                            )
                    )
                }
            }
        }
    }
}
