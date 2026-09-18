package com.ansh.ytmusicpreimum.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ansh.ytmusicpreimum.ui.theme.*

const val MotionSmall = 150
const val MotionMedium = 250
const val MotionLarge = 350
val PremiumEasing = FastOutSlowInEasing

@Composable
fun SectionTitle(title: String, icon: ImageVector, onViewAll: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(com.ansh.ytmusicpreimum.ui.theme.GeminiBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue, 
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ), 
            color = com.ansh.ytmusicpreimum.ui.theme.GeminiTextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (onViewAll != null) {
            Surface(
                onClick = onViewAll,
                shape = RoundedCornerShape(14.dp),
                color = com.ansh.ytmusicpreimum.ui.theme.GeminiCardHover,
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "See all", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold, 
                        color = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        contentDescription = "View All",
                        tint = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btnScale"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HeartBeatButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heartScale"
    )
    
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) Color.Red else tint.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun PlayPauseMorph(isPlaying: Boolean, isBuffering: Boolean, isLandscape: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val size = if (isLandscape) 64.dp else 84.dp
    val iconSize = if (isLandscape) 40.dp else 52.dp
    
    val pulseScale by animateFloatAsState(
        targetValue = if (isPlaying && !isBuffering) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier
            .size(size)
            .graphicsLayer(
                scaleX = if (isPlaying && !isBuffering) pulseScale else 1f,
                scaleY = if (isPlaying && !isBuffering) pulseScale else 1f
            )
            .clip(CircleShape)
            .clickable(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick() 
                }
            ),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.6f), 
                    color = Color.Black,
                    strokeWidth = 3.dp
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying, 
                    transitionSpec = { 
                        (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn())
                            .togetherWith(scaleOut() + fadeOut()) 
                    }, 
                    label = "PlayPauseMorph"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, 
                        contentDescription = null, 
                        tint = Color.Black, 
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
fun FuturisticThumbnail(model: Any?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}

@Composable
fun BrandedBackground(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    val backgroundBrush = if (isDarkTheme) Brush.verticalGradient(listOf(Color(0xFF050A18), Color(0xFF02050D))) else Brush.verticalGradient(listOf(Color(0xFFF0F2F9), Color.White))
    val lineAlpha = if (isDarkTheme) 0.12f else 0.18f
    val noteAlpha = if (isDarkTheme) 0.1f else 0.15f
    val bokehAlpha = if (isDarkTheme) 0.08f else 0.03f
    val tintColor = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF00897B)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val w = size.width
            val h = size.height
            val bokehPoints = listOf(
                Pair(Offset(w * 0.15f, h * 0.25f), 80.dp.toPx()),
                Pair(Offset(w * 0.85f, h * 0.1f), 120.dp.toPx()),
                Pair(Offset(w * 0.25f, h * 0.75f), 100.dp.toPx()),
                Pair(Offset(w * 0.75f, h * 0.85f), 90.dp.toPx()),
                Pair(Offset(w * 0.5f, h * 0.5f), 150.dp.toPx())
            )
            bokehPoints.forEach { (pos, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(tintColor.copy(alpha = bokehAlpha), Color.Transparent), center = pos, radius = radius),
                    radius = radius,
                    center = pos
                )
            }
            val paths = listOf(
                Path().apply {
                    moveTo(w * 1.1f, h * 0.05f)
                    cubicTo(w * 0.8f, h * 0.25f, w * 0.6f, h * 0.1f, w * 0.4f, h * 0.45f)
                    cubicTo(w * 0.2f, h * 0.7f, w * 0.3f, h * 0.95f, w * -0.1f, h * 0.9f)
                },
                Path().apply {
                    moveTo(w * 1.2f, h * 0.25f)
                    cubicTo(w * 0.9f, h * 0.45f, w * 0.7f, h * 0.3f, w * 0.5f, h * 0.65f)
                    cubicTo(w * 0.3f, h * 0.9f, w * 0.4f, h * 1.1f, w * 0.1f, h * 1.1f)
                },
                Path().apply {
                    moveTo(w * 0.9f, h * -0.1f)
                    cubicTo(w * 0.6f, h * 0.2f, w * 1.0f, h * 0.45f, w * 0.5f, h * 0.75f)
                    cubicTo(w * 0.2f, h * 0.95f, w * 0.4f, h * 1.15f, w * -0.1f, h * 1.0f)
                }
            )
            paths.forEachIndexed { i, path ->
                drawPath(path = path, color = tintColor.copy(alpha = lineAlpha / (i + 1)), style = Stroke(width = (2.5f - i * 0.5f).dp.toPx(), cap = StrokeCap.Round))
            }
            val notes = listOf(
                Pair(Offset(w * 0.65f, h * 0.22f), 0.7f),
                Pair(Offset(w * 0.48f, h * 0.42f), 1.1f),
                Pair(Offset(w * 0.72f, h * 0.58f), 0.8f),
                Pair(Offset(w * 0.38f, h * 0.72f), 0.9f),
                Pair(Offset(w * 0.55f, h * 0.88f), 0.6f)
            )
            notes.forEach { (pos, s) ->
                drawCircle(color = tintColor.copy(alpha = noteAlpha), radius = 5.dp.toPx() * s, center = pos)
                drawLine(color = tintColor.copy(alpha = noteAlpha), start = Offset(pos.x + 4.dp.toPx() * s, pos.y), end = Offset(pos.x + 4.dp.toPx() * s, pos.y - 15.dp.toPx() * s), strokeWidth = 2.dp.toPx() * s, cap = StrokeCap.Round)
                drawLine(color = tintColor.copy(alpha = noteAlpha), start = Offset(pos.x + 4.dp.toPx() * s, pos.y - 15.dp.toPx() * s), end = Offset(pos.x + 14.dp.toPx() * s, pos.y - 12.dp.toPx() * s), strokeWidth = 2.dp.toPx() * s, cap = StrokeCap.Round)
            }
        }
        content()
    }
}

@Composable
fun LibraryEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun formatTime(ms: Long): String { 
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds) 
}
