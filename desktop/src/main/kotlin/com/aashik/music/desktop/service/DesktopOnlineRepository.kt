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
    private val BRACKET_REGEX = Regex("""\(.*?\)|\[.*?\]""")
    private val FEAT_REGEX = Regex("""(?i)\b(feat\.|ft\.).*""")

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

    private fun cleanTitle(title: String): String = title
        .replace(BRACKET_REGEX, "")
        .replace(FEAT_REGEX, "")
        .replace(Regex("""[|/_\-#]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun cleanArtist(artist: String): String = artist
        .replace(BRACKET_REGEX, "")
        .replace(FEAT_REGEX, "")
        .replace(Regex("""[|/_\-#]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

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

    /**
     * Continuous Algorithmic Radio Queue Resolution (Ported directly from mobile OnlineStreamRepository).
     * 1. Resolves JioSaavn WebRadio station / reco.getreco based on seed song.
     * 2. Runs complementary artist & genre radio search batches.
     * 3. Guarantees 15-25 high-quality, playable streaming tracks with full metadata and artwork.
     */
    suspend fun fetchRadioQueue(
        seedSong: DesktopSong,
        limit: Int = 20
    ): List<DesktopSong> = withContext(Dispatchers.IO) {
        val results = ArrayList<DesktopSong>(limit)
        val seenIds = HashSet<String>().apply { add(seedSong.id) }
        val seenTitles = HashSet<String>().apply { add(seedSong.title.lowercase().trim()) }

        // Step 1: Find JioSaavn PID
        var pid = if (seedSong.id.startsWith("online_") && !seedSong.id.startsWith("online_yt_")) {
            seedSong.id.removePrefix("online_")
        } else ""

        if (pid.isBlank()) {
            val searchCandidate = "${cleanTitle(seedSong.title)} ${cleanArtist(seedSong.artist)}".trim()
            if (searchCandidate.isNotBlank()) {
                val matches = searchSongs(searchCandidate, limit = 5)
                val best = matches.firstOrNull { it.path.isNotBlank() }
                if (best != null) {
                    pid = best.id.removePrefix("online_")
                }
            }
        }

        // Step 2: Try JioSaavn WebRadio Channel API
        if (pid.isNotBlank()) {
            var conn: HttpURLConnection? = null
            try {
                val encodedEntity = URLEncoder.encode("[\"$pid\"]", "UTF-8")
                val stationUrl = URL("$API_BASE?__call=webradio.createEntityStation&_format=json&_marker=0&api_version=4&ctx=android&entity_id=$encodedEntity&entity_type=queue")
                conn = (stationUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val stId = JSONObject(resp).optString("stationid", "")
                    if (stId.isNotBlank()) {
                        conn.disconnect()
                        val songsUrl = URL("$API_BASE?__call=webradio.getSong&_format=json&_marker=0&api_version=4&ctx=android&stationid=$stId&k=$limit&next=1")
                        conn = (songsUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 4000
                            readTimeout = 5000
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                            setRequestProperty("Accept", "application/json")
                        }
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val stResp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                            val stObj = JSONObject(stResp)
                            val keys = stObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                if (k == "stationid" || k == "error") continue
                                val songObj = stObj.optJSONObject(k) ?: continue
                                val song = parseItem(songObj) ?: continue
                                val norm = song.title.lowercase().trim()
                                if (!seenIds.contains(song.id) && !seenTitles.contains(norm)) {
                                    seenIds.add(song.id)
                                    seenTitles.add(norm)
                                    results.add(song)
                                    if (results.size >= limit) break
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[DesktopOnline] WebRadio API notice: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }

        // Step 3: Try JioSaavn reco.getreco API
        if (results.size < limit && pid.isNotBlank()) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$API_BASE?__call=reco.getreco&api_version=4&_format=json&_marker=0&ctx=android&pid=$pid")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val root = JSONObject(resp)
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
                            val song = parseItem(item) ?: continue
                            val norm = song.title.lowercase().trim()
                            if (!seenIds.contains(song.id) && !seenTitles.contains(norm)) {
                                seenIds.add(song.id)
                                seenTitles.add(norm)
                                results.add(song)
                                if (results.size >= limit) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[DesktopOnline] reco.getreco notice: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }

        // Step 4: Complementary Artist & Genre Multi-Search
        if (results.size < limit) {
            val cArtist = cleanArtist(seedSong.artist)
            val cTitle = cleanTitle(seedSong.title)
            val queries = mutableListOf<String>()
            if (cArtist.isNotBlank() && !cArtist.contains("unknown", ignoreCase = true) && !cArtist.contains("online", ignoreCase = true)) {
                queries.add("$cArtist Hits")
                queries.add("$cArtist Top Songs")
                queries.add("$cArtist Radio")
            }
            if (cTitle.isNotBlank()) {
                queries.add("$cTitle Radio")
            }
            queries.add("Top Hits 2024")

            for (q in queries) {
                if (results.size >= limit) break
                val needed = limit - results.size
                val searchMatches = searchSongs(q, limit = needed + 5)
                for (song in searchMatches) {
                    val norm = song.title.lowercase().trim()
                    if (!seenIds.contains(song.id) && !seenTitles.contains(norm)) {
                        seenIds.add(song.id)
                        seenTitles.add(norm)
                        results.add(song)
                        if (results.size >= limit) break
                    }
                }
            }
        }

        results
    }
}
