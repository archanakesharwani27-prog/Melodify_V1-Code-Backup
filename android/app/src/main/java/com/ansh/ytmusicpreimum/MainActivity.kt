@file:OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.ansh.ytmusicpreimum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
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
import kotlinx.coroutines.delay
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.data.PlaylistEntity
import com.ansh.ytmusicpreimum.ui.HomeViewModel
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.SearchViewModel
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.LoginScreen
import com.ansh.ytmusicpreimum.ui.SplashScreen
import com.ansh.ytmusicpreimum.ui.PrivacyPolicyScreen
import com.ansh.ytmusicpreimum.ui.EqBand
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.ui.theme.*

import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultListener {
    private lateinit var loginViewModel: LoginViewModel
    private var pendingTier: Int = 0
    private var pendingMonths: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
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
                                },
                                onStartPayment = { tier, months, amount ->
                                    startPayment(tier, months, amount)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startPayment(tier: Int, months: Int, amount: String) {
        pendingTier = tier
        pendingMonths = months
        
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
        
        try {
            val options = JSONObject()
            options.put("name", "Melodify Premium")
            options.put("description", "Upgrade to PRO Plan")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#E91E63")
            options.put("currency", "INR")
            options.put("amount", (amount.toInt() * 100).toString()) // Amount in paise
            options.put("prefill.email", loginViewModel.userEmail.value ?: "")
            
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        loginViewModel.onPaymentVerified(pendingTier, pendingMonths)
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
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
}

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    loginViewModel: LoginViewModel,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onStartPayment: (Int, Int, String) -> Unit
) {
    val libraryViewModel: LibraryViewModel = viewModel()
    val navController = rememberNavController()
    
    val userTier by loginViewModel.userTier.collectAsState()
    val subMonths by loginViewModel.subMonths.collectAsState()
    val userEmail by loginViewModel.userEmail.collectAsState()
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()
    val paymentSuccess by loginViewModel.paymentSuccess.collectAsState()

    var pendingUpgrade by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showProcessingDialog by remember { mutableStateOf(false) }
    var trackToDownload by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var showLimitDialog by remember { mutableStateOf<String?>(null) }
    
    // Handle Razorpay Success
    LaunchedEffect(paymentSuccess) {
        paymentSuccess?.let { (tier, months) ->
            pendingUpgrade = tier to months
            showProcessingDialog = true
            loginViewModel.consumePayment()
        }
    }
    
    // Auto-sync when account changes
    LaunchedEffect(userEmail) {
        if (userEmail?.isNotEmpty() == true) {
            libraryViewModel.syncWithCloud()
        }
    }
    
    val context = LocalContext.current
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val isBuffering by musicViewModel.isBuffering.collectAsState()
    val currentMediaItem by musicViewModel.currentMediaItem.collectAsState()
    val currentPosition by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val shuffleModeEnabled by musicViewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()
    val playbackError by musicViewModel.playbackError.collectAsState()
    val volume by musicViewModel.volume.collectAsState()
    val audioQuality by musicViewModel.audioQuality.collectAsState()
    val lyrics by musicViewModel.lyrics.collectAsState()
    val isFetchingLyrics by musicViewModel.isFetchingLyrics.collectAsState()
    var isPlayerFull by remember { mutableStateOf(false) }
    var showMiniPlayer by remember { mutableStateOf(true) }
    
    val likedTracks by libraryViewModel.likedTracks.collectAsState()
    val isLiked = currentMediaItem?.let { item -> likedTracks.any { it.id == item.mediaId } } ?: false

    val currentQueue by musicViewModel.currentQueue.collectAsState()
    val eqBands by musicViewModel.eqBands.collectAsState()
    val eqLevelRange by musicViewModel.eqLevelRange.collectAsState()
    val bassBoostStrength by musicViewModel.bassBoostStrength.collectAsState()
    val virtualizerStrength by musicViewModel.virtualizerStrength.collectAsState()
    val downloadStates by musicViewModel.downloadStates.collectAsState()
    val queuedTracks by musicViewModel.queuedTracks.collectAsState()

    LaunchedEffect(showProcessingDialog) {
        if (showProcessingDialog) {
            delay(3000)
            pendingUpgrade?.let { (tier, months) ->
                loginViewModel.upgradeTier(tier, months)
            }
            showProcessingDialog = false
            pendingUpgrade = null
            Toast.makeText(context, "Payment Verified! Enjoy Premium Features.", Toast.LENGTH_LONG).show()
        }
    }

    val initiatePayment: (Int, Int, String) -> Unit = { tier, months, amount ->
        if (userEmail == "archanakesharwani27@gmail.com" || userEmail == "anshkesharwani0807@gmail.com") {
            loginViewModel.upgradeTier(tier, months)
            Toast.makeText(context, "Developer Override: PRO Activated!", Toast.LENGTH_SHORT).show()
        } else if (loginViewModel.isGuestUser) {
            // Handled
        } else {
            onStartPayment(tier, months, amount)
        }
    }

    LaunchedEffect(playbackError) {
        playbackError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit = { track, list ->
        val canCache = loginViewModel.canCacheLyrics()
        musicViewModel.playSong(track.id, track.title, track.artist, track.thumbnailUrl, list, userTier, canCache)
        isPlayerFull = true
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
                        }
                        trackToDownload = null
                    }
                    QualityOptionRow("High", "Medium", currentQuality = audioQuality) {
                        if (loginViewModel.useDownload()) {
                            musicViewModel.downloadTrack(trackToDownload!!, "Medium", userTier, if(downloadUsingCellular) 1 else 0)
                        }
                        trackToDownload = null
                    }
                    QualityOptionRow("Very high", "High", currentQuality = audioQuality) {
                        if (loginViewModel.useDownload()) {
                            musicViewModel.downloadTrack(trackToDownload!!, "High", userTier, if(downloadUsingCellular) 1 else 0)
                        }
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
            if (!isPlayerFull) {
                BottomNavigationBar(navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            BrandedBackground(isDarkTheme = isDarkTheme) {
                NavHost(navController = navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { 
                        HomeScreen(onSongPlay, onGenreClick = { genre ->
                            navController.navigate(Screen.Search.route)
                        }) 
                    }
                    composable(Screen.Search.route) { 
                        SearchScreen(
                            onSongPlay = { track -> onSongPlay(track, listOf(track)) },
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
                    composable(Screen.Settings.route) { 
                        SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            onThemeChange = onThemeChange,
                            audioQuality = audioQuality,
                            onQualityChange = { musicViewModel.setAudioQuality(it) },
                            loginViewModel = loginViewModel,
                            navController = navController,
                            userTier = userTier,
                            subMonths = subMonths,
                            onUpgradeClick = initiatePayment,
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

            if (currentMediaItem != null && showMiniPlayer) {
                AnimatedVisibility(
                    visible = !isPlayerFull,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                     MiniPlayer(
                        mediaItem = currentMediaItem!!,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onTogglePlay = { musicViewModel.togglePlayPause() },
                        onClick = { isPlayerFull = true },
                        onDismiss = { showMiniPlayer = false }
                    )
                }
            }
        }
    }

    if (showProcessingDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CyberSurface,
            title = { Text("Processing Payment", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Verifying transaction with your bank...", color = Color.Gray, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {}
        )
    }

    if (isPlayerFull) {
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
                eqBands = eqBands,
                eqLevelRange = eqLevelRange,
                bassBoostStrength = bassBoostStrength,
                virtualizerStrength = virtualizerStrength,
                lyrics = lyrics,
                isFetchingLyrics = isFetchingLyrics,
                userTier = userTier,
                downloadState = downloadStates[mediaItem.mediaId],
                onLevelChange = { index, level ->
                    musicViewModel.setEqBandLevel(index, level)
                },
                onBassBoostChange = { strength ->
                    musicViewModel.setBassBoost(strength)
                },
                onVirtualizerChange = { strength ->
                    if (strength > 0 && userTier < 2) {
                        if (loginViewModel.canUse8D()) {
                            musicViewModel.setVirtualizer(strength, userTier)
                        } else {
                            showLimitDialog = "8D"
                        }
                    } else {
                        musicViewModel.setVirtualizer(strength, userTier)
                    }
                },
                onDownloadClick = {
                    if (loginViewModel.canDownload()) {
                        trackToDownload = TrackEntity(
                            id = mediaItem.mediaId,
                            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                            thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                        )
                    } else {
                        showLimitDialog = "Download"
                    }
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
        } ?: run {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            LaunchedEffect(Unit) {
                delay(5000)
                if (currentMediaItem == null) { isPlayerFull = false }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier
                .fillMaxHeight(0.6f)
                .padding(16.dp)) {
                Text(
                    "Playing Queue",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    itemsIndexed(currentQueue) { index: Int, track: TrackEntity ->
                        val isCurrent = currentMediaItem?.mediaId == track.id
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { 
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        musicViewModel.removeFromQueue(index)
                                        true
                                    } else false
                                }
                            ),
                            backgroundContent = {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.8f))
                                    .padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                }
                            }
                        ) {
                            TrackItem(
                                track = track,
                                isCurrent = isCurrent,
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
            NavigationBarItem(
                icon = { Icon(if (selected) screen.selectedIcon else screen.icon, contentDescription = null) },
                label = { Text(screen.label, fontSize = 10.sp) },
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
    homeViewModel: HomeViewModel = viewModel()
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
    val isLoading by homeViewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item { HomeHeader(homeViewModel) }
        item { GenreRow(onGenreClick) }
        
        if (isLoading && trending.isEmpty()) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            if (trending.isNotEmpty()) {
                SectionTitle("Trending Now", icon = Icons.Default.Whatshot)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trending) { track ->
                        SongCard(track) { onSongPlay(track, trending) }
                    }
                }
            }
        }

        item {
            if (forYou.isNotEmpty()) {
                SectionTitle("For You", icon = Icons.Default.AutoAwesome)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(forYou) { track ->
                        SongCard(track) { onSongPlay(track, forYou) }
                    }
                }
            }
        }

        items(moodMixes) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.AutoMirrored.Filled.QueueMusic)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }

        item {
            if (evergreenMixes.isNotEmpty()) {
                Text("🌟 The Evergreen & Emotional Section", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
        }
        items(evergreenMixes) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.Default.Stars)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }

        item {
            if (partyMixes.isNotEmpty()) {
                Text("🔥 The High-Energy & Party Section", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
        }
        items(partyMixes) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.Default.MusicNote)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }

        item {
            if (festiveMixes.isNotEmpty()) {
                Text("🔱 The Cultural & Festive Section", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
        }
        items(festiveMixes) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.Default.Celebration)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }

        items(smartMixes) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.Default.AutoAwesome)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }

        items(globalHits) { (title, tracks) ->
            if (tracks.isNotEmpty()) {
                SectionTitle(title, icon = Icons.Default.Public)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks) { track -> SongCard(track) { onSongPlay(track, tracks) } }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(homeViewModel: HomeViewModel = viewModel(), loginViewModel: LoginViewModel = viewModel()) {
    val userTier by loginViewModel.userTier.collectAsState()
    val subMonths by loginViewModel.subMonths.collectAsState()
    
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning!"
            in 12..16 -> "Good afternoon!"
            in 17..20 -> "Good evening!"
            else -> "Good night!"
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "MELODIFY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            if (userTier >= 2) {
                Spacer(modifier = Modifier.width(6.dp))
                PremiumBadge(subMonths = subMonths)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = greeting, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { homeViewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text(text = "Kya sunna chahoge aaj?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PremiumBadge(subMonths: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShineAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            subMonths >= 12 -> {
                // 1 Year: Animated Shinning Golden Star
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Annual Pro",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                alpha = shineAlpha
                                scaleX = 1.1f
                                scaleY = 1.1f
                            },
                        tint = Color(0xFFFFD700)
                    )
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFFFA000)
                    )
                }
            }
            subMonths >= 6 -> {
                // 6 Months: Golden Star
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "6 Month Pro",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFFD700)
                )
            }
            else -> {
                // 1 Month: Normal Golden Badge
                Surface(
                    modifier = Modifier.size(18.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFD700),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8860B))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumBadgeAnimated(modifier: Modifier = Modifier) {
    // Legacy - replaced by PremiumBadge logic in header
}

