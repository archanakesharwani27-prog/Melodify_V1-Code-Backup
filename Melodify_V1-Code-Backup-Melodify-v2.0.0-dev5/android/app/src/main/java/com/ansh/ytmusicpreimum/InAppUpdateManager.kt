package com.ansh.ytmusicpreimum

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateMetadata(
    val isUpdateAvailable: Boolean,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

/**
 * Production In-App OTA Auto-Updater Engine for Melodify.
 * Checks GitHub Releases API and handles in-app background downloading and installation.
 */
object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"
    const val CURRENT_VERSION_NAME = "2.0.0"
    const val CURRENT_BUILD_NUMBER = 7

    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/archanakesharwani27-prog/Melodify_V1-Code-Backup/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<String>("Up to date (v$CURRENT_VERSION_NAME)")
    val updateState: StateFlow<String> = _updateState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _latestUpdateInfo = MutableStateFlow<AppUpdateMetadata?>(null)
    val latestUpdateInfo: StateFlow<AppUpdateMetadata?> = _latestUpdateInfo.asStateFlow()

    /**
     * Checks remote server / GitHub for latest version.
     */
    suspend fun checkLatestRelease(): AppUpdateMetadata = withContext(Dispatchers.IO) {
        _updateState.value = "Checking for updates..."
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API)
                .header("User-Agent", "MelodifyApp-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val json = JSONObject(bodyString)

                val tagName = json.optString("tag_name", "v2.0.0").replace("v", "").trim()
                val body = json.optString("body", "Bug fixes and performance improvements.")
                
                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val isNew = isVersionGreater(tagName, CURRENT_VERSION_NAME)
                val metadata = AppUpdateMetadata(
                    isUpdateAvailable = isNew,
                    latestVersionName = tagName,
                    downloadUrl = if (apkUrl.isNotBlank()) apkUrl else "https://github.com/archanakesharwani27-prog/Melodify_V1-Code-Backup/releases",
                    releaseNotes = body
                )

                _latestUpdateInfo.value = metadata
                _updateState.value = if (isNew) "🚀 Update Available (v$tagName)" else "✅ You have the latest version (v$CURRENT_VERSION_NAME)"
                return@withContext metadata
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update via GitHub API", e)
        }

        val fallbackMetadata = AppUpdateMetadata(
            isUpdateAvailable = false,
            latestVersionName = CURRENT_VERSION_NAME,
            downloadUrl = "",
            releaseNotes = "App is up to date."
        )
        _updateState.value = "✅ Melodify is up to date (v$CURRENT_VERSION_NAME)"
        return@withContext fallbackMetadata
    }

    /**
     * Downloads APK package in background with progress and triggers Android Installer.
     */
    suspend fun downloadAndInstallApk(context: Context, downloadUrl: String) = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) return@withContext

        _updateState.value = "Downloading Update (0%)..."
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                _updateState.value = "Download failed. Please try again."
                return@withContext
            }

            val apkFile = File(context.cacheDir, "Melodify_Update.apk")
            if (apkFile.exists()) apkFile.delete()

            val body = response.body!!
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (contentLength > 0) {
                    val progress = totalRead.toFloat() / contentLength.toFloat()
                    _downloadProgress.value = progress
                    _updateState.value = "Downloading update... ${(progress * 100).toInt()}%"
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            _updateState.value = "Download Complete! Launching Package Installer..."
            withContext(Dispatchers.Main) {
                installApkIntent(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download update APK", e)
            _updateState.value = "Update download error. Tap to retry."
        }
    }

    private fun installApkIntent(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )
                    } else {
                        Uri.fromFile(apkFile)
                    },
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }

    private fun isVersionGreater(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            return remote != current
        }
        return false
    }
}
