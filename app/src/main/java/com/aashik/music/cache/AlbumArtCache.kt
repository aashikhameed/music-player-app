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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object AlbumArtCache {

    // Fast O(1) in-memory cache tailored for low-spec (2GB RAM) devices
    private val memoryCache = object : LruCache<String, Bitmap>(calculateMaxSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // Size in KB
        }
    }

    // Tracks known files without album art to skip redundant disk I/O & retriever calls
    private val noArtPaths = ConcurrentHashMap.newKeySet<String>()

    private fun calculateMaxSize(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // Allocate up to 8MB max for cover art thumbnails
        return (maxMemoryKb / 32).coerceIn(4096, 8192)
    }

    private const val MAX_DISK_CACHE_SIZE = 25L * 1024L * 1024L

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
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 75, out)
                }
            }
            enforceDiskLimit(file.parentFile)
        } catch (_: Exception) {}
    }

    private fun loadFromDisk(context: Context, path: String): Bitmap? {
        val file = getAlbumArtFile(context, path)
        return if (file.exists()) decodeScaledBitmap(file) else null
    }

    private fun decodeScaledBitmap(file: File, maxSize: Int = 120): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var scale = 1
        while (options.outWidth / scale > maxSize * 2 || options.outHeight / scale > maxSize * 2) {
            scale *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = scale
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

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

    fun contains(path: String) = memoryCache.get(path) != null

    fun get(path: String): Bitmap? = memoryCache.get(path)

    suspend fun getOrLoad(
        context: Context,
        path: String,
        loader: suspend (String) -> Bitmap?
    ): Bitmap? {
        // Fast path 1: In-memory hit
        memoryCache.get(path)?.let { return it }

        // Fast path 2: Known missing artwork
        if (noArtPaths.contains(path)) return null

        return withContext(Dispatchers.IO) {
            // Check disk cache first
            val diskBitmap = loadFromDisk(context, path)
            if (diskBitmap != null) {
                memoryCache.put(path, diskBitmap)
                return@withContext diskBitmap
            }

            // Load via MediaMetadataRetriever
            val loadedBitmap = loader(path)
            if (loadedBitmap != null) {
                memoryCache.put(path, loadedBitmap)
                saveToDisk(context, path, loadedBitmap)
                loadedBitmap
            } else {
                noArtPaths.add(path)
                null
            }
        }
    }

    // Background prefetching for ultra-smooth list scrolling
    private val preloadQueue = ConcurrentLinkedQueue<Pair<Context, String>>()
    private var preloadJob: Job? = null

    fun preload(context: Context, paths: List<String>, loader: suspend (String) -> Bitmap?) {
        val unvisited = paths.filter { !memoryCache.snapshot().containsKey(it) && !noArtPaths.contains(it) }
        preloadQueue.addAll(unvisited.map { context to it })

        if (preloadJob?.isActive != true && preloadQueue.isNotEmpty()) {
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

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        noArtPaths.clear()
        val cacheDir = File(context.cacheDir, "albumart")
        cacheDir.deleteRecursively()
    }
}