@Composable
fun GenreRow(onGenreClick: (String) -> Unit) {
    val genres = listOf("Bollywood" to GenreBollywood, "Punjabi" to GenrePunjabi, "Romantic" to GenreRomantic, "English Pop" to GenreEnglishPop, "Dance/Party" to GenreDanceParty)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
        items(genres) { (name, color) -> GenreChip(name, color, onClick = { onGenreClick(name) }) }
    }
}

@Composable
fun GenreChip(name: String, color: Color, onClick: () -> Unit) {
    Surface(color = color.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Text(text = name, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun SongCard(track: TrackEntity, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(140.dp)
        .clickable(onClick = onClick)) {
        FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(8.dp)))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = track.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = track.artist, color = GrayText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SearchScreen(
    onSongPlay: (TrackEntity) -> Unit, 
    searchViewModel: SearchViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAddToPlaylist: (TrackEntity) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val results by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val recent by searchViewModel.recentSearches.collectAsState()

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent), contentPadding = PaddingValues(bottom = 80.dp)) {
        item { Text("Search", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground) }
        item { SearchBar(query, onQueryChange = { query = it; searchViewModel.search(it) }, onSearch = { searchViewModel.addToHistory(query) }) }
        if (results.isEmpty() && !isSearching) {
            item { Text("Browse All", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = Color.White) }
            item { GenreGrid { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) } }
            item { RecentSearches(recent = recent, onItemClick = { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) }, onDelete = { searchViewModel.removeRecentSearch(it) }, onClearAll = { searchViewModel.clearRecentSearches() }) }
        } else {
            items(results) { track -> 
                TrackItem(
                    track = track,
                    downloadState = downloadStates[track.id],
                    onDownloadClick = { onDownloadClick(track) },
                    onCancelDownload = { onCancelDownload(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) }
                ) { 
                    searchViewModel.addToHistory(query)
                    onSongPlay(track) 
                } 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    TextField(
        value = query, onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp)),
        placeholder = { Text("Koi bhi gaana search karo...", color = GrayText) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
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
fun LibraryScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit, 
    libraryViewModel: LibraryViewModel = viewModel(),
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
             item { LibraryChip("Playlists", count = playlists.size, isSelected = selectedTab == "Playlists", onClick = { selectedTab = "Playlists" }) }
             item { LibraryChip("History", count = history.size, isSelected = selectedTab == "History", onClick = { selectedTab = "History" }) }
             item { LibraryChip("Liked", count = likedTracks.size, isSelected = selectedTab == "Liked", onClick = { selectedTab = "Liked" }) }
             item { LibraryChip("Downloads", count = (downloadedTracks.size + downloadStates.size + queuedTracks.size), isSelected = selectedTab == "Downloads", onClick = { selectedTab = "Downloads" }) }
             item { LibraryChip("Local", count = localTracks.size, isSelected = selectedTab == "Local", onClick = { selectedTab = "Local" }) }
        }

        if (selectedTab == "History") {
            if (history.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No history yet", color = GrayText) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                items(history) { item -> 
                    TrackItem(
                        track = TrackEntity(id = item.trackId, title = item.title, artist = item.artist, thumbnailUrl = item.thumbnailUrl),
                        downloadState = downloadStates[item.trackId],
                        onDownloadClick = { onDownloadClick(TrackEntity(id = item.trackId, title = item.title, artist = item.artist, thumbnailUrl = item.thumbnailUrl)) },
                        onCancelDownload = { onCancelDownload(item.trackId) },
                        onAddToPlaylist = { onAddToPlaylist(TrackEntity(id = item.trackId, title = item.title, artist = item.artist, thumbnailUrl = item.thumbnailUrl)) }
                    ) { 
                        onSongPlay(TrackEntity(id = item.trackId, title = item.title, artist = item.artist, thumbnailUrl = item.thumbnailUrl), history.map { TrackEntity(id = it.trackId, title = it.title, artist = it.artist, thumbnailUrl = it.thumbnailUrl) }) 
                    } 
                } 
            }
        } else if (selectedTab == "Liked") {
            if (likedTracks.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No liked songs yet", color = GrayText) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                items(likedTracks) { track -> 
                    TrackItem(
                        track = track,
                        downloadState = downloadStates[track.id],
                        onDownloadClick = { onDownloadClick(track) },
                        onCancelDownload = { onCancelDownload(track.id) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    ) { onSongPlay(track, likedTracks) } 
                } 
            }
        } else if (selectedTab == "Downloads") {
            val downloadingSongs = downloadStates.keys.toList()
            if (downloadedTracks.isEmpty() && downloadingSongs.isEmpty() && queuedTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No downloads yet", color = GrayText) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                    // 1. Show active downloads
                    items(downloadingSongs) { songId ->
                         val track = downloadedTracks.find { it.id == songId } ?: likedTracks.find { it.id == songId } ?: queuedTracks.find { it.id == songId }
                         if (track != null) {
                            TrackItem(track = track, downloadState = downloadStates[songId], onCancelDownload = { onCancelDownload(songId) }, onAddToPlaylist = { onAddToPlaylist(track) }) { onSongPlay(track, listOf(track)) }
                         }
                    }
                    // 2. Show queued tracks
                    items(queuedTracks.filter { !downloadStates.containsKey(it.id) }) { track ->
                        TrackItem(track = track, downloadState = DownloadState(DownloadStatus.PAUSED, 0f, "Queued"), onCancelDownload = { onCancelDownload(track.id) }, onAddToPlaylist = { onAddToPlaylist(track) }) { onSongPlay(track, listOf(track)) }
                    }
                    // 3. Show completed downloads
                    items(downloadedTracks.filter { !downloadStates.containsKey(it.id) }) { track -> 
                        TrackItem(track = track, onAddToPlaylist = { onAddToPlaylist(track) }) { onSongPlay(track, downloadedTracks) } 
                    }
                }
            }
        } else if (selectedTab == "Playlists") {
             LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 item {
                     Surface(modifier = Modifier
                         .fillMaxWidth()
                         .padding(vertical = 8.dp)
                         .clip(RoundedCornerShape(12.dp))
                         .clickable { selectedTab = "Liked" }, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(8.dp)
                                ), contentAlignment = Alignment.Center) { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White) }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column { Text("Liked Songs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("${likedTracks.size} songs", color = GrayText, fontSize = 12.sp) }
                            Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                 }
                 items(playlists) { playlist ->
                    Surface(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlaylistClick(playlist) }, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier
                                .size(48.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White) 
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column { 
                                Text(playlist.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(playlist.description ?: "User Playlist", color = GrayText, fontSize = 12.sp) 
                            }
                        }
                    }
                 }
             }
        } else if (selectedTab == "Local") {
            if (localTracks.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No local songs found", color = GrayText) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                items(localTracks) { track -> 
                    TrackItem(track = track, onAddToPlaylist = { onAddToPlaylist(track) }) { onSongPlay(track, localTracks) } 
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
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {}
) {
    val playlistWithTracksFlow = remember(playlistId) { libraryViewModel.getPlaylistTracks(playlistId) }
    val playlistWithTracks by playlistWithTracksFlow.collectAsState(initial = null)
    
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs in this playlist", color = GrayText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { track ->
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
fun LibraryChip(label: String, isSelected: Boolean = false, count: Int? = null, onClick: () -> Unit = {}) {
    Surface(color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (label == "Liked") Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else GrayText)
            if (label == "Local") Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else GrayText)
            if (label == "Downloads") Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else GrayText)
            Spacer(modifier = Modifier.width(4.dp)); Text(label, color = if (isSelected) Color.White else GrayText, fontSize = 12.sp)
            if (count != null) { Spacer(modifier = Modifier.width(4.dp)); Text(count.toString(), color = if (isSelected) Color.White.copy(alpha = 0.7f) else GrayText, fontSize = 12.sp) }
        }
    }
}

