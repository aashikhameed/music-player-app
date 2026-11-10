package com.aashik.music.desktop.service

import com.aashik.music.desktop.model.DesktopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object DesktopOnlineRepository {
    private const val API_BASE = "https://www.jiosaavn.com/api.php"
    private const val DES_KEY = "38346591"
    private val BEST_STREAM_REGEX = Regex("""_(\d{2,3})\.(mp4|m4a|mp3)""")

    private val searchCache = ConcurrentHashMap<String, List<DesktopSong>>()

    fun decryptMediaUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val secretKey = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.getDecoder().decode(encryptedUrl)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun selectBestStream(decryptedUrl: String, supports320: Boolean): String {
        if (decryptedUrl.isBlank()) return ""
        val match = BEST_STREAM_REGEX.find(decryptedUrl) ?: return decryptedUrl
        val ext = match.groupValues[2]
        return if (supports320) {
            decryptedUrl.replaceRange(match.range, "_320.$ext")
        } else {
            decryptedUrl.replaceRange(match.range, "_160.$ext")
        }
    }

    private fun unescapeHtml(text: String): String = text
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")

    suspend fun searchSongs(query: String, limit: Int = 20): List<DesktopSong> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val cacheKey = "${q.lowercase()}:$limit"
        searchCache[cacheKey]?.let { return@withContext it }

        var connection: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = URL("$API_BASE?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encoded&p=1&n=$limit")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()

            val text = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val root = JSONObject(text)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()

            val songs = ArrayList<DesktopSong>(results.length())
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val song = parseItem(item) ?: continue
                songs.add(song)
            }

            searchCache[cacheKey] = songs
            songs
        } catch (e: Exception) {
            println("[DesktopOnline] Search error: ${e.message}")
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseItem(item: JSONObject): DesktopSong? {
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
        } else null

        return DesktopSong(
            id = "online_$rawId",
            title = rawTitle,
            artist = artist.ifBlank { "Online Artist" },
            album = album,
            duration = durationMs,
            path = finalStreamUrl,
            artworkUrl = artworkUrl
        )
    }

    suspend fun fetchRadioQueue(seedSong: DesktopSong): List<DesktopSong> = withContext(Dispatchers.IO) {
        val seedQuery = "${seedSong.artist} ${seedSong.title}"
        searchSongs(seedQuery, limit = 15).filter { it.id != seedSong.id }
    }
}

