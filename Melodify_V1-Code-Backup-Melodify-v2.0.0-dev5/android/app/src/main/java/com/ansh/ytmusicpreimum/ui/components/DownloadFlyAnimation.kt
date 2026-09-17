package com.ansh.ytmusicpreimum.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.ansh.ytmusicpreimum.data.TrackEntity

@Composable
fun DownloadFlyAnimation(track: TrackEntity, onAnimationFinished: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    val animProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
        onAnimationFinished()
    }

    if (animProgress.value < 1f) {
        val xOffset = lerp(0.dp, screenWidth / 3, animProgress.value)
        val yOffset = lerp(0.dp, -screenHeight / 2, animProgress.value)
        val scale = 1f + (0.05f - 1f) * animProgress.value
        val alpha = 1f + (0f - 1f) * animProgress.value

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(120.dp)
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                        this.rotationZ = animProgress.value * 45f
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