@Composable
fun SettingsScreen(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit, audioQuality: String, onQualityChange: (String) -> Unit, loginViewModel: LoginViewModel, navController: NavController, userTier: Int, subMonths: Int, onUpgradeClick: (Int, Int, String) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()
    val userName by loginViewModel.userName.collectAsState()
    val userEmail by loginViewModel.userEmail.collectAsState()
    val userPhoto by loginViewModel.userPhotoUrl.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    Column(modifier = Modifier
        .fillMaxSize()
        .background(bgColor)
        .verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
            }
            Text("Account", style = MaterialTheme.typography.titleLarge, color = contentColor, fontWeight = FontWeight.Bold)
        }

        // --- PROFILE SECTION ---
        Row(modifier = Modifier
            .padding(16.dp)
            .clickable { showEditProfileDialog = true }, verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = userPhoto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                )
                if (userTier >= 2) {
                    PremiumBadge(
                        subMonths = subMonths, 
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName ?: "Guest User", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = contentColor)
                Text(userEmail ?: "No email linked", color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = contentColor.copy(alpha = 0.1f))

        SettingsItemRow("History", Icons.Default.History, isDarkTheme) { navController.navigate(Screen.Library.route) }
        SettingsItemRow("Downloads", Icons.Default.Download, isDarkTheme) { navController.navigate(Screen.Library.route) }
        
        Divider(color = contentColor.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
        
        SettingsItemRow("Audio Quality: $audioQuality", Icons.Default.HighQuality, isDarkTheme) {
            val nextQuality = when (audioQuality) {
                "High" -> "Low"
                "Medium" -> "High"
                else -> "Medium"
            }
            onQualityChange(nextQuality)
        }
        
        SettingsItemRow("Download Network: ${if(downloadNetworkType == 0) "WiFi Only" else "All Networks"}", Icons.Default.NetworkCheck, isDarkTheme) {
            loginViewModel.updateDownloadNetwork(if (downloadNetworkType == 0) 1 else 0)
        }
        
        SettingsItemRow("Appearance: ${if(isDarkTheme) "Dark" else "Light"}", Icons.Default.Palette, isDarkTheme) {
            onThemeChange(!isDarkTheme)
        }

        SettingsItemRow("Switch account", Icons.Default.AccountCircle, isDarkTheme) { onLogout() }
        
        SettingsItemRow("Privacy Policy", Icons.Default.PrivacyTip, isDarkTheme) {
            navController.navigate(Screen.PrivacyPolicy.route)
        }

        SettingsItemRow("About Melodify", Icons.Default.Info, isDarkTheme) {
            navController.navigate(Screen.About.route)
        }

        SettingsItemRow("Give Feedback", Icons.Default.Feedback, isDarkTheme) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSc94nJebwL4pM0V2Wrda9xXNFUgpCDKMiWeA0pJKaIxQIU8Iw/viewform?usp=publish-editor"))
            context.startActivity(intent)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Privacy Policy", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.clickable { navController.navigate(Screen.PrivacyPolicy.route) })
                Text("  •  ", color = Color.Gray, fontSize = 12.sp)
                Text("Terms of Service", color = Color.Gray, fontSize = 12.sp)
            }
            Text("Enjoy ad-free music, listen in audio mode and play music offline.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showUpgradeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Get Melodify Premium", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            containerColor = if(isDarkTheme) Color(0xFF212121) else Color.White,
            title = { Text("Melodify Premium", color = contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    UpgradeOption("PRO Monthly", "₹49", "Unlimited Downloads, BG Play, 320kbps", Color(0xFFE91E63)) {
                        onUpgradeClick(2, 1, "49")
                        showUpgradeDialog = false
                    }
                    UpgradeOption("PRO 6 Months", "₹99", "Best Seller! All PRO features for 6 months", Color(0xFFE91E63)) {
                        onUpgradeClick(2, 6, "99")
                        showUpgradeDialog = false
                    }
                    UpgradeOption("PRO Annual", "₹199", "Ultimate Value! Full year of Premium", Color(0xFFFFD700)) {
                        onUpgradeClick(2, 12, "199")
                        showUpgradeDialog = false
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SettingsItemRow(title: String, icon: ImageVector, isDarkTheme: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if(isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Text(title, color = if(isDarkTheme) Color.White else Color.Black, fontSize = 16.sp)
    }
}

@Composable
fun AboutMelodifyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("About Melodify", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Melodify v1.0.0", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Text("Developed by Ansh Kesharwani", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Melodify is a modern music player designed for a high-quality, seamless listening experience. Enjoy ad-free music, offline downloads, and cloud sync across your devices.",
                    color = Color.White,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Key Features", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        AboutFeatureItem("High Quality Audio", "Stream and download music up to 320kbps for crystal clear sound.", Icons.Default.HighQuality)
        AboutFeatureItem("Cloud Sync", "Your liked songs, playlists, and history are safely synced with your Gmail.", Icons.Default.CloudSync)
        AboutFeatureItem("Offline Mode", "Download your favorite tracks and listen anywhere without internet.", Icons.Default.DownloadForOffline)
        AboutFeatureItem("Minimalist Design", "A beautiful, modern UI that keeps your music front and center.", Icons.Default.Brush)
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Subscription Plans", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        UpgradeOption("PRO Monthly", "₹49", "Basic Pro features for 1 month", Color(0xFFE91E63)) {}
        Spacer(modifier = Modifier.height(8.dp))
        UpgradeOption("PRO 6 Months", "₹99", "Best value for medium term", Color(0xFFE91E63)) {}
        Spacer(modifier = Modifier.height(8.dp))
        UpgradeOption("PRO Annual", "₹199", "Ultimate value with Animated Badge", Color(0xFFFFD700)) {}
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AboutFeatureItem(title: String, desc: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(desc, color = GrayText, fontSize = 12.sp)
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
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Black, color = color, fontSize = 18.sp)
                Text(price, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(features, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
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
    downloadState: DownloadState? = null,
    onDownloadClick: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold), color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                track.isDownloaded -> { Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)) }
                downloadState != null -> {
                    val statusText = if (downloadState.totalSize == "Queued") "Queued" else "${(downloadState.progress * 100).toInt()}%"
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier
                                .size(32.dp)
                                .clickable { onCancelDownload() }) {
                                CircularProgressIndicator(progress = { if (downloadState.totalSize == "Queued") 0f else downloadState.progress }, modifier = Modifier.size(24.dp), color = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                Icon(if (downloadState.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Cancel Download", tint = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(statusText, color = if (downloadState.totalSize == "Queued") GrayText else MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (downloadState.totalSize != "Queued") {
                            Text("${downloadState.quality} • ${downloadState.totalSize}", color = GrayText, fontSize = 10.sp)
                        }
                    }
                }
                else -> { IconButton(onClick = onDownloadClick) { Icon(Icons.Default.DownloadForOffline, contentDescription = "Download", tint = GrayText, modifier = Modifier.size(24.dp)) } }
            }
            if (isCurrent) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) } 
            else {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = GrayText) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, offset = DpOffset(x = (-16).dp, y = 0.dp)) {
                    DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { showMenu = false; onAddToPlaylist() }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) })
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit, onDismiss: () -> Unit) {
    Surface(modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 8.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), tonalElevation = 8.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
        Row(modifier = Modifier
            .padding(8.dp)
            .fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            FuturisticThumbnail(model = mediaItem.mediaMetadata.artworkUri, modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mediaItem.mediaMetadata.title?.toString() ?: "Unknown", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mediaItem.mediaMetadata.artist?.toString() ?: "Unknown", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isBuffering) CircularProgressIndicator(modifier = Modifier
                .size(24.dp)
                .padding(4.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            else IconButton(onClick = onTogglePlay) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = GrayText) }
        }
    }
}

