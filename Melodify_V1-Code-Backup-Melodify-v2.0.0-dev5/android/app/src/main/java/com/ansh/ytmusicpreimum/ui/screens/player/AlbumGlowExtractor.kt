package com.ansh.ytmusicpreimum.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumGlowExtractor {
    private val colorCache = mutableMapOf<String, Pair<Color, Color>>()

    suspend fun extractColors(context: Context, artworkUri: Any?): Pair<Color, Color> {
        val key = artworkUri?.toString() ?: return defaultColors()
        colorCache[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .allowHardware(false)
                    .size(64, 64)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(64, 64, Bitmap.Config.ARGB_8888)
                    val pair = sampleVibrantColors(bitmap)
                    colorCache[key] = pair
                    pair
                } else {
                    defaultColors()
                }
            } catch (t: Throwable) {
                defaultColors()
            }
        }
    }

    private fun sampleVibrantColors(bitmap: Bitmap): Pair<Color, Color> {
        var maxSat = 0f
        var bestColor1 = android.graphics.Color.rgb(139, 92, 246) // Electric Purple
        var bestColor2 = android.graphics.Color.rgb(59, 130, 246) // Electric Blue

        val hsv = FloatArray(3)
        for (x in 0 until bitmap.width step 4) {
            for (y in 0 until bitmap.height step 4) {
                val pixel = bitmap.getPixel(x, y)
                android.graphics.Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                if (sat > 0.35f && value > 0.35f && sat > maxSat) {
                    maxSat = sat
                    bestColor1 = pixel
                }
            }
        }

        android.graphics.Color.colorToHSV(bestColor1, hsv)
        hsv[0] = (hsv[0] + 50f) % 360f
        bestColor2 = android.graphics.Color.HSVToColor(hsv)

        return Pair(Color(bestColor1), Color(bestColor2))
    }

    fun defaultColors() = Pair(Color(0xFF8B5CF6), Color(0xFF00E5FF))
}
