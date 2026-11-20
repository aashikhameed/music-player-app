package com.aashik.music.wear.online

import com.aashik.music.wear.model.WearSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object WearOnlineRepository {

    private val API_BASE = String(java.util.Base64.getDecoder().decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tL2FwaS5waHA="), Charsets.UTF_8)
    private val DES_KEY = String(java.util.Base64.getDecoder().decode("MzgzNDY1OTE="), Charsets.UTF_8) // 8-byte key for stream decryption
    private val BEST_STREAM_REGEX = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)(?=[?#]|$)", RegexOption.IGNORE_CASE)

    val CURATED_STATIONS = listOf(
        "Top Hits",
        "Workout Energy",
        "EDM Beats",
        "Acoustic Chill",
        "Rock Classics",
        "Lo-Fi Study",
        "Night Drive"
    )

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
        } catch (_: Exception) {
            ""
        }
    }

    private fun selectBestStream(decryptedUrl: String): String {
        if (decryptedUrl.isBlank()) return ""
        val match = BEST_STREAM_REGEX.find(decryptedUrl) ?: return decryptedUrl
        val ext = match.groupValues[2]
        // On smartwatches, 160kbps offers an optimal balance between crystal sound and Wi-Fi battery savings
        return decryptedUrl.replaceRange(match.range, "_160.$ext")
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

    suspend fun search(query: String, limit: Int = 12): List<WearSong> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList<WearSong>()

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val endpoint = "$API_BASE?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&q=$encodedQuery&p=1&n=$limit"
            val url = URL(endpoint)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (WearOS Standalone Music Player)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext emptyList<WearSong>()
            }

            val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val root = JSONObject(responseText)
            val resultsArray = root.optJSONArray("results") ?: return@withContext emptyList<WearSong>()

            val songs = ArrayList<WearSong>(resultsArray.length())
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val song = parseSongItem(item) ?: continue
                songs.add(song)
            }
            songs
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseSongItem(item: JSONObject): WearSong? {
        val rawId = item.optString("id", "")
        val rawTitle = unescapeHtml(item.optString("title", "").ifBlank { item.optString("song", "") })
        if (rawId.isBlank() || rawTitle.isBlank()) return null

        val moreInfo = item.optJSONObject("more_info")
        val encUrl = moreInfo?.optString("encrypted_media_url", "")?.ifBlank { null }
            ?: item.optString("encrypted_media_url", "")
        if (encUrl.isBlank()) return null

        val decryptedUrl = decryptMediaUrl(encUrl)
        if (decryptedUrl.isBlank()) return null

        val finalStreamUrl = selectBestStream(decryptedUrl)

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

        val durationSec = moreInfo?.optString("duration")?.toLongOrNull() ?: item.optString("duration").toLongOrNull() ?: 180L
        val durationMs = durationSec * 1000L

        val album = unescapeHtml(
            moreInfo?.optString("album")
                ?: item.optString("album", "")
        )

        return WearSong(
            id = "stream_$rawId",
            title = rawTitle,
            artist = artist,
            durationMs = durationMs,
            path = finalStreamUrl,
            album = album
        )
    }
}
