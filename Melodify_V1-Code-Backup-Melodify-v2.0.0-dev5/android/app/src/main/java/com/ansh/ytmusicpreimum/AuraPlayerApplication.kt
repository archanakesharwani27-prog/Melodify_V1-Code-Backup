package com.ansh.ytmusicpreimum

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.schabi.newpipe.extractor.NewPipe
import com.ansh.ytmusicpreimum.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MelodifyApplication : Application() {
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        // Background initialization for non-critical components to improve startup time
        appScope.launch {
            // Initialize Database and provide to Extractor
            val database = AppDatabase.getDatabase(this@MelodifyApplication)
            YouTubeExtractor.init(database.trackDao())
            LyricsManager.init(database.trackDao())

            // Initialize NewPipe
            NewPipe.init(NewPipeDownloader.getInstance())
            DynamicCipherManager.init()
        }

        // Initialize Firebase (Keep on main thread as per Google recommendations for stability)
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check with Debug support
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
