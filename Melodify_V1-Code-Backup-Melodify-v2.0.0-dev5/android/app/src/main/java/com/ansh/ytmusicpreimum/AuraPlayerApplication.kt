package com.ansh.ytmusicpreimum

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.schabi.newpipe.extractor.NewPipe
import com.ansh.ytmusicpreimum.data.AppDatabase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MelodifyApplication : Application() {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("MelodifyApplication", "CoroutineExceptionHandler caught error", throwable)
    }
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    override fun onCreate() {
        super.onCreate()

        // Install crash logger to prevent silent process termination and record diagnostics
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("MelodifyCrash", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
                try {
                    val crashFile = java.io.File(filesDir, "last_crash.txt")
                    crashFile.writeText("Thread: ${thread.name}\nTime: ${java.util.Date()}\nError: ${android.util.Log.getStackTraceString(throwable)}")
                } catch (ignored: Throwable) {}
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "Failed to install crash handler", t)
        }

        // 1. Initialize Firebase first and protect with try-catch
        try {
            FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "FirebaseApp.initializeApp failed", t)
        }

        // 2. Initialize App Check with fallback and protect with try-catch
        try {
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
        } catch (t: Throwable) {
            android.util.Log.w("MelodifyApplication", "FirebaseAppCheck init skipped or failed", t)
        }

        // 3. Initialize NewPipe downloader safely
        try {
            NewPipe.init(NewPipeDownloader.getInstance())
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "NewPipe init failed", t)
        }

        // 4. Background initialization for non-critical components
        appScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@MelodifyApplication)
                YouTubeExtractor.init(database.trackDao())
                LyricsManager.init(database.trackDao())
            } catch (t: Throwable) {
                android.util.Log.e("MelodifyApplication", "Database / extractor background init failed", t)
            }

            try {
                DynamicCipherManager.init()
            } catch (t: Throwable) {
                android.util.Log.e("MelodifyApplication", "DynamicCipherManager init failed", t)
            }
        }
    }
}
