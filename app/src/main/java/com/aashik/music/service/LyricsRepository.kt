package com.aashik.music.service

import com.aashik.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * One synchronized lyric line.
 * [timeMs] marks when the line starts in playback.
 * [text] is the lyric string for this line.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * Ultra-lightweight Automotive Lyrics Engine.
 * Adapted from BitChord's LRCLIB and local sidecar implementation.
 *
 * Fetches time-synced lyrics with zero external dependencies (pure Android HttpURLConnection),
 * maintaining strict automotive low-memory and low-latency performance constraints.
 */
object LyricsRepository {
    private const val BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "MusicPlayerAuto/1.0 (Automotive Android Headunit)"

    private val cache = ConcurrentHashMap<String, List<LyricLine>>()
    private const val MAX_CACHE_SIZE = 50

    private val STAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})]""")
    private val WORD_STAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})[.:](\d{2,3})>""")
    private val NOISE_REGEX = Regex(
        """\((?:from|feat\.?|official|lyrical|video|audio|remix)[^)]*\)|\[[^]]*]|""" +
            """\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b""",
        RegexOption.IGNORE_CASE
    )

    suspend fun getLyrics(song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cacheKey = song.id
        cache[cacheKey]?.let { return@withContext it }

        // 1. Check local sidecar .lrc file first for downloaded or local audio
        if (!song.isStream() && song.path.isNotBlank()) {
            val sidecar = try {
                val pathWithoutExt = song.path.substringBeforeLast('.')
                val lrcFile = File("$pathWithoutExt.lrc")
                if (lrcFile.exists() && lrcFile.isFile && lrcFile.canRead()) {
                    lrcFile.readText()
                } else null
            } catch (_: Exception) { null }

            if (!sidecar.isNullOrBlank()) {
                val parsed = parseLrc(sidecar)
                if (parsed.isNotEmpty()) {
                    putInCache(cacheKey, parsed)
                    return@withContext parsed
                }
            }
        }

        // 2. Query LRCLIB (exact match first, then fuzzy search with artist, then search by title)
        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)
        val durationSec = (song.duration / 1000).toInt()

        val exactLrc = fetchExact(cleanTitle, cleanArtist, durationSec)
        val lrcText = exactLrc
            ?: fetchSearch(cleanTitle, cleanArtist, durationSec)
            ?: fetchSearch(cleanTitle, "", durationSec)

        if (!lrcText.isNullOrBlank()) {
            val parsed = parseLrc(lrcText)
            if (parsed.isNotEmpty()) {
                putInCache(cacheKey, parsed)
                return@withContext parsed
            }
        }

        null
    }

    private fun putInCache(key: String, lines: List<LyricLine>) {
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.clear()
        }
        cache[key] = lines
    }

    private fun fetchExact(title: String, artist: String, seconds: Int): String? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val urlStr = "$BASE_URL/get?track_name=$encTitle&artist_name=$encArtist" +
                    if (seconds > 0) "&duration=$seconds" else ""
            val jsonStr = httpGet(urlStr) ?: return null
            val obj = JSONObject(jsonStr)
            obj.optString("syncedLyrics").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchSearch(title: String, artist: String, seconds: Int): String? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val urlStr = if (artist.isNotBlank()) {
                val encArtist = URLEncoder.encode(artist, "UTF-8")
                "$BASE_URL/search?track_name=$encTitle&artist_name=$encArtist"
            } else {
                "$BASE_URL/search?track_name=$encTitle"
            }
            val jsonStr = httpGet(urlStr) ?: return null
            val array = JSONArray(jsonStr)
            var bestHit: String? = null
            var minDiff = Int.MAX_VALUE

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val synced = item.optString("syncedLyrics")
                if (synced.isBlank()) continue
                val dur = item.optDouble("duration", 0.0).toInt()
                val diff = if (seconds > 0 && dur > 0) abs(dur - seconds) else 0
                if (diff < minDiff) {
                    minDiff = diff
                    bestHit = synced
                    if (diff <= 3) break
                }
            }
            bestHit
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun parseLrc(lrc: String): List<LyricLine> {
        val list = mutableListOf<LyricLine>()
        for (rawLine in lrc.lineSequence()) {
            val line = rawLine.trim()
            val match = STAMP_REGEX.find(line) ?: continue
            val minutes = match.groupValues[1].toLongOrNull() ?: continue
            val seconds = match.groupValues[2].toLongOrNull() ?: continue
            val fractionStr = match.groupValues[3]
            val fractionMs = when (fractionStr.length) {
                2 -> (fractionStr.toLongOrNull() ?: 0L) * 10
                3 -> fractionStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val timeMs = minutes * 60_000L + seconds * 1_000L + fractionMs
            val rawText = line.substring(match.range.last + 1)
            val cleanText = rawText.replace(WORD_STAMP_REGEX, "").trim()
            list.add(LyricLine(timeMs = timeMs, text = cleanText))
        }
        return list.sortedBy { it.timeMs }
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(NOISE_REGEX, " ")
            .substringBefore(" | ")
            .substringBefore(" - ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { title }
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split(',', '&', ';', '/', '•')
            .firstOrNull()
            ?.trim()
            ?: artist
    }
}

