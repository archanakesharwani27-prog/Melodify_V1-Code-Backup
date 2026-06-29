package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import android.text.Html
import android.util.LruCache
import androidx.core.text.HtmlCompat

object YouTubeExtractor {
    private val urlCache = LruCache<String, CachedUrl>(50)

    private data class CachedUrl(val url: String, val timestamp: Long)

    private fun getCachedUrl(videoId: String): String? {
        val cached = urlCache.get(videoId) ?: return null
        // YouTube URLs usually expire in 6 hours, we use 5 hours to be safe (5 * 60 * 60 * 1000)
        return if (System.currentTimeMillis() - cached.timestamp < 18000000) {
            cached.url
        } else {
            urlCache.remove(videoId)
            null
        }
    }

    private fun cacheUrl(videoId: String, url: String) {
        urlCache.put(videoId, CachedUrl(url, System.currentTimeMillis()))
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
        try {
            getCachedUrl(videoId)?.let { 
                android.util.Log.d("YouTubeExtractor", "Using cached URL for $videoId")
                return@withContext it 
            }

            val useFallback = DynamicCipherManager.getRule("use_fallback_extractor") == "true"
            if (useFallback) {
                // Potential fallback logic using a remote API if library breaks
                return@withContext getAudioUrlFromFallback(videoId)
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
                    // Medium: try to find something around 128kbps
                    audioStreams.minByOrNull { Math.abs(it.bitrate - 128000) }
                }
            } ?: streamInfo.audioStreams.minByOrNull { it.bitrate }
            
            if (audioStream != null) {
                android.util.Log.d("YouTubeExtractor", "Found audio stream: ${audioStream.url}")
                audioStream.url?.let { cacheUrl(videoId, it) }
                return@withContext audioStream.url
            }

            val muxedStream = streamInfo.videoStreams
                .filter { it.isVideoOnly.not() }
                .minByOrNull { it.bitrate }
            
            android.util.Log.d("YouTubeExtractor", "Using muxed stream fallback: ${muxedStream?.url}")
            muxedStream?.url?.let { cacheUrl(videoId, it) }
            return@withContext muxedStream?.url
        } catch (e: Throwable) {
            android.util.Log.e("YouTubeExtractor", "Fatal error in getAudioUrl for $videoId", e)
            return@withContext getAudioUrlFromFallback(videoId)
        }
    }

    private suspend fun getAudioUrlFromFallback(videoId: String): String? {
        // This is where you'd implement a dynamic scraper or call a Piped/Invidious instance
        // defined in your Remote Config.
        return null
    }
}
