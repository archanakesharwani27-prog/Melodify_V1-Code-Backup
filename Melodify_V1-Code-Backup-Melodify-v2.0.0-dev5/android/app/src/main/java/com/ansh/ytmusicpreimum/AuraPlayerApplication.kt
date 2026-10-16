package com.ansh.ytmusicpreimum

import android.app.Application
import com.google.firebase.FirebaseApp
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

        // 1. Install bulletproof uncaught exception handler (prevent process termination)
        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("MelodifyCrashShield", "Intercepted uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
                try {
                    val crashFile = java.io.File(filesDir, "last_crash.txt")
                    crashFile.writeText("Thread: ${thread.name}\nTime: ${java.util.Date()}\nError: ${android.util.Log.getStackTraceString(throwable)}")
                } catch (ignored: Throwable) {}
                // Intentionally do NOT delegate to system defaultHandler to prevent Android OS from force closing the app
            }
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "Failed to install crash handler", t)
        }

        // 2. Install Main Looper Guardian (Cockroach pattern) to keep the UI loop alive even if a Compose frame fails
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                while (true) {
                    try {
                        android.os.Looper.loop()
                    } catch (t: Throwable) {
                        android.util.Log.e("MelodifyCrashShield", "Main Looper exception intercepted and recovered: ${t.message}", t)
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "Failed to install looper guardian", t)
        }

        // 1. Initialize Firebase first and protect with try-catch
        try {
            FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
            android.util.Log.e("MelodifyApplication", "FirebaseApp.initializeApp failed", t)
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
