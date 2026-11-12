package com.aashik.music.desktop.ui

import androidx.compose.foundation.Image
import java.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withPermit
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal High-Performance Desktop Artwork Loader & Cacher.
 * Supports:
 *  1. Remote HTTP/HTTPS images (with browser User-Agent headers to bypass CDN blocks).
 *  2. Local image files (.jpg, .png, .webp).
 *  3. Local audio files (.m4a, .mp4, .mp3, .flac) via embedded metadata extraction (covr atom & ID3 APIC frame).
 */
object DesktopImageCache {
    private val memory = ConcurrentHashMap<String, ImageBitmap>()
    private val httpGate = kotlinx.coroutines.sync.Semaphore(6)

    private val diskDir = File(System.getProperty("user.home"), ".musicplayer/cache/art").also { runCatching { it.mkdirs() } }
    private fun diskFile(url: String): File {
        val h = java.security.MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(diskDir, "$h.jpg")
    }

    suspend fun load(rawUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isBlank()) return@withContext null
        memory[url]?.let { return@withContext it }

        try {
            val bmp = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                loadRemote(url)
            } else {
                loadLocalImageOrAudioArt(url)
            }
            if (bmp != null) {
                memory[url] = bmp
                return@withContext bmp
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Silently fall through to null on corrupt or missing art
        }
        null
    }

    /** Remote cover: disk cache first; otherwise fetch (6 at a time, 3 tries), keep the 60 % JPEG on disk. */
    private suspend fun loadRemote(url: String): ImageBitmap? {
        val f = diskFile(url)
        if (f.exists() && f.length() > 0) runCatching { return bitmapOf(f.readBytes()) }
        return httpGate.withPermit {
            var lossy: ByteArray? = null
            var weak = false
            for (attempt in 0 until 3) {
                var raw = runCatching { fetchBytes(url) }.getOrNull()
                weak = false
                // Not every cover exists at 500x500: fall back to the 150x150 the catalogue always has (not cached, so a later launch retries the big one).
                if (raw == null && url.contains("500x500")) { raw = runCatching { fetchBytes(url.replace("500x500", "150x150")) }.getOrNull(); weak = raw != null }
                lossy = raw?.let { encodeLossy(it) }
                if (lossy != null) break
                kotlinx.coroutines.delay(400L * (attempt + 1))
            }
            lossy?.let { l -> if (!weak) runCatching { f.writeBytes(l) }; bitmapOf(l) }
        }
    }

    private fun fetchBytes(urlString: String): ByteArray? {
        val conn = (URL(urlString).openConnection() as? HttpURLConnection) ?: return null
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
            conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            conn.setRequestProperty("Referer", String(Base64.getDecoder().decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tLw=="), Charsets.UTF_8))
            conn.connect()
            return if (conn.responseCode in 200..299) conn.inputStream.use { it.readBytes() } else null
        } finally { conn.disconnect() }
    }

    /**
     * Skia decodes JPEG, PNG, WebP and GIF (ImageIO cannot read WebP, which most online covers use).
     * Artwork is kept at 60 % quality (re-encoded JPEG, longest side <= 600 px): small in memory, plenty for the UI.
     */
    private fun decode(bytes: ByteArray): ImageBitmap? = encodeLossy(bytes)?.let { bitmapOf(it) }

    private fun bitmapOf(encoded: ByteArray): ImageBitmap? = try {
        org.jetbrains.skia.Image.makeFromEncoded(encoded).toComposeImageBitmap()
    } catch (_: Throwable) { null }

    private fun encodeLossy(bytes: ByteArray): ByteArray? = try {
        val img = org.jetbrains.skia.Image.makeFromEncoded(bytes)
        val scale = minOf(1f, MAX_SIDE / maxOf(img.width, img.height).toFloat())
        val w = maxOf(1, (img.width * scale).toInt()); val h = maxOf(1, (img.height * scale).toInt())
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(img, org.jetbrains.skia.Rect.makeWH(img.width.toFloat(), img.height.toFloat()), org.jetbrains.skia.Rect.makeWH(w.toFloat(), h.toFloat()), org.jetbrains.skia.SamplingMode.LINEAR, null, true)
        surface.makeImageSnapshot().encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG, 60)?.bytes
    } catch (_: Throwable) { null }

    private const val MAX_SIDE = 600f

    private fun loadLocalImageOrAudioArt(pathOrUri: String): ImageBitmap? {
        // file: URIs are percent-encoded ("Artist%20-%20Title.webp"): decode them, never strip the prefix by hand.
        val file = if (pathOrUri.startsWith("file:")) {
            runCatching { File(java.net.URI(pathOrUri)) }.getOrElse { File(java.net.URLDecoder.decode(pathOrUri.removePrefix("file://").removePrefix("file:"), "UTF-8")) }
        } else File(pathOrUri)
        if (!file.exists() || !file.isFile) return null

        val ext = file.extension.lowercase()
        // 1. Direct Image Files
        if (ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")) return decode(file.readBytes())

        // 2. Cover saved next to the file (Jio cover from downloads / lookup) beats whatever is embedded (often a YouTube thumbnail)
        listOf("jpg", "jpeg", "webp", "png").map { File(file.parentFile, file.nameWithoutExtension + "." + it) }
            .firstOrNull { it.exists() }?.let { c -> decode(c.readBytes())?.let { return it } }
        // 3. Embedded Audio Artwork (.m4a, .mp4, .mp3, .flac)
        val rawArtBytes = extractEmbeddedArtworkBytes(file)
        if (rawArtBytes != null && rawArtBytes.isNotEmpty()) decode(rawArtBytes)?.let { return it }
        return null
    }

    /**
     * Extracts embedded JPEG or PNG image bytes from M4A (covr atom) or MP3 (ID3v2 APIC).
     */
    fun extractEmbeddedArtworkBytes(file: File): ByteArray? {
        val ext = file.extension.lowercase()
        return try {
            when (ext) {
                "m4a", "mp4", "aac" -> extractM4ACovr(file)
                "mp3" -> extractMp3Apic(file)
                else -> {
                    scanFileForImage(file, maxBytesToScan = 3 * 1024 * 1024)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractM4ACovr(file: File): ByteArray? {
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            val scanLimit = minOf(length, 12L * 1024 * 1024).toInt()
            val buffer = ByteArray(scanLimit)
            raf.readFully(buffer)

            val covrIndex = indexOf(buffer, "covr".toByteArray())
            if (covrIndex != -1) {
                val jpgStart = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), covrIndex)
                if (jpgStart != -1 && jpgStart - covrIndex < 250) {
                    val jpgEnd = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart)
                    if (jpgEnd != -1) {
                        val size = jpgEnd + 2 - jpgStart
                        if (size in 500..10_000_000) {
                            return buffer.copyOfRange(jpgStart, jpgEnd + 2)
                        }
                    }
                }
                val pngMagic = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
                val pngStart = indexOf(buffer, pngMagic, covrIndex)
                if (pngStart != -1 && pngStart - covrIndex < 250) {
                    val iendMagic = "IEND".toByteArray()
                    val pngEnd = indexOf(buffer, iendMagic, pngStart)
                    if (pngEnd != -1) {
                        val size = pngEnd + 8 - pngStart
                        if (size in 500..10_000_000) {
                            return buffer.copyOfRange(pngStart, pngEnd + 8)
                        }
                    }
                }
            }

            if (length > scanLimit) {
                val tailSize = minOf(length - scanLimit, 2L * 1024 * 1024).toInt()
                val tailBuf = ByteArray(tailSize)
                raf.seek(length - tailSize)
                raf.readFully(tailBuf)
                val tailCovr = indexOf(tailBuf, "covr".toByteArray())
                if (tailCovr != -1) {
                    val jpgStart = indexOf(tailBuf, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), tailCovr)
                    if (jpgStart != -1) {
                        val jpgEnd = indexOf(tailBuf, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart)
                        if (jpgEnd != -1) {
                            return tailBuf.copyOfRange(jpgStart, jpgEnd + 2)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun extractMp3Apic(file: File): ByteArray? {
        RandomAccessFile(file, "r").use { raf ->
            val scanLimit = minOf(raf.length(), 4L * 1024 * 1024).toInt()
            val buffer = ByteArray(scanLimit)
            raf.readFully(buffer)

            val apicIndex = indexOf(buffer, "APIC".toByteArray())
            if (apicIndex != -1) {
                val jpgStart = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), apicIndex)
                if (jpgStart != -1 && jpgStart - apicIndex < 400) {
                    val jpgEnd = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart)
                    if (jpgEnd != -1) {
                        return buffer.copyOfRange(jpgStart, jpgEnd + 2)
                    }
                }
                val pngMagic = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
                val pngStart = indexOf(buffer, pngMagic, apicIndex)
                if (pngStart != -1 && pngStart - apicIndex < 400) {
                    val pngEnd = indexOf(buffer, "IEND".toByteArray(), pngStart)
                    if (pngEnd != -1) {
                        return buffer.copyOfRange(pngStart, pngEnd + 8)
                    }
                }
            }
        }
        return null
    }

    private fun scanFileForImage(file: File, maxBytesToScan: Int): ByteArray? {
        RandomAccessFile(file, "r").use { raf ->
            val scanLimit = minOf(raf.length(), maxBytesToScan.toLong()).toInt()
            val buffer = ByteArray(scanLimit)
            raf.readFully(buffer)

            val jpgStart = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), 0)
            if (jpgStart != -1) {
                val jpgEnd = indexOf(buffer, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart)
                if (jpgEnd != -1 && (jpgEnd + 2 - jpgStart) in 1000..5_000_000) {
                    return buffer.copyOfRange(jpgStart, jpgEnd + 2)
                }
            }
        }
        return null
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int = 0): Int {
        if (target.isEmpty() || fromIndex < 0 || fromIndex >= source.size) return -1
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}

/**
 * Modern Liquid Glass Artwork Frame.
 * Features:
 * - Asynchronous artwork loading with in-memory caching.
 * - Specular liquid glass border and subtle dark background.
 * - Graceful fallback to dark slate musical note icon.
 */
@Composable
fun DesktopArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    shape: RoundedCornerShape = DesktopArtShape
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            bitmap = DesktopImageCache.load(url)
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(DarkSurfaceVariant)
            .border(GlassBorderSubtle, shape),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = DarkAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(size / 3)
            )
        }
    }
}


/** Loads an image for the UI and quietly retries (2 s, 4 s, 6 s, 8 s) if it fails; [load] returns null when there is nothing to show. */
@Composable
fun rememberRetryingImage(key: Any?, load: suspend () -> ImageBitmap?): ImageBitmap? {
    var bmp by remember(key) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(key) {
        var tries = 0
        while (bmp == null && tries < 5) {
            bmp = load()
            if (bmp == null) kotlinx.coroutines.delay(2000L * (tries + 1))
            tries++
        }
    }
    return bmp
}
