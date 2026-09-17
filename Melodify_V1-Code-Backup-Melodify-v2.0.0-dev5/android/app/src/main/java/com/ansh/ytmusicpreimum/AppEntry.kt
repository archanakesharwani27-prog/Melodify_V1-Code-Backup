package com.ansh.ytmusicpreimum

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.ui.LoginScreen
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.MainScreen
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.SplashScreen
import com.ansh.ytmusicpreimum.ui.theme.MelodifyTheme

@Composable
fun MelodifyApp() {
    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
        },
        label = "SplashTransition"
    ) { isSplashScreen ->
        if (isSplashScreen) {
            val showNameIntro = loginViewModel.shouldShowNameIntro()
            val name = loginViewModel.userName.collectAsState().value ?: ""

            SplashScreen(
                showNameIntro = showNameIntro,
                userName = name,
                onAnimationFinished = { showSplash = false }
            )
        } else {
            val sharedPrefs = remember { context.getSharedPreferences(AppPreferences.SETTINGS_PREFS, Context.MODE_PRIVATE) }
            var isDarkTheme by remember {
                mutableStateOf(sharedPrefs.getBoolean(AppPreferences.DARK_THEME, true))
            }

            val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

            MelodifyTheme(darkTheme = isDarkTheme) {
                if (!isLoggedIn) {
                    LoginScreen(
                        loginViewModel = loginViewModel,
                        onLoginSuccess = { name, email, photo ->
                            loginViewModel.onLoginSuccess(name, email, photo)
                        }
                    )
                } else {
                    val musicViewModel: MusicViewModel = viewModel()

                    LaunchedEffect(Unit) {
                        musicViewModel.initController(context)
                    }

                    MainScreen(
                        musicViewModel = musicViewModel,
                        loginViewModel = loginViewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeChange = {
                            isDarkTheme = it
                            sharedPrefs.edit().putBoolean(AppPreferences.DARK_THEME, it).apply()
                        }
                    )
                }
            }
        }
    }
}
