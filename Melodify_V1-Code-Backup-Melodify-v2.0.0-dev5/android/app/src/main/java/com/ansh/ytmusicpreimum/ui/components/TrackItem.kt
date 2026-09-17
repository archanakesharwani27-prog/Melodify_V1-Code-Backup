package com.ansh.ytmusicpreimum.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.DownloadStatus
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.theme.GrayText

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