@Composable
fun PlayerScreen(
    mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, currentPosition: Long, duration: Long, isLiked: Boolean, volume: Float, shuffleModeEnabled: Boolean, repeatMode: Int, eqBands: List<EqBand>, eqLevelRange: Pair<Short, Short>, bassBoostStrength: Short, virtualizerStrength: Short, lyrics: String?, isFetchingLyrics: Boolean, userTier: Int, downloadState: DownloadState?, onLevelChange: (Int, Short) -> Unit, onBassBoostChange: (Short) -> Unit, onVirtualizerChange: (Short) -> Unit, onDownloadClick: () -> Unit, onCancelDownload: () -> Unit, onTogglePlay: () -> Unit, onSeek: (Long) -> Unit, onVolumeChange: (Float) -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit, onClose: () -> Unit, onToggleLike: () -> Unit, onOpenQueue: () -> Unit, onOpenSettings: () -> Unit, onAddToPlaylist: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (isLandscape) {
            PlayerLandscape(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, eqBands, eqLevelRange, bassBoostStrength, virtualizerStrength, lyrics, isFetchingLyrics, userTier, downloadState, onLevelChange, onBassBoostChange, onVirtualizerChange, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
            )
        } else {
            PlayerPortrait(
                mediaItem, isPlaying, isBuffering, currentPosition, duration, isLiked, volume, shuffleModeEnabled, repeatMode, eqBands, eqLevelRange, bassBoostStrength, virtualizerStrength, lyrics, isFetchingLyrics, userTier, downloadState, onLevelChange, onBassBoostChange, onVirtualizerChange, onDownloadClick, onCancelDownload, onTogglePlay, onSeek, onVolumeChange, onSkipNext, onSkipPrevious, onToggleShuffle, onToggleRepeat, onClose, onToggleLike, onOpenQueue, onOpenSettings, onAddToPlaylist
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerPortrait(
    mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, currentPosition: Long, duration: Long, isLiked: Boolean, volume: Float, shuffleModeEnabled: Boolean, repeatMode: Int, eqBands: List<EqBand>, eqLevelRange: Pair<Short, Short>, bassBoostStrength: Short, virtualizerStrength: Short, lyrics: String?, isFetchingLyrics: Boolean, userTier: Int, downloadState: DownloadState?, onLevelChange: (Int, Short) -> Unit, onBassBoostChange: (Short) -> Unit, onVirtualizerChange: (Short) -> Unit, onDownloadClick: () -> Unit, onCancelDownload: () -> Unit, onTogglePlay: () -> Unit, onSeek: (Long) -> Unit, onVolumeChange: (Float) -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit, onClose: () -> Unit, onToggleLike: () -> Unit, onOpenQueue: () -> Unit, onOpenSettings: () -> Unit, onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    
    BackHandler { onClose() }
    
    var skipDirection by remember { mutableStateOf(0) }
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
                    rippleAlpha.snapTo(0.6f)
                    launch { rippleScale.animateTo(1.5f, tween(300)) }
                    launch { rippleAlpha.animateTo(0f, tween(300)) }
                }
                onSkipNext()
            } else if (tapCount >= 3) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                skipDirection = -1
                scope.launch {
                    rippleScale.snapTo(0f)
                    rippleAlpha.snapTo(0.6f)
                    launch { rippleScale.animateTo(1.5f, tween(300)) }
                    launch { rippleAlpha.animateTo(0f, tween(300)) }
                }
                onSkipPrevious()
            }
            tapCount = 0
        }
    }

    LaunchedEffect(mediaItem) {
        delay(500)
        skipDirection = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = mediaItem.mediaMetadata.artworkUri,
            transitionSpec = { fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400)) },
            label = "BackgroundCrossfade"
        ) { artworkUri ->
            AsyncImage(model = artworkUri, contentDescription = null, modifier = Modifier
                .fillMaxSize()
                .blur(25.dp), contentScale = ContentScale.Crop)
        }
        
        val overlayColor = if (androidx.compose.material3.MaterialTheme.colorScheme.surface == Color.White) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        overlayColor.copy(alpha = 0.3f),
                        overlayColor.copy(alpha = 0.8f)
                    )
                )
            ))

        val contentColor = if (androidx.compose.material3.MaterialTheme.colorScheme.surface == Color.White) CharcoalBlack else Color.White

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp)) }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("NOW PLAYING", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                
                var showMoreMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = contentColor, modifier = Modifier.size(28.dp))
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download") },
                            onClick = { showMoreMenu = false; onDownloadClick() },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { showMoreMenu = false; onAddToPlaylist() },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (repeatMode == Player.REPEAT_MODE_OFF) "Auto Next: ON" else "Auto Next: OFF") },
                            onClick = { showMoreMenu = false; onToggleRepeat() },
                            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) }
                        )
                    }
                }
                
                IconButton(onClick = onOpenQueue) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp)) }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalPager(state = pagerState, modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val rotationY = pageOffset * -30f
                Box(modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = rotationY; cameraDistance = 8 * density }) {
                    when (page) {
                        0 -> {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .pointerInput(Unit) { detectTapGestures(onTap = { handleTap() }) }) {
                                AnimatedContent(
                                    targetState = mediaItem.mediaId,
                                    transitionSpec = {
                                        if (skipDirection == 1) { (slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()) } 
                                        else if (skipDirection == -1) { (slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut()) } 
                                        else { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }
                                    },
                                    label = "ArtworkTransition"
                                ) { _ -> FuturisticThumbnail(model = mediaItem.mediaMetadata.artworkUri, modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(shadowElevation = 20f)) }
                                Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = Color(0xFF00B29A).copy(alpha = rippleAlpha.value), radius = size.minDimension * rippleScale.value, center = center) }
                            }
                        }
                        1 -> LiveWaveCanvas(isPlaying)
                    }
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.Center) { repeat(2) { i -> Box(modifier = Modifier
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary else contentColor.copy(
                        alpha = 0.3f
                    )
                )
                .size(8.dp)) } }
            
            Spacer(modifier = Modifier.height(32.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(mediaItem.mediaMetadata.title?.toString() ?: "Unknown", color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mediaItem.mediaMetadata.artist?.toString() ?: "Unknown", color = contentColor.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Slider(value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f, onValueChange = { onSeek((it * duration).toLong()) }, colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor, inactiveTrackColor = contentColor.copy(alpha = 0.2f)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(currentPosition), color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp); Text(formatTime(duration), color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp) }
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleShuffle() }) { Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else contentColor) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipPrevious() }) { Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = contentColor, modifier = Modifier.size(56.dp)) }
                PlayPauseMorph(isPlaying, isBuffering, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTogglePlay() })
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipNext() }) { Icon(Icons.Default.SkipNext, contentDescription = null, tint = contentColor, modifier = Modifier.size(56.dp)) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleRepeat() }) { Icon(when (repeatMode) { Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat }, contentDescription = null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else contentColor) }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                 Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = contentColor.copy(alpha = 0.5f))
                 Slider(value = volume, onValueChange = onVolumeChange, modifier = Modifier
                     .weight(1f)
                     .padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor))
                 IconButton(onClick = { onToggleLike(); val msg = if (isLiked) "Removed" else "Added"; Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isLiked) Color.Red else contentColor) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerLandscape(
    mediaItem: androidx.media3.common.MediaItem, isPlaying: Boolean, isBuffering: Boolean, currentPosition: Long, duration: Long, isLiked: Boolean, volume: Float, shuffleModeEnabled: Boolean, repeatMode: Int, eqBands: List<EqBand>, eqLevelRange: Pair<Short, Short>, bassBoostStrength: Short, virtualizerStrength: Short, lyrics: String?, isFetchingLyrics: Boolean, userTier: Int, downloadState: DownloadState?, onLevelChange: (Int, Short) -> Unit, onBassBoostChange: (Short) -> Unit, onVirtualizerChange: (Short) -> Unit, onDownloadClick: () -> Unit, onCancelDownload: () -> Unit, onTogglePlay: () -> Unit, onSeek: (Long) -> Unit, onVolumeChange: (Float) -> Unit, onSkipNext: () -> Unit, onSkipPrevious: () -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit, onClose: () -> Unit, onToggleLike: () -> Unit, onOpenQueue: () -> Unit, onOpenSettings: () -> Unit, onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    BackHandler { onClose() }
    var skipDirection by remember { mutableStateOf(0) }
    val rippleAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0f) }
    var tapCount by remember { mutableIntStateOf(0) }
    var tapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val handleTap = {
        tapCount++
        tapJob?.cancel()
        tapJob = scope.launch { delay(500); if (tapCount == 2) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); skipDirection = 1; scope.launch { rippleScale.snapTo(0f); rippleAlpha.snapTo(0.6f); launch { rippleScale.animateTo(1.5f, tween(300)) }; launch { rippleAlpha.animateTo(0f, tween(300)) } }; onSkipNext() } else if (tapCount >= 3) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); skipDirection = -1; scope.launch { rippleScale.snapTo(0f); rippleAlpha.snapTo(0.6f); launch { rippleScale.animateTo(1.5f, tween(300)) }; launch { rippleAlpha.animateTo(0f, tween(300)) } }; onSkipPrevious() }
        tapCount = 0 }
    }
    LaunchedEffect(mediaItem) { delay(500); skipDirection = 0 }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = mediaItem.mediaMetadata.artworkUri, transitionSpec = { fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400)) }, label = "BackgroundCrossfadeLandscape") { artworkUri ->
            AsyncImage(model = artworkUri, contentDescription = null, modifier = Modifier
                .fillMaxSize()
                .blur(25.dp), contentScale = ContentScale.Crop)
        }
        val overlayColor = if (androidx.compose.material3.MaterialTheme.colorScheme.surface == Color.White) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
        Box(modifier = Modifier
            .fillMaxSize()
            .background(overlayColor.copy(alpha = 0.6f)))
        val contentColor = if (androidx.compose.material3.MaterialTheme.colorScheme.surface == Color.White) CharcoalBlack else Color.White
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)) {
                 HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val rotationY = pageOffset * -30f
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.rotationY = rotationY; cameraDistance = 8 * density }) {
                        when (page) {
                            0 -> { Box(modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .pointerInput(Unit) { detectTapGestures(onTap = { handleTap() }) }) {
                                    AnimatedContent(targetState = mediaItem.mediaId, transitionSpec = { if (skipDirection == 1) { (slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()) } else if (skipDirection == -1) { (slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut()) } else { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) } }, label = "ArtworkTransitionLandscape") { _ -> FuturisticThumbnail(model = mediaItem.mediaMetadata.artworkUri, modifier = Modifier.fillMaxSize()) }
                                    Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = Color(0xFF00B29A).copy(alpha = rippleAlpha.value), radius = size.minDimension * rippleScale.value, center = center) } }
                            }
                            1 -> LiveWaveCanvas(isPlaying)
                        }
                    }
                 }
            }
            Spacer(modifier = Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("NOW PLAYING", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = contentColor, modifier = Modifier.size(28.dp))
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download") },
                                onClick = { showMoreMenu = false; onDownloadClick() },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { showMoreMenu = false; onAddToPlaylist() },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (repeatMode == Player.REPEAT_MODE_OFF) "Auto Next: ON" else "Auto Next: OFF") },
                            onClick = { showMoreMenu = false; onToggleRepeat() },
                            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) }
                        )
                    }
                }

                    IconButton(onClick = onOpenQueue) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = contentColor) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f, onValueChange = { onSeek((it * duration).toLong()) }, colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(currentPosition), color = contentColor.copy(alpha = 0.5f)); Text(formatTime(duration), color = contentColor.copy(alpha = 0.5f)) }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleShuffle() }) { Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else contentColor) }
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipPrevious() }) { Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = contentColor, modifier = Modifier.size(48.dp)) }
                    PlayPauseMorph(isPlaying, isBuffering, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTogglePlay() } )
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSkipNext() }) { Icon(Icons.Default.SkipNext, contentDescription = null, tint = contentColor, modifier = Modifier.size(48.dp)) }
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleRepeat() }) { Icon(when (repeatMode) { Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat }, contentDescription = null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else contentColor) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                     Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = contentColor.copy(alpha = 0.5f))
                     Slider(value = volume, onValueChange = onVolumeChange, modifier = Modifier
                         .weight(1f)
                         .padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = contentColor, activeTrackColor = contentColor))
                     IconButton(onClick = { onToggleLike(); val msg = if (isLiked) "Removed" else "Added"; Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isLiked) Color.Red else contentColor) }
                }
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = contentColor) }
        }
    }
}

