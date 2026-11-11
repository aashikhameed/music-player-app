package com.aashik.music.service

import android.os.Build
import android.util.Log
import com.aashik.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * High-performance Online Stream & Radio Queue Engine.
 * Provides live search, encrypted audio stream resolution, and algorithmic radio queue generation.
 * Built using zero external dependencies (pure Android SDK HttpURLConnection & javax.crypto.Cipher)
 * to maintain strict automotive APK size budget (< 4 MB) and ultra-low RAM usage.
 */
object OnlineStreamRepository {
    private const val TAG = "OnlineStreamRepo"
    private val API_BASE = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tL2FwaS5waHA="), Charsets.UTF_8)
    private val DES_KEY = String(java.util.Base64.getDecoder().decode("MzgzNDY1OTE="), Charsets.UTF_8) // 8-byte key for stream decryption
    private val DOMAIN_YT1 = String(java.util.Base64.getDecoder().decode("eW91dHViZS5jb20="), Charsets.UTF_8)
    private val DOMAIN_YT2 = String(java.util.Base64.getDecoder().decode("eW91dHUuYmU="), Charsets.UTF_8)
    private val VIDEO_WATCH_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly93d3cueW91dHViZS5jb20vd2F0Y2g/dj0="), Charsets.UTF_8)
    private val VIDEO_THUMB_URL = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly9pbWcueW91dHViZS5jb20vdmkv"), Charsets.UTF_8)

    // Lightweight in-memory query cache (caps memory footprint at ~100 KB)
    private val searchCache = ConcurrentHashMap<String, List<Song>>()
    private const val MAX_CACHE_ENTRIES = 40

    // Lightweight genre mapping for streams and categories
    private val songGenreMap = ConcurrentHashMap<String, String>()

    // Global continuous RDAMVM radio candidates pool & session played titles
    private val radioCandidatePool = ConcurrentLinkedQueue<OnlineVideoResolver.OnlineRadioTrack>()
    private val sessionPlayedTitles = ConcurrentHashMap.newKeySet<String>()

    // Direct audio streaming URL and resolved Song cache
    private val streamUrlCache = ConcurrentHashMap<String, String>()
    private val resolvedSongCache = ConcurrentHashMap<String, Song>()

    @Volatile
    var activeStationId: String? = null
        private set

    @Volatile
    private var genreSearchPage = 1

    fun resetActiveStation() {
        activeStationId = null
        genreSearchPage = 1
        radioCandidatePool.clear()
        sessionPlayedTitles.clear()
    }

