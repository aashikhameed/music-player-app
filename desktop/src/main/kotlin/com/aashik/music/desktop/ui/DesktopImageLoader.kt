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

    suspend fun load(rawUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isBlank()) return@withContext null
        memory[url]?.let { return@withContext it }

        try {
            val bmp = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                loadHttpImage(url)
            } else {
                loadLocalImageOrAudioArt(url)
            }
            if (bmp != null) {
                memory[url] = bmp
                return@withContext bmp
            }
        } catch (e: Exception) {
            // Silently fall through to null on corrupt or missing art
        }
        null
    }

    private fun loadHttpImage(urlString: String): ImageBitmap? {
        val conn = (URL(urlString).openConnection() as? HttpURLConnection) ?: return null
        conn.connectTimeout = 5000
        conn.readTimeout = 7000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
        )
        conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        conn.setRequestProperty("Referer", String(Base64.getDecoder().decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tLw=="), Charsets.UTF_8))
        conn.connect()

        if (conn.responseCode in 200..299) {
            conn.inputStream.use { stream -> return decode(stream.readBytes()) }
        }
        return null
    }

    /** Skia decodes JPEG, PNG, WebP and GIF (ImageIO cannot read WebP, which most online covers use). */
    private fun decode(bytes: ByteArray): ImageBitmap? = try {
        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Throwable) { null }

    private fun loadLocalImageOrAudioArt(pathOrUri: String): ImageBitmap? {
        val cleanPath = when {
            pathOrUri.startsWith("file://") -> pathOrUri.substring(7)
            pathOrUri.startsWith("file:") -> pathOrUri.substring(5)
            else -> pathOrUri
        }
        val file = File(cleanPath)
        if (!file.exists() || !file.isFile) return null

        val ext = file.extension.lowercase()
        // 1. Direct Image Files
        if (ext in listOf("jpg", "jpeg", "png", "webp", "bmp", "gif")) return decode(file.readBytes())

        // 2. Embedded Audio Artwork (.m4a, .mp4, .mp3, .flac)
        val rawArtBytes = extractEmbeddedArtworkBytes(file)
        if (rawArtBytes != null && rawArtBytes.isNotEmpty()) decode(rawArtBytes)?.let { return it }
        // 3. Cover saved next to the file (downloads): "<name>.jpg/.webp/.png"
        listOf("jpg", "jpeg", "webp", "png").map { File(file.parentFile, file.nameWithoutExtension + "." + it) }
            .firstOrNull { it.exists() }?.let { return decode(it.readBytes()) }
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
