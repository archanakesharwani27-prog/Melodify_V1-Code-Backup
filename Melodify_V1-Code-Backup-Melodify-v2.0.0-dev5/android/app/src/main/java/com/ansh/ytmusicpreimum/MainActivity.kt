package com.ansh.ytmusicpreimum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Notification permission check failed safely", t)
        }
        
        try {
            setContent {
                MelodifyApp()
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Fatal error in setContent", t)
        }
    }
}
