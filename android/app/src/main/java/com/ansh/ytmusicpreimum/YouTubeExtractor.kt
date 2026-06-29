package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import android.text.Html
import android.util.LruCache
import androidx.core.text.HtmlCompat
import com.ansh.ytmusicpreimum.data.StreamCacheEntity
import com.ansh.ytmusicpreimum.data.TrackDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object YouTubeExtractor {
    private val urlCache = LruCache<String, CachedUrl>(100)
    private val mutex = Mutex()
    private val activeJobs = mutableMapOf<String, Deferred<String?>>()
    private var trackDao: TrackDao? = null

    fun init(dao: TrackDao) {
        this.trackDao = dao
    }

    private data class CachedUrl(val url: String, val timestamp: Long)

    private suspend fun getCachedUrl(videoId: String, quality: String): String? {
        val now = System.currentTimeMillis()
        
        // 1. Memory Cache
        val cached = urlCache.get(videoId)
        if (cached != null) {
            if (now - cached.timestamp < 18000000) { // 5 hours
                return cached.url
            } else {
                urlCache.remove(videoId)
            }
        }

        // 2. Room Persistent Cache
        val dbCache = trackDao?.getStreamCache(videoId, quality)
        if (dbCache != null) {
            if (dbCache.expiryTime > now) {
                // Update memory cache
                urlCache.put(videoId, CachedUrl(dbCache.streamUrl, now - (18000000 - (dbCache.expiryTime - now))))
                return dbCache.streamUrl
            } else {
                trackDao?.clearExpiredCache(now)
            }
        }
        
        return null
    }

    private suspend fun cacheUrl(videoId: String, url: String, quality: String) {
        val now = System.currentTimeMillis()
        val expiry = now + 18000000 // 5 hours
        
        // 1. Memory Cache
        urlCache.put(videoId, CachedUrl(url, now))
        
        // 2. Room Persistent Cache
        trackDao?.insertStreamCache(StreamCacheEntity(videoId, url, expiry, quality))
    }

    private fun decodeHtml(text: String): String {
        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    suspend fun search(query: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val searchInfo = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, service.searchQHFactory.fromQuery(query))
            searchInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = if (item.url.contains("v=")) {
                        item.url.substringAfter("v=").substringBefore("&")
                    } else {
                        item.url.substringAfterLast("/")
                    }
                    TrackEntity(
                        id = videoId,
                        title = decodeHtml(item.name),
                        artist = decodeHtml(item.uploaderName),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: ""
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoId: String, quality: String = "Medium"): String? = withContext(Dispatchers.IO) {
        val existingJob = mutex.withLock { activeJobs[videoId] }
        if (existingJob != null) {
            android.util.Log.d("PlaybackProfile", "YouTubeExtractor: Joining existing job for $videoId")
            return@withContext existingJob.await()
        }

        val job = async {
            for (attempt in 1..2) {
                val startTime = System.currentTimeMillis()
                android.util.Log.d("PlaybackProfile", "YouTubeExtractor Start: $videoId (Attempt $attempt)")
                try {
                    getCachedUrl(videoId, quality)?.let { 
                        android.util.Log.d("PlaybackProfile", "YouTubeExtractor End (Cache Hit): +${System.currentTimeMillis() - startTime}ms")
                        return@async it 
                    }

                    val useFallback = DynamicCipherManager.getRule("use_fallback_extractor") == "true"
                    if (useFallback) {
                        return@async getAudioUrlFromFallback(videoId)
                    }

                    android.util.Log.d("YouTubeExtractor", "Fetching stream info for videoId: $videoId with quality: $quality")
                    val service = ServiceList.YouTube
                    val url = "https://www.youtube.com/watch?v=$videoId"
                    
                    val streamInfo = StreamInfo.getInfo(service, url)
                    
                    val audioStreams = streamInfo.audioStreams.filter { stream -> 
                        val format = stream.format?.name?.lowercase() ?: ""
                        format.contains("m4a") || format.contains("webm") || format.contains("opus")
                    }

                    val audioStream = when (quality) {
                        "Low" -> audioStreams.minByOrNull { it.bitrate }
                        "High" -> audioStreams.maxByOrNull { it.bitrate }
                        else -> {
                            audioStreams.minByOrNull { Math.abs(it.bitrate - 128000) }
                        }
                    } ?: streamInfo.audioStreams.minByOrNull { it.bitrate }
                    
                    if (audioStream != null) {
                        android.util.Log.d("PlaybackProfile", "YouTubeExtractor End (Success): +${System.currentTimeMillis() - startTime}ms")
                        audioStream.url?.let { cacheUrl(videoId, it, quality) }
                        return@async audioStream.url
                    }

                    val muxedStream = streamInfo.videoStreams
                        .filter { it.isVideoOnly.not() }
                        .minByOrNull { it.bitrate }
                    
                    android.util.Log.d("PlaybackProfile", "YouTubeExtractor End (Muxed Fallback): +${System.currentTimeMillis() - startTime}ms")
                    muxedStream?.url?.let { cacheUrl(videoId, it, quality) }
                    return@async muxedStream?.url
                } catch (e: Throwable) {
                    android.util.Log.e("YouTubeExtractor", "Attempt $attempt failed for $videoId", e)
                    if (attempt == 2) {
                        return@async getAudioUrlFromFallback(videoId)
                    }
                    delay(300) // Small delay before retry
                }
            }
            null
        }

        mutex.withLock { activeJobs[videoId] = job }
        try {
            val result = job.await()
            result
        } finally {
            mutex.withLock { activeJobs.remove(videoId) }
        }
    }

    private suspend fun getAudioUrlFromFallback(videoId: String): String? {
        return null
    }

    fun prefetch(videoId: String, quality: String = "Medium", scope: CoroutineScope): Job {
        return scope.launch {
            getAudioUrl(videoId, quality)
        }
    }
}

