package com.aashik.music.wear.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.aashik.music.wear.model.WearSong
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.theme.WearBackgroundBlack
import com.aashik.music.wear.theme.WearOnSurfaceDim
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSecondaryAmber
import com.aashik.music.wear.theme.WearSurfaceCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val SquareChipShape = RoundedCornerShape(12.dp)

private val POPULAR_SEARCHES = listOf(
    "Top Hits", "Workout", "Rock", "Acoustic", "EDM", "Lo-Fi Beats"
)

@Composable
fun WearSearchScreen(
    onSongSelected: (WearSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = isScreenRound()
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<WearSong>>(emptyList()) }
    var lastQuery by remember { mutableStateOf("") }
    val chipShape = remember(isRound) {
        if (isRound) CircleShape else SquareChipShape
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WearBackgroundBlack)
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Online Music & Streams",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            indicatorColor = WearPrimaryCyan,
                            trackColor = WearSurfaceCard,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Results for \"$lastQuery\":",
                        fontSize = 11.sp,
                        color = WearSecondaryAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                items(searchResults) { song ->
                    Chip(
                        onClick = { onSongSelected(song) },
                        label = {
                            Text(
                                text = song.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "${song.artist} • STREAM",
                                fontSize = 10.sp,
                                color = WearPrimaryCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Text(
                        text = "Quick Curated Streams",
                        fontSize = 11.sp,
                        color = WearOnSurfaceDim,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                items(POPULAR_SEARCHES) { genre ->
                    Chip(
                        onClick = {
                            lastQuery = genre
                            isSearching = true
                            coroutineScope.launch {
                                val results = performOnlineSearch(genre)
                                searchResults = results
                                isSearching = false
                            }
                        },
                        label = {
                            Text(
                                text = genre,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private suspend fun performOnlineSearch(query: String): List<WearSong> = withContext(Dispatchers.IO) {
    val results = mutableListOf<WearSong>()
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoint = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=8"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "Mozilla/5.0 (WearOS Music Player)")
        }

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val jsonText = reader.use { it.readText() }
            val root = JSONObject(jsonText)
            val items = root.optJSONArray("results")

            if (items != null) {
                for (i in 0 until items.length()) {
                    val obj = items.getJSONObject(i)
                    val title = obj.optString("trackName", "Unknown Track")
                    val artist = obj.optString("artistName", "Unknown Artist")
                    val previewUrl = obj.optString("previewUrl", "")
                    val durationMs = obj.optLong("trackTimeMillis", 30000L)
                    val id = "online_${obj.optLong("trackId", i.toLong())}"

                    if (previewUrl.isNotEmpty()) {
                        results.add(
                            WearSong(
                                id = id,
                                title = title,
                                artist = artist,
                                durationMs = durationMs,
                                path = previewUrl
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    results
}
