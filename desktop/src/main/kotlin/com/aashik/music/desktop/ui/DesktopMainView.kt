package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.components.*
import kotlinx.coroutines.launch

private val BigArtworkShape = RoundedCornerShape(20.dp)
private val MinimizedArtworkShape = RoundedCornerShape(12.dp)
private val BottomMusicCardShape = RoundedCornerShape(16.dp)

@Composable
fun DesktopMainView() {
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var currentSong by remember { mutableStateOf<DesktopSong?>(null) }
    var lyrics by remember { mutableStateOf<List<DesktopLyricLine>>(emptyList()) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isLyricsExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isRadioLoading by remember { mutableStateOf(false) }
    var isRadioActive by remember { mutableStateOf(false) }

    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val currentPositionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
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

    // Connect end-of-track auto transition
    LaunchedEffect(Unit) {
        DesktopAudioPlayer.setOnTrackFinished {
            playNext()
        }
    }

    // Initial load: scan local music or pre-load trending
    LaunchedEffect(Unit) {
        val local = DesktopLocalScanner.scanLocalMusic()
        if (local.isNotEmpty()) {
            currentSong = local.first()
            queue = local.drop(1)
        } else {
            val hits = DesktopOnlineRepository.searchSongs("Top Hits 2024", limit = 15)
            if (hits.isNotEmpty()) {
                currentSong = hits.first()
                queue = hits.drop(1)
            }
        }
        currentSong?.let {
            lyrics = DesktopLyricsRepository.fetchLyrics(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── TOP STATUS ROW ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Digital Automotive Clock
                DesktopClock(textColor = Color(0xFFF1F5F9))

                // Center Title & Equalizer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DesktopEqualizer(
                        isPlaying = isPlaying,
                        barColor = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "Automotive HUD • Desktop Edition",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Search & Volume Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF131922), shape = CircleShape)
                            .clip(CircleShape)
                            .clickable { isSearchOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Volume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.85f) }
                        )
                        Slider(
                            value = volume,
                            onValueChange = { DesktopAudioPlayer.setVolume(it) },
                            modifier = Modifier.width(90.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0xFF252F42)
                            )
                        )
                    }
                }
            }

            // ─── MAIN INFOTAINMENT STAGE (SPLIT VIEW) ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val artworkSize by animateDpAsState(
                    targetValue = if (isLyricsExpanded) 0.dp else 170.dp,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "ArtworkSize"
                )

                // Left: 160-170dp Album Artwork with smooth slide transition
                if (artworkSize > 0.dp) {
                    Box(
                        modifier = Modifier
                            .size(artworkSize)
                            .background(Color(0xFF131720), shape = BigArtworkShape)
                            .clip(BigArtworkShape)
                            .border(1.dp, Color(0xFF252F42), BigArtworkShape)
                    ) {
                        AnimatedContent(
                            targetState = currentSong,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(200))
                            },
                            label = "ArtworkTransition"
                        ) { song ->
                            DesktopArtwork(
                                url = song?.artworkUrl,
                                size = artworkSize,
                                shape = BigArtworkShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(28.dp))
                }

                // Content Area: Song Info OR Full Synced Lyrics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = isLyricsExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "LyricsExpandTransition"
                    ) { expanded ->
                        if (expanded) {
                            DesktopExpandedLyrics(
                                lyrics = lyrics,
                                currentPositionMs = currentPositionMs,
                                onSeek = { DesktopAudioPlayer.seekTo(it) },
                                onClose = { isLyricsExpanded = false },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(BigArtworkShape)
                                    .background(Color(0xFF131720))
                                    .border(1.dp, Color(0xFF252F42), BigArtworkShape)
                            )
                        } else {
                            // Song Title, Artist, and Interactive Single-Line Lyric Ticker
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Track Title
                                Text(
                                    text = currentSong?.title ?: "Select a Track",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF1F5F9),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Artist • Album
                                Text(
                                    text = "${currentSong?.artist ?: "Unknown Artist"} • ${currentSong?.album ?: "Unknown Album"}",
                                    fontSize = 15.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Interactive Single-Line Lyric Ticker
                                val activeLine = lyrics.lastOrNull { it.timeMs <= currentPositionMs }
                                if (activeLine != null) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1A222F))
                                            .clickable { isLyricsExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Subtitles,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = activeLine.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF00E5FF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── ACTION BAR (DriveHudActionBar) ──────────────────────────────
            AnimatedVisibility(
                visible = !isLyricsExpanded,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180))
            ) {
                DesktopActionBar(
                    currentSong = currentSong,
                    upcomingSongs = queue,
                    isQueueOpen = isQueueOpen,
                    onToggleQueue = { isQueueOpen = !isQueueOpen },
                    isFavorite = isFavorite,
                    onToggleFavorite = { isFavorite = !isFavorite },
                    isRadioActive = isRadioActive,
                    isRadioLoading = isRadioLoading,
                    onStartRadio = {
                        val seed = currentSong
                        if (seed != null) {
                            isRadioLoading = true
                            isRadioActive = true
                            scope.launch {
                                val radioQueue = DesktopOnlineRepository.fetchRadioQueue(seed)
                                queue = radioQueue
                                isRadioLoading = false
                            }
                        }
                    },
                    onPlayNextSong = { next ->
                        queue = queue.filter { it.id != next.id }
                        playSong(next)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // ─── BOTTOM INFOTAINMENT MUSIC CARD ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF131720), shape = BottomMusicCardShape)
                    .border(width = 0.8.dp, color = Color(0xFF252F42), shape = BottomMusicCardShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left 50%: Seeker Bar Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DesktopSeekBar(modifier = Modifier.fillMaxWidth())
                    }

                    // Right 50%: Playback Buttons Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DesktopPlaybackControls(
                            onPrevious = {
                                val currentPos = currentPositionMs
                                if (currentPos > 3000L) {
                                    DesktopAudioPlayer.seekTo(0L)
                                } else {
                                    // Restart or play previous
                                    DesktopAudioPlayer.seekTo(0L)
                                }
                            },
                            onNext = { playNext() }
                        )
                    }
                }
            }
        }

        // ─── SLIDE-IN QUEUE DRAWER OVERLAY ──────────────────────────────────
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            DesktopQueueDrawer(
                queue = queue,
                onPlayTrack = { song, index ->
                    val newQueue = queue.drop(index + 1)
                    playSong(song, newQueue)
                },
                onClose = { isQueueOpen = false }
            )
        }

        // ─── SEARCH DIALOG OVERLAY ──────────────────────────────────────────
        if (isSearchOpen) {
            DesktopSearchDialog(
                onPlaySong = { song ->
                    playSong(song)
                },
                onClose = { isSearchOpen = false }
            )
        }
    }
}
