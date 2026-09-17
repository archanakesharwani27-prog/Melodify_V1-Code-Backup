package com.ansh.ytmusicpreimum.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ansh.ytmusicpreimum.Screen
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.components.*
import com.ansh.ytmusicpreimum.ui.navigation.BottomNavigationBar
import com.ansh.ytmusicpreimum.ui.screens.home.HomeScreen
import com.ansh.ytmusicpreimum.ui.screens.home.SectionDetailScreen
import com.ansh.ytmusicpreimum.ui.screens.search.SearchScreen
import com.ansh.ytmusicpreimum.ui.screens.library.LibraryScreen
import com.ansh.ytmusicpreimum.ui.screens.library.PlaylistDetailScreen
import com.ansh.ytmusicpreimum.ui.screens.player.MiniPlayer
import com.ansh.ytmusicpreimum.ui.screens.player.PlayerScreen
import com.ansh.ytmusicpreimum.ui.screens.settings.AboutMelodifyScreen
import com.ansh.ytmusicpreimum.ui.screens.settings.QualityOptionRow
import com.ansh.ytmusicpreimum.ui.screens.settings.SettingsScreen
import com.ansh.ytmusicpreimum.ui.theme.CyberSurface
import com.ansh.ytmusicpreimum.ui.theme.GrayText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    loginViewModel: LoginViewModel,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val libraryViewModel: LibraryViewModel = viewModel()
    val navController = rememberNavController()
    
    val userTier by loginViewModel.userTier.collectAsState()
    val subMonths by loginViewModel.subMonths.collectAsState()
    val userEmail by loginViewModel.userEmail.collectAsState()
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()

    var trackToDownload by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var showLimitDialog by remember { mutableStateOf<String?>(null) }
    var showDownloadFlyAnimation by remember { mutableStateOf<TrackEntity?>(null) }
    
    // Auto-sync when account changes
    LaunchedEffect(userEmail) {
        if (userEmail?.isNotEmpty() == true) {
            libraryViewModel.syncWithCloud()
        }
    }
    
    val context = LocalContext.current
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val isBuffering by musicViewModel.isBuffering.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentMediaItem by musicViewModel.currentMediaItem.collectAsState()
    val currentPosition by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val shuffleModeEnabled by musicViewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()
    val playbackError by musicViewModel.playbackError.collectAsState()
    playbackError?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    
    val volume by musicViewModel.volume.collectAsState()
    val audioQuality by musicViewModel.audioQuality.collectAsState()
    val lyrics by musicViewModel.lyrics.collectAsState()
    val isFetchingLyrics by musicViewModel.isFetchingLyrics.collectAsState()
    var isPlayerFull by remember { mutableStateOf(false) }
    var showMiniPlayer by remember { mutableStateOf(true) }
    
    val likedTracks by libraryViewModel.likedTracks.collectAsState()
    val isLiked = currentMediaItem?.let { item -> likedTracks.any { it.id == item.mediaId } } ?: false

    val currentQueue by musicViewModel.currentQueue.collectAsState()
    val downloadStates by musicViewModel.downloadStates.collectAsState()
    val queuedTracks by musicViewModel.queuedTracks.collectAsState()

    val onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit = { track, list ->
        val canCache = loginViewModel.canCacheLyrics()
        isPlayerFull = true
        musicViewModel.playSong(track.id, track.title, track.artist, track.thumbnailUrl, list, userTier, canCache)
        showMiniPlayer = true
    }

    var showQueueSheet by remember { mutableStateOf(false) }

    if (trackToAddToPlaylist != null) {
        val playlists by libraryViewModel.playlists.collectAsState()
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create one in Library tab first!", color = Color.Gray)
                    } else {
                        Text("Choose a playlist:", color = GrayText, modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)) {
                            items(playlists) { playlist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            libraryViewModel.addTrackToPlaylist(
                                                playlist.id,
                                                trackToAddToPlaylist!!
                                            )
                                            trackToAddToPlaylist = null
                                            Toast.makeText(
                                                context,
                                                "Added to ${playlist.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    color = Color.White.copy(alpha = 0.05f)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(playlist.name, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) { Text("Cancel") }
            },
            containerColor = CyberSurface
        )
    }

    if (showLimitDialog != null) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = null },
            containerColor = Color(0xFF1D1D27),
            title = { Text("Premium Feature", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    if (showLimitDialog == "Download") "Offline downloads are available for Melodify PRO members only. Upgrade now to listen anywhere!"
                    else "Free users can use 8D Spatial Audio for 1 minute per session. Go PRO for uninterrupted 3D sound.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(onClick = { showLimitDialog = null; navController.navigate(Screen.Settings.route) }) {
                    Text("Upgrade Now")
                }
            }
        )
    }

    if (trackToDownload != null) {
        var downloadUsingCellular by remember { mutableStateOf(downloadNetworkType != 0) }
        AlertDialog(
            onDismissRequest = { trackToDownload = null },
            containerColor = Color(0xFF121212),
            title = { 
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { trackToDownload = null }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                    Text("Music Quality", color = Color.White, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    
                    QualityOptionRow("Normal (Recommended)", "Low", currentQuality = audioQuality) {
                        if (loginViewModel.useDownload()) {
                            musicViewModel.downloadTrack(trackToDownload!!, "Low", userTier, if(downloadUsingCellular) 1 else 0)
                            showDownloadFlyAnimation = trackToDownload
                        }
                        trackToDownload = null
                    }
                    QualityOptionRow("High", "Medium", currentQuality = audioQuality) {
                        if (loginViewModel.useDownload()) {
                            musicViewModel.downloadTrack(trackToDownload!!, "Medium", userTier, if(downloadUsingCellular) 1 else 0)
                            showDownloadFlyAnimation = trackToDownload
                        }
                        trackToDownload = null
                    }
                    QualityOptionRow("Very high", "High", currentQuality = audioQuality) {
                        if (loginViewModel.useDownload()) {
                            musicViewModel.downloadTrack(trackToDownload!!, "High", userTier, if(downloadUsingCellular) 1 else 0)
                            showDownloadFlyAnimation = trackToDownload
                        }
                        trackToDownload = null
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Using Cellular", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = downloadUsingCellular,
                            onCheckedChange = { 
                                downloadUsingCellular = it
                                loginViewModel.updateDownloadNetwork(if(it) 1 else 0)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1DB954), checkedTrackColor = Color(0xFF1DB954).copy(alpha = 0.5f))
                        )
                    }
                    Text(
                        "Please keep Melodify open and your screen unlocked. Downloads will pause if your device is idle for more than two minutes.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !isPlayerFull,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomNavigationBar(navController)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
            ) {
                BrandedBackground(isDarkTheme = isDarkTheme) {
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { 
                            HomeScreen(
                                onSongPlay = onSongPlay, 
                                onGenreClick = { _ ->
                                    navController.navigate(Screen.Search.route)
                                },
                                onViewAll = { title ->
                                    navController.navigate("section_detail/$title")
                                }
                            ) 
                        }
                        composable(Screen.Search.route) { 
                            SearchScreen(
                                onSongPlay = { track, results -> onSongPlay(track, results) },
                                downloadStates = downloadStates,
                                onDownloadClick = { track ->
                                    if (loginViewModel.canDownload()) {
                                        trackToDownload = track
                                    } else {
                                        showLimitDialog = "Download"
                                    }
                                },
                                onCancelDownload = { id -> musicViewModel.cancelDownload(id) },
                                onAddToPlaylist = { track -> trackToAddToPlaylist = track }
                            ) 
                        }
                        composable(Screen.Library.route) { 
                            LibraryScreen(
                                onSongPlay = onSongPlay,
                                downloadStates = downloadStates,
                                queuedTracks = queuedTracks,
                                onDownloadClick = { track ->
                                    if (loginViewModel.canDownload()) {
                                        trackToDownload = track
                                    } else {
                                        showLimitDialog = "Download"
                                    }
                                },
                                onCancelDownload = { id -> musicViewModel.cancelDownload(id) },
                                onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                                onPlaylistClick = { playlist ->
                                    navController.navigate("playlist_detail/${playlist.id}/${playlist.name}")
                                }
                            ) 
                        }
                        composable(
                            route = Screen.PlaylistDetail.route,
                            arguments = listOf(
                                navArgument("playlistId") { type = NavType.LongType },
                                navArgument("playlistName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistName = playlistName,
                                onSongPlay = onSongPlay,
                                onBack = { navController.popBackStack() },
                                downloadStates = downloadStates,
                                onDownloadClick = { track ->
                                    if (loginViewModel.canDownload()) {
                                        trackToDownload = track
                                    } else {
                                        showLimitDialog = "Download"
                                    }
                                },
                                onCancelDownload = { id -> musicViewModel.cancelDownload(id) }
                            )
                        }
                        composable(
                            route = Screen.SectionDetail.route,
                            arguments = listOf(navArgument("title") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title") ?: "Explore"
                            SectionDetailScreen(
                                title = title,
                                onSongPlay = onSongPlay,
                                onBack = { navController.popBackStack() },
                                downloadStates = downloadStates,
                                onDownloadClick = { track ->
                                    if (loginViewModel.canDownload()) {
                                        trackToDownload = track
                                    } else {
                                        showLimitDialog = "Download"
                                    }
                                },
                                onCancelDownload = { id -> musicViewModel.cancelDownload(id) }
                            )
                        }
                        composable(Screen.Settings.route) { 
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = onThemeChange,
                                audioQuality = audioQuality,
                                onQualityChange = { musicViewModel.setAudioQuality(it) },
                                musicViewModel = musicViewModel,
                                loginViewModel = loginViewModel,
                                navController = navController,
                                userTier = userTier,
                                subMonths = subMonths,
                                onLogout = {
                                    musicViewModel.resetPlayer()
                                    loginViewModel.logout(context)
                                }
                            ) 
                        }
                        composable(Screen.PrivacyPolicy.route) {
                            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.About.route) {
                            AboutMelodifyScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }

                showDownloadFlyAnimation?.let { track ->
                    DownloadFlyAnimation(
                        track = track,
                        onAnimationFinished = { showDownloadFlyAnimation = null }
                    )
                }

                if (currentMediaItem != null && showMiniPlayer) {
                    AnimatedVisibility(
                        visible = !isPlayerFull,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(MotionMedium, easing = PremiumEasing)
                        ) + fadeIn(tween(MotionSmall)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(MotionMedium, easing = PremiumEasing)
                        ) + fadeOut(tween(MotionSmall)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        MiniPlayer(
                            mediaItem = currentMediaItem!!,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            downloadStates = downloadStates,
                            onTogglePlay = { musicViewModel.togglePlayPause() },
                            onClick = { isPlayerFull = true },
                            onDismiss = { 
                                showMiniPlayer = false
                                musicViewModel.resetPlayer()
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isPlayerFull,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(MotionLarge, easing = PremiumEasing)
                ) + fadeIn(tween(MotionMedium)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(MotionLarge, easing = PremiumEasing)
                ) + fadeOut(tween(MotionMedium))
            ) {
                currentMediaItem?.let { mediaItem ->
                    PlayerScreen(
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
                        downloadState = downloadStates[mediaItem.mediaId],
                        onDownloadClick = {
                            trackToDownload = TrackEntity(
                                id = mediaItem.mediaId,
                                title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                                artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                                thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                            )
                        },
                        onCancelDownload = {
                            musicViewModel.cancelDownload(mediaItem.mediaId)
                        },
                        onTogglePlay = { 
                            musicViewModel.togglePlayPause() 
                        },
                        onSeek = { 
                            musicViewModel.seekTo(it) 
                        },
                        onVolumeChange = {
                            musicViewModel.setVolume(it)
                        },
                        onSkipNext = { 
                            musicViewModel.skipNext(userTier) 
                        },
                        onSkipPrevious = { 
                            musicViewModel.skipPrevious(userTier) 
                        },
                        onToggleShuffle = { 
                            musicViewModel.toggleShuffle() 
                        },
                        onToggleRepeat = { 
                            musicViewModel.toggleRepeatMode() 
                        },
                        onClose = { isPlayerFull = false },
                        onToggleLike = {
                            val track = TrackEntity(
                                id = mediaItem.mediaId,
                                title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                                artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                                thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                            )
                            libraryViewModel.toggleLike(track)
                        },
                        onOpenQueue = { showQueueSheet = true },
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onAddToPlaylist = {
                            trackToAddToPlaylist = TrackEntity(
                                id = mediaItem.mediaId,
                                title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                                artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                                thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                            )
                        }
                    )
                }
            }

            if (showQueueSheet) {
                val currentIndex by musicViewModel.currentIndex.collectAsState()
                
                ModalBottomSheet(
                    onDismissRequest = { showQueueSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        
                        // Queue Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Playing Queue",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                val posText = if (currentIndex != -1) "${currentIndex + 1} of ${currentQueue.size}" else "${currentQueue.size} songs"
                                Text(
                                    text = "$posText • Up Next",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrayText
                                )
                            }
                            
                            IconButton(onClick = { musicViewModel.toggleShuffle() }) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else GrayText
                                )
                            }
                        }

                        val lazyListState = rememberLazyListState()
                        
                        LazyColumn(
                            state = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(currentQueue, key = { _, track -> track.id }) { index: Int, track: TrackEntity ->
                                val isCurrent = currentMediaItem?.mediaId == track.id
                                
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    TrackItem(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isPlaying = isPlaying && isCurrent,
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { 
                                                    val removedTrack = currentQueue[index]
                                                    musicViewModel.removeFromQueue(index)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Removed from queue",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            musicViewModel.insertIntoQueue(index, removedTrack)
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.DragHandle,
                                                    contentDescription = "Reorder",
                                                    tint = GrayText,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .padding(4.dp)
                                                        .pointerInput(Unit) {
                                                            detectDragGesturesAfterLongPress(
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    val threshold = 50f
                                                                    if (dragAmount.y > threshold && index < currentQueue.size - 1) {
                                                                        musicViewModel.moveInQueue(index, index + 1)
                                                                    } else if (dragAmount.y < -threshold && index > 0) {
                                                                        musicViewModel.moveInQueue(index, index - 1)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                )
                                            }
                                        },
                                        onClick = { 
                                            musicViewModel.playSong(track.id, track.title, track.artist, track.thumbnailUrl, currentQueue)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