fun formatTime(ms: Long): String { val totalSeconds = ms / 1000; val minutes = totalSeconds / 60; val seconds = totalSeconds % 60; return String.format("%d:%02d", minutes, seconds) }

@Composable
fun LiveWaveCanvas(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val waveAnim by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "WaveAnim")
    val barCount = 48
    val barHeights = remember { List(barCount) { (2..10).random() / 10f } }
    Surface(modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(24.dp)), color = Color.Black.copy(alpha = 0.4f)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 32.dp)) {
            val width = size.width; val height = size.height; val spacing = 3.dp.toPx(); val barWidth = (width - (barCount * spacing)) / barCount; val midY = height / 2; val baseAmplitude = if (isPlaying) height * 0.45f else height * 0.05f
            for (i in 0 until barCount) {
                val wave1 = Math.sin((i.toDouble() / barCount) * Math.PI * 2 + (waveAnim * Math.PI * 2)).toFloat(); val wave2 = Math.cos((i.toDouble() / barCount) * Math.PI * 4 - (waveAnim * Math.PI)).toFloat(); val combinedWave = (wave1 * 0.6f + wave2 * 0.4f); val animatedHeight = baseAmplitude * barHeights[i] * (0.5f + 0.5f * Math.abs(combinedWave)); val x = i * (barWidth + spacing)
                drawRoundRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.9f), Color(0xFFE91E63).copy(alpha = 0.9f), Color(0xFF9C27B0).copy(alpha = 0.9f))), topLeft = Offset(x, midY - animatedHeight), size = androidx.compose.ui.geometry.Size(barWidth, animatedHeight * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2))
            }
        }
    }
}

