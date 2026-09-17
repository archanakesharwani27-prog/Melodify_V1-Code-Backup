package com.ansh.ytmusicpreimum.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ansh.ytmusicpreimum.VisualizerData

@Composable
fun LiveWaveCanvas(isPlaying: Boolean) {
    val fftData by VisualizerData.fftData.collectAsState()
    val barCount = 48
    
    val barHeights = remember { FloatArray(barCount) { 0.1f } }
    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = 0.9f), 
                Color(0xFFE91E63).copy(alpha = 0.9f), 
                Color(0xFF9C27B0).copy(alpha = 0.9f)
            )
        )
    }
    
    // Process FFT frame in-place without memory reallocations
    remember(fftData, isPlaying) {
        if (isPlaying && fftData.isNotEmpty()) {
            for (i in 0 until barCount) {
                val fftIndex = (i * (fftData.size / 2) / barCount) * 2
                if (fftIndex + 1 < fftData.size) {
                    val real = fftData[fftIndex].toInt()
                    val imag = fftData[fftIndex + 1].toInt()
                    val magnitude = kotlin.math.hypot(real.toDouble(), imag.toDouble()).toFloat()
                    
                    val normalized = (magnitude / 128f).coerceIn(0.1f, 1f)
                    val sensitivity = if (i < barCount / 4) 1.6f else 1.0f 
                    val targetHeight = normalized * sensitivity
                    
                    barHeights[i] = barHeights[i] * 0.3f + targetHeight * 0.7f
                }
            }
        } else {
            for (i in 0 until barCount) {
                barHeights[i] = barHeights[i] * 0.85f
            }
        }
        true
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)), 
        color = Color.Black.copy(alpha = 0.4f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 32.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = 3.dp.toPx()
            val barWidth = (width - (barCount * spacing)) / barCount
            val midY = height / 2
            val baseAmplitude = height * 0.45f

            for (i in 0 until barCount) {
                val animatedHeight = baseAmplitude * barHeights[i]
                val x = i * (barWidth + spacing)
                
                drawRoundRect(
                    brush = gradientBrush, 
                    topLeft = Offset(x, midY - animatedHeight), 
                    size = androidx.compose.ui.geometry.Size(barWidth, animatedHeight * 2), 
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}
