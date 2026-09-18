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
    primary = GeminiBlue,
    secondary = GeminiPurple,
    tertiary = GeminiPink,
    background = GeminiBg,
    surface = GeminiCardBg,
    onPrimary = Color(0xFF00325B),
    onSecondary = Color(0xFF381E72),
    onTertiary = Color(0xFF492532),
    onBackground = GeminiTextPrimary,
    onSurface = GeminiTextPrimary,
    onSurfaceVariant = GeminiTextSecondary,
    surfaceVariant = GeminiCardHover
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
