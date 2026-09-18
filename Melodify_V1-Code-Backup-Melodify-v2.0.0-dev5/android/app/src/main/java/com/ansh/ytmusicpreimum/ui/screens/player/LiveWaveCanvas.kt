package com.ansh.ytmusicpreimum.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansh.ytmusicpreimum.VisualizerData

@Composable
fun LiveWaveCanvas(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val fftData by VisualizerData.fftData.collectAsState()
    val barCount = 42

    val infiniteTransition = rememberInfiniteTransition(label = "waveLoop")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animPhase"
    )

    val barHeights = remember { FloatArray(barCount) { 0.12f } }
    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00E5FF), // Cyan neon top
                Color(0xFFE91E63), // Pink/Magenta mid
                Color(0xFF9C27B0)  // Deep Purple base
            )
        )
    }

    val hasHardwareFft = remember(fftData) {
        fftData.isNotEmpty() && fftData.any { it != 0.toByte() }
    }

    // Update bar heights smoothly
    if (isPlaying) {
        if (hasHardwareFft) {
            for (i in 0 until barCount) {
                val fftIndex = (i * (fftData.size / 2) / barCount) * 2
                if (fftIndex + 1 < fftData.size) {
                    val real = fftData[fftIndex].toInt()
                    val imag = fftData[fftIndex + 1].toInt()
                    val magnitude = kotlin.math.hypot(real.toDouble(), imag.toDouble()).toFloat()
                    val normalized = (magnitude / 128f).coerceIn(0.08f, 1f)
                    val sensitivity = if (i < barCount / 4) 1.8f else 1.2f
                    val targetHeight = (normalized * sensitivity).coerceIn(0.08f, 1f)
                    barHeights[i] = barHeights[i] * 0.35f + targetHeight * 0.65f
                }
            }
        } else {
            // Algorithmic energetic beat rhythm based on animPhase
            val kick = kotlin.math.max(0f, kotlin.math.sin(animPhase * 2.2f)).let { it * it * it }

            for (i in 0 until barCount) {
                val freqOffset = i.toFloat() / barCount.toFloat()
                val wave1 = kotlin.math.sin(animPhase * 2.8f + freqOffset * 7.5f) * 0.26f
                val wave2 = kotlin.math.cos(animPhase * 4.6f - freqOffset * 10.5f) * 0.20f
                val bassPunch = if (i < barCount / 3) kick * (1f - (i.toFloat() / (barCount / 3f))) * 0.60f else 0f
                val targetHeight = (0.18f + wave1 + wave2 + bassPunch).coerceIn(0.08f, 0.95f)
                barHeights[i] = barHeights[i] * 0.45f + targetHeight * 0.55f
            }
        }
    } else {
        for (i in 0 until barCount) {
            barHeights[i] = barHeights[i] * 0.85f
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        color = Color.Black.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 28.dp)
            ) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas

                val width = size.width
                val height = size.height
                val spacing = 3.5.dp.toPx()
                val totalSpacing = (barCount - 1) * spacing
                val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(1f)
                if (barWidth <= 0f) return@Canvas

                val midY = height / 2f
                val baseAmplitude = height * 0.44f
                val cornerRadiusPx = (barWidth / 2f).coerceAtLeast(0.5f)

                for (i in 0 until barCount) {
                    val animatedHeight = (baseAmplitude * barHeights[i]).coerceIn(3f, height / 2f)
                    val x = i * (barWidth + spacing)
                    val topY = (midY - animatedHeight).coerceAtLeast(0f)
                    val totalBarHeight = (animatedHeight * 2f).coerceIn(6f, height)

                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, totalBarHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                }
            }

            // Sleek glowing label in top right corner
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
                        .background(if (isPlaying) Color(0xFF00E5FF) else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "BEAT SPECTRUM" else "PAUSED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