    private val NOISE_REGEX = Regex(
        """\((?:official|lyric|lyrics|lyrical|audio|video|visuali[sz]er|full song|hd|4k)[^)]*\)""" +
            """|\(from[^)]*\)""" +
            """|\[[^]]*]""" +
            """|\b(?:official (?:video|audio|music video)|lyrical video|full video|4k video)\b""",
        RegexOption.IGNORE_CASE
    )
    private val TOPIC_REGEX = Regex("""\s*-\s*topic\b""", RegexOption.IGNORE_CASE)
    private val PUNCT_REGEX = Regex("""[^\p{L}\p{N}]+""")
    private val SPACES_REGEX = Regex("""\s+""")
    private val AUDIO_EXT_REGEX = Regex("(?i)(\\.(mp3|m4a|flac|wav|aac|ogg)|_\\d+$)")
    private val CLEAN_FROM_REGEX = Regex("""(?i)\(From\s+["'][^"']+["']\)""")
    private val CLEAN_REPRISE_REGEX = Regex("""(?i)\(Reprise\)""")
    private val CLEAN_OFFICIAL_REGEX = Regex("""(?i)\(Official.*?\)""")
    private val BRACKET_REGEX = Regex("""\(.*?\)|\[.*?\]""")
    private val GREEDY_BRACKET_REGEX = Regex("(?i)(\\(.*\\)|\\[.*\\])")
    private val FEAT_REGEX = Regex("(?i)(feat\\.|featuring|ft\\.|vs\\.|&|,|•).*")
    private val BEST_STREAM_REGEX = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)(?=[?#]|$)", RegexOption.IGNORE_CASE)
    private val YT_V_PARAM_REGEX = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
    private val YT_SHORT_REGEX = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""")
    private val YT_EMBED_REGEX = Regex("""embed/([a-zA-Z0-9_-]{11})""")

    fun cleanTitleForMatching(raw: String): String = raw
        .replace(AUDIO_EXT_REGEX, "")
        .replace(CLEAN_FROM_REGEX, "")
        .replace(CLEAN_REPRISE_REGEX, "")
        .replace(CLEAN_OFFICIAL_REGEX, "")
        .replace(BRACKET_REGEX, "")
        .replace(FEAT_REGEX, "")
        .replace(NOISE_REGEX, " ")
        .replace(PUNCT_REGEX, " ")
        .replace(SPACES_REGEX, " ")
        .trim()
        .lowercase(java.util.Locale.ROOT)

    fun normalisedTitle(raw: String): String = cleanTitleForMatching(raw)

    fun artistSet(raw: String): Set<String> = raw.lowercase(java.util.Locale.ROOT)
        .replace(TOPIC_REGEX, " ")
        .split(",", "&", "·", "•", ";", " feat.", " feat ", " ft.", " ft ", " x ", " with ", " and ")
        .map { it.replace(PUNCT_REGEX, " ").replace(SPACES_REGEX, " ").trim() }
        .filter { it.isNotEmpty() && it != "unknown" && it != "various artists" }
        .toSet()

    fun isAccurateMatch(
        expectedTitle: String,
        expectedArtist: String,
        candidateTitle: String,
        candidateArtist: String
    ): Boolean {
        val t1 = cleanTitleForMatching(expectedTitle)
        val t2 = cleanTitleForMatching(candidateTitle)
        if (t1.isEmpty() || t2.isEmpty()) return false

        // 1. Check title equivalence or strong prefix/containment
        val titlesMatch = (t1 == t2) ||
                (t1.length >= 6 && t2.length >= 6 && (t1.startsWith(t2) || t2.startsWith(t1)))

        if (!titlesMatch) {
            val words1 = t1.split(" ").filter { it.length > 2 }
            val words2 = t2.split(" ").filter { it.length > 2 }
            val commonWords = words1.count { it in words2 }
            val threshold = maxOf(2, (minOf(words1.size, words2.size) * 0.75).toInt())
            if (words1.size < 2 || words2.size < 2 || commonWords < threshold) {
                return false
            }
        }

        // 2. Check artist matching
        val a1 = artistSet(expectedArtist)
        val a2 = artistSet(candidateArtist)

        if (a1.isNotEmpty() && a2.isNotEmpty()) {
            val hasCommonArtist = a1.any { left ->
                a2.any { right ->
                    left == right || (left.length >= 4 && right.length >= 4 && (left.contains(right) || right.contains(left)))
                }
            }
            return hasCommonArtist
        }

        // If artist is unknown or missing on either side, only accept if title is sufficiently specific (>= 8 chars)
        return t1.length >= 8 && t1 == t2
    }

    fun isSameRecording(aTitle: String, aArtist: String, bTitle: String, bArtist: String): Boolean {
        return isAccurateMatch(aTitle, aArtist, bTitle, bArtist)
    }

    suspend fun resolvePlayableSong(song: Song): Song = withContext(Dispatchers.IO) {
        if (song.isDirectPlayable()) return@withContext song

        val cleanTitle = cleanTitleForMatching(song.title)
        val cleanArtist = song.artist
            .replace(FEAT_REGEX, "")
            .replace(BRACKET_REGEX, "")
            .trim()

        val cacheKey = "${cleanTitle}::${cleanArtist.lowercase()}"
        resolvedSongCache[cacheKey]?.let { cached ->
            return@withContext cached
        }
        streamUrlCache[cacheKey]?.let { cachedUrl ->
            return@withContext song.copy(path = cachedUrl)
        }

        val primaryArtist = artistSet(cleanArtist).firstOrNull() ?: cleanArtist
        val searchQueries = mutableListOf<String>()
        if (cleanArtist.isNotBlank() && cleanArtist != "<unknown>") {
            searchQueries.add("$cleanTitle $cleanArtist".trim())
            if (primaryArtist.isNotBlank() && !primaryArtist.equals(cleanArtist, ignoreCase = true)) {
                searchQueries.add("$cleanTitle $primaryArtist".trim())
            }
        }
        searchQueries.add(cleanTitle)

        for (query in searchQueries) {
            val matches = searchSongs(query, limit = 5)
            val best = matches.firstOrNull { candidate ->
                candidate.isDirectPlayable() && isAccurateMatch(song.title, song.artist, candidate.title, candidate.artist)
            }
            if (best != null) {
                streamUrlCache[cacheKey] = best.path
                val art = if (!best.artworkUrl.isNullOrBlank()) best.artworkUrl else song.artworkUrl
                if (!art.isNullOrBlank()) {
                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(best.path, art)
                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(song.path, art)
                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(song.id, art)
                }
                val resolvedSong = song.copy(
                    title = if (best.title.isNotBlank()) best.title else song.title,
                    artist = if (best.artist.isNotBlank()) best.artist else song.artist,
                    album = if (best.album.isNotBlank() && best.album != "<unknown>") best.album else song.album,
                    path = best.path,
                    duration = if (best.duration > 0) best.duration else song.duration,
                    artworkUrl = art
                )
                resolvedSongCache[cacheKey] = resolvedSong
                return@withContext resolvedSong
            }
        }

        // Strictly do not attach any mismatched stream URL if no authentic match was found
        song
    }

    fun registerSongGenre(idOrPath: String, genre: String) {
        if (idOrPath.isNotBlank() && genre.isNotBlank()) {
            songGenreMap[idOrPath] = genre
        }
    }

    fun getSongGenre(song: Song?): String? {
        if (song == null) return null
        return songGenreMap[song.id] ?: songGenreMap[song.path]
    }

    fun decryptMediaUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val secretKey = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decodedBytes = try {
                java.util.Base64.getDecoder().decode(encryptedUrl)
            } catch (_: Throwable) {
                android.util.Base64.decode(encryptedUrl, android.util.Base64.DEFAULT)
            }

            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            try {
                Log.e(TAG, "Media URL decryption failed: ${e.message}")
            } catch (_: Throwable) {}
            ""
        }
    }

    fun selectBestStream(decryptedUrl: String, supports320: Boolean): String {
        if (decryptedUrl.isBlank()) return ""
        val match = BEST_STREAM_REGEX.find(decryptedUrl) ?: return decryptedUrl
        val ext = match.groupValues[2]
        // Automotive optimization: Use 320kbps if supported, otherwise fallback to 160kbps for crystal audio
        return if (supports320) {
            decryptedUrl.replaceRange(match.range, "_320.$ext")
        } else {
            decryptedUrl.replaceRange(match.range, "_160.$ext")
        }
    }

    private fun unescapeHtml(text: String): String {
        if (text.isEmpty()) return ""
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
    }

    suspend fun searchSongs(query: String, limit: Int = 15): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList<Song>()

        // Direct Video Link Detection
        if (q.contains(DOMAIN_YT1, ignoreCase = true) || q.contains(DOMAIN_YT2, ignoreCase = true)) {
            val vParam = YT_V_PARAM_REGEX.find(q)?.groupValues?.get(1)
                ?: YT_SHORT_REGEX.find(q)?.groupValues?.get(1)
                ?: YT_EMBED_REGEX.find(q)?.groupValues?.get(1)
            if (vParam != null) {
                val ytSong = Song(
                    id = "online_video_$vParam",
                    title = "Online Video ($vParam)",
                    artist = "Online Stream",
                    album = "Video Single",
                    duration = 0L,
                    path = q,
                    artworkUrl = "$VIDEO_THUMB_URL$vParam/hqdefault.jpg"
                )
                return@withContext listOf(ytSong)
            }
        }

        val cacheKey = "${q.lowercase()}:$limit"
        searchCache[cacheKey]?.let { return@withContext it }

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val urlString = "$API_BASE?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encodedQuery&p=1&n=$limit"
            val url = URL(urlString)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Search request failed with HTTP ${connection.responseCode}")
                return@withContext emptyList<Song>()
            }

            val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val root = JSONObject(responseText)
            val resultsArray = root.optJSONArray("results") ?: return@withContext emptyList<Song>()

            val songs = ArrayList<Song>(resultsArray.length())
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val song = parseSongItem(item) ?: continue
                songs.add(song)
            }

            if (searchCache.size > MAX_CACHE_ENTRIES) {
                searchCache.clear()
            }
            searchCache[cacheKey] = songs
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Online search error: ${e.message}", e)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    fun parseSongItem(rawItem: JSONObject): Song? {
        val item = rawItem.optJSONObject("song") ?: rawItem
        val rawId = item.optString("id", "")
        val rawTitle = unescapeHtml(item.optString("title", "").ifBlank { item.optString("song", "") })
        if (rawId.isBlank() || rawTitle.isBlank()) return null

        val moreInfo = item.optJSONObject("more_info")
        val encUrl = moreInfo?.optString("encrypted_media_url", "")?.ifBlank { null }
            ?: item.optString("encrypted_media_url", "")
        if (encUrl.isBlank()) return null

        val decryptedUrl = decryptMediaUrl(encUrl)
        if (decryptedUrl.isBlank()) return null

        val supports320 = (moreInfo?.optString("320kbps") ?: item.optString("320kbps", "false")).equals("true", ignoreCase = true)
        val finalStreamUrl = selectBestStream(decryptedUrl, supports320)

        // Extract primary artist
        var artist = ""
        val artistMap = moreInfo?.optJSONObject("artistMap") ?: item.optJSONObject("artistMap")
        val primaryArtists = artistMap?.optJSONArray("primary_artists")
        if (primaryArtists != null && primaryArtists.length() > 0) {
            val firstArtist = primaryArtists.optJSONObject(0)
            artist = firstArtist?.optString("name", "") ?: ""
        }
        if (artist.isBlank()) {
            val rawArtist = moreInfo?.optString("music")
                ?: item.optString("primary_artists")
                ?: item.optString("singers")
                ?: item.optString("subtitle", "Online Artist")
            artist = unescapeHtml(rawArtist.ifBlank { "Online Artist" })
        }
        if (artist.isBlank()) artist = "Online Artist"

        val album = unescapeHtml(
            moreInfo?.optString("album")
                ?: item.optString("album", "Streaming Single")
                ?: "Streaming Single"
        )
        val durationSec = (moreInfo?.optString("duration") ?: item.optString("duration", "0")).toLongOrNull() ?: 0L
        val durationMs = durationSec * 1000L

        val rawImage = item.optString("image", "").ifBlank {
            moreInfo?.optString("image", "") ?: ""
        }
        val artworkUrl = if (rawImage.isNotBlank()) {
            rawImage.replace("150x150", "500x500").replace("50x50", "500x500")
        } else ""

        if (artworkUrl.isNotBlank()) {
            com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(finalStreamUrl, artworkUrl)
        }

        val rawLanguage = item.optString("language", "").ifBlank {
            moreInfo?.optString("language", "") ?: ""
        }
        val languageOrGenre = rawLanguage.trim().replaceFirstChar { it.uppercase() }
        if (languageOrGenre.isNotBlank()) {
            registerSongGenre("online_$rawId", languageOrGenre)
            registerSongGenre(finalStreamUrl, languageOrGenre)
        }

        return Song(
            id = "online_$rawId",
            title = rawTitle,
            artist = artist,
            album = album,
            duration = durationMs,
            path = finalStreamUrl,
            artworkUrl = artworkUrl
        )
    }

    suspend fun createRadioStation(pid: String): String? = withContext(Dispatchers.IO) {
        if (pid.isBlank()) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            val encodedEntity = URLEncoder.encode("[\"$pid\"]", "UTF-8")
            val urlString = "$API_BASE?__call=webradio.createEntityStation&_format=json&_marker=0&api_version=4&ctx=android&entity_id=$encodedEntity&entity_type=queue"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(responseText)
                val stationId = root.optString("stationid", "")
                if (stationId.isNotBlank()) {
                    activeStationId = stationId
                    return@withContext stationId
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "createRadioStation failed for pid $pid: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchStationSongs(
        stationId: String,
        limit: Int = 15,
        targetGenre: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        if (stationId.isBlank()) return@withContext emptyList()
        var connection: HttpURLConnection? = null
        try {
            val urlString = "$API_BASE?__call=webradio.getSong&_format=json&_marker=0&api_version=4&ctx=android&stationid=$stationId&k=$limit&next=1"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(responseText)
                val songs = ArrayList<Song>(limit)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k == "stationid" || k == "error") continue
                    val obj = root.optJSONObject(k) ?: continue
                    val song = parseSongItem(obj) ?: continue
                    if (targetGenre != null && targetGenre.isNotBlank()) {
                        registerSongGenre(song.id, targetGenre)
                        registerSongGenre(song.path, targetGenre)
                    }
                    songs.add(song)
                }
                return@withContext songs
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "fetchStationSongs failed for station $stationId: ${e.message}")
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchNextRadioBatch(targetGenre: String? = null, limit: Int = 15): List<Song> = withContext(Dispatchers.IO) {
        // If we have RDAMVM candidates in the pool, drain them first
        if (radioCandidatePool.isNotEmpty()) {
            val batch = ArrayList<Song>(limit)
            val genre = targetGenre?.trim()?.takeIf { it.isNotBlank() }
            val perArtist = mutableMapOf<String, Int>()

            repeat(limit * 4) { // over-sample to skip duplicates
                if (batch.size >= limit) return@repeat
                val track = radioCandidatePool.poll() ?: return@repeat
                val normTitle = normalisedTitle(track.title)
                if (normTitle.isBlank() || sessionPlayedTitles.contains(normTitle)) return@repeat

                val artists = artistSet(track.artist)
                val key = artists.minOrNull()
                if (key != null) {
                    val played = perArtist[key] ?: 0
                    if (played >= 2) return@repeat
                }

                val artUrl = track.thumbnailUrl ?: ""
                val unplayableSong = Song(
                    id = "online_yt_${track.videoId}",
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    path = "$VIDEO_WATCH_URL${track.videoId}",
                    duration = track.durationMs,
                    artworkUrl = artUrl
                )
                val playableSong = resolvePlayableSong(unplayableSong)
                if (playableSong.isDirectPlayable()) {
                    sessionPlayedTitles.add(normTitle)
                    if (key != null) perArtist[key] = (perArtist[key] ?: 0) + 1
                    val finalArt = playableSong.artworkUrl?.takeIf { it.isNotBlank() } ?: artUrl
                    if (!finalArt.isNullOrBlank()) {
                        com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(playableSong.path, finalArt)
                        com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(unplayableSong.path, finalArt)
                        com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(unplayableSong.id, finalArt)
                    }
                    if (genre != null) {
                        registerSongGenre(playableSong.id, genre)
                        registerSongGenre(playableSong.path, genre)
                    }
                    batch.add(playableSong)
                }
            }
            if (batch.isNotEmpty()) return@withContext batch
        }
        // Fallback: Online station refill
        val stId = activeStationId ?: return@withContext emptyList()
        fetchStationSongs(stId, limit = limit, targetGenre = targetGenre)
    }

    /**
     * Algorithmic Radio Queue Generator (RDAMVM Radio Queue + WebRadio fallback).
     * Given an active seed track, queries the RDAMVM radio endpoint to generate
     * an authentic queue of related tracks, then falls back to WebRadio if needed.
     */
    suspend fun getRadioTracksForSong(
        seedSong: Song,
        targetGenre: String? = null,
        limit: Int = 15
    ): List<Song> = withContext(Dispatchers.IO) {
        val filtered = ArrayList<Song>(limit)
        val seedNormTitle = normalisedTitle(seedSong.title)
        val seedArtists = artistSet(seedSong.artist)
        val perArtist = mutableMapOf<String, Int>()
        val seenIds = HashSet<String>().apply { add(seedSong.id) }
        val seenTitles = HashSet<String>().apply { add(seedNormTitle) }

        sessionPlayedTitles.add(seedNormTitle)

        // Resolve target genre (e.g. "Tamil", "Hindi", "Rock", "Lo-Fi", etc.)
        val genre = targetGenre?.trim()?.takeIf { it.isNotBlank() }
            ?: getSongGenre(seedSong)
            ?: run {
                val artistPrefix = seedSong.artist.substringBefore("•").trim()
                if (seedSong.artist.contains("•") && artistPrefix.isNotBlank()) artistPrefix else null
            }

        // Step 1: Resolve provider PID for authentic WebRadio
        var pid = when {
            seedSong.id.startsWith("online_") && !seedSong.id.startsWith("online_yt_") -> seedSong.id.removePrefix("online_")
            else -> ""
        }

        if (pid.isBlank()) {
            val cleanTitle = cleanTitleForMatching(seedSong.title)
            val cleanArtist = seedSong.artist
                .replace(FEAT_REGEX, "")
                .replace(BRACKET_REGEX, "")
                .trim()
            val searchQuery = "$cleanTitle $cleanArtist".trim()
            if (searchQuery.isNotBlank()) {
                val matches = searchSongs(searchQuery, limit = 5)
                val bestMatch = matches.firstOrNull { candidate ->
                    candidate.isDirectPlayable() && isAccurateMatch(seedSong.title, seedSong.artist, candidate.title, candidate.artist)
                }
                if (bestMatch != null) {
                    pid = bestMatch.id.removePrefix("online_")
                }
            }
        }

        // Step 2: Native WebRadio Engine (Primary — 100% matched direct stream, title, artist, art)
        if (pid.isNotBlank()) {
            try {
                val stationId = createRadioStation(pid)
                if (stationId != null) {
                    val stationSongs = fetchStationSongs(stationId, limit = limit + 4, targetGenre = genre)
                    for (parsed in stationSongs) {
                        val normTitle = normalisedTitle(parsed.title)
                        if (!seenIds.contains(parsed.id) && !seenTitles.contains(normTitle) && !sessionPlayedTitles.contains(normTitle)) {
                            seenIds.add(parsed.id)
                            seenTitles.add(normTitle)
                            sessionPlayedTitles.add(normTitle)
                            if (genre != null) {
                                registerSongGenre(parsed.id, genre)
                                registerSongGenre(parsed.path, genre)
                            }
                            filtered.add(parsed)
                            if (filtered.size >= limit) break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native radio station failed: ${e.message}")
            }
        }

        // Step 3: Algorithmic Recommendations Fallback (reco.getreco)
        if (filtered.size < limit && pid.isNotBlank()) {
            var connection: HttpURLConnection? = null
            try {
                val urlString = "$API_BASE?__call=reco.getreco&api_version=4&_format=json&_marker=0&ctx=android&pid=$pid"
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val root = JSONObject(responseText)
                    val recoArray = root.optJSONArray(pid) ?: run {
                        val keys = root.keys()
                        var arr: org.json.JSONArray? = null
                        while (keys.hasNext()) {
                            val k = keys.next()
                            arr = root.optJSONArray(k)
                            if (arr != null && arr.length() > 0) break
                        }
                        arr
                    }

                    if (recoArray != null) {
                        for (i in 0 until recoArray.length()) {
                            val item = recoArray.optJSONObject(i) ?: continue
                            val parsed = parseSongItem(item) ?: continue
                            val normTitle = normalisedTitle(parsed.title)
                            if (!seenIds.contains(parsed.id) && !seenTitles.contains(normTitle) && !sessionPlayedTitles.contains(normTitle)) {
                                seenIds.add(parsed.id)
                                seenTitles.add(normTitle)
                                sessionPlayedTitles.add(normTitle)
                                if (genre != null) {
                                    registerSongGenre(parsed.id, genre)
                                    registerSongGenre(parsed.path, genre)
                                }
                                filtered.add(parsed)
                                if (filtered.size >= limit) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "reco.getreco query failed: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }

        // Step 4: RDAMVM Radio (for video tracks or when provider returned insufficient tracks)
        if (filtered.size < limit) {
            val ytVideoId: String? = when {
                seedSong.id.startsWith("online_yt_") -> seedSong.id.removePrefix("online_yt_")
                seedSong.path.contains(VIDEO_WATCH_URL) || seedSong.path.contains(DOMAIN_YT1) ->
                    seedSong.path.substringAfter("v=").substringBefore("&").takeIf { it.length == 11 }
                else -> null
            } ?: try {
                OnlineVideoResolver.resolveVideoId(seedSong.title, seedSong.artist)
            } catch (_: Exception) { null }

            if (!ytVideoId.isNullOrBlank()) {
                try {
                    val radioTracks = OnlineVideoResolver.fetchRadioQueue(ytVideoId)
                    radioCandidatePool.clear()

                    for (track in radioTracks) {
                        val normTitle = normalisedTitle(track.title)
                        if (normTitle.isBlank()) continue
                        if (isSameRecording(seedSong.title, seedSong.artist, track.title, track.artist)) continue
                        if (sessionPlayedTitles.contains(normTitle) || seenTitles.contains(normTitle)) continue

                        val artists = artistSet(track.artist)
                        val key = artists.minOrNull()
                        if (key != null) {
                            val cap = if (artists.any { it in seedArtists }) 4 else 2
                            val played = perArtist[key] ?: 0
                            if (played >= cap) continue
                        }

                        if (filtered.size < limit) {
                            val artUrl = track.thumbnailUrl ?: ""
                            val unplayableSong = Song(
                                id = "online_yt_${track.videoId}",
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                path = "$VIDEO_WATCH_URL${track.videoId}",
                                duration = track.durationMs,
                                artworkUrl = artUrl
                            )
                            val playableSong = resolvePlayableSong(unplayableSong)
                            if (playableSong.isDirectPlayable()) {
                                filtered.add(playableSong)
                                seenTitles.add(normTitle)
                                sessionPlayedTitles.add(normTitle)
                                if (key != null) perArtist[key] = (perArtist[key] ?: 0) + 1
                                val finalArt = playableSong.artworkUrl?.takeIf { it.isNotBlank() } ?: artUrl
                                if (!finalArt.isNullOrBlank()) {
                                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(playableSong.path, finalArt)
                                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(unplayableSong.path, finalArt)
                                    com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(unplayableSong.id, finalArt)
                                }
                                if (genre != null) {
                                    registerSongGenre(playableSong.id, genre)
                                    registerSongGenre(playableSong.path, genre)
                                }
                            } else {
                                radioCandidatePool.offer(track)
                            }
                        } else {
                            radioCandidatePool.offer(track)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RDAMVM radio fetch failed for $ytVideoId: ${e.message}")
                }
            }
        }

        // Step 4: Genre-Targeted Complementary Search with dynamic pagination (never repeats)
        if (filtered.size < limit) {
            val cleanArtist = seedSong.artist
                .replace(FEAT_REGEX, "")
                .trim()
            val isGenericArtist = cleanArtist.isBlank() || cleanArtist.contains("unknown", ignoreCase = true) || cleanArtist.contains("online", ignoreCase = true)

            val queries = mutableListOf<String>()
            if (genre != null && genre.isNotBlank()) {
                if (!isGenericArtist) {
                    queries.add("$cleanArtist $genre Hits")
                }
                queries.add("$genre Trending Hits")
                queries.add("$genre Hits")
                queries.add("$genre Top Songs")
            } else if (!isGenericArtist) {
                queries.add("$cleanArtist Hits")
            } else {
                val cleanTitle = seedSong.title.replace(GREEDY_BRACKET_REGEX, "").replace('_', ' ').trim()
                if (cleanTitle.isNotBlank()) {
                    queries.add("$cleanTitle Radio")
                }
            }

            val page = genreSearchPage++
            for (q in queries) {
                if (filtered.size >= limit) break
                val needed = limit - filtered.size
                var conn: HttpURLConnection? = null
                try {
                    val encodedQuery = URLEncoder.encode(q, "UTF-8")
                    val urlString = "$API_BASE?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encodedQuery&p=$page&n=${needed + 4}"
                    val url = URL(urlString)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 4000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        val root = JSONObject(responseText)
                        val resultsArray = root.optJSONArray("results")
                        if (resultsArray != null) {
                            for (i in 0 until resultsArray.length()) {
                                val item = resultsArray.optJSONObject(i) ?: continue
                                val parsed = parseSongItem(item) ?: continue
                                val normTitle = parsed.title.lowercase().trim()
                                if (!seenIds.contains(parsed.id) && !seenTitles.contains(normTitle)) {
                                    seenIds.add(parsed.id)
                                    seenTitles.add(normTitle)
                                    if (genre != null) {
                                        registerSongGenre(parsed.id, genre)
                                        registerSongGenre(parsed.path, genre)
                                    }
                                    filtered.add(parsed)
                                    if (filtered.size >= limit) break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Search pagination fallback error: ${e.message}")
                } finally {
                    conn?.disconnect()
                }
            }
        }

        filtered
    }

    suspend fun searchPlaylists(query: String, limit: Int = 8): List<ExploreShelfItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val urlString = "$API_BASE?__call=search.getPlaylistResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encodedQuery&p=1&n=$limit"
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(text)
                val arr = root.optJSONArray("results") ?: return@withContext emptyList()
                val list = ArrayList<ExploreShelfItem>(arr.length())
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val title = unescapeHtml(item.optString("title", ""))
                    if (id.isBlank() || title.isBlank()) continue
                    val subtitle = unescapeHtml(item.optString("subtitle", "Playlist"))
                    val image = item.optString("image", "").replace("150x150", "500x500")
                    list.add(ExploreShelfItem(id = id, title = title, subtitle = subtitle, artworkUrl = image, type = "playlist"))
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "searchPlaylists failed: ${e.message}")
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun searchAlbums(query: String, limit: Int = 8): List<ExploreShelfItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val urlString = "$API_BASE?__call=search.getAlbumResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encodedQuery&p=1&n=$limit"
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(text)
                val arr = root.optJSONArray("results") ?: return@withContext emptyList()
                val list = ArrayList<ExploreShelfItem>(arr.length())
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val title = unescapeHtml(item.optString("title", ""))
                    if (id.isBlank() || title.isBlank()) continue
                    val subtitle = unescapeHtml(item.optString("subtitle", "Album"))
                    val image = item.optString("image", "").replace("150x150", "500x500")
                    list.add(ExploreShelfItem(id = id, title = title, subtitle = subtitle, artworkUrl = image, type = "album"))
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "searchAlbums failed: ${e.message}")
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchPlaylistSongs(listId: String): List<Song> = withContext(Dispatchers.IO) {
        if (listId.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val urlString = "$API_BASE?__call=playlist.getDetails&_format=json&_marker=0&api_version=4&ctx=android&listid=$listId"
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(text)
                val arr = root.optJSONArray("list") ?: root.optJSONArray("songs") ?: return@withContext emptyList()
                val songs = ArrayList<Song>(arr.length())
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val song = parseSongItem(item) ?: continue
                    songs.add(song)
                }
                return@withContext songs
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "fetchPlaylistSongs failed: ${e.message}")
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchAlbumSongs(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val urlString = "$API_BASE?__call=content.getAlbumDetails&_format=json&_marker=0&api_version=4&ctx=android&albumid=$albumId"
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(text)
                val arr = root.optJSONArray("list") ?: root.optJSONArray("songs") ?: return@withContext emptyList()
                val songs = ArrayList<Song>(arr.length())
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val song = parseSongItem(item) ?: continue
                    songs.add(song)
                }
                return@withContext songs
            }
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "fetchAlbumSongs failed: ${e.message}")
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchCategoryDetails(genre: String, ytParams: String = ""): ExploreCategoryDetails = withContext(Dispatchers.IO) {
        // Attempt online category shelves first (returns real Songs, Featured, Community playlists, Albums)
        if (ytParams.isNotBlank()) {
            try {
                val shelves = OnlineVideoResolver.fetchCategoryShelves(ytParams)
                if (shelves != null) {
                    // Convert OnlineRadioTrack → Song
                    val ytSongs = ArrayList<Song>(shelves.songs.size)
                    for (track in shelves.songs) {
                        val artUrl = track.thumbnailUrl ?: ""
                        val song = Song(
                            id = "online_yt_${track.videoId}",
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            path = "$VIDEO_WATCH_URL${track.videoId}",
                            duration = track.durationMs,
                            artworkUrl = artUrl
                        )
                        ytSongs.add(song)
                        if (artUrl.isNotBlank()) {
                            com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(song.path, artUrl)
                        }
                    }
                    // Convert OnlineCategoryItem → ExploreShelfItem
                    fun convertItems(items: List<OnlineVideoResolver.OnlineCategoryItem>): List<ExploreShelfItem> =
                        items.map {
                            ExploreShelfItem(
                                id = it.id,
                                title = it.title,
                                subtitle = it.subtitle,
                                artworkUrl = it.artworkUrl,
                                type = if (it.id.startsWith("MPREb") || it.id.startsWith("OLAK")) "album" else "playlist",
                                thumbnailUrls = it.thumbnailUrls
                            )
                        }

                    return@withContext ExploreCategoryDetails(
                        genre = genre,
                        songs = ytSongs,
                        featuredPlaylists = convertItems(shelves.featuredPlaylists),
                        communityPlaylists = convertItems(shelves.communityPlaylists),
                        albums = convertItems(shelves.albums)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchCategoryDetails YT fallback for genre=$genre: ${e.message}")
            }
        }
        // Fallback: Provider search-based category details
        val songs = searchSongs("$genre Top Songs", limit = 10)
        val featuredPlaylists = searchPlaylists(genre, limit = 8)
        val communityPlaylists = searchPlaylists("$genre Hits", limit = 8).ifEmpty { searchPlaylists("$genre Viral", limit = 8) }
        val albums = searchAlbums(genre, limit = 8)
        ExploreCategoryDetails(
            genre = genre,
            songs = songs,
            featuredPlaylists = featuredPlaylists,
            communityPlaylists = communityPlaylists,
            albums = albums
        )
    }
}

data class ExploreShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val type: String,
    val thumbnailUrls: List<String> = emptyList()
)

data class ExploreCategoryDetails(
    val genre: String,
    val songs: List<Song>,
    val featuredPlaylists: List<ExploreShelfItem>,
    val communityPlaylists: List<ExploreShelfItem>,
    val albums: List<ExploreShelfItem>
)
