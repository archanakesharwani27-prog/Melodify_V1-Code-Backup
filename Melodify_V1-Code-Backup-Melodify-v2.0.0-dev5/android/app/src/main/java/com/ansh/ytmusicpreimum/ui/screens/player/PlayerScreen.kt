package com.ansh.ytmusicpreimum.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.LyricsState
import com.ansh.ytmusicpreimum.ui.components.AnimatedIconButton
import com.ansh.ytmusicpreimum.ui.components.FuturisticThumbnail
import com.ansh.ytmusicpreimum.ui.components.HeartBeatButton
import com.ansh.ytmusicpreimum.ui.components.PlayPauseMorph
import com.ansh.ytmusicpreimum.ui.components.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    mediaItem: androidx.media3.common.MediaItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    isLiked: Boolean,
    volume: Float,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    lyrics: LyricsState,
    userTier: Int,
    downloadState: DownloadState?,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (isLandscape) {
            PlayerLandscape(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, lyrics, userTier, downloadState, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
            )
        } else {
            PlayerPortrait(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, lyrics, userTier, downloadState, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerPortrait(
    mediaItem: androidx.media3.common.MediaItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    isLiked: Boolean,
    volume: Float,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    lyrics: LyricsState,
    userTier: Int,
    downloadState: DownloadState?,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    BackHandler { onClose() }
    
    // Performance: Remember derived values
    val currentTitle = remember(mediaItem) { mediaItem.mediaMetadata.title?.toString() ?: "Unknown" }
    val currentArtist = remember(mediaItem) { mediaItem.mediaMetadata.artist?.toString() ?: "Unknown" }
    val currentArtwork = remember(mediaItem) { mediaItem.mediaMetadata.artworkUri }

    var skipDirection by remember { mutableIntStateOf(0) }
    val rippleAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0f) }

    LaunchedEffect(mediaItem) {
        delay(250)
        skipDirection = 0
    }

    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectVerticalDragGestures { change, dragAmount ->
                change.consume()
                if (dragAmount > 50) onClose()
                if (dragAmount < -50) onOpenQueue()
            }
        }
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        
        // --- BACKGROUND ---
        AsyncImage(
            model = currentArtwork,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black
                    )
                )
            ))

        val contentColor = Color.White

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = (screenWidth.value * 0.08f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // --- TOP BAR ---
            Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedIconButton(onClick = { onClose() }) { 
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = contentColor, modifier = Modifier.size(32.dp)) 
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { 
                    Text(
                        text = "NOW PLAYING", 
                        color = contentColor.copy(alpha = 0.6f), 
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    ) 
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    AnimatedIconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = contentColor, modifier = Modifier.size(28.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(x = (-16).dp, y = 0.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download Song", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                onDownloadClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                onAddToPlaylist()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                onOpenSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.1f))
            
            // --- ARTWORK ---
            val artworkSize = minOf(screenWidth * 0.88f, screenHeight * 0.42f)
            
            Box(modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    shadowElevation = 50f
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                }
                .pointerInput(Unit) { 
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) onSkipPrevious() else onSkipNext()
                        }
                    ) 
                }) {
                AnimatedContent(
                    targetState = mediaItem.mediaId,
                    transitionSpec = {
                        val enter = if (skipDirection == 1) {
                            slideInHorizontally(initialOffsetX = { (it * 0.2f).toInt() }) + fadeIn(tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                        } else if (skipDirection == -1) {
                            slideInHorizontally(initialOffsetX = { -(it * 0.2f).toInt() }) + fadeIn(tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                        } else {
                            fadeIn(tween(350)) + scaleIn(initialScale = 0.96f, animationSpec = tween(350))
                        }
                        val exit = if (skipDirection == 1) {
                            slideOutHorizontally(targetOffsetX = { -(it * 0.2f).toInt() }) + fadeOut(tween(250)) + scaleOut(targetScale = 0.95f, animationSpec = tween(250))
                        } else if (skipDirection == -1) {
                            slideOutHorizontally(targetOffsetX = { (it * 0.2f).toInt() }) + fadeOut(tween(250)) + scaleOut(targetScale = 0.95f, animationSpec = tween(250))
                        } else {
                            fadeOut(tween(350)) + scaleOut(targetScale = 0.96f, animationSpec = tween(350))
                        }
                        (enter togetherWith exit).using(SizeTransform(clip = false))
                    },
                    label = "ArtworkTransition"
                ) { _ -> 
                    FuturisticThumbnail(
                        model = currentArtwork, 
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) { 
                    drawCircle(color = Color.White.copy(alpha = rippleAlpha.value), radius = size.minDimension * rippleScale.value, center = center) 
                }
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        color = contentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.2f))
            
            // --- SONG INFO ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Crossfade(targetState = currentTitle, label = "TitleCrossfade") { title ->
                        Text(
                            text = title, 
                            color = contentColor, 
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1, 
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                    
                    Text(
                        text = currentArtist, 
                        color = contentColor.copy(alpha = 0.7f), 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                HeartBeatButton(
                    isLiked = isLiked,
                    onClick = onToggleLike,
                    tint = contentColor
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- SEEK BAR ---
            val progressState = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            val timeElapsed = formatTime(currentPosition)
            val timeRemaining = formatTime(duration - currentPosition)
            
            Slider(
                value = progressState, 
                onValueChange = { onSeek((it * duration).toLong()) }, 
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, 
                    activeTrackColor = Color.White, 
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(timeElapsed, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium) 
                Text("-$timeRemaining", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
            }

            // --- DOWNLOAD PROGRESS ---
            AnimatedVisibility(visible = downloadState != null && downloadState.status == DownloadStatus.DOWNLOADING) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading... ${(downloadState?.progress?.times(100))?.toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${downloadState?.totalSize} • ${downloadState?.quality}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadState?.progress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.1f))
            
            // --- CONTROLS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AnimatedIconButton(onClick = { onToggleShuffle() }) { 
                    Icon(
                        imageVector = Icons.Default.Shuffle, 
                        contentDescription = "Shuffle", 
                        tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f)
                    ) 
                }
                AnimatedIconButton(onClick = { onSkipPrevious() }) { 
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = contentColor, modifier = Modifier.size(48.dp)) 
                }
                
                PlayPauseMorph(isPlaying, isBuffering, onClick = { onTogglePlay() })
                
                AnimatedIconButton(onClick = { onSkipNext() }) { 
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor, modifier = Modifier.size(48.dp)) 
                }
                AnimatedIconButton(onClick = { onToggleRepeat() }) {
                    Icon(
                        imageVector = when (repeatMode) { 
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat 
                        }, 
                        contentDescription = "Repeat", 
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f)
                    ) 
                }
            }
            
            Spacer(modifier = Modifier.weight(0.2f))
            
            // --- VOLUME & QUEUE ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                 Slider(
                     value = volume, 
                     onValueChange = onVolumeChange, 
                     modifier = Modifier.weight(1f).padding(end = 12.dp),
                     colors = SliderDefaults.colors(
                         thumbColor = Color.White, 
                         activeTrackColor = MaterialTheme.colorScheme.primary,
                         inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                     )
                 )
                 AnimatedIconButton(onClick = onOpenQueue) { 
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Queue", tint = Color.White, modifier = Modifier.size(28.dp)) 
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerLandscape(
    mediaItem: androidx.media3.common.MediaItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    isLiked: Boolean,
    volume: Float,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    lyrics: LyricsState,
    userTier: Int,
    downloadState: DownloadState?,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    BackHandler { onClose() }
    
    var skipDirection by remember { mutableIntStateOf(0) }
    val rippleAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0f) }
    var tapCount by remember { mutableIntStateOf(0) }
    var tapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    val handleTap = {
        tapCount++
        tapJob?.cancel()
        tapJob = scope.launch { 
            delay(500)
            if (tapCount == 2) { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                skipDirection = 1
                scope.launch { 
                    rippleScale.snapTo(0f)
                    rippleAlpha.snapTo(0.4f)
                    launch { rippleScale.animateTo(1.5f, tween(250)) }
                    launch { rippleAlpha.animateTo(0f, tween(250)) } 
                }
                onSkipNext() 
            } else if (tapCount >= 3) { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                skipDirection = -1
                scope.launch { 
                    rippleScale.snapTo(0f)
                    rippleAlpha.snapTo(0.4f)
                    launch { rippleScale.animateTo(1.5f, tween(250)) }
                    launch { rippleAlpha.animateTo(0f, tween(250)) } 
                }
                onSkipPrevious() 
            }
            tapCount = 0 
        }
    }

    LaunchedEffect(mediaItem) { 
        delay(250)
        skipDirection = 0 
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        
        AsyncImage(
            model = mediaItem.mediaMetadata.artworkUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
            
        val contentColor = Color.White
        
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Volume Bar (Vertical)
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .width(64.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { onVolumeChange((volume + 0.066f).coerceAtMost(1f)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Volume Up", tint = Color.White)
                }
                
                Text(
                    text = "${(volume * 15).toInt()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        steps = 15,
                        modifier = Modifier
                            .graphicsLayer(rotationZ = -90f)
                            .width(screenHeight * 0.6f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                IconButton(onClick = { onVolumeChange((volume - 0.066f).coerceAtLeast(0f)) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Volume Down", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Left Side: Artwork
            val artworkSize = screenHeight * 0.8f
            Box(modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    shadowElevation = 40f
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                }) {
                 HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { handleTap() }) }) {
                        AnimatedContent(
                            targetState = mediaItem.mediaId, 
                            transitionSpec = { 
                                val enter = if (skipDirection == 1) {
                                    slideInHorizontally(initialOffsetX = { (it * 0.2f).toInt() }) + fadeIn(tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                                } else if (skipDirection == -1) {
                                    slideInHorizontally(initialOffsetX = { -(it * 0.2f).toInt() }) + fadeIn(tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                                } else {
                                    fadeIn(tween(350)) + scaleIn(initialScale = 0.96f, animationSpec = tween(350))
                                }
                                val exit = if (skipDirection == 1) {
                                    slideOutHorizontally(targetOffsetX = { -(it * 0.2f).toInt() }) + fadeOut(tween(250)) + scaleOut(targetScale = 0.95f, animationSpec = tween(250))
                                } else if (skipDirection == -1) {
                                    slideOutHorizontally(targetOffsetX = { (it * 0.2f).toInt() }) + fadeOut(tween(250)) + scaleOut(targetScale = 0.95f, animationSpec = tween(250))
                                } else {
                                    fadeOut(tween(350)) + scaleOut(targetScale = 0.96f, animationSpec = tween(350))
                                }
                                (enter togetherWith exit).using(SizeTransform(clip = false))
                            }, 
                            label = "ArtworkTransitionLandscape"
                        ) { _ -> 
                            FuturisticThumbnail(
                                model = mediaItem.mediaMetadata.artworkUri, 
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                 }
                if (isBuffering) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = contentColor)
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            
            // Right Side: Info & Controls
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown", 
                    color = contentColor, 
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1, 
                    modifier = Modifier.fillMaxWidth().basicMarquee() 
                )
                Text(
                    text = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown", 
                    color = contentColor.copy(alpha = 0.7f), 
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // --- SEEK BAR ---
                val progressState = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                Slider(
                    value = progressState, 
                    onValueChange = { onSeek((it * duration).toLong()) }, 
                    colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor, inactiveTrackColor = contentColor.copy(alpha = 0.2f))
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { 
                    Text(formatTime(currentPosition), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    Text("-" + formatTime(duration - currentPosition), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall) 
                }

                if (downloadState != null && downloadState.status == DownloadStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading... ${(downloadState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${downloadState.totalSize} • ${downloadState.quality}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AnimatedIconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = contentColor, modifier = Modifier.size(44.dp)) }
                    PlayPauseMorph(isPlaying, isBuffering, isLandscape = true, onClick = onTogglePlay)
                    AnimatedIconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, contentDescription = null, tint = contentColor, modifier = Modifier.size(44.dp)) }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AnimatedIconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) }
                    HeartBeatButton(isLiked = isLiked, onClick = onToggleLike, tint = Color.White)
                    AnimatedIconButton(onClick = onOpenQueue) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        AnimatedIconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = (-16).dp, y = 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download Song", color = Color.White) },
                                onClick = { showMenu = false; onDownloadClick() },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.White) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = Color.White) },
                                onClick = { showMenu = false; onAddToPlaylist() },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = Color.White) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White) },
                                onClick = { showMenu = false; onOpenSettings() },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
                            )
                        }
                    }

                    AnimatedIconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun LyricsScreen(lyrics: String?, isFetching: Boolean, contentColor: Color) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        color = Color.Black.copy(alpha = 0.1f)
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            if (isFetching) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = contentColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching Lyrics...", color = contentColor)
                }
            } else if (lyrics != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = lyrics,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lyrics not found.", color = contentColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SubscriptionGate(tierName: String, onUpgrade: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Premium Feature", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("This feature is available for $tierName members only.", textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUpgrade) { Text("Upgrade in Settings") }
        }
    }
}
