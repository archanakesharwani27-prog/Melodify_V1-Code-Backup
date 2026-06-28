package com.ansh.ytmusicpreimum.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonRed,
    secondary = AccentPurple,
    tertiary = NeonBlue,
    background = CyberDarkBg,
    surface = CyberSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC4C6D0), // Subtitle/Secondary text color for dark mode
    surfaceVariant = SurfaceGrey
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPink,
    secondary = NeonBlue,
    tertiary = AccentPurple,
    background = Color(0xFFF0F2F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = CharcoalBlack,
    onSurface = CharcoalBlack,
    onSurfaceVariant = CharcoalBlack.copy(alpha = 0.7f),
    surfaceVariant = Color(0xFFE1E2EC)
)

@Composable
fun MelodifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