@Composable
fun PlayPauseMorph(isPlaying: Boolean, isBuffering: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(Color.White)
        .clickable(onClick = onClick)) {
        if (isBuffering) CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color.Black)
        else {
            AnimatedContent(targetState = isPlaying, transitionSpec = { scaleIn() togetherWith scaleOut() }, label = "PlayPauseMorph") { playing ->
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(48.dp))
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
fun GlassyEqualizer(
    bands: List<EqBand>, 
    range: Pair<Short, Short>, 
    bassBoost: Short, 
    virtualizer: Short, 
    userTier: Int, 
    onLevelChange: (Int, Short) -> Unit, 
    onBassBoostChange: (Short) -> Unit, 
    onVirtualizerChange: (Short) -> Unit
) {
    var selectedPreset by remember { mutableStateOf("Flat") }
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF212121))
        .padding(16.dp)) {
        // Skeuomorphic Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)) {
                Text(selectedPreset.uppercase(), color = Color(0xFF00E5FF), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("MUSIC EQUALIZER", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp)
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sliders Area
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))) {
            Row(modifier = Modifier
                .fillMaxSize()
                .padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                bands.forEach { band ->
                    VerticalSkeuomorphicSlider(band, range, onLevelChange)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Knobs Area
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KnobControlImageStyle(label = "BASS", value = bassBoost.toFloat() / 1000f, onValueChange = { onBassBoostChange((it * 1000).toInt().toShort()) })
            KnobControlImageStyle(label = "VIRTUALIZER", value = virtualizer.toFloat() / 1000f, onValueChange = { onVirtualizerChange((it * 1000).toInt().toShort()) })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Presets
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Classical", "Heavy")
            items(presets) { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = { selectedPreset = preset; applyPreset(preset, bands, range, onLevelChange) },
                    label = { Text(preset, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF), selectedLabelColor = Color.Black)
                )
            }
        }
    }
}

