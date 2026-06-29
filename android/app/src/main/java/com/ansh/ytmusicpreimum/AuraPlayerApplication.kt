package com.ansh.ytmusicpreimum

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.schabi.newpipe.extractor.NewPipe
import com.ansh.ytmusicpreimum.data.AppDatabase

class MelodifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Database and provide to Extractor
        val database = AppDatabase.getDatabase(this)
        YouTubeExtractor.init(database.trackDao())

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check with Debug support
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d("MelodifyApp", "Firebase App Check: Debug Provider installed.")
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Initialize NewPipe
        NewPipe.init(NewPipeDownloader.getInstance())
        DynamicCipherManager.init()
    }
}
