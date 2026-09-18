package com.ansh.ytmusicpreimum.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.ui.components.FuturisticThumbnail
import com.ansh.ytmusicpreimum.ui.theme.*

@Composable
fun MiniPlayer(
    mediaItem: MediaItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GeminiCardBg)
            .border(1.dp, GeminiBorder.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FuturisticThumbnail(
                        model = mediaItem.mediaMetadata.artworkUri,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    )
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = GeminiBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                val rawTitle = mediaItem.mediaMetadata.title?.toString() ?: "Unknown Song"
                val rawArtist = mediaItem.mediaMetadata.artist?.toString() ?: "Melodify Stream"
                val displayTitle = remember(rawTitle) { com.ansh.ytmusicpreimum.YouTubeExtractor.cleanSongTitle(rawTitle) }
                val displayArtist = remember(rawTitle, rawArtist) { com.ansh.ytmusicpreimum.YouTubeExtractor.cleanArtistName(rawTitle, rawArtist) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = GeminiTextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayArtist,
                        color = GeminiTextSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = onTogglePlay,
                        shape = CircleShape,
                        color = GeminiCardHover,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = GeminiBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp),
                            tint = GeminiTextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Download progress indicator if active
            val currentSongId = mediaItem.mediaId
            val downloadState = remember(currentSongId, downloadStates) { downloadStates[currentSongId] }
            if (downloadState != null && downloadState.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = GeminiBlue,
                    trackColor = GeminiCardHover
                )
            }
        }
    }
}