private fun applyPreset(preset: String, bands: List<EqBand>, range: Pair<Short, Short>, onLevelChange: (Int, Short) -> Unit) {
    val levels = when(preset) {
        "Pop" -> listOf(0, 1, 2, 1, 0)
        "Rock" -> listOf(3, 2, -1, 2, 3)
        "Jazz" -> listOf(2, 1, 0, 1, 3)
        "Classical" -> listOf(3, 2, 0, 2, 3)
        "Heavy" -> listOf(4, 3, 1, 3, 4)
        else -> listOf(0, 0, 0, 0, 0)
    }
    bands.forEachIndexed { i, band -> 
        if(i < levels.size) { 
            val level = (levels[i] * (range.second / 5)).toShort()
            onLevelChange(band.index, level) 
        } 
    }
}

@Composable
fun VerticalSkeuomorphicSlider(band: EqBand, range: Pair<Short, Short>, onLevelChange: (Int, Short) -> Unit) {
    val freqLabel = when {
        band.centerFreq >= 1000000 -> "${band.centerFreq / 1000000}k"
        else -> "${band.centerFreq / 1000}"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
        .fillMaxHeight()
        .width(40.dp)) {
        Text(freqLabel, color = Color.White, fontSize = 9.sp)
        Text("+15", color = Color.Gray, fontSize = 7.sp)
        
        Box(modifier = Modifier
            .weight(1f)
            .width(40.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .width(1.5.dp)
                .background(Color.Black))
            Slider(
                value = band.level.toFloat(),
                onValueChange = { onLevelChange(band.index, it.toInt().toShort()) },
                valueRange = range.first.toFloat()..range.second.toFloat(),
                modifier = Modifier
                    .graphicsLayer(rotationZ = -90f)
                    .width(160.dp),
                colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                thumb = {
                    Surface(
                        modifier = Modifier.size(24.dp, 16.dp),
                        color = Color(0xFFBDBDBD),
                        shape = RoundedCornerShape(2.dp),
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Color.DarkGray))
                        }
                    }
                }
            )
        }
        
        Text("-15", color = Color.Gray, fontSize = 7.sp)
        Text("0", color = Color.Gray, fontSize = 7.sp)
    }
}

