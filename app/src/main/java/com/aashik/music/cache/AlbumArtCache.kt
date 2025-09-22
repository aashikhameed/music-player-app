package com.aashik.music.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AlbumArtCache {

    // --- In-memory LruCache ---
    private val memoryCache = object : LruCache<String, Bitmap>(calculateMaxSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // size in KB
        }
    }

    private fun calculateMaxSize(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return maxMemoryKb / 16 // 1/16th of memory to reduce size ~50%
    }

    // --- Disk cache helpers ---
    private fun getAlbumArtFile(context: Context, path: String): File {
        val cacheDir = File(context.cacheDir, "albumart")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = path.hashCode().toString() + ".webp"
        return File(cacheDir, fileName)
    }

    private fun saveToDisk(context: Context, path: String, bitmap: Bitmap) {
        try {
            val file = getAlbumArtFile(context, path)
            FileOutputStream(file).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    TODO("VERSION.SDK_INT < R")
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadFromDisk(context: Context, path: String): Bitmap? {
        val file = getAlbumArtFile(context, path)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    // --- Public API ---
    fun contains(path: String) = memoryCache.get(path) != null

    fun get(path: String): Bitmap? = memoryCache.get(path)

    suspend fun getOrLoad(
        context: Context,
        path: String,
        loader: suspend (String) -> Bitmap?
    ): Bitmap? {
        // 1. Try memory cache
        memoryCache.get(path)?.let { return it }

        // 2. Try disk cache
        loadFromDisk(context, path)?.let {
            memoryCache.put(path, it)
            return it
        }

        // 3. Load from provided loader (e.g., metadata extraction)
        return withContext(Dispatchers.IO) {
            loader(path)?.also {
                memoryCache.put(path, it)
                saveToDisk(context, path, it)
            }
        }
    }
}
