package com.aashik.music.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

object AlbumArtCache {

    // Fast O(1) in-memory cache tailored for low-spec (2GB RAM) devices
    private val memoryCache = object : LruCache<String, Bitmap>(calculateMaxSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // Size in KB
        }
    }

    // Tracks known files without album art to skip redundant disk I/O & retriever calls
    private val noArtPaths = ConcurrentHashMap.newKeySet<String>()

    // Deduplicate in-flight decode jobs for the same track
    private val inFlightLoads = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Bitmap?>>()

    // Concurrency throttle: at most 3 MediaMetadataRetriever decodes concurrently
    private val decodeSemaphore = Semaphore(3)
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Throttle disk cache size checks to avoid flash I/O bottlenecks during scrolling
    private val diskWriteCount = AtomicInteger(0)

    private fun calculateMaxSize(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // Allocate up to 3MB max for 8KB cover art thumbnails (avoids GC pressure on 1-2GB RAM)
        return (maxMemoryKb / 64).coerceIn(2048, 4096)
    }

    private const val MAX_DISK_CACHE_SIZE = 25L * 1024L * 1024L

    private fun getAlbumArtFile(context: Context, path: String): File {
        val cacheDir = File(context.cacheDir, "albumart")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = path.hashCode().toString() + ".jpg"
        return File(cacheDir, fileName)
    }

    private fun saveToDisk(context: Context, path: String, bitmap: Bitmap) {
        try {
            val file = getAlbumArtFile(context, path)
            FileOutputStream(file).use { out ->
                // Fast NEON-accelerated JPEG encoding avoids pegging CPU cores during scrolling
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            if (diskWriteCount.incrementAndGet() % 100 == 0) {
                cacheScope.launch {
                    enforceDiskLimit(file.parentFile)
                }
            }
        } catch (_: Exception) {}
    }

    private fun getNoArtFile(context: Context, path: String): File {
        val cacheDir = File(context.cacheDir, "albumart")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = path.hashCode().toString() + ".noart"
        return File(cacheDir, fileName)
    }

    private fun loadFromDisk(context: Context, path: String): Bitmap? {
        if (getNoArtFile(context, path).exists()) {
            noArtPaths.add(path)
            return null
        }
        val file = getAlbumArtFile(context, path)
        return if (file.exists()) decodeScaledBitmap(file) else null
    }

    private fun decodeScaledBitmap(file: File, maxSize: Int = 64): Bitmap? {
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

    fun hasNoArt(path: String): Boolean = noArtPaths.contains(path)

    suspend fun getOrLoad(
        context: Context,
        path: String,
        loader: suspend (String) -> Bitmap?
    ): Bitmap? {
        // Fast path 1: In-memory hit
        memoryCache.get(path)?.let { return it }

        // Fast path 2: Known missing artwork
        if (noArtPaths.contains(path)) return null

        // Fast path 3: Return existing in-flight decode if already running
        inFlightLoads[path]?.let { return it.await() }

        return withContext(Dispatchers.IO) {
            memoryCache.get(path)?.let { return@withContext it }

            // Check disk cache first
            val diskBitmap = loadFromDisk(context, path)
            if (diskBitmap != null) {
                memoryCache.put(path, diskBitmap)
                return@withContext diskBitmap
            }

            // Deduplicate and throttle decoder concurrency
            val deferred = async {
                decodeSemaphore.withPermit {
                    try {
                        val loadedBitmap = loader(path)
                        if (loadedBitmap != null) {
                            memoryCache.put(path, loadedBitmap)
                            saveToDisk(context, path, loadedBitmap)
                            loadedBitmap
                        } else {
                            noArtPaths.add(path)
                            try { getNoArtFile(context, path).createNewFile() } catch (_: Exception) {}
                            null
                        }
                    } finally {
                        inFlightLoads.remove(path)
                    }
                }
            }
            inFlightLoads[path] = deferred
            deferred.await()
        }
    }

    // Background prefetching for ultra-smooth list scrolling
    private val preloadQueue = ConcurrentLinkedQueue<Pair<Context, String>>()
    private var preloadJob: Job? = null

    fun preload(context: Context, paths: List<String>, loader: suspend (String) -> Bitmap?) {
        for (path in paths) {
            if (memoryCache.get(path) == null && !noArtPaths.contains(path)) {
                preloadQueue.add(context to path)
            }
        }

        if (preloadJob?.isActive != true && preloadQueue.isNotEmpty()) {
            preloadJob = CoroutineScope(Dispatchers.IO).launch {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
                kotlinx.coroutines.delay(600) // Yield CPU to initial UI composition
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
