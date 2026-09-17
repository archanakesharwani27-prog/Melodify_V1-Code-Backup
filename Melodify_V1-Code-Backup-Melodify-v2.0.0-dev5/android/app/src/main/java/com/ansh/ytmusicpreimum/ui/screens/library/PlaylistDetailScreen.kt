package com.ansh.ytmusicpreimum.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.components.LibraryEmptyState
import com.ansh.ytmusicpreimum.ui.components.TrackItem

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
