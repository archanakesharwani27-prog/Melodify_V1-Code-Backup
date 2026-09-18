package com.ansh.ytmusicpreimum.ui.screens.library

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.data.PlaylistEntity
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.components.LibraryEmptyState
import com.ansh.ytmusicpreimum.ui.components.TrackItem
import com.ansh.ytmusicpreimum.ui.theme.*

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = com.ansh.ytmusicpreimum.ui.theme.GeminiCardBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.ansh.ytmusicpreimum.ui.theme.GeminiBorder.copy(alpha = 0.8f))
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
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            com.ansh.ytmusicpreimum.ui.theme.GeminiCardHover,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = com.ansh.ytmusicpreimum.ui.theme.GeminiBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = com.ansh.ytmusicpreimum.ui.theme.GeminiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description ?: "Custom Playlist",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = com.ansh.ytmusicpreimum.ui.theme.GeminiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = com.ansh.ytmusicpreimum.ui.theme.GeminiTextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun LibraryChip(
    label: String,
    isSelected: Boolean,
    count: Int? = null,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) com.ansh.ytmusicpreimum.ui.theme.GeminiBlue.copy(alpha = 0.18f) else com.ansh.ytmusicpreimum.ui.theme.GeminiCardBg
    val contentColor = if (isSelected) com.ansh.ytmusicpreimum.ui.theme.GeminiBlue else com.ansh.ytmusicpreimum.ui.theme.GeminiTextSecondary
    val borderColor = if (isSelected) com.ansh.ytmusicpreimum.ui.theme.GeminiBlue.copy(alpha = 0.5f) else com.ansh.ytmusicpreimum.ui.theme.GeminiBorder.copy(alpha = 0.8f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
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

@OptIn(ExperimentalFoundationApi::class)
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
