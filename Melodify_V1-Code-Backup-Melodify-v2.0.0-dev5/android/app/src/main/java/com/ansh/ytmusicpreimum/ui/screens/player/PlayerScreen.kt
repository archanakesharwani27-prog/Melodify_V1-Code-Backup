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
import androidx.compose.foundation.clickable
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
import com.ansh.ytmusicpreimum.AudioEffectsManager
import com.ansh.ytmusicpreimum.ui.components.AnimatedIconButton
import com.ansh.ytmusicpreimum.ui.components.FuturisticThumbnail
import com.ansh.ytmusicpreimum.ui.components.HeartBeatButton
import com.ansh.ytmusicpreimum.ui.components.PlayPauseMorph
import com.ansh.ytmusicpreimum.ui.components.formatTime
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.view.ViewGroup
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.YouTubeExtractor
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
    isVideoMode: Boolean = false,
    onToggleVideoMode: (Boolean) -> Unit = {},
    videoQuality: String = "720p",
    onVideoQualityChange: (String) -> Unit = {},
    audioQuality: String = "Medium",
    onAudioQualityChange: (String) -> Unit = {},
    relatedTracks: List<TrackEntity> = emptyList(),
    isLoadingRelated: Boolean = false,
    onPlayRelatedTrack: (TrackEntity) -> Unit = {},
    playerController: androidx.media3.common.Player? = null,
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
                mediaItem = mediaItem,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPosition = currentPosition,
                duration = duration,
                isLiked = isLiked,
                volume = volume,
                shuffleModeEnabled = shuffleModeEnabled,
                repeatMode = repeatMode,
                lyrics = lyrics,
                userTier = userTier,
                downloadState = downloadState,
                isVideoMode = isVideoMode,
                onToggleVideoMode = onToggleVideoMode,
                videoQuality = videoQuality,
                onVideoQualityChange = onVideoQualityChange,
                audioQuality = audioQuality,
                onAudioQualityChange = onAudioQualityChange,
                relatedTracks = relatedTracks,
                isLoadingRelated = isLoadingRelated,
                onPlayRelatedTrack = onPlayRelatedTrack,
                playerController = playerController,
                onDownloadClick = onDownloadClick,
                onCancelDownload = onCancelDownload,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onClose = onClose,
                onToggleLike = onToggleLike,
                onOpenQueue = onOpenQueue,
                onOpenSettings = onOpenSettings,
                onAddToPlaylist = onAddToPlaylist
            )
        } else {
            PlayerPortrait(
                mediaItem = mediaItem,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPosition = currentPosition,
                duration = duration,
                isLiked = isLiked,
                volume = volume,
                shuffleModeEnabled = shuffleModeEnabled,
                repeatMode = repeatMode,
                lyrics = lyrics,
                userTier = userTier,
                downloadState = downloadState,
                isVideoMode = isVideoMode,
                onToggleVideoMode = onToggleVideoMode,
                videoQuality = videoQuality,
                onVideoQualityChange = onVideoQualityChange,
                audioQuality = audioQuality,
                onAudioQualityChange = onAudioQualityChange,
                relatedTracks = relatedTracks,
                isLoadingRelated = isLoadingRelated,
                onPlayRelatedTrack = onPlayRelatedTrack,
                playerController = playerController,
                onDownloadClick = onDownloadClick,
                onCancelDownload = onCancelDownload,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onClose = onClose,
                onToggleLike = onToggleLike,
                onOpenQueue = onOpenQueue,
                onOpenSettings = onOpenSettings,
                onAddToPlaylist = onAddToPlaylist
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
    isVideoMode: Boolean = false,
    onToggleVideoMode: (Boolean) -> Unit = {},
    videoQuality: String = "720p",
    onVideoQualityChange: (String) -> Unit = {},
    audioQuality: String = "Medium",
    onAudioQualityChange: (String) -> Unit = {},
    relatedTracks: List<TrackEntity> = emptyList(),
    isLoadingRelated: Boolean = false,
    onPlayRelatedTrack: (TrackEntity) -> Unit = {},
    playerController: androidx.media3.common.Player? = null,
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
    val currentTitle = remember(mediaItem) { 
        val raw = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        YouTubeExtractor.cleanSongTitle(raw)
    }
    val currentArtist = remember(mediaItem) { 
        val rawTitle = mediaItem.mediaMetadata.title?.toString() ?: ""
        val rawArtist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
        YouTubeExtractor.cleanArtistName(rawTitle, rawArtist)
    }
    val currentArtwork = remember(mediaItem) { mediaItem.mediaMetadata.artworkUri }

    var skipDirection by remember { mutableIntStateOf(0) }
    val rippleAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0f) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf<String?>(null) } // null: Artwork/Video, "lyrics": Synced Lyrics, "visualizer": Beat Spectrum, "related": Similar Tracks

    LaunchedEffect(mediaItem) {
        delay(250)
        skipDirection = 0
    }

    if (showQualityDialog) {
        QualitySelectorDialog(
            isVideoMode = isVideoMode,
            currentQuality = if (isVideoMode) videoQuality else audioQuality,
            onQualitySelected = { q ->
                if (isVideoMode) onVideoQualityChange(q) else onAudioQualityChange(q)
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showEffectsDialog) {
        AudioEffectsDialog(
            onDismiss = { showEffectsDialog = false }
        )
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
        
        // --- BACKGROUND BLUR ---
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = (screenWidth.value * 0.08f).dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- TOP BAR WITH AUDIO/VIDEO SWITCHER & QUALITY ---
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIconButton(onClick = { onClose() }) { 
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = contentColor, modifier = Modifier.size(32.dp)) 
                }
                
                // YouTube Music-Style Audio / Video Switcher Pill
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { 
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { 
                                    onToggleVideoMode(false)
                                    if (activeTab == null) {
                                        // Reset to normal artwork
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (!isVideoMode) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote, 
                                        contentDescription = null, 
                                        tint = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.6f), 
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Song", 
                                        color = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.6f), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                onClick = { 
                                    onToggleVideoMode(true)
                                    activeTab = null // Switch to video display immediately
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isVideoMode) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Videocam, 
                                        contentDescription = null, 
                                        tint = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.6f), 
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Video", 
                                        color = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.6f), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
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
                            text = { Text("Quality: ${if (isVideoMode) videoQuality else audioQuality}", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                showQualityDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sound Effects & Equalizer", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                showEffectsDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White) }
                        )
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
            
            // --- CENTER DISPLAY (Artwork / Video / Synced Lyrics / Similar Tracks) ---
            val displayHeight = minOf(screenWidth * 0.88f, screenHeight * 0.42f)
            
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(displayHeight)
                .graphicsLayer {
                    shadowElevation = 50f
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                }
            ) {
                Crossfade(targetState = activeTab, label = "CenterTabTransition") { tab ->
                    when (tab) {
                        "lyrics" -> {
                            SpotifyLyricsView(
                                lyrics = lyrics,
                                currentPosition = currentPosition,
                                contentColor = contentColor,
                                onSeekToLine = onSeek
                            )
                        }
                        "visualizer" -> {
                            LiveWaveCanvas(
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "related" -> {
                            RelatedTracksSection(
                                relatedTracks = relatedTracks,
                                isLoading = isLoadingRelated,
                                onTrackClick = onPlayRelatedTrack
                            )
                        }
                        else -> {
                            // Video or Album Artwork
                            if (isVideoMode) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                    if (playerController != null) {
                                        AndroidView(
                                            factory = { ctx ->
                                                PlayerView(ctx).apply {
                                                    useController = false
                                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    player = playerController
                                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                    )
                                                }
                                            },
                                            update = { pv ->
                                                pv.player = playerController
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        FuturisticThumbnail(
                                            model = currentArtwork, 
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            } else {
                                // Artwork with double tap skip
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) { 
                                            detectTapGestures(
                                                onDoubleTap = { offset ->
                                                    if (offset.x < size.width / 2) onSkipPrevious() else onSkipNext()
                                                }
                                            ) 
                                        }
                                ) {
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
                                }
                            }
                        }
                    }
                }
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        color = contentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.15f))
            
            // --- SONG INFO ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Crossfade(targetState = currentTitle, label = "TitleCrossfade") { title ->
                        Text(
                            text = title, 
                            color = contentColor, 
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 3000,
                                    delayMillis = 2000,
                                    velocity = 30.dp
                                )
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

            // --- DISCOVERY ACTION PILLS (Lyrics / Similar Tracks / Artwork) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics Pill
                Surface(
                    onClick = { activeTab = if (activeTab == "lyrics") null else "lyrics" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeTab == "lyrics") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (activeTab == "lyrics") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic, 
                            contentDescription = null, 
                            tint = if (activeTab == "lyrics") Color.White else Color.White.copy(alpha = 0.8f), 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Lyrics", 
                            color = if (activeTab == "lyrics") Color.White else Color.White.copy(alpha = 0.8f), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Visualizer Pill
                Surface(
                    onClick = { activeTab = if (activeTab == "visualizer") null else "visualizer" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeTab == "visualizer") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (activeTab == "visualizer") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GraphicEq, 
                            contentDescription = null, 
                            tint = if (activeTab == "visualizer") Color.White else Color.White.copy(alpha = 0.8f), 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Visualizer", 
                            color = if (activeTab == "visualizer") Color.White else Color.White.copy(alpha = 0.8f), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Similar Tracks Pill
                Surface(
                    onClick = { activeTab = if (activeTab == "related") null else "related" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeTab == "related") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (activeTab == "related") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            contentDescription = null, 
                            tint = if (activeTab == "related") Color.White else Color.White.copy(alpha = 0.8f), 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Similar Tracks", 
                            color = if (activeTab == "related") Color.White else Color.White.copy(alpha = 0.8f), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Switch back to Artwork button when lyrics or similar is open
                if (activeTab != null) {
                    Surface(
                        onClick = { activeTab = null },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = null, 
                                tint = Color.White.copy(alpha = 0.8f), 
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Artwork", 
                                color = Color.White.copy(alpha = 0.8f), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // --- SEEK BAR ---
            val progressState = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            val timeElapsed = formatTime(currentPosition)
            val timeRemaining = formatTime(duration - currentPosition)
            
            Slider(
                value = progressState, 
                onValueChange = { onSeek((it * duration).toLong()) }, 
                colors = SliderDefaults.colors(
                    thumbColor = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue, 
                    activeTrackColor = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue, 
                    inactiveTrackColor = com.ansh.ytmusicpreimum.ui.theme.GeminiBorder.copy(alpha = 0.8f)
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
}@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    isVideoMode: Boolean = false,
    onToggleVideoMode: (Boolean) -> Unit = {},
    videoQuality: String = "720p",
    onVideoQualityChange: (String) -> Unit = {},
    audioQuality: String = "Medium",
    onAudioQualityChange: (String) -> Unit = {},
    relatedTracks: List<TrackEntity> = emptyList(),
    isLoadingRelated: Boolean = false,
    onPlayRelatedTrack: (TrackEntity) -> Unit = {},
    playerController: androidx.media3.common.Player? = null,
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
    
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }

    val currentTitle = remember(mediaItem) { 
        val raw = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
        YouTubeExtractor.cleanSongTitle(raw)
    }
    val currentArtist = remember(mediaItem) { 
        val rawTitle = mediaItem.mediaMetadata.title?.toString() ?: ""
        val rawArtist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
        YouTubeExtractor.cleanArtistName(rawTitle, rawArtist)
    }

    if (showQualityDialog) {
        QualitySelectorDialog(
            isVideoMode = isVideoMode,
            currentQuality = if (isVideoMode) videoQuality else audioQuality,
            onQualitySelected = { q ->
                if (isVideoMode) onVideoQualityChange(q) else onAudioQualityChange(q)
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showEffectsDialog) {
        AudioEffectsDialog(
            onDismiss = { showEffectsDialog = false }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        
        // Background Blur
        AsyncImage(
            model = mediaItem.mediaMetadata.artworkUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))
            
        val contentColor = Color.White
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // --- TOP BAR IN LANDSCAPE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = contentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // YouTube Music-Style Audio / Video Switcher Pill
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { onToggleVideoMode(false) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (!isVideoMode) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Song",
                                        color = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                onClick = { onToggleVideoMode(true) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isVideoMode) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Video",
                                        color = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedIconButton(onClick = { showEffectsDialog = true }) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- MAIN LANDSCAPE BODY ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Video or Artwork
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.95f)
                        .graphicsLayer {
                            shadowElevation = 35f
                            shape = RoundedCornerShape(20.dp)
                            clip = true
                        }
                ) {
                    if (isVideoMode) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            if (playerController != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            player = playerController
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { pv -> pv.player = playerController },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                FuturisticThumbnail(
                                    model = mediaItem.mediaMetadata.artworkUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        FuturisticThumbnail(
                            model = mediaItem.mediaMetadata.artworkUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isBuffering) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = contentColor)
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Right Side: Info & Controls & Volume
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTitle,
                        color = contentColor,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 3000,
                                delayMillis = 2000,
                                velocity = 30.dp
                            )
                    )
                    Text(
                        text = currentArtist,
                        color = contentColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Seekbar
                    val progressState = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    Slider(
                        value = progressState,
                        onValueChange = { onSeek((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = contentColor,
                            activeTrackColor = contentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.2f)
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        Text("-" + formatTime(duration - currentPosition), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }

                    if (downloadState != null && downloadState.status == DownloadStatus.DOWNLOADING) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Playback Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = contentColor, modifier = Modifier.size(38.dp))
                        }
                        PlayPauseMorph(isPlaying, isBuffering, isLandscape = true, onClick = onTogglePlay)
                        AnimatedIconButton(onClick = onSkipNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = contentColor, modifier = Modifier.size(38.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sleek Integrated Volume Slider (Replaces clunky debug widget)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (volume == 0f) Icons.Default.VolumeOff else if (volume < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Actions Row: Shuffle, Liked, Queue, More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        HeartBeatButton(isLiked = isLiked, onClick = onToggleLike, tint = Color.White)
                        AnimatedIconButton(onClick = onOpenQueue) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            AnimatedIconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = (-16).dp, y = 0.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Quality: ${if (isVideoMode) videoQuality else audioQuality}", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        showQualityDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White) }
                                )
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
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyLyricsView(
    lyrics: LyricsState,
    currentPosition: Long,
    contentColor: Color,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        color = Color.Black.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (lyrics.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading live lyrics...", color = contentColor.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            } else if (lyrics.isSynced && lyrics.lines.isNotEmpty()) {
                val listState = rememberLazyListState()
                val activeIndex = remember(currentPosition, lyrics.lines) {
                    lyrics.lines.indexOfLast { it.time <= currentPosition + 150L }
                }

                LaunchedEffect(activeIndex) {
                    if (activeIndex >= 0 && activeIndex in lyrics.lines.indices) {
                        val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                        listState.animateScrollToItem(targetIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 90.dp, bottom = 120.dp, start = 12.dp, end = 12.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isCurrent = index == activeIndex
                        val isPast = activeIndex >= 0 && index < activeIndex
                        
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = if (isCurrent) FontWeight.Black else if (isPast) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = if (isCurrent) 22.sp else 17.sp,
                                lineHeight = if (isCurrent) 30.sp else 24.sp
                            ),
                            color = when {
                                isCurrent -> Color.White
                                isPast -> Color.White.copy(alpha = 0.65f)
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeekToLine(line.time) }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }
                }
            } else if (!lyrics.plainLyrics.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = lyrics.plainLyrics,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MicOff, contentDescription = null, tint = contentColor.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            lyrics.error ?: "No lyrics found for this track",
                            color = contentColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelatedTracksSection(
    relatedTracks: List<TrackEntity>,
    isLoading: Boolean,
    onTrackClick: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        color = Color.Black.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        } else if (relatedTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No similar tracks found", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(relatedTracks) { track ->
                    Surface(
                        onClick = { onTrackClick(track) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualitySelectorDialog(
    isVideoMode: Boolean,
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = if (isVideoMode) {
        listOf("1080p", "720p", "360p", "Auto")
    } else {
        listOf("High (320kbps)", "Medium (160kbps)", "Low (64kbps)")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isVideoMode) "Video Streaming Quality" else "Audio Streaming Quality",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    val isSelected = if (isVideoMode) {
                        currentQuality.contains(opt, ignoreCase = true)
                    } else {
                        val key = when {
                            opt.startsWith("High") -> "High"
                            opt.startsWith("Medium") -> "Medium"
                            else -> "Low"
                        }
                        currentQuality.equals(key, ignoreCase = true)
                    }
                    Surface(
                        onClick = {
                            val selectedKey = if (isVideoMode) {
                                opt
                            } else {
                                when {
                                    opt.startsWith("High") -> "High"
                                    opt.startsWith("Medium") -> "Medium"
                                    else -> "Low"
                                }
                            }
                            onQualitySelected(selectedKey)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E1E28),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun LyricsScreen(lyrics: String?, isFetching: Boolean, contentColor: Color) {
    SpotifyLyricsView(
        lyrics = LyricsState(plainLyrics = lyrics, isLoading = isFetching),
        currentPosition = 0L,
        contentColor = contentColor,
        onSeekToLine = {}
    )
}

@Composable
fun AudioEffectsDialog(
    onDismiss: () -> Unit
) {
    val currentPreset by AudioEffectsManager.currentPreset.collectAsState()
    val presets = AudioEffectsManager.AVAILABLE_PRESETS

    val presetDescriptions = mapOf(
        "Flat" to "Standard natural studio sound profile",
        "Pop" to "Crisp vocal clarity with vibrant upper mids",
        "Rock" to "Punchy bass with aggressive highs & tight kick",
        "DJ / Dance" to "Club curve, heavy beat pump & fast dynamics",
        "Bass Boost" to "Rich, deep warm bass frequency amplification",
        "Deep Bass" to "Subwoofer focus: 60Hz max gain + hardware BassBoost 950",
        "3D Surrounding Sound" to "Ultra-wide 3D spatial field (Virtualizer 1000)",
        "Dolby Atmos / Studio" to "Professional master: Wide soundstage + +3.5dB Dynamic Gain"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sound Effects & Equalizer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = preset == currentPreset
                    Surface(
                        onClick = {
                            AudioEffectsManager.applyPreset(preset)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                )
                                presetDescriptions[preset]?.let { desc ->
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E1E28),
        shape = RoundedCornerShape(20.dp)
    )
}
