package com.aashik.music.service

import android.os.Build
import android.util.Log
import com.aashik.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
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
    private const val API_BASE = "https://www.jiosaavn.com/api.php"
    private const val DES_KEY = "38346591" // Standard 8-byte DES key for stream decryption

    // Lightweight in-memory query cache (caps memory footprint at ~100 KB)
    private val searchCache = ConcurrentHashMap<String, List<Song>>()
    private const val MAX_CACHE_ENTRIES = 40

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
        val regex = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)(?=[?#]|$)", RegexOption.IGNORE_CASE)
        val match = regex.find(decryptedUrl) ?: return decryptedUrl
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
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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
                val rawId = item.optString("id", "")
                val rawTitle = unescapeHtml(item.optString("title", ""))
                if (rawId.isBlank() || rawTitle.isBlank()) continue

                val moreInfo = item.optJSONObject("more_info")
                val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                if (encUrl.isBlank()) continue

                val decryptedUrl = decryptMediaUrl(encUrl)
                if (decryptedUrl.isBlank()) continue

                val supports320 = moreInfo?.optString("320kbps", "false").equals("true", ignoreCase = true)
                val finalStreamUrl = selectBestStream(decryptedUrl, supports320)

                // Extract primary artist
                var artist = ""
                val artistMap = moreInfo?.optJSONObject("artistMap")
                val primaryArtists = artistMap?.optJSONArray("primary_artists")
                if (primaryArtists != null && primaryArtists.length() > 0) {
                    val firstArtist = primaryArtists.optJSONObject(0)
                    artist = firstArtist?.optString("name", "") ?: ""
                }
                if (artist.isBlank()) {
                    val rawArtist = moreInfo?.optString("music") ?: item.optString("subtitle", "Online Artist")
                    artist = unescapeHtml(rawArtist.ifBlank { "Online Artist" })
                }
                if (artist.isBlank()) artist = "Online Artist"

                val album = unescapeHtml(moreInfo?.optString("album", "Streaming Single") ?: "Streaming Single")
                val durationSec = moreInfo?.optString("duration", "0")?.toLongOrNull() ?: 0L
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

                val song = Song(
                    id = "online_$rawId",
                    title = rawTitle,
                    artist = artist,
                    album = album,
                    duration = durationMs,
                    path = finalStreamUrl,
                    artworkUrl = artworkUrl
                )
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

    /**
     * Algorithmic Radio Queue Generator (inspired by BitChord RDAMVM queue builder).
     * Given an active seed track (streaming or local), queries related songs by artist,
     * sub-genres, or complementary tracks to synthesize an uninterrupted radio station.
     */
    suspend fun getRadioTracksForSong(seedSong: Song, limit: Int = 12): List<Song> = withContext(Dispatchers.IO) {
        val cleanArtist = seedSong.artist
            .replace(Regex("(?i)(feat\\.|featuring|ft\\.|vs\\.|&|,).*"), "")
            .trim()

        val candidateQuery = if (cleanArtist.isNotBlank() && !cleanArtist.equals("Unknown Artist", ignoreCase = true)) {
            cleanArtist
        } else {
            seedSong.title.replace(Regex("(?i)(\\(.*\\)|\\[.*\\])"), "").trim()
        }

        if (candidateQuery.isBlank()) return@withContext emptyList<Song>()

        val rawResults = searchSongs(candidateQuery, limit = limit + 4)
        // Filter out the seed song itself and duplicate titles
        val filtered = ArrayList<Song>(limit)
        val seenTitles = HashSet<String>()
        seenTitles.add(seedSong.title.lowercase().trim())

        for (track in rawResults) {
            val normTitle = track.title.lowercase().trim()
            if (track.id != seedSong.id && !seenTitles.contains(normTitle)) {
                seenTitles.add(normTitle)
                filtered.add(track)
                if (filtered.size >= limit) break
            }
        }

        // If artist search had too few items, attempt fallback query on seed title
        if (filtered.size < 4 && seedSong.title.isNotBlank()) {
            val titleResults = searchSongs(seedSong.title, limit = 8)
            for (track in titleResults) {
                val normTitle = track.title.lowercase().trim()
                if (track.id != seedSong.id && !seenTitles.contains(normTitle)) {
                    seenTitles.add(normTitle)
                    filtered.add(track)
                    if (filtered.size >= limit) break
                }
            }
        }

        filtered
    }
}
