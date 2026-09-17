package com.ansh.ytmusicpreimum.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.HomeViewModel
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.components.LibraryEmptyState
import com.ansh.ytmusicpreimum.ui.components.TrackItem

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
