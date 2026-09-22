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
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = AppDatabase.getDatabase(context)
    private val trackDao = database.trackDao()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    private val _queuedTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queuedTracks = _queuedTracks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val downloadQueue = java.util.Collections.synchronizedList(mutableListOf<DownloadRequest>())

    data class DownloadRequest(
        val track: TrackEntity,
        val url: String,
        val quality: String,
        val isVideo: Boolean = false,
        val retryCount: Int = 0
    )

    private var maxConcurrentDownloads = 1
    private var currentNetworkPreference: Int = 0
    private var currentUserTier: Int = 0

    init {
        createNotificationChannel()
        registerNetworkCallback()
        syncDownloadedFiles()
    }

    fun syncDownloadedFiles() {
        scope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = getDownloadsDir()
                val files = downloadsDir.listFiles { _, name ->
                    name.endsWith(".mp3", ignoreCase = true) || name.endsWith(".mp4", ignoreCase = true)
                } ?: return@launch

                for (file in files) {
                    val songId = file.nameWithoutExtension
                    if (songId.isBlank()) continue

                    val existing = trackDao.getTrackById(songId)
                    if (existing == null) {
                        var title = songId
                        var artist = "Downloaded Song"
                        var duration = 0L
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val metaTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                            val metaArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            val metaDur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            if (!metaTitle.isNullOrBlank()) title = metaTitle
                            if (!metaArtist.isNullOrBlank()) artist = metaArtist
                            if (!metaDur.isNullOrBlank()) duration = metaDur.toLongOrNull() ?: 0L
                            retriever.release()
                        } catch (e: Throwable) {
                            android.util.Log.w("MelodifyDownload", "Metadata retriever error for ${file.name}", e)
                        }

                        val recoveredTrack = TrackEntity(
                            id = songId,
                            title = title,
                            artist = artist,
                            album = "Downloaded",
                            isDownloaded = true,
                            localFilePath = file.absolutePath,
                            addedDate = file.lastModified()
                        )
                        trackDao.insertTrack(recoveredTrack)
                        android.util.Log.i("MelodifyDownload", "Auto-recovered offline download: $songId ($title)")
                    } else if (!existing.isDownloaded || existing.localFilePath != file.absolutePath) {
                        val updated = existing.copy(
                            isDownloaded = true,
                            localFilePath = file.absolutePath
                        )
                        trackDao.insertTrack(updated)
                        android.util.Log.i("MelodifyDownload", "Updated download path in DB: $songId")
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("MelodifyDownload", "Error syncing downloaded files", e)
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    processQueue(currentUserTier)
                }

                override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) {
                    processQueue(currentUserTier)
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("MelodifyDownload", "Error registering network callback", e)
        }
    }

    fun downloadMedia(track: TrackEntity, url: String, quality: String, isVideo: Boolean = false, userTier: Int = 0, networkType: Int = 0) {
        if (activeJobs.containsKey(track.id) || downloadQueue.any { it.track.id == track.id }) return

        currentNetworkPreference = networkType
        currentUserTier = userTier
        maxConcurrentDownloads = if (userTier >= 2) 5 else 2

        _queuedTracks.value += track
        downloadQueue.add(DownloadRequest(track, url, quality, isVideo = isVideo, retryCount = 0))

        if (!isNetworkAllowed(networkType)) {
            android.util.Log.i("MelodifyDownload", "Queued '${track.title}' waiting for preferred network connection")
            _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.PAUSED, quality = quality)) }
            return
        }

        processQueue(userTier)
    }

    fun downloadSong(track: TrackEntity, audioUrl: String, quality: String, userTier: Int = 0, networkType: Int = 0) {
        downloadMedia(track, audioUrl, quality, isVideo = false, userTier = userTier, networkType = networkType)
    }

    fun downloadVideo(track: TrackEntity, videoUrl: String, quality: String, userTier: Int = 0, networkType: Int = 0) {
        downloadMedia(track, videoUrl, quality, isVideo = true, userTier = userTier, networkType = networkType)
    }

    fun retryDownload(track: TrackEntity, quality: String, userTier: Int = 0, networkType: Int = 0) {
        currentUserTier = userTier
        _downloadStates.update { it - track.id }
        scope.launch {
            val freshUrl = YouTubeExtractor.getAudioUrl(track.id, quality)
            if (freshUrl != null) {
                downloadSong(track, freshUrl, quality, userTier, networkType)
            } else {
                _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.FAILED, quality = quality)) }
            }
        }
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
        val isVideo = request.isVideo
        val retryCount = request.retryCount

        _queuedTracks.value = _queuedTracks.value.filter { it.id != track.id }

        val job = scope.launch {
            var file: File? = null
            try {
                // App-private folder with .nomedia so files are hidden from phone gallery and removed on uninstall
                val folder = getDownloadsDir()

                val ext = if (isVideo) "mp4" else "mp3"
                file = File(folder, "${track.id}.$ext")
                val okHttpRequest = Request.Builder().url(url).build()

                client.newCall(okHttpRequest).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to download HTTP ${response.code}: $response")

                    val body = response.body ?: throw Exception("Body null")
                    val totalBytes = body.contentLength()

                    val totalMb = if (totalBytes > 0) {
                        String.format("%.1f MB", totalBytes.toFloat() / (1024 * 1024))
                    } else {
                        "Unknown Size"
                    }

                    _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.DOWNLOADING, 0f, totalMb, quality)) }

                    val inputStream: InputStream = body.byteStream()
                    val outputStream = FileOutputStream(file)

                    val bufferSize = if (userTier >= 2) 65536 else 8192
                    val buffer = ByteArray(bufferSize)

                    var bytesRead: Int
                    var currentBytes = 0L
                    var lastProgressUpdate = 0L

                    try {
                        while (isActive) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break

                            outputStream.write(buffer, 0, bytesRead)
                            currentBytes += bytesRead

                            if (totalBytes > 0) {
                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate > 500) { // Update every 500ms
                                    val progress = currentBytes.toFloat() / totalBytes.toFloat()
                                    _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.DOWNLOADING, progress, totalMb, quality)) }
                                    updateOngoingNotification(track, (progress * 100).toInt(), isVideo)
                                    lastProgressUpdate = now
                                }
                            }
                        }
                        outputStream.flush()
                    } finally {
                        outputStream.close()
                        inputStream.close()
                    }

                    if (isActive) {
                        val updatedTrack = track.copy(isDownloaded = true, localFilePath = file.absolutePath)
                        trackDao.insertTrack(updatedTrack)
                        showDownloadCompleteNotification(track, totalMb, isVideo)
                        _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.COMPLETED, 1f, totalMb, quality)) }
                    } else {
                        if (file.exists()) file.delete()
                        _downloadStates.update { it - track.id }
                        cancelOngoingNotification(track.id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MelodifyDownload", "Error downloading ${track.title} (attempt $retryCount)", e)
                file?.let { if (it.exists()) it.delete() } // Clean up partial download

                // Auto-retry with fresh resolved URL if expired/stale URL caused failure
                if (retryCount < 2 && isActive) {
                    android.util.Log.i("MelodifyDownload", "Retrying download with renewed URL for ${track.title}")
                    try {
                        YouTubeExtractor.invalidateCache(track.id)
                        val freshUrl = if (isVideo) YouTubeExtractor.getVideoUrl(track.id, quality) else YouTubeExtractor.getAudioUrl(track.id, quality)
                        if (freshUrl != null) {
                            downloadQueue.add(0, DownloadRequest(track, freshUrl, quality, isVideo = isVideo, retryCount = retryCount + 1))
                            return@launch
                        }
                    } catch (retryEx: Exception) {
                        android.util.Log.e("MelodifyDownload", "Failed to resolve fresh URL for retry", retryEx)
                    }
                }

                _downloadStates.update { it + (track.id to DownloadState(DownloadStatus.FAILED, quality = quality)) }
                cancelOngoingNotification(track.id)
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
        _downloadStates.update { it - songId }
        cancelOngoingNotification(songId)

        // Clean up partial files
        val folder = getDownloadsDir()
        File(folder, "$songId.mp3").let { if (it.exists()) it.delete() }
        File(folder, "$songId.mp4").let { if (it.exists()) it.delete() }
    }

    private fun getDownloadsDir(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(baseDir, "downloads").apply {
            if (!exists()) mkdirs()
            val noMedia = File(this, ".nomedia")
            if (!noMedia.exists()) {
                try { noMedia.createNewFile() } catch (e: Exception) {}
            }
        }
    }

    private fun updateOngoingNotification(track: TrackEntity, progress: Int, isVideo: Boolean = false) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val typeStr = if (isVideo) "Video" else "Song"
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $typeStr: ${track.title}")
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(track.id.hashCode(), notification)
    }

    private fun showDownloadCompleteNotification(track: TrackEntity, size: String, isVideo: Boolean = false) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val typeStr = if (isVideo) "Video" else "Song"
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("$typeStr Download Complete")
            .setContentText("'${track.title}' ($size) is now available offline.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(track.id.hashCode(), notification)
    }

    private fun cancelOngoingNotification(trackId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(trackId.hashCode())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("download_channel", "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Music download progress and notifications"
                setShowBadge(false)
            }
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
