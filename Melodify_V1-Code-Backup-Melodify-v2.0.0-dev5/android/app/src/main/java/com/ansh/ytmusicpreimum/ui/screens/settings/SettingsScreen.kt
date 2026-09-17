package com.ansh.ytmusicpreimum.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ansh.ytmusicpreimum.Screen
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
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
            val dailyDownloadsUsed by loginViewModel.dailyDownloadsUsed.collectAsState()
            
            PremiumHeroCard(
                userName = userName ?: "Guest User",
                userEmail = userEmail ?: "No email linked",
                userPhoto = userPhoto,
                userTier = userTier,
                subMonths = subMonths,
                dailyDownloadsUsed = dailyDownloadsUsed,
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
                    icon = Icons.Default.Shuffle,
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
fun PremiumHeroCard(
    userName: String,
    userEmail: String,
    userPhoto: String?,
    userTier: Int,
    subMonths: Int,
    dailyDownloadsUsed: Int,
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
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "✨ Melodify PRO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
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
