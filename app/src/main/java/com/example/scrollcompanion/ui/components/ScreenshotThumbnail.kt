package com.example.scrollcompanion.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private object ThumbnailCache {
    private const val MAX_ENTRIES = 60
    private const val TARGET_WIDTH_PX = 240

    private val cache = LruCache<String, ImageBitmap>(MAX_ENTRIES)

    fun get(path: String): ImageBitmap? = cache.get(path)

    /** Decodes a downsampled copy so lists of full-screen screenshots stay light on memory. */
    fun load(path: String): ImageBitmap? {
        if (!File(path).exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_WIDTH_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, options)?.asImageBitmap() ?: return null
        cache.put(path, bitmap)
        return bitmap
    }
}

@Composable
fun ScreenshotThumbnail(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState(initialValue = ThumbnailCache.get(path), path) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { runCatching { ThumbnailCache.load(path) }.getOrNull() }
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = modifier.clip(RoundedCornerShape(10.dp)),
        )
    }
}
