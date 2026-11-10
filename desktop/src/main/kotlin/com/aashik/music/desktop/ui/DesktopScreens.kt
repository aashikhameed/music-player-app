package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopRadioCategory
import com.aashik.music.desktop.model.DesktopRadioData
import com.aashik.music.desktop.model.DesktopRadioStation
import com.aashik.music.desktop.service.DesktopOnlineRepository
import kotlinx.coroutines.launch
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.ui.components.DesktopActionBar
import com.aashik.music.desktop.ui.components.DesktopEqualizer
import com.aashik.music.desktop.ui.components.DesktopExpandedLyrics
import java.io.File

private val TableRowShape = RoundedCornerShape(8.dp)
private val CardShape = RoundedCornerShape(12.dp)

// ─── 1. ALL SONGS (DESKTOP DATA TABLE) ───────────────────────────────────────
@Composable
fun DesktopLibraryTable(
    songs: List<DesktopSong>,
    currentSong: DesktopSong?,
    favoriteIds: Set<String>,
    onToggleFavorite: (DesktopSong) -> Unit,
    onPlaySong: (DesktopSong, List<DesktopSong>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.width(36.dp))
            Text("TITLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.weight(0.42f))
            Text("ARTIST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.weight(0.28f))
            Text("ALBUM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.weight(0.20f))
            Text("TIME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.width(54.dp))
            Spacer(modifier = Modifier.width(36.dp))
        }

        Divider(color = Color(0xFF1E2638), thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(6.dp))

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found in library", color = Color(0xFF64748B), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = song.id == currentSong?.id
                    val isFav = favoriteIds.contains(song.id)
                    val rowBg = if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.08f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TableRowShape)
                            .background(rowBg)
                            .clickable {
                                onPlaySong(song, songs.filter { it.id != song.id })
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track index / Playing Equalizer
                        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
                            if (isCurrent && isPlaying) {
                                DesktopEqualizer(isPlaying = true, barColor = Color(0xFF00E5FF))
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF64748B)
                                )
                            }
                        }

                        // Title with Artwork thumbnail
                        Row(
                            modifier = Modifier.weight(0.42f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DesktopArtwork(url = song.artworkUrl, size = 38.dp, shape = RoundedCornerShape(6.dp))
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFFF1F5F9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Artist
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.28f)
                        )

                        // Album
                        Text(
                            text = song.album,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.20f)
                        )

                        // Duration
                        Text(
                            text = formatMs(song.duration),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.width(54.dp)
                        )

                        // Favorite Star/Heart
                        IconButton(
                            onClick = { onToggleFavorite(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFFF4081) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 2. NOW PLAYING HUD STAGE (WIDESCREEN DESKTOP PLAYER) ────────────────────
@Composable
fun DesktopNowPlayingStage(
    currentSong: DesktopSong?,
    lyrics: List<DesktopLyricLine>,
    queue: List<DesktopSong>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    isRadioLoading: Boolean,
    onStartRadio: () -> Unit,
    onPlayNextSong: (DesktopSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPositionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Left Column: Large Artwork, Metadata, Action Bar
        Column(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Large 260×260dp Artwork
                DesktopArtwork(
                    url = currentSong?.artworkUrl,
                    size = 260.dp,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF252F42), RoundedCornerShape(18.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Song Title & Artist
                Text(
                    text = currentSong?.title ?: "No Song Playing",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentSong?.artist ?: "Unknown Artist"} • ${currentSong?.album ?: "Unknown Album"}",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Actions: Favorite, Radio, Next Song
            DesktopActionBar(
                currentSong = currentSong,
                upcomingSongs = queue,
                isQueueOpen = false,
                onToggleQueue = {},
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                isRadioActive = false,
                isRadioLoading = isRadioLoading,
                onStartRadio = onStartRadio,
                onPlayNextSong = onPlayNextSong,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        // Right Column: Full Synchronized Lyrics Stage
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121620))
        ) {
            DesktopExpandedLyrics(
                lyrics = lyrics,
                currentPositionMs = currentPositionMs,
                onSeek = { DesktopAudioPlayer.seekTo(it) },
                onClose = {},
                modifier = Modifier.fillMaxSize().padding(12.dp)
            )
        }
    }
}

// ─── 3. FOLDERS EXPLORER ────────────────────────────────────────────────────
@Composable
fun DesktopFolderBrowser(
    songs: List<DesktopSong>,
    onPlaySong: (DesktopSong, List<DesktopSong>) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders = remember(songs) {
        songs.groupBy { File(it.path).parent ?: "Local Music" }
    }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        if (selectedFolder == null) {
            Text("Local Folders", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(folders.keys.toList()) { folderPath ->
                    val name = File(folderPath).name.ifBlank { folderPath }
                    val count = folders[folderPath]?.size ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(Color(0xFF161B26))
                            .clickable { selectedFolder = folderPath }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(32.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFF1F5F9), maxLines = 1)
                            Text("$count tracks", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        } else {
            val folderSongs = folders[selectedFolder] ?: emptyList()
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color(0xFF00E5FF))
                }
                Text(File(selectedFolder!!).name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
            }
            DesktopLibraryTable(
                songs = folderSongs,
                currentSong = null,
                favoriteIds = emptySet(),
                onToggleFavorite = {},
                onPlaySong = onPlaySong
            )
        }
    }
}

// ─── 4. RADIO & EXPLORE VIEW ────────────────────────────────────────────────
@Composable
fun DesktopExploreView(
    currentSong: DesktopSong?,
    onPlaySong: (DesktopSong, List<DesktopSong>?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<DesktopRadioCategory?>(null) }
    var categorySongs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var isLoadingCategory by remember { mutableStateOf(false) }

    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val stations = remember { DesktopRadioData.CURATED_STATIONS }
    val moodCategories = remember { DesktopRadioData.MOOD_CATEGORIES }
    val genreCategories = remember { DesktopRadioData.GENRE_CATEGORIES }

    if (selectedCategory == null) {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header: Title and "Surprise Me" button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Radio & Music Discovery",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = "Stream curated global stations, moods, and genre sleeves",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Surprise Me Button
                    Button(
                        onClick = {
                            val randomStation = stations.random()
                            val song = randomStation.toSong()
                            val playlist = stations.map { it.toSong() }
                            onPlaySong(song, playlist)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("Surprise Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            // Section 1: Live Broadcast Radio Stations
            item {
                Text(
                    text = "Live Radio Stations & Community Streams",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(stations) { station ->
                        val isStationPlaying = isPlaying && currentSong?.path == station.streamUrl
                        DesktopRadioStationCard(
                            station = station,
                            isPlaying = isStationPlaying,
                            onPlay = {
                                val song = station.toSong()
                                val playlist = stations.map { it.toSong() }
                                onPlaySong(song, playlist)
                            }
                        )
                    }
                }
            }

            // Section 2: Moods & Moments
            item {
                Text(
                    text = "Moods & Moments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodCategories) { category ->
                        DesktopMoodCategoryCard(
                            category = category,
                            onClick = {
                                selectedCategory = category
                                isLoadingCategory = true
                                scope.launch {
                                    val results = DesktopOnlineRepository.searchSongs("${category.title} Hits", limit = 20)
                                    categorySongs = results
                                    isLoadingCategory = false
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Genres & Styles
            item {
                Text(
                    text = "All Genres & Styles",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreCategories) { category ->
                        DesktopMoodCategoryCard(
                            category = category,
                            onClick = {
                                selectedCategory = category
                                isLoadingCategory = true
                                scope.launch {
                                    val results = DesktopOnlineRepository.searchSongs("${category.title} Hits", limit = 20)
                                    categorySongs = results
                                    isLoadingCategory = false
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        // Category Detail View
        val category = selectedCategory!!
        val relatedStations = remember(category) {
            stations.filter { it.categoryId == category.id }
        }

        Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            // Category Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color(0xFF00E5FF))
                }
                DesktopArtwork(url = category.imageUrl, size = 52.dp, shape = RoundedCornerShape(10.dp))
                Column {
                    Text(category.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                    Text("Curated category stream & playlist", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }

            if (isLoadingCategory) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 2.5.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Shelf 1: Live Stations in this category (if any)
                    if (relatedStations.isNotEmpty()) {
                        item {
                            Text("Live Radio Stations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(relatedStations) { st ->
                                    val isStPlaying = isPlaying && currentSong?.path == st.streamUrl
                                    DesktopRadioStationCard(
                                        station = st,
                                        isPlaying = isStPlaying,
                                        onPlay = {
                                            val song = st.toSong()
                                            onPlaySong(song, relatedStations.map { it.toSong() })
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Shelf 2: Songs
                    item {
                        Text("Category Songs & Playlists", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 8.dp))
                    }

                    itemsIndexed(categorySongs) { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TableRowShape)
                                .background(Color(0xFF141822))
                                .clickable {
                                    onPlaySong(song, categorySongs.filter { it.id != song.id })
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${idx + 1}", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.width(24.dp))
                            DesktopArtwork(url = song.artworkUrl, size = 40.dp, shape = RoundedCornerShape(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF1F5F9), maxLines = 1)
                                Text(song.artist, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1)
                            }
                            if (song.duration > 0) {
                                Text(formatMs(song.duration), fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopRadioStationCard(
    station: DesktopRadioStation,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .height(130.dp)
            .clickable { onPlay() },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B26))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DesktopArtwork(url = station.imageUrl, size = 36.dp, shape = RoundedCornerShape(8.dp))
                // Live Badge or Equalizer
                if (isPlaying) {
                    DesktopEqualizer(isPlaying = true, barColor = Color(0xFF00E676))
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                    }
                }
            }

            Column {
                Text(station.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(station.description, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(station.genre, fontSize = 10.sp, color = Color(0xFF64748B))
                Text(station.bitrate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00E5FF))
            }
        }
    }
}

@Composable
fun DesktopMoodCategoryCard(
    category: DesktopRadioCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(120.dp)
            .clickable { onClick() },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DesktopArtwork(
                url = category.imageUrl,
                size = 180.dp,
                shape = CardShape,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
            Text(
                text = category.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

// ─── 5. 10-BAND GRAPHIC EQUALIZER ───────────────────────────────────────────
@Composable
fun DesktopEqualizerView(modifier: Modifier = Modifier) {
    val bands = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val bandLevels = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) }
    val presets = listOf("Flat", "Bass Boost", "Vocal", "Rock", "Pop", "Classical")
    var activePreset by remember { mutableStateOf("Flat") }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("10-Band Graphic Equalizer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
        Text("Real-time desktop acoustic DSP tuning", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

        // Preset Pills
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            presets.forEach { preset ->
                val isSel = activePreset == preset
                val bg = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.18f) else Color(0xFF161B26)
                val tint = if (isSel) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable {
                            activePreset = preset
                            when (preset) {
                                "Flat" -> for (i in bandLevels.indices) bandLevels[i] = 0f
                                "Bass Boost" -> { bandLevels[0] = 5f; bandLevels[1] = 4f; bandLevels[2] = 3f; bandLevels[3] = 1f; bandLevels[4] = 0f }
                                "Vocal" -> { bandLevels[3] = 2f; bandLevels[4] = 4f; bandLevels[5] = 4f; bandLevels[6] = 2f }
                                "Rock" -> { bandLevels[0] = 4f; bandLevels[1] = 3f; bandLevels[4] = -1f; bandLevels[8] = 3f; bandLevels[9] = 4f }
                                else -> for (i in bandLevels.indices) bandLevels[i] = 1f
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(preset, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = tint)
                }
            }
        }

        // Sliders Deck
        Card(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121620))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bands.forEachIndexed { idx, freq ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text("+12dB", fontSize = 10.sp, color = Color(0xFF475569))
                        Slider(
                            value = bandLevels[idx],
                            onValueChange = { bandLevels[idx] = it },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0xFF1E2638)
                            ),
                            modifier = Modifier.width(120.dp).padding(vertical = 4.dp)
                        )
                        Text("-12dB", fontSize = 10.sp, color = Color(0xFF475569))
                        Text(freq, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                    }
                }
            }
        }
    }
}

// ─── 6. SETTINGS VIEW ───────────────────────────────────────────────────────
@Composable
fun DesktopSettingsView(modifier: Modifier = Modifier) {
    val userHome = System.getProperty("user.home") ?: "~"
    val defaultPath = "$userHome/Music/MusicPlayer"

    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Application Preferences", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color(0xFF121620)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Offline Storage Directory", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF1F5F9))
                Text("Downloaded tracks and tagged companion .jpg covers are stored here:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(defaultPath, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00E5FF))
            }
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color(0xFF121620)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Audio Engine Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF1F5F9))
                Text("Backend: Apple AVFoundation (macOS Native Audio)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text("Hardware Acceleration: Apple Silicon ARM NEON enabled", fontSize = 12.sp, color = Color(0xFF00E676))
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}

