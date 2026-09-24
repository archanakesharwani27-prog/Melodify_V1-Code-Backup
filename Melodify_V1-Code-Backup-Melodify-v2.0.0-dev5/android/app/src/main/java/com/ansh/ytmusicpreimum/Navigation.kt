package com.ansh.ytmusicpreimum

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Science
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Search : Screen("search", "Search", Icons.Outlined.Search, Icons.Filled.Search)
    data object Library : Screen("library", "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    data object PrivacyPolicy : Screen("privacy", "Privacy Policy", Icons.Outlined.PrivacyTip, Icons.Filled.PrivacyTip)
    data object About : Screen("about", "About", Icons.Outlined.Info, Icons.Filled.Info)
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}", "Playlist", Icons.Outlined.PlaylistPlay, Icons.Filled.PlaylistPlay)
    data object SectionDetail : Screen("section_detail/{title}", "Explore", Icons.Outlined.GridView, Icons.Filled.GridView)
}

object AppPreferences {
    const val SETTINGS_PREFS = "settings"
    const val DARK_THEME = "dark_theme"
}
