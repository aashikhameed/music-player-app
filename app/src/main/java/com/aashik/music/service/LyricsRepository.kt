package com.aashik.music.service

import android.content.Context
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
import java.util.Locale
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
 * Adapted from BitChord's multi-source (BiniLyrics / Lrc.red TTML + LRCLIB + Sidecar) architecture.
 *
 * Features:
 *  - Multi-tier lyrics resolution: Local sidecar -> BiniLyrics / Apple Music TTML -> LRCLIB
 *  - Instant zero-latency caching: Memory LRU + Persistent disk cache (.lrc format)
 *  - Plain lyrics fallback: converts unsynced lyrics to timed lines when sync is unavailable
 *  - Pure standard Android HTTP (HttpURLConnection), zero external dependencies
 */
object LyricsRepository {
    private const val LRCLIB_URL = "https://lrclib.net/api"
    private const val BINI_URL = "https://lyrics-api.binimum.org"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Automotive) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val memoryCache = ConcurrentHashMap<String, List<LyricLine>>()
    private const val MAX_MEMORY_CACHE = 150

    private var diskCacheDir: File? = null

    private val STAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})]""")
    private val WORD_STAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})[.:](\d{2,3})>""")
    private val TTML_P_REGEX = Regex("""<p\b([^>]*)>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val BEGIN_ATTR_REGEX = Regex("""\bbegin=["']([^"']+)["']""")
    private val XML_TAG_REGEX = Regex("""<[^>]+>""")

    private val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".flac", ".wav", ".ogg", ".aac", ".opus")
    private val TRACK_NUMBER_REGEX = Regex("""^\d{1,3}[\s.-]+""")
    private val UPLOAD_NOISE_REGEX = Regex(
        """\((?:from|feat\.?|official|lyrical|video|audio|remix|full video|4k video)[^)]*\)|\[(?:from|feat\.?|official|lyrical|video|audio|remix|full video|4k video)[^\]]*]|""" +
            """\b(?:official (?:video|audio|music video)|lyrical (?:video|song)|full (?:song|video)|4k video|video song)\b""",
        RegexOption.IGNORE_CASE
    )

    fun init(context: Context) {
        if (diskCacheDir == null) {
            try {
                diskCacheDir = File(context.cacheDir, "lyrics").apply { mkdirs() }
            } catch (_: Exception) {}
        }
    }

    /**
     * Fast synchronous cache lookup for instant frame rendering during Next/Previous skips.
     * Checks memory cache first, then persistent disk cache.
     */
    fun getCachedLyrics(song: Song): List<LyricLine>? {
        val memKey = song.id
        memoryCache[memKey]?.let { return it }

        val normKey = getNormalizedKey(song)
        memoryCache[normKey]?.let { return it }

        // Check persistent disk cache
        val diskLines = readFromDisk(normKey) ?: readFromDisk(sanitizeKey(song.id))
        if (diskLines != null && diskLines.isNotEmpty()) {
            putInMemory(memKey, diskLines)
            putInMemory(normKey, diskLines)
            return diskLines
        }

        return null
    }

    /**
     * Fetches lyrics for the given song across all sources (Local Sidecar -> BiniLyrics TTML -> LRCLIB).
     */
    suspend fun getLyrics(song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        // 1. Check memory & disk cache
        getCachedLyrics(song)?.let { return@withContext it }

        val memKey = song.id
        val normKey = getNormalizedKey(song)

        // 2. Check local sidecar .lrc file for downloaded or local audio
        if (!song.isStream() && song.path.isNotBlank()) {
            val sidecar = try {
                val pathWithoutExt = song.path.substringBeforeLast('.')
                val lrcFile = File("$pathWithoutExt.lrc")
                if (lrcFile.exists() && lrcFile.isFile && lrcFile.canRead()) {
                    lrcFile.readText()
                } else {
                    val folderLrc = File(File(song.path).parentFile, "${song.title}.lrc")
                    if (folderLrc.exists() && folderLrc.isFile && folderLrc.canRead()) {
                        folderLrc.readText()
                    } else null
                }
            } catch (_: Exception) { null }

            if (!sidecar.isNullOrBlank()) {
                val parsed = parseLrc(sidecar)
                if (parsed.isNotEmpty()) {
                    cacheResult(memKey, normKey, parsed)
                    return@withContext parsed
                }
            }
        }

        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)
        val durationSec = (song.duration / 1000).toInt()

        // 3. Query BiniLyrics / Apple Music TTML (Primary source - vast coverage of worldwide and Indian songs)
        val biniLines = fetchBiniLyrics(cleanTitle, cleanArtist, song.duration)
            ?: if (cleanArtist.isNotBlank()) fetchBiniLyrics(cleanTitle, "", song.duration) else null

        if (biniLines != null && biniLines.isNotEmpty()) {
            cacheResult(memKey, normKey, biniLines)
            return@withContext biniLines
        }

        // 4. Query LRCLIB (Secondary fallback source)
        val lrcLines = fetchLrcLib(cleanTitle, cleanArtist, durationSec, song.duration)
            ?: if (cleanArtist.isNotBlank()) fetchLrcLib(cleanTitle, "", durationSec, song.duration) else null

        if (lrcLines != null && lrcLines.isNotEmpty()) {
            cacheResult(memKey, normKey, lrcLines)
            return@withContext lrcLines
        }

        null
    }

    private fun cacheResult(memKey: String, normKey: String, lines: List<LyricLine>) {
        putInMemory(memKey, lines)
        putInMemory(normKey, lines)
        saveToDisk(normKey, lines)
    }

    private fun putInMemory(key: String, lines: List<LyricLine>) {
        if (memoryCache.size >= MAX_MEMORY_CACHE) {
            memoryCache.clear()
        }
        memoryCache[key] = lines
    }

    private fun getNormalizedKey(song: Song): String {
        val t = cleanSongTitle(song.title).lowercase().replace(Regex("""[^a-z0-9]"""), "")
        val a = cleanArtistName(song.artist).lowercase().replace(Regex("""[^a-z0-9]"""), "")
        return if (a.isNotBlank()) "${t}_${a}" else t
    }

    private fun sanitizeKey(raw: String): String {
        return raw.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }

    private fun saveToDisk(key: String, lines: List<LyricLine>) {
        val dir = diskCacheDir ?: return
        try {
            val file = File(dir, "$key.lrc")
            file.writeText(linesToLrc(lines))
        } catch (_: Exception) {}
    }

    private fun readFromDisk(key: String): List<LyricLine>? {
        val dir = diskCacheDir ?: return null
        return try {
            val file = File(dir, "$key.lrc")
            if (file.exists() && file.isFile && file.length() > 0) {
                parseLrc(file.readText())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // ── BiniLyrics / Lrc.red TTML Fetcher ──

    private fun fetchBiniLyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val urlStr = if (artist.isNotBlank()) {
                val encArtist = URLEncoder.encode(artist, "UTF-8")
                "$BINI_URL/?track=$encTitle&artist=$encArtist"
            } else {
                "$BINI_URL/?track=$encTitle"
            }

            val jsonStr = httpGet(urlStr) ?: return null
            val obj = JSONObject(jsonStr)
            val results = obj.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            val targetSec = (durationMs / 1000).toInt()
            var bestHit: JSONObject? = null
            var minDiff = Int.MAX_VALUE

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val lyricsUrl = item.optString("lyricsUrl")
                if (lyricsUrl.isBlank()) continue

                val dur = item.optInt("duration", 0)
                val diff = if (targetSec > 0 && dur > 0) abs(dur - targetSec) else 0
                if (diff < minDiff) {
                    minDiff = diff
                    bestHit = item
                    if (diff <= 3) break
                }
            }

            val chosen = bestHit ?: results.optJSONObject(0) ?: return null
            val lyricsUrl = chosen.optString("lyricsUrl")
            if (lyricsUrl.isBlank()) return null

            val docContent = httpGet(lyricsUrl) ?: return null
            if (lyricsUrl.endsWith(".ttml", ignoreCase = true) || docContent.contains("<tt")) {
                parseTtml(docContent, durationMs)
            } else {
                parseLrc(docContent)
            }
        } catch (_: Exception) {
            null
        }
    }

    // ── LRCLIB Fetcher ──

    private fun fetchLrcLib(title: String, artist: String, seconds: Int, durationMs: Long): List<LyricLine>? {
        // Try exact match first
        val exactMatch = fetchLrcLibExact(title, artist, seconds, durationMs)
        if (exactMatch != null && exactMatch.isNotEmpty()) return exactMatch

        // Try search match
        return fetchLrcLibSearch(title, artist, seconds, durationMs)
    }

    private fun fetchLrcLibExact(title: String, artist: String, seconds: Int, durationMs: Long): List<LyricLine>? {
        if (artist.isBlank()) return null
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val urlStr = "$LRCLIB_URL/get?track_name=$encTitle&artist_name=$encArtist" +
                    if (seconds > 0) "&duration=$seconds" else ""
            val jsonStr = httpGet(urlStr) ?: return null
            val obj = JSONObject(jsonStr)
            val synced = obj.optString("syncedLyrics")
            if (synced.isNotBlank()) {
                val parsed = parseLrc(synced)
                if (parsed.isNotEmpty()) return parsed
            }
            val plain = obj.optString("plainLyrics")
            if (plain.isNotBlank()) {
                return parsePlainLyrics(plain, durationMs)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLrcLibSearch(title: String, artist: String, seconds: Int, durationMs: Long): List<LyricLine>? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val urlStr = if (artist.isNotBlank()) {
                val encArtist = URLEncoder.encode(artist, "UTF-8")
                "$LRCLIB_URL/search?track_name=$encTitle&artist_name=$encArtist"
            } else {
                "$LRCLIB_URL/search?track_name=$encTitle"
            }
            val jsonStr = httpGet(urlStr) ?: return null
            val array = JSONArray(jsonStr)
            var bestSynced: String? = null
            var bestPlain: String? = null
            var minDiff = Int.MAX_VALUE

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val synced = item.optString("syncedLyrics")
                val plain = item.optString("plainLyrics")
                if (synced.isBlank() && plain.isBlank()) continue

                val dur = item.optDouble("duration", 0.0).toInt()
                val diff = if (seconds > 0 && dur > 0) abs(dur - seconds) else 0
                if (diff < minDiff) {
                    minDiff = diff
                    if (synced.isNotBlank()) bestSynced = synced
                    if (plain.isNotBlank()) bestPlain = plain
                    if (diff <= 3 && synced.isNotBlank()) break
                }
            }

            if (!bestSynced.isNullOrBlank()) {
                val parsed = parseLrc(bestSynced)
                if (parsed.isNotEmpty()) return parsed
            }
            if (!bestPlain.isNullOrBlank()) {
                return parsePlainLyrics(bestPlain, durationMs)
            }
            null
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
            conn.setRequestProperty("Accept", "application/json, text/xml, */*")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    // ── Parsers ──

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

    fun parseTtml(ttml: String, durationMs: Long): List<LyricLine> {
        val list = mutableListOf<LyricLine>()
        val pMatches = TTML_P_REGEX.findAll(ttml)

        for (match in pMatches) {
            val attrs = match.groupValues[1]
            val content = match.groupValues[2]

            val beginMatch = BEGIN_ATTR_REGEX.find(attrs)
            val timeMs = beginMatch?.groupValues?.get(1)?.let { parseTimeMs(it) }

            val cleanText = unescapeHtml(content.replace(XML_TAG_REGEX, "")).trim()
            if (cleanText.isNotBlank()) {
                list.add(LyricLine(timeMs = timeMs ?: -1L, text = cleanText))
            }
        }

        if (list.isEmpty()) return emptyList()

        // Distribute timestamps if missing
        val stepMs = if (durationMs > 10_000L) {
            (durationMs - 5_000L) / list.size.coerceAtLeast(1)
        } else {
            3_000L
        }

        val result = mutableListOf<LyricLine>()
        for (i in list.indices) {
            val item = list[i]
            val actualTime = if (item.timeMs >= 0L) item.timeMs else (i * stepMs)
            result.add(LyricLine(timeMs = actualTime, text = item.text))
        }

        return result.sortedBy { it.timeMs }
    }

    private fun parsePlainLyrics(plain: String, durationMs: Long): List<LyricLine> {
        val lines = plain.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) return emptyList()

        val stepMs = if (durationMs > 10_000L) {
            (durationMs - 5_000L) / lines.size.coerceAtLeast(1)
        } else {
            3_000L
        }

        val result = ArrayList<LyricLine>(lines.size)
        for (i in lines.indices) {
            result.add(LyricLine(timeMs = i * stepMs, text = lines[i]))
        }
        return result
    }

    fun linesToLrc(lines: List<LyricLine>): String {
        val sb = StringBuilder()
        for (line in lines) {
            val totalSec = line.timeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val cs = (line.timeMs % 1000) / 10
            sb.append(String.format(Locale.US, "[%02d:%02d.%02d]%s\n", min, sec, cs, line.text))
        }
        return sb.toString()
    }

    private fun parseTimeMs(timeStr: String): Long {
        val clean = timeStr.trim()
        if (clean.contains(':')) {
            val parts = clean.split(':')
            if (parts.size == 3) {
                val h = parts[0].toDoubleOrNull() ?: 0.0
                val m = parts[1].toDoubleOrNull() ?: 0.0
                val s = parts[2].toDoubleOrNull() ?: 0.0
                return (h * 3_600_000 + m * 60_000 + s * 1_000).toLong()
            } else if (parts.size == 2) {
                val m = parts[0].toDoubleOrNull() ?: 0.0
                val s = parts[1].toDoubleOrNull() ?: 0.0
                return (m * 60_000 + s * 1_000).toLong()
            }
        }
        val seconds = clean.toDoubleOrNull() ?: 0.0
        return (seconds * 1_000).toLong()
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    // ── Metadata Sanitization ──

    fun cleanSongTitle(title: String): String {
        var t = title.trim()
        // Remove common audio file extensions
        for (ext in AUDIO_EXTENSIONS) {
            if (t.endsWith(ext, ignoreCase = true)) {
                t = t.substring(0, t.length - ext.length)
                break
            }
        }
        // Replace underscores with spaces
        t = t.replace('_', ' ')
        // Remove track numbers at start (e.g. "01 - ", "1. ")
        t = t.replace(TRACK_NUMBER_REGEX, "")
        // Remove YouTube/upload video packaging
        t = t.replace(UPLOAD_NOISE_REGEX, " ")
        // Remove pipe metadata
        if (t.contains(" | ")) {
            t = t.substringBefore(" | ")
        } else if (t.contains("|")) {
            t = t.substringBefore("|")
        }
        return t.replace(Regex("""\s+"""), " ").trim().ifBlank { title }
    }

    fun cleanArtistName(artist: String): String {
        var a = artist.replace(" - Topic", "").trim()
        a = a.replace(Regex("""(?i)\b(?:feat\.?|ft\.).*$"""), "").trim()
        val first = a.split(',', '&', ';', '/', '•').firstOrNull()?.trim() ?: a
        val lower = first.lowercase()
        if (lower in listOf("unknown", "<unknown>", "unknown artist", "various artists", "youtube", "musicplayer")) {
            return ""
        }
        return first
    }
}
