package com.ansh.ytmusicpreimum.ui

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    showNameIntro: Boolean = false,
    userName: String = "",
    onAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val pathPortion = remember { Animatable(0f) }
    val fillAlpha = remember { Animatable(0f) }
    val auraAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    val mediaPlayer = remember {
        try {
            val resId = context.resources.getIdentifier("love_story_intro", "raw", context.packageName)
            if (resId != 0) {
                MediaPlayer.create(context, resId)?.apply { 
                    try { setVolume(1.0f, 1.0f) } catch (t: Throwable) {}
                }
            } else null
        } catch (e: Throwable) { null }
    }

    val tealLogo = Color(0xFF00B29A)
    val cyanLogo = Color(0xFF00E5FF)
    val mintLogo = Color(0xFFB2FF59)
    val darkBlueBg = Color(0xFF050A18)

    val logoGradient = Brush.linearGradient(colors = listOf(tealLogo, cyanLogo, mintLogo))

    LaunchedEffect(Unit) {
        try {
            try { mediaPlayer?.start() } catch (t: Throwable) {}
            
            // Logo Animation
            launch { 
                try { scale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) } catch (t: Throwable) {}
            }
            try {
                pathPortion.animateTo(1f, tween(1500, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)))
            } catch (t: Throwable) {}

            launch { try { fillAlpha.animateTo(1f, tween(1000)) } catch (t: Throwable) {} }
            launch { try { auraAlpha.animateTo(0.6f, tween(1500)) } catch (t: Throwable) {} }

            delay(300)
            try { textAlpha.animateTo(1f, tween(800)) } catch (t: Throwable) {}

            if (showNameIntro && userName.isNotEmpty()) {
                delay(1000)
                // Fade out logo text, Fade in User Name
                try { textAlpha.animateTo(0f, tween(500)) } catch (t: Throwable) {}
                try { nameAlpha.animateTo(1f, tween(1000)) } catch (t: Throwable) {}
                delay(1500)
            } else {
                delay(1500)
            }

            // Fade out Audio & Exit
            launch {
                try {
                    for (i in 20 downTo 0) {
                        val v = i.toFloat() / 20
                        try { mediaPlayer?.setVolume(v, v) } catch (t: Throwable) {}
                        delay(40)
                    }
                    try { mediaPlayer?.stop() } catch (t: Throwable) {}
                } catch (t: Throwable) {}
            }

            try { scale.animateTo(1.1f, tween(800)) } catch (t: Throwable) {}
        } catch (t: Throwable) {
            android.util.Log.e("SplashScreen", "Animation error in splash screen", t)
        } finally {
            onAnimationFinished()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (t: Throwable) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(darkBlueBg), contentAlignment = Alignment.Center) {
        // Aura
        Canvas(modifier = Modifier.size(500.dp)) {
            if (size.width <= 0f || size.height <= 0f || auraAlpha.value <= 0.001f) return@Canvas
            val radius = (size.width * 0.4f * auraAlpha.value).coerceAtLeast(0.1f)
            val gradRadius = (size.width / 2f).coerceAtLeast(1f)
            try {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cyanLogo.copy(alpha = 0.2f * auraAlpha.value), Color.Transparent),
                        center = center,
                        radius = gradRadius
                    ),
                    radius = radius
                )
            } catch (t: Throwable) {
                drawCircle(
                    color = cyanLogo.copy(alpha = 0.05f * auraAlpha.value),
                    radius = radius
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)) {
            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val w = size.width
                    val h = size.height
                    val mainPath = Path().apply {
                        moveTo(w * 0.2f, h * 0.7f); cubicTo(w * 0.1f, h * 0.65f, w * 0.1f, h * 0.45f, w * 0.2f, h * 0.4f); lineTo(w * 0.35f, h * 0.3f); lineTo(w * 0.35f, h * 0.7f)
                        cubicTo(w * 0.4f, h * 0.6f, w * 0.45f, h * 0.4f, w * 0.5f, h * 0.15f); cubicTo(w * 0.6f, h * 0.05f, w * 0.65f, h * 0.2f, w * 0.55f, h * 0.45f)
                        cubicTo(w * 0.45f, h * 0.65f, w * 0.4f, h * 0.95f, w * 0.5f, h * 0.9f); cubicTo(w * 0.65f, h * 0.85f, w * 0.6f, h * 0.65f, w * 0.5f, h * 0.65f)
                        moveTo(w * 0.55f, h * 0.25f); lineTo(w * 0.75f, h * 0.3f); lineTo(w * 0.75f, h * 0.7f); cubicTo(w * 0.9f, h * 0.75f, w * 0.9f, h * 0.55f, w * 0.8f, h * 0.5f)
                    }
                    val leftWave = Path().apply { moveTo(w * 0.12f, h * 0.45f); cubicTo(w * 0.05f, h * 0.5f, w * 0.05f, h * 0.6f, w * 0.12f, h * 0.65f) }
                    val rightWave = Path().apply { moveTo(w * 0.88f, h * 0.45f); cubicTo(w * 0.95f, h * 0.5f, w * 0.95f, h * 0.6f, w * 0.88f, h * 0.65f) }
                    val pathMeasure = PathMeasure(); pathMeasure.setPath(mainPath, false); val length = pathMeasure.length
                    
                    try {
                        drawPath(path = mainPath, brush = logoGradient, alpha = fillAlpha.value)
                    } catch (t: Throwable) {}

                    if (length > 1f) {
                        try {
                            val phase = ((1f - pathPortion.value) * length).coerceAtLeast(0f)
                            drawPath(
                                path = mainPath,
                                color = cyanLogo,
                                style = Stroke(
                                    width = 5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(length, length), phase)
                                )
                            )
                        } catch (t: Throwable) {
                            try {
                                drawPath(path = mainPath, color = cyanLogo, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            } catch (e: Throwable) {}
                        }
                    } else {
                        try {
                            drawPath(path = mainPath, color = cyanLogo, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } catch (t: Throwable) {}
                    }

                    if (fillAlpha.value > 0.1f) {
                        try {
                            drawPath(leftWave, mintLogo, fillAlpha.value * 0.5f, Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(rightWave, mintLogo, fillAlpha.value * 0.5f, Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        } catch (t: Throwable) {}
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Box(contentAlignment = Alignment.Center) {
                Text("MELODIFY", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 12.sp, color = Color.White), modifier = Modifier.alpha(textAlpha.value))
                if (showNameIntro) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(nameAlpha.value)) {
                        Text("WELCOME", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 4.sp, color = cyanLogo))
                        Text(userName.uppercase(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp, color = Color.White))
                    }
                }
            }
        }
    }
}
