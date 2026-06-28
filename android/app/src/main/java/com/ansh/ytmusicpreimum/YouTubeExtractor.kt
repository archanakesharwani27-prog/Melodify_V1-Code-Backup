package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import android.text.Html
import androidx.core.text.HtmlCompat

object YouTubeExtractor {

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
                return@withContext audioStream.url
            }

            val muxedStream = streamInfo.videoStreams
                .filter { it.isVideoOnly.not() }
                .minByOrNull { it.bitrate }
            
            return@withContext muxedStream?.url
        } catch (e: Exception) {
            android.util.Log.e("YouTubeExtractor", "Error in getAudioUrl for $videoId, trying dynamic fallback", e)
            return@withContext getAudioUrlFromFallback(videoId)
        }
    }

    private suspend fun getAudioUrlFromFallback(videoId: String): String? {
        // This is where you'd implement a dynamic scraper or call a Piped/Invidious instance
        // defined in your Remote Config.
        return null
    }
}
