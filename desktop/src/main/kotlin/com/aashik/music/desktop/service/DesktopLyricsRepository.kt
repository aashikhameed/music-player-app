package com.aashik.music.desktop.service

import com.aashik.music.desktop.model.DesktopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class DesktopLyricLine(
    val timeMs: Long,
    val text: String
)

object DesktopLyricsRepository {
    private const val LRCLIB_URL = "https://lrclib.net/api"
    private val memoryCache = ConcurrentHashMap<String, List<DesktopLyricLine>>()
    private val STAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})]""")

    suspend fun fetchLyrics(song: DesktopSong): List<DesktopLyricLine> = withContext(Dispatchers.IO) {
        memoryCache[song.id]?.let { return@withContext it }

        var connection: HttpURLConnection? = null
        try {
            val encTrack = URLEncoder.encode(song.title, "UTF-8")
            val encArtist = URLEncoder.encode(song.artist, "UTF-8")
            val durSec = (song.duration / 1000L).coerceAtLeast(0L)

            val urlString = "$LRCLIB_URL/get?track_name=$encTrack&artist_name=$encArtist&duration=$durSec"
            val url = URL(urlString)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 4000
                setRequestProperty("User-Agent", "MusicPlayerDesktop/1.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val obj = JSONObject(jsonText)
                val syncedLrc = obj.optString("syncedLyrics", "")
                val plainLyrics = obj.optString("plainLyrics", "")

                val lines = if (syncedLrc.isNotBlank()) {
                    parseLrcText(syncedLrc)
                } else if (plainLyrics.isNotBlank()) {
                    parsePlainLyrics(plainLyrics, song.duration)
                } else emptyList()

                if (lines.isNotEmpty()) {
                    memoryCache[song.id] = lines
                    return@withContext lines
                }
            }
        } catch (_: Exception) {
        } finally {
            connection?.disconnect()
        }

        emptyList()
    }

    private fun parseLrcText(lrc: String): List<DesktopLyricLine> {
        val list = mutableListOf<DesktopLyricLine>()
        lrc.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            val match = STAMP_REGEX.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) (msStr.toLongOrNull() ?: 0L) * 10L else (msStr.toLongOrNull() ?: 0L)
                val totalMs = (min * 60L + sec) * 1000L + ms
                val text = trimmed.substring(match.range.last + 1).trim()
                if (text.isNotBlank()) {
                    list.add(DesktopLyricLine(timeMs = totalMs, text = text))
                }
            }
        }
        return list.sortedBy { it.timeMs }
    }

    private fun parsePlainLyrics(plain: String, totalDurationMs: Long): List<DesktopLyricLine> {
        val nonBlank = plain.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return emptyList()
        val interval = if (totalDurationMs > 0) totalDurationMs / nonBlank.size else 4000L
        return nonBlank.mapIndexed { idx, text ->
            DesktopLyricLine(timeMs = idx * interval, text = text)
        }
    }
}
