package com.aashik.music.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, zero-external-dependency YouTube Video Resolver.
 * Uses YouTube Music's InnerTube API (WEB_REMIX client, exactly like BitChord)
 * to resolve official music video IDs for any streaming track in ~200ms.
 * Enforces zero-bloat APK constraints (< 4.0 MB) with zero third-party libraries.
 */
object YouTubeVideoResolver {
    private const val TAG = "YouTubeVideoResolver"
    private val videoCache = ConcurrentHashMap<String, String>()

    suspend fun resolveVideoId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title
            .replace("&amp;", "&")
            .replace(Regex("""(?i)\(From\s+["'][^"']+["']\)"""), "")
            .replace(Regex("""(?i)\(Reprise\)"""), "")
            .replace(Regex("""(?i)\(Official.*?\)"""), "")
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val cleanArtist = artist
            .replace("&amp;", "&")
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val cacheKey = "$cleanTitle::$cleanArtist".lowercase()
        videoCache[cacheKey]?.let { return@withContext it }

        val query = "$cleanTitle $cleanArtist official music video"
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/search")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            }

            val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20260707.12.00","hl":"en","gl":"US"}},"query":"$escapedQuery"}"""
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex(""""videoId":\s*"([a-zA-Z0-9_-]{11})"""").find(response)
                val videoId = match?.groupValues?.get(1)
                if (!videoId.isNullOrBlank()) {
                    videoCache[cacheKey] = videoId
                    return@withContext videoId
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Video resolution failed for $cleanTitle: ${e.message}")
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
        null
    }

    suspend fun resolveVideoIdForSong(song: com.aashik.music.model.Song): String? {
        val directId = song.getYouTubeVideoId()
        if (!directId.isNullOrBlank()) return directId
        return resolveVideoId(song.title, song.artist)
    }
}
