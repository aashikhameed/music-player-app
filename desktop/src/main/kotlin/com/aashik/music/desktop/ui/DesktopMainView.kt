package com.aashik.music.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.service.DesktopOnlineRepository
import kotlinx.coroutines.launch

enum class DesktopTab {
    HUD, SEARCH, LOCAL, QUEUE
}

@Composable
fun DesktopMainView() {
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DesktopTab.HUD) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var localSongs by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val currentPositionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()
    val volume by DesktopAudioPlayer.volume.collectAsState()

    // Play a song helper
    fun playSong(song: DesktopSong, newQueue: List<DesktopSong>? = null) {
        currentSong = song
        if (newQueue != null) {
            queue = newQueue
        }
        DesktopAudioPlayer.play(song)
        scope.launch {
            lyrics = DesktopLyricsRepository.fetchLyrics(song)
        }
    }

    // Play next song
    fun playNext() {
        if (queue.isNotEmpty()) {
            val next = queue.first()
            queue = queue.drop(1)
            playSong(next)
        }
    }

    // Connect end of track
    LaunchedEffect(Unit) {
        DesktopAudioPlayer.setOnTrackFinished {
            playNext()
        }
    }

    // Initial local scan and featured songs
    LaunchedEffect(Unit) {
        localSongs = DesktopLocalScanner.scanLocalMusic()
        if (localSongs.isNotEmpty() && currentSong == null) {
            currentSong = localSongs.first()
            queue = localSongs.drop(1)
        }
        // Prefetch popular search
        if (searchResults.isEmpty()) {
            searchResults = DesktopOnlineRepository.searchSongs("Top Hits 2024", limit = 15)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesktopBg)
    ) {
        // ─── TOP BAR ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(DesktopSurface)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & App Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DesktopPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = DesktopPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Music Player",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DesktopTextPrimary
                )
            }

            // Navigation Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopNavPill(
                    label = "Player HUD",
                    icon = Icons.Rounded.Speed,
                    selected = currentTab == DesktopTab.HUD,
                    onClick = { currentTab = DesktopTab.HUD }
                )
                DesktopNavPill(
                    label = "Search & Explore",
                    icon = Icons.Rounded.TravelExplore,
                    selected = currentTab == DesktopTab.SEARCH,
                    onClick = { currentTab = DesktopTab.SEARCH }
                )
                DesktopNavPill(
                    label = "Local Library (${localSongs.size})",
                    icon = Icons.Rounded.FolderSpecial,
                    selected = currentTab == DesktopTab.LOCAL,
                    onClick = { currentTab = DesktopTab.LOCAL }
                )
                DesktopNavPill(
                    label = "Queue (${queue.size})",
                    icon = Icons.Rounded.QueueMusic,
                    selected = currentTab == DesktopTab.QUEUE,
                    onClick = { currentTab = DesktopTab.QUEUE }
                )
            }

            // Search input & Volume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, artists...", fontSize = 13.sp, color = DesktopTextSecondary) },
                    singleLine = true,
                    shape = DesktopPillShape,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = DesktopTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = DesktopPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                currentTab = DesktopTab.SEARCH
                                scope.launch {
                                    searchResults = DesktopOnlineRepository.searchSongs(searchQuery)
                                    isSearching = false
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DesktopSurfaceVariant,
                        unfocusedContainerColor = DesktopSurfaceVariant,
                        focusedBorderColor = DesktopPrimary,
                        unfocusedBorderColor = DesktopBorder,
                        focusedTextColor = DesktopTextPrimary,
                        unfocusedTextColor = DesktopTextPrimary
                    ),
                    modifier = Modifier.width(260.dp).height(44.dp)
                )

                // Volume slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = null,
                        tint = DesktopTextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.8f) }
                    )
                    Slider(
                        value = volume,
                        onValueChange = { DesktopAudioPlayer.setVolume(it) },
                        modifier = Modifier.width(90.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = DesktopPrimary,
                            activeTrackColor = DesktopPrimary,
                            inactiveTrackColor = DesktopBorder
                        )
                    )
                }
            }
        }

        // ─── BODY (SPLIT VIEW) ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT PANEL: Large Player HUD
            Card(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight(),
                shape = DesktopCardShape,
                colors = CardDefaults.cardColors(containerColor = DesktopSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Artwork
                    DesktopArtwork(
                        url = currentSong?.artworkUrl,
                        size = 280.dp,
                        shape = DesktopArtShape,
                        modifier = Modifier
                            .border(1.dp, DesktopBorder, DesktopArtShape)
                    )

                    // Track Typography
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = currentSong?.title ?: "Select a Track",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesktopTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            fontSize = 15.sp,
                            color = DesktopTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Seeker Slider
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Slider(
                            value = progress,
                            onValueChange = { frac ->
                                val target = (frac * durationMs).toLong()
                                DesktopAudioPlayer.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = DesktopPrimary,
                                activeTrackColor = DesktopPrimary,
                                inactiveTrackColor = DesktopBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatMs(currentPositionMs), fontSize = 12.sp, color = DesktopTextSecondary)
                            Text(text = formatMs(durationMs), fontSize = 12.sp, color = DesktopTextSecondary)
                        }
                    }

                    // Playback Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Radio generator button
                        IconButton(
                            onClick = {
                                val seed = currentSong
                                if (seed != null) {
                                    scope.launch {
                                        val radio = DesktopOnlineRepository.fetchRadioQueue(seed)
                                        queue = radio
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Radio, contentDescription = "Radio", tint = DesktopPrimary)
                        }

                        // Play / Pause Circle Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DesktopPrimary)
                                .clickable {
                                    if (currentSong != null) {
                                        DesktopAudioPlayer.togglePlayPause()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next Track Button
                        IconButton(
                            onClick = { playNext() },
                            enabled = queue.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = if (queue.isNotEmpty()) DesktopTextPrimary else DesktopTextSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // RIGHT PANEL: Dynamic View (Lyrics / Search / Local / Queue)
            Card(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight(),
                shape = DesktopCardShape,
                colors = CardDefaults.cardColors(containerColor = DesktopSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    when (currentTab) {
                        DesktopTab.HUD -> {
                            // Synced Lyrics View
                            DesktopLyricsView(
                                lyrics = lyrics,
                                currentPositionMs = currentPositionMs,
                                onLineClicked = { ms -> DesktopAudioPlayer.seekTo(ms) }
                            )
                        }
                        DesktopTab.SEARCH -> {
                            // Search Results
                            DesktopSongListView(
                                title = if (searchQuery.isBlank()) "Recommended & Trending" else "Results for '$searchQuery'",
                                songs = searchResults,
                                onSongClicked = { song -> playSong(song, searchResults.filter { it.id != song.id }) }
                            )
                        }
                        DesktopTab.LOCAL -> {
                            // Local Songs
                            DesktopSongListView(
                                title = "Local Music Library (~/Music)",
                                songs = localSongs,
                                onSongClicked = { song -> playSong(song, localSongs.filter { it.id != song.id }) }
                            )
                        }
                        DesktopTab.QUEUE -> {
                            // Queue View
                            DesktopSongListView(
                                title = "Up Next (${queue.size} songs)",
                                songs = queue,
                                onSongClicked = { song ->
                                    val idx = queue.indexOfFirst { it.id == song.id }
                                    if (idx != -1) {
                                        val newQ = queue.drop(idx + 1)
                                        playSong(song, newQ)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopNavPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) DesktopPrimary.copy(alpha = 0.18f) else Color.Transparent)
    val tint by animateColorAsState(if (selected) DesktopPrimary else DesktopTextSecondary)

    Row(
        modifier = Modifier
            .clip(DesktopPillShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = tint)
    }
}

@Composable
fun DesktopLyricsView(
    lyrics: List<DesktopLyricLine>,
    currentPositionMs: Long,
    onLineClicked: (Long) -> Unit
) {
    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = DesktopTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No synchronized lyrics found for this song", color = DesktopTextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    val activeIndex = lyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyrics.size) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val color = if (isActive) DesktopPrimary else DesktopTextSecondary.copy(alpha = 0.65f)
            val fontSize = if (isActive) 18.sp else 15.sp
            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

            Text(
                text = line.text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLineClicked(line.timeMs) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun DesktopSongListView(
    title: String,
    songs: List<DesktopSong>,
    onSongClicked: (DesktopSong) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DesktopTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs available", color = DesktopTextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DesktopCardShape)
                            .background(DesktopSurfaceVariant)
                            .clickable { onSongClicked(song) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DesktopArtwork(url = song.artworkUrl, size = 44.dp, shape = DesktopCardShape)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DesktopTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = DesktopTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (song.duration > 0) {
                            Text(
                                text = formatMs(song.duration),
                                fontSize = 12.sp,
                                color = DesktopTextSecondary
                            )
                        }
                    }
                }
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