@Composable
fun KnobControlImageStyle(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        val newValue = (value + 0.1f).let { if (it > 1.05f) 0f else it }
                        onValueChange(newValue)
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(Color(0xFF424242), Color(0xFF212121))), radius = size.minDimension / 2)
                val angle = 135f + (value.coerceIn(0f, 1f) * 270f)
                val rad = Math.toRadians(angle.toDouble())
                val x = center.x + (size.minDimension / 2.8f) * Math.cos(rad).toFloat()
                val y = center.y + (size.minDimension / 2.8f) * Math.sin(rad).toFloat()
                drawCircle(Color(0xFF00E5FF), radius = 3.dp.toPx(), center = Offset(x, y))
            }
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFF333333), tonalElevation = 2.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier
                        .size(18.dp)
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape))
                }
            }
        }
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
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
fun SubscriptionGate(tierName: String, onUpgrade: () -> Unit = {}) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), contentAlignment = Alignment.Center) {
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

@Composable
fun EqualizerSheet(bands: List<EqBand>, range: Pair<Short, Short>, bassBoost: Short, virtualizer: Short, userTier: Int, onLevelChange: (Int, Short) -> Unit, onBassBoostChange: (Short) -> Unit, onVirtualizerChange: (Short) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)) {
        Text("Audio Equalizer", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text("Customize your sound", color = GrayText, fontSize = 14.sp); Spacer(modifier = Modifier.height(24.dp))
        GlassyEqualizer(bands = bands, range = range, bassBoost = bassBoost, virtualizer = virtualizer, userTier = userTier, onLevelChange = onLevelChange, onBassBoostChange = onBassBoostChange, onVirtualizerChange = onVirtualizerChange)
        Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { bands.forEach { onLevelChange(it.index, 0) }; onBassBoostChange(0); onVirtualizerChange(0) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Reset to Flat", color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
fun FuturisticThumbnail(model: Any?, modifier: Modifier = Modifier) {

    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
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
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
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
        })
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
