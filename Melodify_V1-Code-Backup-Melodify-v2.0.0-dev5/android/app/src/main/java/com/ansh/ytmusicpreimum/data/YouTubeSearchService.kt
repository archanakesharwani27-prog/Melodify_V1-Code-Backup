package com.ansh.ytmusicpreimum.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.text.Html
import androidx.core.text.HtmlCompat

object YouTubeSearchService {
    private val client = OkHttpClient()
    private const val API_KEY = "AIzaSyCTSGX_TDoYS_8lICnWALYTa1ZM1Ub2Bgk"
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3"

    private fun decodeHtml(text: String): String {
        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    suspend fun searchSongs(query: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/search?part=snippet&q=$query&type=video&videoCategoryId=10&maxResults=20&key=$API_KEY"
        val request = Request.Builder().url(url).build()
        
        try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val items = json.getJSONArray("items")
            val results = mutableListOf<TrackEntity>()
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.getJSONObject("id").getString("videoId")
                val snippet = item.getJSONObject("snippet")
                val title = decodeHtml(snippet.getString("title"))
                val artist = decodeHtml(snippet.getString("channelTitle"))
                val thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url")
                
                results.add(TrackEntity(id, title, artist, thumbnailUrl))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTrendingSongs(): List<TrackEntity> = withContext(Dispatchers.IO) {
        // Fetch popular music videos
        val url = "$BASE_URL/videos?part=snippet&chart=mostPopular&videoCategoryId=10&maxResults=10&regionCode=IN&key=$API_KEY"
        val request = Request.Builder().url(url).build()
        
        try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val items = json.getJSONArray("items")
            val results = mutableListOf<TrackEntity>()
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.getString("id")
                val snippet = item.getJSONObject("snippet")
                val title = decodeHtml(snippet.getString("title"))
                val artist = decodeHtml(snippet.getString("channelTitle"))
                val thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url")
                
                results.add(TrackEntity(id, title, artist, thumbnailUrl))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
