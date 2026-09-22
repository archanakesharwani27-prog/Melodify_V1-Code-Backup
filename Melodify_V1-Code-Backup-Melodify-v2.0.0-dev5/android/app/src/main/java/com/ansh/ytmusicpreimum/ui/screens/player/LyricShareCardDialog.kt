package com.ansh.ytmusicpreimum.ui.screens.player

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@Composable
fun LyricShareCardDialog(
    initialLine: String,
    allLines: List<String>,
    songTitle: String,
    songArtist: String,
    artworkUrl: Any?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedLines = remember { mutableStateListOf(initialLine) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F0F14))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Share Lyric Card",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1DB954).copy(alpha = 0.35f),
                                    Color(0xFF191414),
                                    Color(0xFF0A0A0E)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        // Card Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    songTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                Text(
                                    songArtist,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quote Text
                        val displayedQuote = selectedLines.joinToString("\n")
                        Text(
                            text = "“$displayedQuote”",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Branding Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Melodify Music",
                                color = Color(0xFF1DB954),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "music.youtube.com",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Share Button
                Button(
                    onClick = {
                        shareCardAsImage(
                            context = context,
                            lines = selectedLines.toList(),
                            songTitle = songTitle,
                            songArtist = songArtist
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to Instagram / WhatsApp", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

private fun shareCardAsImage(
    context: Context,
    lines: List<String>,
    songTitle: String,
    songArtist: String
) {
    try {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient
        val bgPaint = android.graphics.Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    android.graphics.Color.rgb(18, 50, 28),
                    android.graphics.Color.rgb(15, 15, 20),
                    android.graphics.Color.rgb(5, 5, 8)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Surface
        val cardRect = RectF(80f, 160f, (width - 80).toFloat(), (height - 160).toFloat())
        val cardPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 25, 28, 36)
            style = Style.FILL
        }
        val cardBorder = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 29, 185, 84)
            style = Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardBorder)

        // Song Title
        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = 52f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val maxTitleWidth = (width - 260)
        val safeTitle = if (songTitle.length > 28) songTitle.take(25) + "..." else songTitle
        canvas.drawText(safeTitle, 140f, 280f, titlePaint)

        // Artist Name
        val artistPaint = TextPaint().apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 38f
            isAntiAlias = true
        }
        canvas.drawText(songArtist, 140f, 340f, artistPaint)

        // Quotation mark
        val quoteMarkPaint = TextPaint().apply {
            color = android.graphics.Color.argb(100, 29, 185, 84)
            textSize = 120f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("“", 130f, 490f, quoteMarkPaint)

        // Lyrics body
        val quoteText = lines.joinToString("\n")
        val lyricPaint = TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = 58f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val staticLayout = StaticLayout.Builder.obtain(
            quoteText,
            0,
            quoteText.length,
            lyricPaint,
            width - 280
        ).build()

        canvas.save()
        canvas.translate(140f, 520f)
        staticLayout.draw(canvas)
        canvas.restore()

        // Melodify Footer
        val brandPaint = TextPaint().apply {
            color = android.graphics.Color.rgb(29, 185, 84)
            textSize = 40f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("MELODIFY", 140f, (height - 230).toFloat(), brandPaint)

        val subBrandPaint = TextPaint().apply {
            color = android.graphics.Color.argb(140, 255, 255, 255)
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("Listen ad-free on Melodify Music", 140f, (height - 180).toFloat(), subBrandPaint)

        // Save file in cache
        val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
        val shareFile = File(shareDir, "lyric_card_${System.currentTimeMillis()}.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Share via FileProvider
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "$songTitle Lyrics")
            putExtra(Intent.EXTRA_TEXT, "🎵 \"$quoteText\"\n\n$songTitle - $songArtist\n🎧 Shared via Melodify")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Lyric Card via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("LyricShareCard", "Error sharing lyric card", e)
    }
}
