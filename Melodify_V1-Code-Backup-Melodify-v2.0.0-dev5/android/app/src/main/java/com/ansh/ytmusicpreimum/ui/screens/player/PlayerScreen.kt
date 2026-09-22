package com.ansh.ytmusicpreimum.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
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
import com.ansh.ytmusicpreimum.ABLooperManager
import com.ansh.ytmusicpreimum.HapticBassManager
import com.ansh.ytmusicpreimum.AmbientSoundscapeManager
import com.ansh.ytmusicpreimum.SongShareHelper
import com.ansh.ytmusicpreimum.SleepTimerManager
import androidx.compose.ui.platform.LocalContext
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
    isOfflineTrack: Boolean = false,
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
                isOfflineTrack = isOfflineTrack,
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
                isOfflineTrack = isOfflineTrack,
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
    isOfflineTrack: Boolean = false,
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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var lyricToShare by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf<String?>(null) } // null: Artwork/Video, "lyrics": Synced Lyrics, "visualizer": Beat Spectrum, "related": Similar Tracks

    val context = LocalContext.current
    val isSleepTimerActive by SleepTimerManager.isActive.collectAsState()
    var ambientColors by remember { mutableStateOf(AlbumGlowExtractor.defaultColors()) }

    LaunchedEffect(currentArtwork) {
        ambientColors = AlbumGlowExtractor.extractColors(context, currentArtwork)
    }

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

        // --- DYNAMIC AMBIENT GLOW (Apple Music style) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambientColors.first.copy(alpha = 0.40f),
                        ambientColors.second.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.32f),
                    radius = size.width * 0.88f
                )
            )
        }
        
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.88f),
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
                .padding(horizontal = (screenWidth.value * 0.08f).dp)
                .padding(bottom = 16.dp), 
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
                
                // If not offline track, show YouTube Music-Style Audio / Video Switcher Pill
                if (!isOfflineTrack) {
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
                } else {
                    // Sleek Offline Song Badge
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1DB954).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Offline Audio",
                                    color = Color(0xFF1DB954),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                        if (!isOfflineTrack) {
                            DropdownMenuItem(
                                text = { Text("Quality: ${if (isVideoMode) videoQuality else audioQuality}", color = Color.White) },
                                onClick = { 
                                    showMenu = false
                                    showQualityDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Quality: Original (Offline)", color = Color.White) },
                                onClick = { showMenu = false },
                                leadingIcon = { Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color(0xFF1DB954)) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Sound Effects & Equalizer", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                showEffectsDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White) }
                        )
                        if (!isOfflineTrack) {
                            DropdownMenuItem(
                                text = { Text("Download Song", color = Color.White) },
                                onClick = { 
                                    showMenu = false
                                    onDownloadClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.White) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Saved to Offline Storage", color = Color(0xFF1DB954)) },
                                onClick = { showMenu = false },
                                leadingIcon = { Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color(0xFF1DB954)) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                onAddToPlaylist()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Song", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                SongShareHelper.shareTrack(
                                    context = context,
                                    title = currentTitle,
                                    artist = currentArtist,
                                    videoId = mediaItem.mediaId,
                                    offlineFilePath = if (isOfflineTrack) mediaItem.localConfiguration?.uri?.path else null
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                showSleepTimerDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFA78BFA)) }
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

            // Live Sleep Timer Indicator Pill
            if (isSleepTimerActive) {
                Surface(
                    onClick = { showSleepTimerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFC4B5FD), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Sleep Timer: ${SleepTimerManager.formatRemainingTime()}",
                            color = Color(0xFFC4B5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.08f, fill = false))
            
            // --- CENTER DISPLAY (Artwork / Video / Synced Lyrics / Similar Tracks) ---
            val targetDisplayHeight = if (activeTab != null) {
                minOf(screenWidth * 0.70f, screenHeight * 0.28f)
            } else {
                minOf(screenWidth * 0.82f, screenHeight * 0.38f)
            }
            val displayHeight by animateDpAsState(
                targetValue = targetDisplayHeight,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "DisplayHeightAnim"
            )
            
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
                                onSeekToLine = onSeek,
                                onShareLyricLine = { lyricToShare = it }
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
                        "ab_looper" -> {
                            ABLooperCard(
                                currentPosition = currentPosition,
                                onSeek = onSeek
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
            
            Spacer(modifier = Modifier.weight(0.08f, fill = false))
            
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            SongShareHelper.shareTrack(
                                context = context,
                                title = currentTitle,
                                artist = currentArtist,
                                videoId = mediaItem.mediaId,
                                offlineFilePath = if (isOfflineTrack) mediaItem.localConfiguration?.uri?.path else null
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Song",
                            tint = contentColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    HeartBeatButton(
                        isLiked = isLiked,
                        onClick = onToggleLike,
                        tint = contentColor
                    )
                }
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

                // Similar Tracks Pill (Online only)
                if (!isOfflineTrack) {
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
                }

                // A-B Looper Pill
                val isLoopActive by ABLooperManager.isLooping.collectAsState()
                Surface(
                    onClick = { activeTab = if (activeTab == "ab_looper") null else "ab_looper" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeTab == "ab_looper" || isLoopActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (activeTab == "ab_looper" || isLoopActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Repeat, 
                            contentDescription = null, 
                            tint = if (activeTab == "ab_looper" || isLoopActive) Color.White else Color.White.copy(alpha = 0.8f), 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isLoopActive) "Loop Active" else "A-B Looper", 
                            color = if (activeTab == "ab_looper" || isLoopActive) Color.White else Color.White.copy(alpha = 0.8f), 
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
            
            Spacer(modifier = Modifier.weight(0.08f, fill = false))
            
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
            
            Spacer(modifier = Modifier.heightIn(min = 6.dp).weight(0.12f, fill = false))
            
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
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(onDismiss = { showSleepTimerDialog = false })
    }

    if (lyricToShare != null) {
        LyricShareCardDialog(
            initialLine = lyricToShare!!,
            allLines = lyrics.lines.map { it.text },
            songTitle = currentTitle,
            songArtist = currentArtist,
            artworkUrl = currentArtwork,
            onDismiss = { lyricToShare = null }
        )
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
    isOfflineTrack: Boolean = false,
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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var lyricToShare by remember { mutableStateOf<String?>(null) }
    val isSleepTimerActive by SleepTimerManager.isActive.collectAsState()
    val context = LocalContext.current
    val currentArtwork = remember(mediaItem) { mediaItem.mediaMetadata.artworkUri }

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

                // Audio / Video Switcher Pill (Online) vs Offline Badge (Local)
                if (!isOfflineTrack) {
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
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1DB954).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Offline Audio",
                                    color = Color(0xFF1DB954),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                    text = { Text("Share Song", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        SongShareHelper.shareTrack(
                                            context = context,
                                            title = currentTitle,
                                            artist = currentArtist,
                                            videoId = mediaItem.mediaId,
                                            offlineFilePath = if (isOfflineTrack) mediaItem.localConfiguration?.uri?.path else null
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        showSleepTimerDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFA78BFA)) }
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

    if (showSleepTimerDialog) {
        SleepTimerDialog(onDismiss = { showSleepTimerDialog = false })
    }

    if (lyricToShare != null) {
        LyricShareCardDialog(
            initialLine = lyricToShare!!,
            allLines = lyrics.lines.map { it.text },
            songTitle = currentTitle,
            songArtist = currentArtist,
            artworkUrl = currentArtwork,
            onDismiss = { lyricToShare = null }
        )
    }
}

@Composable
fun SpotifyLyricsView(
    lyrics: LyricsState,
    currentPosition: Long,
    contentColor: Color,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onShareLyricLine: (String) -> Unit = {}
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 8.dp, end = 8.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isCurrent = index == activeIndex
                        val isPast = activeIndex >= 0 && index < activeIndex
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSeekToLine(line.time) }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                IconButton(
                                    onClick = { onShareLyricLine(line.text) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Lyric Card",
                                        tint = Color(0xFF1DB954),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!lyrics.plainLyrics.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LYRICS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { onShareLyricLine(lyrics.plainLyrics.take(280)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Lyric Card",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
fun ABLooperCard(
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pointA by ABLooperManager.pointA.collectAsState()
    val pointB by ABLooperManager.pointB.collectAsState()
    val isLooping by ABLooperManager.isLooping.collectAsState()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF141420),
        border = BorderStroke(1.5.dp, if (isLooping) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "A-B Hook & Segment Looper",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                if (isLooping) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Current Time & Loop Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                val loopDescription = if (pointA != null && pointB != null) {
                    val durationSec = ((pointB!! - pointA!!) / 1000).coerceAtLeast(1)
                    "Repeating ${formatTime(pointA!!)} → ${formatTime(pointB!!)} (${durationSec}s loop)"
                } else if (pointA != null) {
                    "Point A locked at ${formatTime(pointA!!)}. Tap [Set B] to loop."
                } else {
                    "Tap [Set Point A] to mark start of loop"
                }
                Text(
                    text = loopDescription,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // A and B Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { ABLooperManager.setPointA(currentPosition) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pointA != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = if (pointA != null) "A: ${formatTime(pointA!!)}" else "Set Point [A]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { ABLooperManager.setPointB(currentPosition) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pointB != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = if (pointB != null) "B: ${formatTime(pointB!!)}" else "Set Point [B]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Actions (Clear / Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pointA != null || pointB != null) {
                    OutlinedButton(
                        onClick = { ABLooperManager.clearLoop() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Loop", fontSize = 11.sp)
                    }
                }
                if (pointA != null && pointB != null) {
                    Button(
                        onClick = {
                            ABLooperManager.toggleLoop()
                            val a = pointA
                            if (!isLooping && a != null) onSeek(a)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isLooping) "Pause Loop" else "Resume Loop", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioEffectsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        AudioEffectsManager.init(context, 0)
    }

    val isEnabled by AudioEffectsManager.isEnabled.collectAsState()
    val currentPreset by AudioEffectsManager.currentPreset.collectAsState()
    val bandFreqs by AudioEffectsManager.bandCenterFreqs.collectAsState()
    val bandLevels by AudioEffectsManager.bandLevels.collectAsState()
    val bassBoostStrength by AudioEffectsManager.bassBoostStrength.collectAsState()
    val virtualizerStrength by AudioEffectsManager.virtualizerStrength.collectAsState()
    val loudnessGain by AudioEffectsManager.loudnessGain.collectAsState()

    val isHapticEnabled by HapticBassManager.isEnabled.collectAsState()
    val hapticIntensity by HapticBassManager.intensity.collectAsState()
    val ambientSoundscape by AmbientSoundscapeManager.selectedSoundscape.collectAsState()
    val ambientVolume by AmbientSoundscapeManager.volume.collectAsState()

    val presets = AudioEffectsManager.AVAILABLE_PRESETS
    val minLevel = AudioEffectsManager.minBandLevel
    val maxLevel = AudioEffectsManager.maxBandLevel

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .wrapContentHeight(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Equalizer & Audio FX",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnabled) "DSP Engine Active" else "DSP Engine Bypassed",
                            fontSize = 11.sp,
                            color = if (isEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { AudioEffectsManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Presets Horizontal Row
                Text(
                    text = "PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = preset == currentPreset
                        Surface(
                            onClick = { AudioEffectsManager.applyPreset(preset) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = preset,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // 5-Band Equalizer Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5-BAND EQUALIZER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "±15 dB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val defaultLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                    val bandCount = bandFreqs.size.coerceAtLeast(5)

                    for (i in 0 until bandCount) {
                        val freqLabel = if (i < bandFreqs.size) {
                            val f = bandFreqs[i]
                            if (f >= 1000) {
                                val kHz = f / 1000f
                                if (kHz % 1f == 0f) "${kHz.toInt()}kHz" else "${String.format(java.util.Locale.US, "%.1f", kHz)}kHz"
                            } else {
                                "${f}Hz"
                            }
                        } else {
                            defaultLabels.getOrElse(i) { "Band $i" }
                        }
                        val level = if (i < bandLevels.size) bandLevels[i] else 0

                        VerticalBandFader(
                            freqLabel = freqLabel,
                            levelMilliBels = level,
                            minLevel = minLevel,
                            maxLevel = maxLevel,
                            enabled = isEnabled,
                            onLevelChange = { newLevel ->
                                AudioEffectsManager.setBandLevel(i, newLevel)
                            }
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // DSP Enhancements
                Text(
                    text = "HARDWARE DSP ENHANCERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )

                // Bass Boost
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Bass Boost",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${(bassBoostStrength / 10f).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                    }
                    Slider(
                        value = bassBoostStrength.toFloat(),
                        onValueChange = { AudioEffectsManager.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF5252),
                            activeTrackColor = Color(0xFFFF5252),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                // 3D Spatial Virtualizer
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "3D Spatial Virtualizer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${(virtualizerStrength / 10f).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Slider(
                        value = virtualizerStrength.toFloat(),
                        onValueChange = { AudioEffectsManager.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                // Loudness Preamp Gain
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Loudness Preamp (Studio Boost)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        val gainDb = loudnessGain / 100f
                        Text(
                            text = if (gainDb > 0) "+${String.format(java.util.Locale.US, "%.1f", gainDb)} dB" else "0.0 dB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }
                    Slider(
                        value = loudnessGain.toFloat(),
                        onValueChange = { AudioEffectsManager.setLoudness(it.toInt()) },
                        valueRange = 0f..800f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB300),
                            activeTrackColor = Color(0xFFFFB300),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Haptic Bass Subwoofer Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color(0xFFE040FB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Haptic Bass Subwoofer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Feel bass drop kicks in your hands",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { HapticBassManager.setEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE040FB),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                    if (isHapticEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vibration Force", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${(hapticIntensity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
                        }
                        Slider(
                            value = hapticIntensity,
                            onValueChange = { HapticBassManager.setIntensity(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE040FB),
                                activeTrackColor = Color(0xFFE040FB),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Ambient Lo-Fi Soundscape Layer Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Ambient Soundscape Layer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Overlay relaxing rain, cafe, or vinyl crackle",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbientSoundscapeManager.AVAILABLE_SOUNDSCAPES.forEach { soundscape ->
                            val isSelected = soundscape == ambientSoundscape
                            Surface(
                                onClick = { AmbientSoundscapeManager.selectSoundscape(soundscape) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Text(
                                    text = soundscape,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    if (ambientSoundscape != "Off") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ambient Mix Volume", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${(ambientVolume * 100 / 0.6f).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                        }
                        Slider(
                            value = ambientVolume,
                            onValueChange = { AmbientSoundscapeManager.setVolume(it) },
                            valueRange = 0.05f..0.6f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF64B5F6),
                                activeTrackColor = Color(0xFF64B5F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { AudioEffectsManager.resetToFlat() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Flat")
            }
        },
        containerColor = Color(0xFF181822),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun VerticalBandFader(
    freqLabel: String,
    levelMilliBels: Int,
    minLevel: Int,
    maxLevel: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val db = (levelMilliBels / 100)
    val dbText = if (db > 0) "+${db}dB" else "${db}dB"
    val range = (maxLevel - minLevel).coerceAtLeast(1)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = dbText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (!enabled) Color.Gray else if (db > 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.03f))
                .pointerInput(enabled, minLevel, maxLevel) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newLevel = (minLevel + fraction * range).toInt()
                        onLevelChange(newLevel)
                    }
                }
                .pointerInput(enabled, minLevel, maxLevel) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val newLevel = (minLevel + fraction * range).toInt()
                        onLevelChange(newLevel)
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val fillFraction = ((levelMilliBels - minLevel).toFloat() / range).coerceIn(0f, 1f)
            // Fill gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(
                        Brush.verticalGradient(
                            if (enabled) listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ) else listOf(
                                Color.Gray.copy(alpha = 0.4f),
                                Color.Gray.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            // Center 0 dB guide line
            val zeroFraction = ((0 - minLevel).toFloat() / range).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(zeroFraction),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
            // Thumb slider handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(width = 28.dp, height = 7.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (enabled) Color.White else Color.Gray)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = freqLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White.copy(alpha = 0.9f) else Color.Gray
        )
    }
}
