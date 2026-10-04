package com.ansh.ytmusicpreimum.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ansh.ytmusicpreimum.AudioEffectsManager
import com.ansh.ytmusicpreimum.Screen
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.screens.player.AudioEffectsDialog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    audioQuality: String,
    onQualityChange: (String) -> Unit,
    musicViewModel: MusicViewModel,
    loginViewModel: LoginViewModel,
    navController: NavController,
    userTier: Int,
    subMonths: Int,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadNetworkType by loginViewModel.downloadNetworkType.collectAsState()
    val crossfadeDuration by musicViewModel.crossfadeDuration.collectAsState()
    val userName by loginViewModel.userName.collectAsState()
    val userEmail by loginViewModel.userEmail.collectAsState()
    val userPhoto by loginViewModel.userPhotoUrl.collectAsState()
    val currentDspPreset by AudioEffectsManager.currentPreset.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEffectsDialog by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) Color(0xFF0D0F14) else Color(0xFFF7F9FC)
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    // Calculate Cache & Downloads Size
    var cacheSize by remember { mutableStateOf("0 MB") }
    var downloadsSize by remember { mutableStateOf("0 MB") }
    var freeStorageSize by remember { mutableStateOf("0 GB") }

    fun refreshStorageStats() {
        val cacheBytes = getFolderSize(context.cacheDir) + getFolderSize(context.externalCacheDir ?: context.cacheDir)
        cacheSize = formatSize(cacheBytes)

        val downloadsFolder = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads")
        val downloadBytes = getFolderSize(downloadsFolder)
        downloadsSize = formatSize(downloadBytes)

        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            freeStorageSize = formatSize(freeBytes)
        } catch (t: Throwable) {
            freeStorageSize = "N/A"
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageStats()
    }

    val syncRotation by animateFloatAsState(
        targetValue = if (isSyncingCloud) 360f else 0f,
        animationSpec = if (isSyncingCloud) infiniteRepeatable(tween(800, easing = LinearEasing)) else tween(200),
        label = "syncRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings & Audio Engine", 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = contentColor)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- 1. HERO PROFILE & CLOUD SYNC CARD ---
            EnhancedHeroProfileCard(
                userName = userName ?: "Guest User",
                userEmail = userEmail ?: "No Google Account Linked",
                userPhoto = userPhoto,
                userTier = userTier,
                isDarkTheme = isDarkTheme,
                isSyncing = isSyncingCloud,
                syncRotation = syncRotation,
                onEditClick = { showEditProfileDialog = true },
                onSyncClick = {
                    if (!isSyncingCloud) {
                        isSyncingCloud = true
                        coroutineScope.launch {
                            try {
                                musicViewModel.syncWithCloud()
                                Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
                            } catch (t: Throwable) {
                                Toast.makeText(context, "Sync complete", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSyncingCloud = false
                            }
                        }
                    }
                }
            )

            // --- 2. AUDIO & DSP PLAYBACK ENGINE ---
            EnhancedSettingsCard(title = "Audio Engine & Playback", icon = Icons.Default.MusicNote, isDarkTheme = isDarkTheme) {
                // Audio Quality Segmented Selector
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Audio Streaming Quality",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        "Controls streaming bitrate for online tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val qualities = listOf("Low" to "96 kbps", "Medium" to "160 kbps", "High" to "320 kbps")
                        qualities.forEach { (q, bitrate) ->
                            val isSelected = audioQuality == q
                            Surface(
                                onClick = { onQualityChange(q) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color(0xFF1E222B) else Color(0xFFE9EEF5)),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = q,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else contentColor
                                    )
                                    Text(
                                        text = bitrate,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                // Crossfade Selector
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Crossfade Duration",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        "Blends the end of a song smoothly into the next",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val crossfades = listOf(0L to "Off", 2000L to "2s", 4000L to "4s", 6000L to "6s")
                        crossfades.forEach { (duration, label) ->
                            val isSelected = crossfadeDuration == duration
                            Surface(
                                onClick = { musicViewModel.setCrossfadeDuration(duration) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color(0xFF1E222B) else Color(0xFFE9EEF5))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else contentColor
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                // DSP Equalizer & Presets Shortcut Row
                EnhancedSettingsRow(
                    title = "Sound Effects & Equalizer",
                    subtitle = "Active Preset: $currentDspPreset (Pop, Rock, DJ, Bass, Dolby)",
                    icon = Icons.Default.GraphicEq,
                    iconColor = Color(0xFF00E5FF),
                    isDarkTheme = isDarkTheme,
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                        ) {
                            Text(
                                "Configure",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    },
                    onClick = { showEffectsDialog = true }
                )
            }

            // --- 3. OFFLINE STORAGE & DOWNLOADS CARD ---
            EnhancedSettingsCard(title = "Downloads & Local Storage", icon = Icons.Default.Download, isDarkTheme = isDarkTheme) {
                // Storage Meter Bar
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Device Storage Usage",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Text(
                            "Free: $freeStorageSize",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1DB954),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Downloads: $downloadsSize", fontSize = 11.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE040FB)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cache: $cacheSize", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                // Download Network Preference
                EnhancedSettingsRow(
                    title = "Download Over Network",
                    subtitle = if (downloadNetworkType == 0) "Wi-Fi Only (Recommended)" else "All Networks (Cellular + Wi-Fi)",
                    icon = Icons.Default.NetworkCheck,
                    iconColor = Color(0xFF1DB954),
                    isDarkTheme = isDarkTheme,
                    trailing = {
                        Switch(
                            checked = downloadNetworkType == 1,
                            onCheckedChange = { loginViewModel.updateDownloadNetwork(if (it) 1 else 0) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    onClick = { loginViewModel.updateDownloadNetwork(if (downloadNetworkType == 0) 1 else 0) }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                // Clear Cache Action
                EnhancedSettingsRow(
                    title = "Clear App Cache",
                    subtitle = "Frees up temporary audio & image data ($cacheSize)",
                    icon = Icons.Default.CleaningServices,
                    iconColor = Color(0xFFFF9800),
                    isDarkTheme = isDarkTheme,
                    onClick = { showClearCacheDialog = true }
                )
            }

            // --- 4. APPEARANCE & INTERFACE ---
            EnhancedSettingsCard(title = "Appearance", icon = Icons.Default.Palette, isDarkTheme = isDarkTheme) {
                EnhancedSettingsRow(
                    title = "Dark Theme",
                    subtitle = if (isDarkTheme) "Sleek AMOLED dark aesthetic" else "Clean light interface",
                    icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    iconColor = Color(0xFFFFD54F),
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
                    },
                    onClick = { onThemeChange(!isDarkTheme) }
                )
            }

            // --- 5. ABOUT & SYSTEM INFO ---
            EnhancedSettingsCard(title = "About & Community", icon = Icons.Default.Info, isDarkTheme = isDarkTheme) {
                EnhancedSettingsRow(
                    title = "Melodify Version",
                    subtitle = "v2.0.0-dev5 (Build 2026.09.19)",
                    icon = Icons.Default.Verified,
                    iconColor = Color(0xFF00E5FF),
                    isDarkTheme = isDarkTheme,
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                "Latest",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    },
                    onClick = {}
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                EnhancedSettingsRow(
                    title = "Check for Updates",
                    subtitle = "Search for new releases and features (v2.0.0)",
                    icon = Icons.Default.SystemUpdate,
                    iconColor = Color(0xFF4CAF50),
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        coroutineScope.launch {
                            val info = com.ansh.ytmusicpreimum.InAppUpdateManager.checkLatestRelease()
                            if (info.isUpdateAvailable) {
                                Toast.makeText(context, "New Update Available: v${info.latestVersionName}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Melodify is up to date (v2.0.0)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                EnhancedSettingsRow(
                    title = "Feedback & Feature Request",
                    subtitle = "Help make Melodify even better",
                    icon = Icons.Default.Feedback,
                    iconColor = Color(0xFF4CAF50),
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSc94nJebwL4pM0V2Wrda9xXNFUgpCDKMiWeA0pJKaIxQIU8Iw/viewform?usp=publish-editor"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                EnhancedSettingsRow(
                    title = "Privacy Policy",
                    subtitle = "Read our data & privacy terms",
                    icon = Icons.Default.Security,
                    iconColor = Color.Gray,
                    isDarkTheme = isDarkTheme,
                    onClick = { navController.navigate(Screen.PrivacyPolicy.route) }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                EnhancedSettingsRow(
                    title = "Switch Account / Sign Out",
                    subtitle = "Manage connected Google Account",
                    icon = Icons.Default.Logout,
                    iconColor = Color(0xFFFF5252),
                    isDarkTheme = isDarkTheme,
                    onClick = onLogout
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- DIALOGS ---

    // 1. Audio Effects Dialog
    if (showEffectsDialog) {
        AudioEffectsDialog(onDismiss = { showEffectsDialog = false })
    }

    // 2. Edit Profile Dialog
    if (showEditProfileDialog) {
        var newName by remember { mutableStateOf(userName ?: "") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF1E222B) else Color.White,
            title = { Text("Edit Profile", color = contentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .clickable { galleryLauncher.launch("image/*") }
                    ) {
                        AsyncImage(
                            model = selectedImageUri ?: userPhoto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = com.ansh.ytmusicpreimum.R.drawable.ic_launcher_foreground)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                        }
                    }
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Tap avatar above to pick photo from gallery", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    loginViewModel.updateProfile(newName, selectedImageUri?.toString() ?: userPhoto)
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                }) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 3. Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF1E222B) else Color.White,
            title = { Text("Clear Temporary Cache?", fontWeight = FontWeight.Bold, color = contentColor) },
            text = { 
                Text(
                    "This will delete temporary image & stream cache ($cacheSize). Your downloaded songs and offline tracks will NOT be deleted.",
                    color = Color.Gray
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        context.cacheDir.deleteRecursively()
                        context.externalCacheDir?.deleteRecursively()
                        refreshStorageStats()
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Cache") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Modern Glassmorphic Hero Profile Card with Glowing Avatar and Cloud Sync button
 */
@Composable
fun EnhancedHeroProfileCard(
    userName: String,
    userEmail: String,
    userPhoto: String?,
    userTier: Int,
    isDarkTheme: Boolean,
    isSyncing: Boolean,
    syncRotation: Float,
    onEditClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val gradientBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF161B26),
                    Color(0xFF1F2433)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF0F4F9)
                )
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .border(1.dp, Color.White.copy(alpha = if (isDarkTheme) 0.1f else 0.4f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with glowing gradient border
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFFE040FB), Color(0xFF00E5FF))
                                ),
                                shape = CircleShape
                            )
                            .clickable(onClick = onEditClick)
                    ) {
                        AsyncImage(
                            model = userPhoto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = com.ansh.ytmusicpreimum.R.drawable.ic_launcher_foreground)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (userTier >= 2) "✨ VIP PRO" else "FREE MEMBER",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Cloud Sync Status Pill & Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = if (isDarkTheme) 0.25f else 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cloud Sync (Liked & Playlists)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
                            )
                        }

                        TextButton(
                            onClick = onSyncClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync Now",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(syncRotation)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSyncing) "Syncing..." else "Sync Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Card Container for Settings Sections
 */
@Composable
fun EnhancedSettingsCard(
    title: String,
    icon: ImageVector,
    isDarkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF141822) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = if (isDarkTheme) 0.07f else 0.15f)
            )
        ) {
            Column(content = content)
        }
    }
}

/**
 * Modern Row Item for Settings Options
 */
@Composable
fun EnhancedSettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = Color.White,
    isDarkTheme: Boolean,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    iconColor.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
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
fun QualityOptionRow(
    label: String,
    value: String,
    currentQuality: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        if (value == currentQuality) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

