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

    suspend fun invalidateCache(videoId: String) {
        urlCache.remove(videoId)
        try {
            trackDao?.deleteStreamCache(videoId)
        } catch (e: Exception) {
            android.util.Log.e("YouTubeExtractor", "Failed to clear stream cache for $videoId", e)
        }
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
            val encodedQuery = try {
                java.net.URLEncoder.encode(query, "UTF-8")
            } catch (e: Exception) {
                query
            }
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"
            val queryHandler = org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler(
                searchUrl,
                searchUrl,
                query,
                listOf("all"),
                ""
            )
            val searchInfo = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, queryHandler)
            searchInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = if (item.url.contains("v=")) {
                        item.url.substringAfter("v=").substringBefore("&")
                    } else {
                        item.url.substringAfterLast("/")
                    }
                    val decodedName = decodeHtml(item.name)
                    val decodedUploader = decodeHtml(item.uploaderName)
                    TrackEntity(
                        id = videoId,
                        title = cleanSongTitle(decodedName),
                        artist = cleanArtistName(decodedName, decodedUploader),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: ""
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRelatedTracks(videoId: String, artist: String = "", title: String = ""): List<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(service, url)
            val related = streamInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val id = if (item.url.contains("v=")) {
                        item.url.substringAfter("v=").substringBefore("&")
                    } else {
                        item.url.substringAfterLast("/")
                    }
                    val decodedName = decodeHtml(item.name)
                    val decodedUploader = decodeHtml(item.uploaderName)
                    TrackEntity(
                        id = id,
                        title = cleanSongTitle(decodedName),
                        artist = cleanArtistName(decodedName, decodedUploader),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: ""
                    )
                }
                .filter { it.id != videoId }
            val currentCore = extractCoreTitle(title).lowercase()
            val filteredRelated = related.filter { item ->
                val itemCore = extractCoreTitle(item.title).lowercase()
                val isSameTitle = itemCore.isNotEmpty() && currentCore.isNotEmpty() && 
                    (itemCore == currentCore || (itemCore.length > 3 && currentCore.length > 3 && (itemCore.contains(currentCore) || currentCore.contains(itemCore))))
                !isSameTitle
            }
            if (filteredRelated.isNotEmpty()) {
                return@withContext filteredRelated
            } else if (related.isNotEmpty()) {
                return@withContext related
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeExtractor", "Failed to fetch related items via StreamInfo for $videoId", e)
        }

        val cleanTitle = extractCoreTitle(title)
        val cleanArtist = artist.replace(" - Topic", "").trim()
        val isLabelArtist = listOf("t-series", "sony", "tips", "zee", "speed", "saregama", "yrf", "universal", "music", "vevo")
            .any { cleanArtist.lowercase().contains(it) }

        val query = if (cleanArtist.isNotBlank() && !isLabelArtist) {
            "$cleanArtist Top Hit Songs"
        } else if (cleanTitle.isNotBlank()) {
            "$cleanTitle Bollywood Radio"
        } else {
            "Trending Bollywood Hits"
        }
        val currentCore = cleanTitle.lowercase()
        search(query).filter { item ->
            val itemCore = extractCoreTitle(item.title).lowercase()
            item.id != videoId && (currentCore.isEmpty() || !itemCore.contains(currentCore))
        }
    }

    fun cleanSongTitle(rawTitle: String): String {
        var title = rawTitle

        // 1. Handle Colon split:
        if (title.contains(":")) {
            val left = title.substringBefore(":").trim()
            val right = title.substringAfter(":").trim()
            val lowerLeft = left.lowercase()
            val isPrefixJunk = listOf("lyrical", "lyric", "video", "audio", "teaser", "trailer", "promo", "full", "official", "song", "hd", "4k")
                .any { lowerLeft.contains(it) }
            
            if (isPrefixJunk && right.isNotBlank()) {
                title = right
            } else if (left.isNotBlank() && !lowerLeft.startsWith("full") && !lowerLeft.startsWith("official") && left.length > 2) {
                // E.g. "Akhiyaan Gulaab (Song): Shahid Kapoor..." -> left is the song title
                title = left
            }
        }

        // 2. Delimiters like |, •, /, ~
        val segment = title.split("|", "•", "/", "~").firstOrNull() ?: title
        title = segment

        // 3. Remove dashes if it's "Title - Movie" or "Artist - Title"
        if (title.contains(" - ") || title.contains(" – ")) {
            val parts = title.split(Regex(" [–-] "))
            if (parts.size >= 2) {
                val p0 = parts[0].trim()
                val p1 = parts[1].trim()
                title = if (p0.length in 2..35) p0 else p1
            }
        }

        // 4. Strip parenthetical junk like (Song), (Official Video), (Lyrical), (Full Song), [4K], etc.
        title = title
            .replace(Regex("(?i)\\((?:official\\s*(?:music\\s*)?video|lyric(?:al)?(?:\\s*video)?|full\\s*(?:video\\s*)?song|video\\s*song|audio|song|remix|hd|4k|from\\s*\"[^\"]+\"|from\\s*'[^']+')\\)"), "")
            .replace(Regex("(?i)\\[(?:official\\s*(?:music\\s*)?video|lyric(?:al)?(?:\\s*video)?|full\\s*(?:video\\s*)?song|video\\s*song|audio|song|remix|hd|4k)\\]"), "")

        // 5. Strip unbracketed trailing clutter like "Official Music Video", "Lyrics", "Song"
        title = title
            .replace(Regex("(?i)\\b(?:official\\s*(?:music\\s*)?video|lyric(?:al)?(?:\\s*video)?|full\\s*(?:video\\s*)?song|video\\s*song|lyrics?)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (title.isNotBlank()) title else rawTitle.trim()
    }

    fun cleanArtistName(rawTitle: String, rawUploader: String): String {
        val uploader = rawUploader.replace(" - Topic", "", ignoreCase = true).trim()
        val isLabel = listOf(
            "t-series", "tseries", "sony music", "sony", "tips official", "tips", 
            "zee music", "zeemusic", "speed records", "universal music", "saregama", 
            "yrf", "white hill", "geet mp3", "desi music factory", "dm - desi music factory",
            "aditya music", "lahari music", "svf", "vevo", "records", "music", "channel", "official"
        ).any { uploader.lowercase().contains(it) }

        if (!isLabel && uploader.isNotBlank()) {
            return uploader
        }

        val knownSingers = listOf(
            "Arijit Singh", "Mitraz", "Atif Aslam", "Shreya Ghoshal", "Jubin Nautiyal",
            "Yo Yo Honey Singh", "Honey Singh", "B Praak", "Diljit Dosanjh", "Badshah",
            "Sidhu Moose Wala", "Karan Aujla", "AP Dhillon", "Sunidhi Chauhan", "Sonu Nigam",
            "Mohit Chauhan", "KK", "Kumar Sanu", "Udit Narayan", "Alka Yagnik", "Pritam",
            "A.R. Rahman", "Anirudh", "Darshan Raval", "Armaan Malik", "Guru Randhawa",
            "Hardy Sandhu", "Vishal Mishra", "Jasleen Royal", "Stebin Ben", "Sachin-Jigar",
            "Amitabh Bhattacharya", "Neha Kakkar", "Lata Mangeshkar", "Kishore Kumar"
        )
        val matchedSinger = knownSingers.find { rawTitle.contains(it, ignoreCase = true) }
        if (matchedSinger != null) {
            return matchedSinger
        }

        val segments = rawTitle.split("|", "•").map { it.trim() }
        for (seg in segments.drop(1)) {
            val s = seg.replace(Regex("\\(.*?\\)"), "").trim()
            if (s.isNotBlank() && s.length < 30 && !s.lowercase().contains("t-series") && !s.lowercase().contains("official") && !s.lowercase().contains("video")) {
                if (s.split(" ").size in 1..3) {
                    return s
                }
            }
        }

        return if (uploader.isNotBlank()) uploader else "Various Artists"
    }

    private fun extractCoreTitle(raw: String): String {
        return cleanSongTitle(raw)
    }

    suspend fun getVideoUrl(videoId: String, quality: String = "720p"): String? = withContext(Dispatchers.IO) {
        val cacheKey = "vid_${videoId}_$quality"
        getCachedUrl(cacheKey, quality)?.let { return@withContext it }
        
        try {
            val service = ServiceList.YouTube
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(service, url)
            
            // Look for muxed video streams first (contains synchronized audio & video)
            val muxedStreams = streamInfo.videoStreams.filter { !it.isVideoOnly }
            
            val chosenStream = when (quality) {
                "360p" -> muxedStreams.find { it.resolution?.contains("360") == true }
                    ?: muxedStreams.minByOrNull { it.bitrate }
                "720p" -> muxedStreams.find { it.resolution?.contains("720") == true }
                    ?: muxedStreams.maxByOrNull { it.bitrate }
                "1080p" -> muxedStreams.find { it.resolution?.contains("1080") == true }
                    ?: muxedStreams.maxByOrNull { it.bitrate }
                else -> muxedStreams.find { it.resolution?.contains("720") == true }
                    ?: muxedStreams.firstOrNull()
            } ?: streamInfo.videoStreams.firstOrNull()
            
            val videoUrl = chosenStream?.url
            if (videoUrl != null) {
                cacheUrl(cacheKey, videoUrl, quality)
            }
            return@withContext videoUrl
        } catch (e: Exception) {
            android.util.Log.e("YouTubeExtractor", "Failed to extract video url for $videoId", e)
            null
        }
    }

    suspend fun getAudioUrl(videoId: String, quality: String = "Medium"): String? = withContext(Dispatchers.IO) {
        val existingJob = mutex.withLock { activeJobs[videoId] }
        if (existingJob != null) {
            return@withContext existingJob.await()
        }

        val job = async {
            for (attempt in 1..2) {
                try {
                    getCachedUrl(videoId, quality)?.let { 
                        return@async it 
                    }

                    val useFallback = DynamicCipherManager.getRule("use_fallback_extractor") == "true"
                    if (useFallback) {
                        return@async getAudioUrlFromFallback(videoId)
                    }

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
                        audioStream.url?.let { cacheUrl(videoId, it, quality) }
                        return@async audioStream.url
                    }

                    val muxedStream = streamInfo.videoStreams
                        .filter { it.isVideoOnly.not() }
                        .minByOrNull { it.bitrate }
                    
                    muxedStream?.url?.let { cacheUrl(videoId, it, quality) }
                    return@async muxedStream?.url
                } catch (e: Throwable) {
                    if (attempt == 2) {
                        android.util.Log.e("YouTubeExtractor", "Failed to extract $videoId", e)
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

    suspend fun fetchTrackMetadata(videoId: String): TrackEntity? = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(service, url)
            val decodedName = decodeHtml(streamInfo.name)
            val decodedUploader = decodeHtml(streamInfo.uploaderName)
            val thumb = streamInfo.thumbnails.firstOrNull()?.url ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            TrackEntity(
                id = videoId,
                title = cleanSongTitle(decodedName),
                artist = cleanArtistName(decodedName, decodedUploader),
                thumbnailUrl = thumb
            )
        } catch (e: Throwable) {
            android.util.Log.e("YouTubeExtractor", "Failed to fetch metadata for $videoId", e)
            null
        }
    }
}

