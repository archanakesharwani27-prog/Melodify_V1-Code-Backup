package com.ansh.ytmusicpreimum

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.NotificationCompat
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus {
    DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class DownloadState(
    val status: DownloadStatus,
    val progress: Float = 0f,
    val totalSize: String = "0 MB",
    val quality: String = ""
)

class MelodifyDownloadManager(private val context: Context) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = AppDatabase.getDatabase(context)
    private val trackDao = database.trackDao()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    private val _queuedTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queuedTracks = _queuedTracks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val downloadQueue = java.util.Collections.synchronizedList(mutableListOf<DownloadRequest>())
    
    data class DownloadRequest(val track: TrackEntity, val url: String, val quality: String)
    
    private var maxConcurrentDownloads = 1
    private var currentNetworkPreference: Int = 0

    init {
        createNotificationChannel()
    }

    fun downloadSong(track: TrackEntity, audioUrl: String, quality: String, userTier: Int = 0, networkType: Int = 0) {
        if (activeJobs.containsKey(track.id) || downloadQueue.any { it.track.id == track.id }) return
        
        currentNetworkPreference = networkType

        if (!isNetworkAllowed(networkType)) {
            android.util.Log.w("MelodifyDownload", "Download not allowed on current network")
            return
        }

        maxConcurrentDownloads = if (userTier >= 2) 5 else 2
        
        _queuedTracks.value += track
        downloadQueue.add(DownloadRequest(track, audioUrl, quality))
        
        processQueue(userTier)
    }

    private fun isNetworkAllowed(preferredType: Int): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when (preferredType) {
            0 -> capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) // Wifi only
            1 -> true // All Networks
            2 -> capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) // Mobile Data Only
            else -> true
        }
    }

    private fun processQueue(userTier: Int) {
        if (activeJobs.size >= maxConcurrentDownloads) return
        if (downloadQueue.isEmpty()) return
        if (!isNetworkAllowed(currentNetworkPreference)) return

        val request = downloadQueue.removeAt(0)
        val track = request.track
        val url = request.url
        val quality = request.quality
        
        _queuedTracks.value = _queuedTracks.value.filter { it.id != track.id }
        
        val job = scope.launch {
            try {
                val folder = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                if (folder?.exists() == false) folder.mkdirs()
                
                val file = File(folder, "${track.id}.mp3")
                val okHttpRequest = Request.Builder().url(url).build()

                client.newCall(okHttpRequest).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to download: $response")

                    val body = response.body ?: throw Exception("Body null")
                    val totalBytes = body.contentLength()
                    
                    val totalMb = if (totalBytes > 0) {
                        String.format("%.1f MB", totalBytes.toFloat() / (1024 * 1024))
                    } else {
                        "Unknown Size"
                    }
                    
                    _downloadStates.value += (track.id to DownloadState(DownloadStatus.DOWNLOADING, 0f, totalMb, quality))

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(file)
                    
                    val bufferSize = if (userTier >= 2) 65536 else 8192 
                    val buffer = ByteArray(bufferSize)
                    
                    var bytesRead: Int
                    var currentBytes = 0L

                    while (isActive) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        
                        outputStream.write(buffer, 0, bytesRead)
                        currentBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = currentBytes.toFloat() / totalBytes.toFloat()
                            _downloadStates.value += (track.id to DownloadState(DownloadStatus.DOWNLOADING, progress, totalMb, quality))
                        }
                    }

                    outputStream.close()
                    inputStream.close()

                    if (isActive) {
                        val updatedTrack = track.copy(isDownloaded = true, localFilePath = file.absolutePath)
                        trackDao.insertTrack(updatedTrack)
                        showDownloadNotification(track.title)
                        _downloadStates.value += (track.id to DownloadState(DownloadStatus.COMPLETED, 1f, totalMb, quality))
                    } else {
                        if (file.exists()) file.delete()
                        _downloadStates.value -= track.id
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MelodifyDownload", "Error downloading ${track.title}", e)
                _downloadStates.value += (track.id to DownloadState(DownloadStatus.FAILED, quality = quality))
            } finally {
                activeJobs.remove(track.id)
                processQueue(userTier)
            }
        }
        activeJobs[track.id] = job
    }

    fun cancelDownload(songId: String) {
        val queueItem = downloadQueue.find { it.track.id == songId }
        if (queueItem != null) {
            downloadQueue.remove(queueItem)
            _queuedTracks.value = _queuedTracks.value.filter { it.id != songId }
        }

        activeJobs[songId]?.cancel()
        activeJobs.remove(songId)
        _downloadStates.value -= songId
    }

    private fun showDownloadNotification(title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText("Downloaded $title successfully!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(title.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("download_channel", "Downloads", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    companion object {
        @Volatile private var INSTANCE: MelodifyDownloadManager? = null
        fun getInstance(context: Context): MelodifyDownloadManager = INSTANCE ?: synchronized(this) {
            MelodifyDownloadManager(context.applicationContext).also { INSTANCE = it }
        }
    }
}
