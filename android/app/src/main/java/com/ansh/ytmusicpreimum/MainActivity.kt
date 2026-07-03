@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.ansh.ytmusicpreimum


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.io.File
import kotlin.OptIn
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.scale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.data.PlaylistEntity
import com.ansh.ytmusicpreimum.data.PlaylistWithTracks
import com.ansh.ytmusicpreimum.ui.HomeViewModel
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.SearchViewModel
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.LoginScreen
import com.ansh.ytmusicpreimum.ui.SplashScreen
import com.ansh.ytmusicpreimum.ui.PrivacyPolicyScreen
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.ui.theme.*

class MainActivity : ComponentActivity() {
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            loginViewModel = viewModel()
            val context = LocalContext.current
            var showSplash by remember { mutableStateOf(true) }

            AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                },
                label = "SplashTransition"
            ) { isSplashScreen ->
                if (isSplashScreen) {
                    val showNameIntro = loginViewModel.shouldShowNameIntro()
                    val name = loginViewModel.userName.collectAsState().value ?: ""
                    
                    SplashScreen(
                        showNameIntro = showNameIntro,
                        userName = name,
                        onAnimationFinished = { showSplash = false }
                    )
                } else {
                    val sharedPrefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
                    var isDarkTheme by remember { 
                        mutableStateOf(sharedPrefs.getBoolean("dark_theme", true)) 
                    }

                    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

                    MelodifyTheme(darkTheme = isDarkTheme) {
                        if (!isLoggedIn) {
                            LoginScreen(
                                loginViewModel = loginViewModel,
                                onLoginSuccess = { name, email, photo ->
                                    loginViewModel.onLoginSuccess(name, email, photo)
                                }
                            )
                        } else {
                            val musicViewModel: MusicViewModel = viewModel()
                            
                            LaunchedEffect(Unit) {
                                musicViewModel.initController(context)
                            }

                            MainScreen(
                                musicViewModel = musicViewModel,
                                loginViewModel = loginViewModel,
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { 
                                    isDarkTheme = it
                                    sharedPrefs.edit().putBoolean("dark_theme", it).apply()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Search : Screen("search", "Search", Icons.Outlined.Search, Icons.Filled.Search)
    object Library : Screen("library", "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    object PrivacyPolicy : Screen("privacy", "Privacy Policy", Icons.Outlined.PrivacyTip, Icons.Filled.PrivacyTip)
    object About : Screen("about", "About", Icons.Outlined.Info, Icons.Filled.Info)
    object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}", "Playlist", Icons.Outlined.PlaylistPlay, Icons.Filled.PlaylistPlay)
    object SectionDetail : Screen("section_detail/{title}", "Explore", Icons.Outlined.GridView, Icons.Filled.GridView)
}

val MotionSmall = 150
val MotionMedium = 250
val MotionLarge = 350
val MelodifyEasing = FastOutSlowInEasing

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    loginViewModel: LoginViewModel,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val libraryViewModel: LibraryViewModel = viewModel()
    val navController = rememberNavController()
    
    val userEmail by loginViewModel.userEmail.collectAsState()
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()

    var trackToDownload by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
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
        isPlayerFull = true
        musicViewModel.playSong(track.id, track.title, track.artist, track.thumbnailUrl, list)
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
                        musicViewModel.downloadTrack(trackToDownload!!, "Low", if(downloadUsingCellular) 1 else 0)
                        showDownloadFlyAnimation = trackToDownload
                        trackToDownload = null
                    }
                    QualityOptionRow("High", "Medium", currentQuality = audioQuality) {
                        musicViewModel.downloadTrack(trackToDownload!!, "Medium", if(downloadUsingCellular) 1 else 0)
                        showDownloadFlyAnimation = trackToDownload
                        trackToDownload = null
                    }
                    QualityOptionRow("Very high", "High", currentQuality = audioQuality) {
                        musicViewModel.downloadTrack(trackToDownload!!, "High", if(downloadUsingCellular) 1 else 0)
                        showDownloadFlyAnimation = trackToDownload
                        trackToDownload = null
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

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
                                    trackToDownload = track
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
                                    trackToDownload = track
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
                                    trackToDownload = track
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
                                    trackToDownload = track
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
                            animationSpec = tween(MotionMedium, easing = MelodifyEasing)
                        ) + fadeIn(tween(MotionSmall)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(MotionMedium, easing = MelodifyEasing)
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
                    animationSpec = tween(MotionLarge, easing = MelodifyEasing)
                ) + fadeIn(tween(MotionMedium)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(MotionLarge, easing = MelodifyEasing)
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
                            musicViewModel.skipNext() 
                        },
                        onSkipPrevious = { 
                            musicViewModel.skipPrevious() 
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

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Library, Screen.Settings)
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "iconScale"
            )

            NavigationBarItem(
                icon = { 
                    Icon(
                        if (selected) screen.selectedIcon else screen.icon, 
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                    ) 
                },
                label = { 
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(tween(MotionSmall)) + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut(tween(MotionSmall)) + shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        Text(screen.label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = GrayText,
                    unselectedTextColor = GrayText,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun HomeScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit,
    onGenreClick: (String) -> Unit = {},
    onViewAll: (String) -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel()
) {
    val libraryViewModel: LibraryViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        homeViewModel.setRepository(libraryViewModel.repository)
    }

    val trending by homeViewModel.trendingSongs.collectAsState()
    val forYou by homeViewModel.forYouSongs.collectAsState()
    val moodMixes by homeViewModel.moodMixes.collectAsState()
    val evergreenMixes by homeViewModel.evergreenEmotionalMixes.collectAsState()
    val partyMixes by homeViewModel.highEnergyPartyMixes.collectAsState()
    val festiveMixes by homeViewModel.culturalFestiveMixes.collectAsState()
    val smartMixes by homeViewModel.smartMixes.collectAsState()
    val globalHits by homeViewModel.globalHits.collectAsState()
    val curatedSections by homeViewModel.curatedSections.collectAsState()
    val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    // Predictive Prefetch for Home
    LaunchedEffect(trending) {
        if (trending.isNotEmpty()) {
            musicViewModel.prefetchTracks(trending)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. GREETING
        item { HomeHeader(homeViewModel) }

        // 2. MELODIFY PICKS (Hero)
        item {
            val picks = curatedSections.find { it.first == "Melodify Picks" }?.second ?: emptyList()
            if (picks.isNotEmpty()) {
                CuratedHeroSection(tracks = picks, onSongPlay = onSongPlay)
            }
        }

        // 3. GENRE CHIPS
        item { GenreRow(onGenreClick) }
        
        if (isLoading && trending.isEmpty()) {
            items(5) { HomeSectionSkeleton() }
        } else {
        // 4. TRENDING NOW
        item {
            if (trending.isNotEmpty()) {
                SectionTitle("Trending Now", icon = Icons.Default.Whatshot)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trending) { track -> SongCard(track) { onSongPlay(track, trending) } }
                }
            }
        }

        // 5. FOR YOU
        item {
            if (forYou.isNotEmpty()) {
                SectionTitle("For You", icon = Icons.Default.AutoAwesome)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(forYou) { track -> SongCard(track) { onSongPlay(track, forYou) } }
                }
            }
        }

        // 6. MOOD MIXES (Grouped)
        item {
            val workout = moodMixes.find { it.first.contains("Workout", ignoreCase = true) }?.second ?: emptyList()
            if (workout.isNotEmpty()) {
                SectionTitle("Workout Energy", icon = Icons.Default.Bolt)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(workout) { track -> SongCard(track) { onSongPlay(track, workout) } }
                }
            }
        }

        item {
            val chill = moodMixes.find { it.first.contains("Chill", ignoreCase = true) }?.second ?: emptyList()
            if (chill.isNotEmpty()) {
                SectionTitle("Chill Zone", icon = Icons.Default.Nightlight)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chill) { track -> SongCard(track) { onSongPlay(track, chill) } }
                }
            }
        }

        item {
            val focus = moodMixes.find { it.first.contains("Focus", ignoreCase = true) }?.second ?: emptyList()
            if (focus.isNotEmpty()) {
                SectionTitle("Focus Mode", icon = Icons.Default.MenuBook)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(focus) { track -> SongCard(track) { onSongPlay(track, focus) } }
                }
            }
        }

        // 7. HIDDEN GEMS
        item {
            val gems = curatedSections.find { it.first == "Hidden Gems" }?.second ?: emptyList()
            if (gems.isNotEmpty()) {
                SectionTitle("Hidden Gems", icon = Icons.Default.Diamond)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(gems) { track -> SongCard(track) { onSongPlay(track, gems) } }
                }
            }
        }

        // 8. CULTURAL MIXES
        item {
            val bhakti = festiveMixes.find { it.first.contains("Bhakti", ignoreCase = true) }?.second ?: emptyList()
            if (bhakti.isNotEmpty()) {
                SectionTitle("Bhakti Ras", icon = Icons.Default.SelfImprovement)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bhakti) { track -> SongCard(track) { onSongPlay(track, bhakti) } }
                }
            }
        }

        item {
            val shaadi = festiveMixes.find { it.first.contains("Shaadi", ignoreCase = true) }?.second ?: emptyList()
            if (shaadi.isNotEmpty()) {
                SectionTitle("Wedding Vibes", icon = Icons.Default.Favorite)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(shaadi) { track -> SongCard(track) { onSongPlay(track, shaadi) } }
                }
            }
        }

        // 9. GLOBAL HITS
        item {
            val india = globalHits.find { it.first == "Top India" }?.second ?: emptyList()
            if (india.isNotEmpty()) {
                SectionTitle("Top India", icon = Icons.Default.Public)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(india) { track -> SongCard(track) { onSongPlay(track, india) } }
                }
            }
        }

        // 10. SMART MIXES
        item {
            val radar = smartMixes.find { it.first.contains("Radar", ignoreCase = true) }?.second ?: emptyList()
            if (radar.isNotEmpty()) {
                SectionTitle("New Releases", icon = Icons.Default.NewReleases)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(radar) { track -> SongCard(track) { onSongPlay(track, radar) } }
                }
            }
        }

        // 11. RECENTLY PLAYED
        item {
            if (recentlyPlayed.isNotEmpty()) {
                SectionTitle("Recently Played", icon = Icons.Default.History)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recentlyPlayed.take(10)) { track ->
                        SongCard(track) { onSongPlay(track, recentlyPlayed) }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun HomeHeader(homeViewModel: HomeViewModel = viewModel(), loginViewModel: LoginViewModel = viewModel()) {
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MELODIFY", 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ), 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greeting, 
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ), 
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Surface(
                onClick = { homeViewModel.refresh() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Refresh", 
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your daily dose of music is ready.", 
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun GenreRow(onGenreClick: (String) -> Unit) {
    val genres = listOf(
        "Bollywood" to GenreBollywood, 
        "Punjabi" to GenrePunjabi, 
        "Romantic" to GenreRomantic, 
        "English Pop" to GenreEnglishPop, 
        "Dance/Party" to GenreDanceParty
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp), 
        horizontalArrangement = Arrangement.spacedBy(12.dp), 
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(genres) { (name, color) -> 
            FilterChip(
                selected = false,
                onClick = { onGenreClick(name) },
                label = { Text(text = name, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = color.copy(alpha = 0.15f),
                    labelColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = color.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun CuratedHeroSection(tracks: List<TrackEntity>, onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit) {
    if (tracks.isEmpty()) return
    
    // Reduced count to 4 picks as requested
    val pagerState = rememberPagerState(pageCount = { tracks.size.coerceAtMost(4) }) 
    
    Column(modifier = Modifier.padding(vertical = 12.dp)) { // Reduced vertical padding
        SectionTitle("Melodify Picks", icon = Icons.Default.Star)
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val track = tracks[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Reduced height from 200.dp
                    .clickable { onSongPlay(track, tracks) },
                shape = RoundedCornerShape(20.dp), // Slightly smaller radius
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box {
                    FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 100f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "FEATURED", 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(track.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(track.artist, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector, onViewAll: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary, 
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), 
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onViewAll != null) {
            IconButton(onClick = onViewAll) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = "View All",
                    tint = GrayText
                )
            }
        }
    }
}

@Composable
fun SongCard(track: TrackEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp) // Reduced width from 160.dp
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            )
            .padding(4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            FuturisticThumbnail(
                model = track.thumbnailUrl, 
                modifier = Modifier
                    .size(132.dp) // Reduced size from 152.dp
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer
        Text(
            text = track.title, 
            color = MaterialTheme.colorScheme.onBackground, 
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), // Smaller text style
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = track.artist, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            style = MaterialTheme.typography.labelSmall, // Smaller text style
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun HomeSectionSkeleton() {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp).shimmerEffect())
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(150.dp, 24.dp).shimmerEffect())
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(4) {
                Column(modifier = Modifier.width(150.dp)) {
                    Box(modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).shimmerEffect())
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).shimmerEffect())
                }
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width,
        targetValue = 2 * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(1200)
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.1f),
                Color.Gray.copy(alpha = 0.3f),
                Color.Gray.copy(alpha = 0.1f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width, size.height)
        )
    ).onGloballyPositioned {
        size = it.size.toSize()
    }
}

@Composable
fun SearchScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit, 
    searchViewModel: SearchViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAddToPlaylist: (TrackEntity) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val results by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchError by searchViewModel.searchError.collectAsState()
    val recent by searchViewModel.recentSearches.collectAsState()

    // Predictive Prefetch
    LaunchedEffect(results) {
        if (results.isNotEmpty()) {
            musicViewModel.prefetchTracks(results)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Text("Search", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        SearchBar(query, onQueryChange = { query = it; searchViewModel.search(it) }, onSearch = { searchViewModel.addToHistory(query) })
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (query.isBlank()) {
                item { SearchEmptyState() }
                item { Text("Browse All", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = Color.White) }
                item { GenreGrid { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) } }
                item { RecentSearches(recent = recent, onItemClick = { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) }, onDelete = { searchViewModel.removeRecentSearch(it) }, onClearAll = { searchViewModel.clearRecentSearches() }) }
            } else if (isSearching) {
                items(10) { SearchResultSkeleton() }
            } else if (searchError) {
                item { SearchErrorState { searchViewModel.search(query) } }
            } else if (results.isEmpty()) {
                item { NoResultsState() }
            } else {
                items(results, key = { it.id }) { track -> 
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(track.id) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        TrackItem(
                            track = track,
                            downloadState = downloadStates[track.id],
                            onDownloadClick = { onDownloadClick(track) },
                            onCancelDownload = { onCancelDownload(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        ) { 
                            searchViewModel.addToHistory(query)
                            onSongPlay(track, results) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchEmptyState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎵", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Search your favorite songs, artists or albums.", style = MaterialTheme.typography.bodyLarge, color = GrayText, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
fun NoResultsState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No results found", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Try another keyword.", style = MaterialTheme.typography.bodyMedium, color = GrayText)
        }
    }
}

@Composable
fun SearchErrorState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Network error", style = MaterialTheme.typography.titleMedium, color = Color.White)
            TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SearchResultSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    TextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(28.dp)),
        placeholder = { Text("Koi bhi gaana search karo...", color = GrayText) },
        leadingIcon = { 
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = if (isFocused) MaterialTheme.colorScheme.primary else GrayText,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (isFocused) 1.1f else 1.0f
                    scaleY = if (isFocused) 1.1f else 1.0f
                }
            ) 
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = GrayText)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
fun GenreGrid(onGenreClick: (String) -> Unit) {
    val genres = listOf("Bollywood" to GenreBollywood, "Punjabi" to GenrePunjabi, "Romantic" to GenreRomantic, "English Pop" to GenreEnglishPop, "Dance/Party" to GenreDanceParty, "Hip Hop" to GenreHipHop)
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier
        .height(300.dp)
        .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(genres) { (name, color) -> GenreCard(name, color, onClick = { onGenreClick(name) }) }
    }
}

