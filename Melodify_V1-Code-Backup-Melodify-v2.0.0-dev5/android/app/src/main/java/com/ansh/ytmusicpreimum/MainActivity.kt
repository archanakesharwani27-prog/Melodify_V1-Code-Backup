package com.ansh.ytmusicpreimum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                MelodifyApp()
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Fatal error in setContent", t)
        }
    }
}
