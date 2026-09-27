package com.ayush.aspect.core.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small in-memory thumbnail cache backed by MediaStore's thumbnail API.
 * Full-resolution gallery media is never decoded for the grid.
 */
object ThumbnailLoader {
    private const val THUMBNAIL_SIZE = 256

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        val key = uri.toString()
        synchronized(cache) {
            cache.get(key)?.let { return it }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.loadThumbnail(uri, Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
            }.getOrNull()
        } ?: return null

        synchronized(cache) {
            cache.put(key, bitmap)
        }
        return bitmap
    }

    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }

    private fun cacheSizeKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return (maxMemoryKb / 12).coerceIn(4 * 1024, 16 * 1024)
    }
}