@Composable
fun GenreCard(name: String, color: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier
        .fillMaxWidth()
        .height(90.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick), color = color) {
        Box(modifier = Modifier.padding(12.dp)) { Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

@Composable
fun RecentSearches(recent: List<String>, onItemClick: (String) -> Unit, onDelete: (String) -> Unit, onClearAll: () -> Unit) {
    if (recent.isEmpty()) return
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = GrayText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp)); Text("Recent Searches", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f)); Text("Clear all", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onClearAll))
        }
        recent.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item) }
                .padding(vertical = 8.dp)) {
                 Icon(Icons.Default.History, contentDescription = null, tint = GrayText, modifier = Modifier.size(18.dp))
                 Spacer(modifier = Modifier.width(16.dp)); Text(item, color = GrayText, modifier = Modifier.weight(1f))
                 IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Close, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp)) }
            }
        }
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!playlist.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description ?: "Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GrayText
            )
        }
    }
}

@Composable
fun LibraryScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit, 
    libraryViewModel: LibraryViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    queuedTracks: List<TrackEntity> = emptyList(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAddToPlaylist: (TrackEntity) -> Unit = {},
    onPlaylistClick: (PlaylistEntity) -> Unit = {}
) {
    val likedTracks by libraryViewModel.likedTracks.collectAsState()
    val localTracks by libraryViewModel.localTracks.collectAsState()
    val downloadedTracks by libraryViewModel.downloadedTracks.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    val history by libraryViewModel.playbackHistory.collectAsState()
    var selectedTab by remember { mutableStateOf("Playlists") }
    val context = LocalContext.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Predictive Prefetch for Library Tabs
    LaunchedEffect(selectedTab, likedTracks, downloadedTracks, history, localTracks) {
        val tracksToPrefetch = when (selectedTab) {
            "Liked" -> likedTracks
            "Downloads" -> downloadedTracks
            "History" -> history.map { TrackEntity(id = it.trackId, title = it.title, artist = it.artist, thumbnailUrl = it.thumbnailUrl) }
            "Local" -> localTracks
            else -> emptyList()
        }
        if (tracksToPrefetch.isNotEmpty()) {
            musicViewModel.prefetchTracks(tracksToPrefetch)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) libraryViewModel.loadLocalTracks() else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show() }
    
    LaunchedEffect(selectedTab) { 
        if (selectedTab == "Local") { 
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) libraryViewModel.loadLocalTracks() 
            else launcher.launch(permission) 
        } 
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist", color = Color.White) },
            text = {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (playlistName.isNotBlank()) {
                        libraryViewModel.createPlaylist(playlistName)
                        showCreatePlaylistDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            },
            containerColor = CyberSurface
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your Library", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreatePlaylistDialog = true }, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) { 
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) 
            }
        }
        LazyRow(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
             item(key = "tab_playlists") { LibraryChip("Playlists", count = playlists.size, isSelected = selectedTab == "Playlists", onClick = { selectedTab = "Playlists" }) }
             item(key = "tab_history") { LibraryChip("History", count = history.size, isSelected = selectedTab == "History", onClick = { selectedTab = "History" }) }
             item(key = "tab_liked") { LibraryChip("Liked", count = likedTracks.size, isSelected = selectedTab == "Liked", onClick = { selectedTab = "Liked" }) }
             item(key = "tab_downloads") { LibraryChip("Downloads", count = (downloadedTracks.size + downloadStates.size + queuedTracks.size), isSelected = selectedTab == "Downloads", onClick = { selectedTab = "Downloads" }) }
             item(key = "tab_local") { LibraryChip("Local", count = localTracks.size, isSelected = selectedTab == "Local", onClick = { selectedTab = "Local" }) }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "tabContent"
        ) { targetTab ->
            when (targetTab) {
                "History" -> {
                    if (history.isEmpty()) {
                        LibraryEmptyState(
                            icon = Icons.Default.History,
                            title = "No history yet",
                            subtitle = "Songs you play will appear here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(history, key = { it.trackId + it.timestamp }) { item ->
                                val track = remember(item) {
                                    TrackEntity(id = item.trackId, title = item.title, artist = item.artist, thumbnailUrl = item.thumbnailUrl)
                                }
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    TrackItem(
                                        track = track,
                                        downloadState = downloadStates[item.trackId],
                                        onDownloadClick = { onDownloadClick(track) },
                                        onCancelDownload = { onCancelDownload(item.trackId) },
                                        onAddToPlaylist = { onAddToPlaylist(track) }
                                    ) {
                                        onSongPlay(track, history.map { TrackEntity(id = it.trackId, title = it.title, artist = it.artist, thumbnailUrl = it.thumbnailUrl) })
                                    }
                                }
                            }
                        }
                    }
                }
                "Liked" -> {
                    if (likedTracks.isEmpty()) {
                        LibraryEmptyState(
                            icon = Icons.Default.Favorite,
                            title = "❤️ No favorite songs yet.",
                            subtitle = "Start adding songs you love."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(likedTracks, key = { it.id }) { track ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    TrackItem(
                                        track = track,
                                        downloadState = downloadStates[track.id],
                                        onDownloadClick = { onDownloadClick(track) },
                                        onCancelDownload = { onCancelDownload(track.id) },
                                        onAddToPlaylist = { onAddToPlaylist(track) }
                                    ) { onSongPlay(track, likedTracks) }
                                }
                            }
                        }
                    }
                }
                "Downloads" -> {
                    val downloadingSongs = downloadStates.keys.toList()
                    if (downloadedTracks.isEmpty() && downloadingSongs.isEmpty() && queuedTracks.isEmpty()) {
                        LibraryEmptyState(
                            icon = Icons.Default.DownloadForOffline,
                            title = "📥 No downloaded songs.",
                            subtitle = "Download music to listen offline."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // 1. Show active downloads
                            items(downloadingSongs, key = { "downloading_$it" }) { songId ->
                                val track = downloadedTracks.find { it.id == songId } ?: likedTracks.find { it.id == songId } ?: queuedTracks.find { it.id == songId }
                                if (track != null) {
                                    Box(modifier = Modifier.animateItemPlacement()) {
                                        TrackItem(
                                            track = track,
                                            downloadState = downloadStates[songId],
                                            onCancelDownload = { onCancelDownload(songId) },
                                            onAddToPlaylist = { onAddToPlaylist(track) }
                                        ) { onSongPlay(track, listOf(track)) }
                                    }
                                }
                            }
                            // 2. Show queued tracks
                            items(queuedTracks.filter { !downloadStates.containsKey(it.id) }, key = { "queued_${it.id}" }) { track ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    TrackItem(
                                        track = track,
                                        downloadState = DownloadState(DownloadStatus.PAUSED, 0f, "Queued"),
                                        onCancelDownload = { onCancelDownload(track.id) },
                                        onAddToPlaylist = { onAddToPlaylist(track) }
                                    ) { onSongPlay(track, listOf(track)) }
                                }
                            }
                            // 3. Show completed downloads
                            items(downloadedTracks.filter { !downloadStates.containsKey(it.id) }, key = { "downloaded_${it.id}" }) { track ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    TrackItem(
                                        track = track,
                                        onAddToPlaylist = { onAddToPlaylist(track) }
                                    ) { onSongPlay(track, downloadedTracks) }
                                }
                            }
                        }
                    }
                }
                "Playlists" -> {
                    if (playlists.isEmpty() && likedTracks.isEmpty()) {
                        LibraryEmptyState(
                            icon = Icons.Default.PlaylistPlay,
                            title = "🎵 No playlists yet.",
                            subtitle = "Create your first playlist."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item(key = "liked_songs_header") {
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedTab = "Liked" },
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                                        ),
                                                        RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White) }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Liked Songs", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                Text("${likedTracks.size} songs", color = GrayText, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                            }
                            items(playlists, key = { it.id }) { playlist ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                                }
                            }
                        }
                    }
                }
                "Local" -> {
                    if (localTracks.isEmpty()) {
                        LibraryEmptyState(
                            icon = Icons.Default.Folder,
                            title = "No local songs found",
                            subtitle = "Grant permission to see your music."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(localTracks, key = { it.id }) { track ->
                                TrackItem(track = track, onAddToPlaylist = { onAddToPlaylist(track) }) { onSongPlay(track, localTracks) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit,
    onBack: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {}
) {
    val playlistWithTracksFlow = remember(playlistId) { libraryViewModel.getPlaylistWithTracks(playlistId) }
    val playlistWithTracks by playlistWithTracksFlow.collectAsState(initial = null)
    
    // Predictive Prefetch for Playlist
    LaunchedEffect(playlistWithTracks) {
        playlistWithTracks?.tracks?.let { tracks ->
            if (tracks.isNotEmpty()) {
                musicViewModel.prefetchTracks(tracks)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(playlistName, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val tracks = playlistWithTracks?.tracks ?: emptyList()
        if (tracks.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.QueueMusic,
                title = "No songs in this playlist",
                subtitle = "Add some music to get started!"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        downloadState = downloadStates[track.id],
                        onDownloadClick = { onDownloadClick(track) },
                        onCancelDownload = { onCancelDownload(track.id) }
                    ) { onSongPlay(track, tracks) }
                }
            }
        }
    }
}


@Composable
fun SectionDetailScreen(
    title: String,
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {}
) {
    val tracks = homeViewModel.getTracksBySection(title)
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (tracks.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.MusicNote,
                title = "No songs found",
                subtitle = "Check back later!"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        downloadState = downloadStates[track.id],
                        onDownloadClick = { onDownloadClick(track) },
                        onCancelDownload = { onCancelDownload(track.id) },
                        onAddToPlaylist = { libraryViewModel.toggleLike(track) } // Simple implementation
                    ) { onSongPlay(track, tracks) }
                }
            }
        }
    }
}
@Composable
fun LibraryChip(
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        label = "chipBackground"
    )
    val contentColor by animateColorAsState(
        if (isSelected) Color.White else GrayText,
        label = "chipContent"
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (label) {
                "Liked" -> Icons.Default.Favorite
                "Local" -> Icons.Default.Folder
                "Downloads" -> Icons.Default.DownloadForOffline
                "Playlists" -> Icons.Default.PlaylistPlay
                "History" -> Icons.Default.History
                else -> null
            }
            
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
            
            if (count != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = count.toString(),
                    color = contentColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    audioQuality: String,
    onQualityChange: (String) -> Unit,
    musicViewModel: MusicViewModel,
    loginViewModel: LoginViewModel,
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()
    val crossfadeDuration by musicViewModel.crossfadeDuration.collectAsState()
    val userName by loginViewModel.userName.collectAsState()
    val userEmail by loginViewModel.userEmail.collectAsState()
    val userPhoto by loginViewModel.userPhotoUrl.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) Color.Black else Color(0xFFF7F9FC)
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    // Calculate cache size
    var cacheSize by remember { mutableStateOf("0 MB") }
    LaunchedEffect(Unit) {
        val size = getFolderSize(context.cacheDir) + getFolderSize(context.externalCacheDir ?: context.cacheDir)
        cacheSize = formatSize(size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- PROFILE SECTION ---
            
            ProfileHeroCard(
                userName = userName ?: "Guest User",
                userEmail = userEmail ?: "No email linked",
                userPhoto = userPhoto,
                isDarkTheme = isDarkTheme,
                onClick = { showEditProfileDialog = true }
            )

            // --- PLAYBACK SECTION ---
            SettingsSection(title = "Playback", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Audio Quality",
                    subtitle = audioQuality,
                    icon = Icons.Default.HighQuality,
                    isDarkTheme = isDarkTheme
                ) {
                    val nextQuality = when (audioQuality) {
                        "High" -> "Low"
                        "Medium" -> "High"
                        else -> "Medium"
                    }
                    onQualityChange(nextQuality)
                }
                SettingsItemRow(
                    title = "Crossfade",
                    subtitle = when (crossfadeDuration) {
                        0L -> "Off"
                        2000L -> "2 Seconds"
                        4000L -> "4 Seconds"
                        6000L -> "6 Seconds"
                        else -> "Off"
                    },
                    icon = Icons.Default.Shuffle, // Using shuffle icon as a placeholder for transition
                    isDarkTheme = isDarkTheme
                ) {
                    val nextDuration = when (crossfadeDuration) {
                        0L -> 2000L
                        2000L -> 4000L
                        4000L -> 6000L
                        else -> 0L
                    }
                    musicViewModel.setCrossfadeDuration(nextDuration)
                }
            }

            // --- DOWNLOADS SECTION ---
            SettingsSection(title = "Downloads", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Download Network",
                    subtitle = if (downloadNetworkType == 0) "WiFi Only" else "All Networks",
                    icon = Icons.Default.NetworkCheck,
                    isDarkTheme = isDarkTheme
                ) {
                    loginViewModel.updateDownloadNetwork(if (downloadNetworkType == 0) 1 else 0)
                }
                SettingsItemRow(
                    title = "Offline Downloads",
                    subtitle = "Manage your downloaded tracks",
                    icon = Icons.Default.Download,
                    isDarkTheme = isDarkTheme
                ) {
                    navController.navigate(Screen.Library.route)
                }
            }

            // --- APPEARANCE SECTION ---
            SettingsSection(title = "Appearance", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Dark Theme",
                    icon = Icons.Default.Palette,
                    isDarkTheme = isDarkTheme,
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onThemeChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) {
                    onThemeChange(!isDarkTheme)
                }
            }

            // --- STORAGE SECTION ---
            SettingsSection(title = "Storage", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Clear Cache",
                    subtitle = "Used: $cacheSize",
                    icon = Icons.Default.Storage,
                    isDarkTheme = isDarkTheme
                ) {
                    showClearCacheDialog = true
                }
            }

            // --- FEEDBACK & ABOUT ---
            SettingsSection(title = "Feedback & About", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Give Feedback",
                    subtitle = "Help us improve Melodify",
                    icon = Icons.Default.Feedback,
                    isDarkTheme = isDarkTheme
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSc94nJebwL4pM0V2Wrda9xXNFUgpCDKMiWeA0pJKaIxQIU8Iw/viewform?usp=publish-editor"))
                    context.startActivity(intent)
                }
                SettingsItemRow(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip,
                    isDarkTheme = isDarkTheme
                ) {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
                SettingsItemRow(
                    title = "About Melodify",
                    subtitle = "Version 1.0.0",
                    icon = Icons.Default.Info,
                    isDarkTheme = isDarkTheme
                ) {
                    navController.navigate(Screen.About.route)
                }
            }

            // --- ACCOUNT SECTION ---
            SettingsSection(title = "Account", isDarkTheme = isDarkTheme) {
                SettingsItemRow(
                    title = "Switch Account",
                    icon = Icons.Default.AccountCircle,
                    isDarkTheme = isDarkTheme,
                    iconColor = Color.Gray
                ) {
                    onLogout()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showEditProfileDialog) {
        var newName by remember { mutableStateOf(userName ?: "") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = if(isDarkTheme) Color(0xFF212121) else Color.White,
            title = { Text("Edit Profile", color = contentColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { galleryLauncher.launch("image/*") }) {
                        AsyncImage(
                            model = selectedImageUri ?: userPhoto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.ic_menu_camera)
                        )
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                        }
                    }
                    TextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                    Text("Tap photo to select from gallery", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    loginViewModel.updateProfile(newName, selectedImageUri?.toString() ?: userPhoto)
                    showEditProfileDialog = false
                }) { Text("Save") }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Are you sure you want to clear the app cache? This will not delete your downloads.") },
            confirmButton = {
                TextButton(onClick = {
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                    val size = getFolderSize(context.cacheDir) + getFolderSize(context.externalCacheDir ?: context.cacheDir)
                    cacheSize = formatSize(size)
                    showClearCacheDialog = false
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileHeroCard(
    userName: String,
    userEmail: String,
    userPhoto: String?,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "heroScale")
    
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = accentColor.copy(alpha = 0.1f)),
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF1A1A1A).copy(alpha = 0.8f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (!isDarkTheme) androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = userPhoto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = SolidColor(Color.Gray.copy(alpha = 0.2f)),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                )

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    isDarkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF121212) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if(!isDarkTheme) androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)) else null
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    isDarkTheme: Boolean,
    iconColor: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    (iconColor ?: (if (isDarkTheme) Color.White else Color.Black)).copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor ?: (if (isDarkTheme) Color.White else Color.Black),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun AboutMelodifyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Melodify", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF00B29A), Color(0xFF00E5FF))),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Melodify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "v2.0.0 Stable",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Melodify is a high-fidelity music experience designed for those who value quality and simplicity.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    AboutFeatureItem("High Fidelity", "Lossless audio streaming and downloads.", Icons.Default.HighQuality)
                    AboutFeatureItem("Cloud Library", "Sync your music across all devices.", Icons.Default.CloudSync)
                    AboutFeatureItem("Smart Mixes", "Personalized music for every mood.", Icons.Default.AutoAwesome)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Developed by Ansh Kesharwani",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Terms • Privacy • Licenses",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

