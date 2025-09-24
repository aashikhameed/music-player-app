package com.aashik.music.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

object AlbumArtCache {

    // --- In-memory LruCache ---
    private val memoryCache = object : LruCache<String, Bitmap>(calculateMaxSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // size in KB
        }
    }

    private fun calculateMaxSize(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return maxMemoryKb / 16 // 1/16th of memory
    }

    // --- Disk cache ---
    private const val MAX_DISK_CACHE_SIZE = 50L * 1024L * 1024L // 50MB

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
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }
            enforceDiskLimit(file.parentFile)
        } catch (_: Exception) {}
    }

    private fun loadFromDisk(context: Context, path: String): Bitmap? {
        val file = getAlbumArtFile(context, path)
        return if (file.exists()) decodeScaledBitmap(file) else null
    }

    // --- Scale large images ---
    private fun decodeScaledBitmap(file: File, maxSize: Int = 512): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var scale = 1
        while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
            scale *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = scale }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    // --- Disk limit enforcement ---
    private fun enforceDiskLimit(cacheDir: File?) {
        cacheDir ?: return
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalSize = files.sumOf { it.length() }
        for (file in files) {
            if (totalSize <= MAX_DISK_CACHE_SIZE) break
            totalSize -= file.length()
            file.delete()
        }
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

        // 3. Load from loader
        return withContext(Dispatchers.IO) {
            loader(path)?.also {
                memoryCache.put(path, it)
                saveToDisk(context, path, it)
            }
        }
    }

    // --- Async preloading ---
    private val preloadQueue = ConcurrentLinkedQueue<Pair<Context, String>>()
    private var preloadJob: Job? = null

    fun preload(context: Context, paths: List<String>, loader: suspend (String) -> Bitmap?) {
        preloadQueue.addAll(paths.map { context to it })
        if (preloadJob?.isActive != true) {
            preloadJob = CoroutineScope(Dispatchers.IO).launch {
                while (preloadQueue.isNotEmpty()) {
                    val (ctx, path) = preloadQueue.poll() ?: continue
                    try {
                        getOrLoad(ctx, path, loader)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun cancelPreload() {
        preloadJob?.cancel()
        preloadJob = null
        preloadQueue.clear()
    }

    // --- Clear caches ---
    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        val cacheDir = File(context.cacheDir, "albumart")
        cacheDir.deleteRecursively()
    }
}
