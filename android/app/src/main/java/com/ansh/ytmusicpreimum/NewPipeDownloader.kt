package com.ansh.ytmusicpreimum

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.Headers
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor() : Downloader() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private var userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val body = if (dataToSend != null) {
            okhttp3.RequestBody.create(null, dataToSend)
        } else if (method == "POST") {
            okhttp3.RequestBody.create(null, ByteArray(0))
        } else {
            null
        }

        val builder = okhttp3.Request.Builder()
            .url(url)
            .method(method, body)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.youtube.com/")

        headers.forEach { (key, values) ->
            values.forEach { value ->
                builder.addHeader(key, value)
            }
        }

        val okHttpRequest = builder.build()

        client.newCall(okHttpRequest).execute().use { okHttpResponse ->
            val responseBody = okHttpResponse.body?.string() ?: ""
            
            return Response(
                okHttpResponse.code,
                okHttpResponse.message,
                okHttpResponse.headers.toMultimap(),
                responseBody,
                okHttpResponse.request.url.toString()
            )
        }
    }

    fun setUserAgent(ua: String) {
        this.userAgent = ua
    }

    companion object {
        @Volatile
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloader().also { instance = it }
            }
        }

        fun updateUserAgent(ua: String) {
            instance?.setUserAgent(ua)
        }
    }
}