fun getFolderSize(file: File): Long {
    var size: Long = 0
    if (file.exists() && file.isDirectory) {
        file.listFiles()?.forEach {
            size += if (it.isDirectory) getFolderSize(it) else it.length()
        }
    } else if (file.exists()) {
        size = file.length()
    }
    return size
}

fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        else -> "%.2f KB".format(kb)
    }
}

@Composable
fun AboutFeatureItem(title: String, desc: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun DownloadQualityItem(label: String, estimate: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, color = color)
                Text(estimate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.Download, contentDescription = null, tint = color)
        }
    }
}

@Composable
fun UpgradeOption(name: String, price: String, features: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Black, color = color, fontSize = 18.sp)
                Text(price, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(features, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column { 
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) 
        }
    }
}

@Composable
fun TrackItem(
    track: TrackEntity,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    downloadState: DownloadState? = null,
    onDownloadClick: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "trackScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(MotionSmall),
        label = "trackElevation"
    )

    val backgroundColor = remember(isCurrent) {
        if (isCurrent) {
            Color.Transparent // Better for production performance when combined with outer surface
        } else {
            Color.Transparent
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.primary),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                FuturisticThumbnail(
                    model = track.thumbnailUrl,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent && !isPlaying) {
                     Icon(Icons.Default.PauseCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                }
                
                when {
                    track.isDownloaded -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp).padding(end = 4.dp)
                        )
                    }
                    downloadState != null -> {
                        val statusText = if (downloadState.totalSize == "Queued") "Queued" else "${(downloadState.progress * 100).toInt()}%"
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onCancelDownload() }) {
                                    CircularProgressIndicator(
                                        progress = { if (downloadState.totalSize == "Queued") 0.1f else downloadState.progress },
                                        modifier = Modifier.size(20.dp),
                                        color = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Icon(
                                        imageVector = if (downloadState.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Cancel Download",
                                        tint = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    color = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (downloadState.totalSize != "Queued") {
                                Text(
                                    text = "${downloadState.totalSize} • ${downloadState.quality}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GrayText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    else -> {
                        if (!isCurrent) {
                            IconButton(onClick = onDownloadClick) {
                                Icon(
                                    imageVector = Icons.Default.DownloadForOffline,
                                    contentDescription = "Download",
                                    tint = GrayText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                if (trailingContent != null) {
                    trailingContent()
                } else {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = GrayText
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(x = (-16).dp, y = 0.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { showMenu = false; onAddToPlaylist() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun MiniPlayer(
    mediaItem: androidx.media3.common.MediaItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                FuturisticThumbnail(
                    model = mediaItem.mediaMetadata.artworkUri,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show download progress if downloading
                val currentSongId = mediaItem.mediaId
                val downloadState = remember(currentSongId, downloadStates) { downloadStates[currentSongId] }
                if (downloadState != null && downloadState.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, currentPosition: Long, duration: Long, isLiked: Boolean, volume: Float, shuffleModeEnabled: Boolean, repeatMode: Int, lyrics: LyricsState, downloadState: DownloadState?, onDownloadClick: () -> Unit, onCancelDownload: () -> Unit, onTogglePlay: () -> Unit, onSeek: (Long) -> Unit, onVolumeChange: (Float) -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit, onClose: () -> Unit, onToggleLike: () -> Unit, onOpenQueue: () -> Unit, onOpenSettings: () -> Unit, onAddToPlaylist: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (isLandscape) {
            PlayerLandscape(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, lyrics, downloadState, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
            )
        } else {
            PlayerPortrait(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, lyrics, downloadState, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
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
    mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, currentPosition: Long, duration: Long, isLiked: Boolean, volume: Float, shuffleModeEnabled: Boolean, repeatMode: Int, lyrics: LyricsState, downloadState: DownloadState?, onDownloadClick: () -> Unit, onCancelDownload: () -> Unit, onTogglePlay: () -> Unit, onSeek: (Long) -> Unit, onVolumeChange: (Float) -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit, onClose: () -> Unit, onToggleLike: () -> Unit, onOpenQueue: () -> Unit, onOpenSettings: () -> Unit, onAddToPlaylist: () -> Unit
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
fun formatTime(ms: Long): String { 
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds) 
}

@Composable
fun LiveWaveCanvas(isPlaying: Boolean) {
    val fftData by VisualizerData.fftData.collectAsState()
    val barCount = 48
    
    // Optimized: Use a simple FloatArray to avoid overhead of SnapshotStateList
    val barHeights = remember { FloatArray(barCount) { 0.1f } }
    
    // Use an Animatable or a time-based update for smoother visualization if needed, 
    // but first let's just optimize the processing.
    
    val smoothedFft = remember(fftData) {
        if (isPlaying && fftData.isNotEmpty()) {
            for (i in 0 until barCount) {
                // Map FFT index to bar index.
                val fftIndex = (i * (fftData.size / 2) / barCount) * 2
                if (fftIndex + 1 < fftData.size) {
                    val real = fftData[fftIndex].toInt()
                    val imag = fftData[fftIndex + 1].toInt()
                    val magnitude = kotlin.math.hypot(real.toDouble(), imag.toDouble()).toFloat()
                    
                    val normalized = (magnitude / 128f).coerceIn(0.1f, 1f)
                    val sensitivity = if (i < barCount / 4) 1.6f else 1.0f 
                    val targetHeight = normalized * sensitivity
                    
                    // Simple smoothing
                    barHeights[i] = barHeights[i] * 0.3f + targetHeight * 0.7f
                }
            }
        } else {
            for (i in 0 until barCount) {
                barHeights[i] = barHeights[i] * 0.85f
            }
        }
        barHeights.copyOf() // Return a copy to trigger redraw
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
                val animatedHeight = baseAmplitude * smoothedFft[i]
                val x = i * (barWidth + spacing)
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.9f), 
                            Color(0xFFE91E63).copy(alpha = 0.9f), 
                            Color(0xFF9C27B0).copy(alpha = 0.9f)
                        )
                    ), 
                    topLeft = Offset(x, midY - animatedHeight), 
                    size = androidx.compose.ui.geometry.Size(barWidth, animatedHeight * 2), 
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                )
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
fun QualityOptionRow(label: String, value: String, currentQuality: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp)
        if (value == currentQuality) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1DB954))
        }
    }
}


@Composable
fun LyricsScreen(lyrics: String?, isFetching: Boolean, contentColor: Color) {
    Surface(modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(24.dp)), color = Color.Black.copy(alpha = 0.1f)) {
        Box(modifier = Modifier.padding(24.dp)) {
            if (isFetching) { Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(color = contentColor); Spacer(modifier = Modifier.height(16.dp)); Text("Fetching Lyrics...", color = contentColor) } } 
            else if (lyrics != null) { Column(modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) { Text(text = "Lyrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor, modifier = Modifier.padding(bottom = 16.dp)); Text(text = lyrics, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp), color = contentColor.copy(alpha = 0.9f)) } }
            else { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Lyrics not found.", color = contentColor.copy(alpha = 0.5f)) } }
        }
    }
}

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
    val lineAlpha = if (isDarkTheme) 0.12f else 0.18f; val noteAlpha = if (isDarkTheme) 0.1f else 0.15f; val bokehAlpha = if (isDarkTheme) 0.08f else 0.03f; val tintColor = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF00897B)
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val bokehPoints = listOf(Pair(Offset(w * 0.15f, h * 0.25f), 80.dp.toPx()), Pair(Offset(w * 0.85f, h * 0.1f), 120.dp.toPx()), Pair(Offset(w * 0.25f, h * 0.75f), 100.dp.toPx()), Pair(Offset(w * 0.75f, h * 0.85f), 90.dp.toPx()), Pair(Offset(w * 0.5f, h * 0.5f), 150.dp.toPx()))
            bokehPoints.forEach { (pos, radius) -> drawCircle(brush = Brush.radialGradient(colors = listOf(tintColor.copy(alpha = bokehAlpha), Color.Transparent), center = pos, radius = radius), radius = radius, center = pos) }
            val paths = listOf(Path().apply { moveTo(w * 1.1f, h * 0.05f); cubicTo(w * 0.8f, h * 0.25f, w * 0.6f, h * 0.1f, w * 0.4f, h * 0.45f); cubicTo(w * 0.2f, h * 0.7f, w * 0.3f, h * 0.95f, w * -0.1f, h * 0.9f) }, Path().apply { moveTo(w * 1.2f, h * 0.25f); cubicTo(w * 0.9f, h * 0.45f, w * 0.7f, h * 0.3f, w * 0.5f, h * 0.65f); cubicTo(w * 0.3f, h * 0.9f, w * 0.4f, h * 1.1f, w * 0.1f, h * 1.1f) }, Path().apply { moveTo(w * 0.9f, h * -0.1f); cubicTo(w * 0.6f, h * 0.2f, w * 1.0f, h * 0.45f, w * 0.5f, h * 0.75f); cubicTo(w * 0.2f, h * 0.95f, w * 0.4f, h * 1.15f, w * -0.1f, h * 1.0f) })
            paths.forEachIndexed { i, path -> drawPath(path = path, color = tintColor.copy(alpha = lineAlpha / (i + 1)), style = Stroke(width = (2.5f - i * 0.5f).dp.toPx(), cap = StrokeCap.Round)) }
            val notes = listOf(Pair(Offset(w * 0.65f, h * 0.22f), 0.7f), Pair(Offset(w * 0.48f, h * 0.42f), 1.1f), Pair(Offset(w * 0.72f, h * 0.58f), 0.8f), Pair(Offset(w * 0.38f, h * 0.72f), 0.9f), Pair(Offset(w * 0.55f, h * 0.88f), 0.6f))
            notes.forEach { (pos, s) -> drawCircle(color = tintColor.copy(alpha = noteAlpha), radius = 5.dp.toPx() * s, center = pos); drawLine(color = tintColor.copy(alpha = noteAlpha), start = Offset(pos.x + 4.dp.toPx() * s, pos.y), end = Offset(pos.x + 4.dp.toPx() * s, pos.y - 15.dp.toPx() * s), strokeWidth = 2.dp.toPx() * s, cap = StrokeCap.Round); drawLine(color = tintColor.copy(alpha = noteAlpha), start = Offset(pos.x + 4.dp.toPx() * s, pos.y - 15.dp.toPx() * s), end = Offset(pos.x + 14.dp.toPx() * s, pos.y - 12.dp.toPx() * s), strokeWidth = 2.dp.toPx() * s, cap = StrokeCap.Round) }
        }
        content()
    }
}
