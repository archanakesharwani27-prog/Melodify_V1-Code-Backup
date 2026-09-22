package com.ansh.ytmusicpreimum

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object SongShareHelper {
    fun shareTrack(
        context: Context,
        title: String,
        artist: String,
        videoId: String,
        offlineFilePath: String? = null
    ) {
        try {
            val youtubeUrl = if (videoId.startsWith("http")) videoId else "https://music.youtube.com/watch?v=$videoId"
            val shareMessage = buildString {
                append("🎵 Listening to \"$title\" by $artist\n\n")
                if (!videoId.startsWith("content://") && !videoId.startsWith("file://")) {
                    append("▶️ Stream on YouTube Music: $youtubeUrl\n\n")
                }
                append("🎧 Experience high-fidelity ad-free music on Melodify!")
            }

            // Check given offlineFilePath or check downloads directory
            var file = offlineFilePath?.let { File(it) }
            if (file == null || !file.exists()) {
                val downloadsFolder = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads")
                val mp3File = File(downloadsFolder, "$videoId.mp3")
                val mp4File = File(downloadsFolder, "$videoId.mp4")
                if (mp3File.exists()) {
                    file = mp3File
                } else if (mp4File.exists()) {
                    file = mp4File
                }
            }

            if (file != null && file.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (isVideo) "video/mp4" else "audio/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "$title - $artist")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooserTitle = if (isVideo) "Share Video \"$title\"" else "Share Audio \"$title\""
                val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "$title - $artist")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                val chooser = Intent.createChooser(shareIntent, "Share \"$title\" via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (t: Throwable) {
            android.util.Log.e("SongShareHelper", "Error sharing track", t)
        }
    }
}
