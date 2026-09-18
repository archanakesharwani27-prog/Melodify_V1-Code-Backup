package com.ansh.ytmusicpreimum.ui.screens.player

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansh.ytmusicpreimum.VisualizerData
import kotlinx.coroutines.isActive

@Composable
fun LiveWaveCanvas(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val fftData by VisualizerData.fftData.collectAsState()
    val barCount = 44
    
    var frameTick by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                withFrameNanos { time ->
                    frameTick = time
                }
            }
        }
    }

    val barHeights = remember { FloatArray(barCount) { 0.1f } }
    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00E5FF), 
                Color(0xFFE91E63), 
                Color(0xFF9C27B0)
            )
        )
    }

    val hasHardwareFft = remember(fftData) {
        fftData.isNotEmpty() && fftData.any { it != 0.toByte() }
    }

    val timeSec = (frameTick / 1_000_000_000.0).toFloat()

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
            // Algorithmic energetic beat visualizer when hardware FFT is dormant
            val beatPhase = (timeSec * 4.4f) % (2f * Math.PI.toFloat())
            val kick = kotlin.math.max(0f, kotlin.math.sin(beatPhase)).let { it * it * it }
            
            for (i in 0 until barCount) {
                val freqOffset = i.toFloat() / barCount.toFloat()
                val wave1 = kotlin.math.sin(timeSec * 5.2f + freqOffset * 8.5f) * 0.28f
                val wave2 = kotlin.math.cos(timeSec * 9.1f - freqOffset * 11.5f) * 0.22f
                val bassPunch = if (i < barCount / 3) kick * (1f - (i.toFloat() / (barCount / 3f))) * 0.65f else 0f
                val targetHeight = (0.16f + wave1 + wave2 + bassPunch).coerceIn(0.08f, 0.95f)
                barHeights[i] = barHeights[i] * 0.5f + targetHeight * 0.5f
            }
        }
    } else {
        for (i in 0 until barCount) {
            barHeights[i] = barHeights[i] * 0.82f
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
                val width = size.width
                val height = size.height
                val spacing = 3.5.dp.toPx()
                val barWidth = (width - (barCount * spacing)) / barCount
                val midY = height / 2
                val baseAmplitude = height * 0.44f

                for (i in 0 until barCount) {
                    val animatedHeight = baseAmplitude * barHeights[i]
                    val x = i * (barWidth + spacing)
                    
                    drawRoundRect(
                        brush = gradientBrush, 
                        topLeft = Offset(x, midY - animatedHeight), 
                        size = androidx.compose.ui.geometry.Size(barWidth, (animatedHeight * 2).coerceAtLeast(6f)), 
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
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
