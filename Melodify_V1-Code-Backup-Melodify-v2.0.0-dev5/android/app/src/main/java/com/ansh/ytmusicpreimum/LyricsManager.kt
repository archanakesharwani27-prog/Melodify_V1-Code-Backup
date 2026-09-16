package com.ansh.ytmusicpreimum

import android.util.LruCache
import com.ansh.ytmusicpreimum.data.LyricsEntity
import com.ansh.ytmusicpreimum.data.TrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class LyricLine(val time: Long, val text: String)

data class LyricsState(
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val isSynced: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

object LyricsManager {
    private val memoryCache = LruCache<String, LyricsState>(20)
    private var trackDao: TrackDao? = null
    private val client = OkHttpClient()

    fun init(dao: TrackDao) {
        this.trackDao = dao
    }

    suspend fun getLyrics(videoId: String, title: String, artist: String): LyricsState = withContext(Dispatchers.IO) {
        // 1. Memory Cache
        memoryCache.get(videoId)?.let { return@withContext it }

        // 2. Disk Cache
        trackDao?.getLyrics(videoId)?.let { entity ->
            if (entity.expiryTime > System.currentTimeMillis()) {
                val state = parseLyrics(entity.lyrics, entity.isSynced)
                memoryCache.put(videoId, state)
                return@withContext state
            }
        }

        // 3. Network Fetch
        try {
            val cleanTitle = cleanSearchQuery(title)
            val url = "https://lrclib.net/api/get?artist=${URLEncoder.encode(artist, "UTF-8")}&track=${URLEncoder.encode(cleanTitle, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: throw Exception("Empty body")
                    val json = JSONObject(body)
                    
                    val syncedLyrics = if (json.has("syncedLyrics")) json.getString("syncedLyrics") else null
                    val plainLyrics = if (json.has("plainLyrics")) json.getString("plainLyrics") else json.optString("lyrics", "")
                    
                    val lyricsToStore: String
                    val isSynced: Boolean
                    
                    if (!syncedLyrics.isNullOrBlank()) {
                        lyricsToStore = syncedLyrics
                        isSynced = true
                    } else if (!plainLyrics.isNullOrBlank()) {
                        lyricsToStore = plainLyrics
                        isSynced = false
                    } else {
                        return@withContext LyricsState(error = "No lyrics found")
                    }
                    
                    // Store in cache
                    val expiry = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
                    trackDao?.insertLyrics(LyricsEntity(videoId, lyricsToStore, isSynced, expiry))
                    
                    val state = parseLyrics(lyricsToStore, isSynced)
                    memoryCache.put(videoId, state)
                    return@withContext state
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricsManager", "Error fetching lyrics", e)
        }
        
        return@withContext LyricsState(error = "Lyrics not available")
    }

    private fun parseLyrics(content: String, isSynced: Boolean): LyricsState {
        if (!isSynced) {
            return LyricsState(plainLyrics = content, isSynced = false)
        }
        
        val lines = mutableListOf<LyricLine>()
        val timeRegex = Regex("\\[(\\d+):(\\d+\\.?\\d*)\\]")
        
        content.lines().forEach { line ->
            val timeMatches = timeRegex.findAll(line).toList()
            if (timeMatches.isNotEmpty()) {
                val text = line.substring(timeMatches.last().range.last + 1).trim()
                if (text.isNotEmpty()) {
                    timeMatches.forEach { match ->
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toFloat()
                        val timeMs = (min * 60 * 1000 + (sec * 1000).toLong())
                        lines.add(LyricLine(timeMs, text))
                    }
                }
            }
        }
        
        lines.sortBy { it.time }
        
        return if (lines.isNotEmpty()) {
            LyricsState(lines = lines, isSynced = true)
        } else {
            LyricsState(plainLyrics = content, isSynced = false)
        }
    }

    private fun cleanSearchQuery(query: String): String {
        return query.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace("Official Video", "", ignoreCase = true)
            .replace("Lyrics", "", ignoreCase = true)
            .trim()
    }
}
