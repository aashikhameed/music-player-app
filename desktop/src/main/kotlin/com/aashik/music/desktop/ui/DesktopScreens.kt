package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.aashik.music.desktop.model.DesktopRadioCategory
import com.aashik.music.desktop.model.DesktopRadioData
import com.aashik.music.desktop.model.DesktopRadioStation
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.utils.DesktopFolderPicker
import kotlinx.coroutines.launch
import java.io.File

private val AppleTableRowShape = RoundedCornerShape(6.dp)
private val AppleCardShape = RoundedCornerShape(12.dp)

// ─── 1. ALL SONGS (APPLE MUSIC 1:1 DATA TABLE) ──────────────────────────────
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
            Text("#", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextSecondary, modifier = Modifier.width(36.dp))
            Text("TITLE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextSecondary, modifier = Modifier.weight(0.42f))
            Text("ARTIST", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextSecondary, modifier = Modifier.weight(0.28f))
            Text("ALBUM", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextSecondary, modifier = Modifier.weight(0.20f))
            Text("TIME", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextSecondary, modifier = Modifier.width(54.dp))
            Spacer(modifier = Modifier.width(36.dp))
        }

        Divider(color = AppleMusicBorderSubtle, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(4.dp))

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AppleMusicTextTertiary, modifier = Modifier.size(44.dp))
                    Text("No songs found in library", color = AppleMusicTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = song.id == currentSong?.id
                    val isFav = favoriteIds.contains(song.id)
                    val rowBg = if (isCurrent) AppleMusicSurfaceVariant else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppleTableRowShape)
                            .background(rowBg)
                            .clickable {
                                onPlaySong(song, songs.filter { it.id != song.id })
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track index / Audio wave icon
                        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
                            if (isCurrent && isPlaying) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AppleMusicRed, modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    color = if (isCurrent) AppleMusicRed else AppleMusicTextTertiary
                                )
                            }
                        }

                        // Title with cover art thumbnail
                        Row(
                            modifier = Modifier.weight(0.42f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DesktopArtwork(
                                url = song.artworkUrl,
                                size = 36.dp,
                                shape = RoundedCornerShape(6.dp)
                            )
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) AppleMusicRed else AppleMusicTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Artist
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = AppleMusicTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.28f)
                        )

                        // Album
                        Text(
                            text = song.album,
                            fontSize = 13.sp,
                            color = AppleMusicTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.20f)
                        )

                        // Duration
                        Text(
                            text = if (song.duration > 0) formatMs(song.duration) else "Live",
                            fontSize = 12.sp,
                            color = AppleMusicTextTertiary,
                            modifier = Modifier.width(54.dp)
                        )

                        // Heart Favorite Button
                        IconButton(
                            onClick = { onToggleFavorite(song) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) AppleMusicRed else AppleMusicTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 2. APPLE MUSIC 1:1 NOW PLAYING FULLSTAGE HUD ───────────────────────────
@Composable
fun DesktopNowPlayingStage(
    currentSong: DesktopSong?,
    lyrics: List<DesktopLyricLine>,
    queue: List<DesktopSong>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    isRadioMode: Boolean,
    isRadioLoading: Boolean,
    onStartRadio: () -> Unit,
    onExitRadio: () -> Unit,
    onPlaySong: (DesktopSong) -> Unit,
    onRemoveFromQueue: (DesktopSong) -> Unit,
    onClearQueue: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isShuffleOn: Boolean,
    onToggleShuffle: () -> Unit,
    isRepeatOn: Boolean,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val positionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()
    val volume by DesktopAudioPlayer.volume.collectAsState()

    var activeTab by remember { mutableStateOf(if (lyrics.isNotEmpty()) 0 else 1) }

    LaunchedEffect(lyrics.isNotEmpty()) {
        if (lyrics.isNotEmpty() && activeTab == 1 && queue.isEmpty()) {
            activeTab = 0
        }
    }

    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val progress = if (isDraggingSeek) {
        dragProgress
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // ─── LEFT: APPLE MUSIC LARGE ARTWORK & TRANSPORT CONTROLS ───────────
        Column(
            modifier = Modifier
                .width(440.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Info Stack: Album Artwork, Title, Artist, Lossless Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Radio Mode Banner
                if (isRadioMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppleMusicRed.copy(alpha = 0.12f))
                            .border(1.dp, AppleMusicRed.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Podcasts,
                                    contentDescription = null,
                                    tint = AppleMusicRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Apple Music Continuous Radio",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleMusicRed
                                )
                            }
                            Text(
                                text = "Exit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleMusicTextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onExitRadio() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Apple Music Large 280×280dp Framed Artwork
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppleMusicSurface)
                        .border(1.dp, AppleMusicBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DesktopArtwork(
                        url = currentSong?.artworkUrl,
                        size = 280.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Song Title
                Text(
                    text = currentSong?.title ?: "Not Playing",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleMusicTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Artist & Album
                Text(
                    text = "${currentSong?.artist ?: "Unknown Artist"} — ${currentSong?.album ?: "Unknown Album"}",
                    fontSize = 14.sp,
                    color = AppleMusicTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Apple Lossless / Live Audio Pill
                if (durationMs <= 0L && currentSong != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF30D158).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF30D158)))
                        Text("LIVE BROADCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                    }
                } else if (currentSong != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppleMusicSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LOSSLESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextSecondary)
                        Text("•", fontSize = 10.sp, color = AppleMusicTextTertiary)
                        Text("Apple ALAC 24-bit/48kHz", fontSize = 10.sp, color = AppleMusicTextSecondary)
                    }
                }
            }

            // Lower Info Stack: Apple Music Scrubber, Transport Deck & Quick Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrubber (for audio with duration)
                if (durationMs > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatMs(if (isDraggingSeek) (dragProgress * durationMs).toLong() else positionMs),
                            fontSize = 11.5.sp,
                            color = AppleMusicTextTertiary,
                            modifier = Modifier.width(36.dp)
                        )

                        Slider(
                            value = progress,
                            onValueChange = {
                                isDraggingSeek = true
                                dragProgress = it
                            },
                            onValueChangeFinished = {
                                DesktopAudioPlayer.seekTo((dragProgress * durationMs).toLong())
                                isDraggingSeek = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AppleMusicRed,
                                inactiveTrackColor = AppleMusicSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = formatMs(durationMs),
                            fontSize = 11.5.sp,
                            color = AppleMusicTextTertiary,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transport Deck: Shuffle, Prev, Play/Pause, Next, Repeat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Shuffle
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleOn) AppleMusicRed else AppleMusicTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous
                    IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = AppleMusicTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause Circle (Apple Music Vibrant Red)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AppleMusicRed)
                            .clickable { DesktopAudioPlayer.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Next
                    IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = AppleMusicTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatOn) AppleMusicRed else AppleMusicTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Utility Row: Favorite Heart, Radio Button, Volume Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Favorite Heart
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) AppleMusicRed else AppleMusicTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Start Continuous Radio
                    Button(
                        onClick = onStartRadio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRadioMode) AppleMusicRed.copy(alpha = 0.22f) else AppleMusicSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRadioLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleMusicRed
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Podcasts,
                                    contentDescription = null,
                                    tint = if (isRadioMode) AppleMusicRed else AppleMusicTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isRadioMode) "Radio Active" else "Create Station",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRadioMode) AppleMusicRed else AppleMusicTextPrimary
                            )
                        }
                    }

                    // Volume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Icon(
                            imageVector = if (volume > 0.05f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Volume",
                            tint = AppleMusicTextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    DesktopAudioPlayer.setVolume(if (volume > 0.05f) 0f else 0.85f)
                                }
                        )
                        Slider(
                            value = volume,
                            onValueChange = { DesktopAudioPlayer.setVolume(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AppleMusicRed,
                                inactiveTrackColor = AppleMusicSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ─── RIGHT: APPLE MUSIC CONTEXT DECK (LYRICS / PLAYING NEXT QUEUE) ───
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppleMusicSidebar),
            border = BorderStroke(1.dp, AppleMusicBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                // Apple Music Segmented Tab Switcher: [ Lyrics ] | [ Playing Next ]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusicSurface)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Lyrics Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 0) AppleMusicCard else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FormatQuote,
                                    contentDescription = null,
                                    tint = if (activeTab == 0) AppleMusicRed else AppleMusicTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Lyrics",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (activeTab == 0) AppleMusicTextPrimary else AppleMusicTextSecondary
                                )
                            }
                        }

                        // Playing Next Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 1) AppleMusicCard else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) AppleMusicRed else AppleMusicTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Playing Next (${queue.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (activeTab == 1) AppleMusicTextPrimary else AppleMusicTextSecondary
                                )
                            }
                        }
                    }

                    if (activeTab == 1 && queue.isNotEmpty()) {
                        TextButton(
                            onClick = onClearQueue,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = AppleMusicRed)
                        }
                    }
                }

                Divider(color = AppleMusicBorderSubtle, thickness = 0.8.dp)

                // Tab 0: Synchronized Lyrics
                if (activeTab == 0) {
                    if (lyrics.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SubtitlesOff,
                                contentDescription = null,
                                tint = AppleMusicTextTertiary,
                                modifier = Modifier.size(46.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No lyrics available for this song",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleMusicTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Showing Playing Next queue instead",
                                    fontSize = 12.sp,
                                    color = AppleMusicTextTertiary
                                )
                            }

                            Button(
                                onClick = { activeTab = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = AppleMusicSurfaceVariant),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("View Playing Next (${queue.size})", fontSize = 12.sp, color = AppleMusicRed)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Instant mini queue preview
                            if (queue.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(queue.take(4)) { idx, song ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(AppleTableRowShape)
                                                .background(AppleMusicSurface)
                                                .clickable { onPlaySong(song) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("${idx + 1}", fontSize = 12.sp, color = AppleMusicTextTertiary, modifier = Modifier.width(20.dp))
                                            DesktopArtwork(url = song.artworkUrl, size = 36.dp, shape = RoundedCornerShape(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextPrimary, maxLines = 1)
                                                Text(song.artist, fontSize = 11.sp, color = AppleMusicTextSecondary, maxLines = 1)
                                            }
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = AppleMusicRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Apple Music iconic lyrics waterfall
                        val activeIndex = lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
                        val listState = rememberLazyListState()

                        LaunchedEffect(activeIndex) {
                            if (activeIndex in lyrics.indices) {
                                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            itemsIndexed(lyrics) { index, line ->
                                val isActive = index == activeIndex
                                Text(
                                    text = line.text,
                                    fontSize = if (isActive) 22.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) AppleMusicTextPrimary else AppleMusicTextTertiary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { DesktopAudioPlayer.seekTo(line.timeMs) }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Tab 1: Playing Next Queue
                if (activeTab == 1) {
                    if (queue.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = AppleMusicTextTertiary, modifier = Modifier.size(46.dp))
                                Text("Playing Next queue is empty", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextPrimary)
                                Text("Create station to dynamically fill upcoming recommendations", fontSize = 12.sp, color = AppleMusicTextTertiary)
                                Button(
                                    onClick = onStartRadio,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Start Radio Station", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(queue) { idx, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppleTableRowShape)
                                        .background(AppleMusicSurface)
                                        .clickable { onPlaySong(song) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("${idx + 1}", fontSize = 12.sp, color = AppleMusicTextTertiary, modifier = Modifier.width(24.dp))
                                    DesktopArtwork(
                                        url = song.artworkUrl,
                                        size = 38.dp,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleMusicTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 11.sp,
                                            color = AppleMusicTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (song.duration > 0) {
                                        Text(formatMs(song.duration), fontSize = 11.sp, color = AppleMusicTextTertiary)
                                    }
                                    IconButton(
                                        onClick = { onRemoveFromQueue(song) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = AppleMusicTextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
            Text("Music Folders", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary, modifier = Modifier.padding(bottom = 16.dp))
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
                            .clip(AppleCardShape)
                            .background(AppleMusicSurface)
                            .border(1.dp, AppleMusicBorder, AppleCardShape)
                            .clickable { selectedFolder = folderPath }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleMusicRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = AppleMusicRed, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count songs", fontSize = 12.sp, color = AppleMusicTextSecondary)
                        }
                    }
                }
            }
        } else {
            val folderSongs = folders[selectedFolder] ?: emptyList()
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AppleMusicRed)
                }
                Text(File(selectedFolder!!).name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary)
                Text("(${folderSongs.size} tracks)", fontSize = 13.sp, color = AppleMusicTextSecondary)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(folderSongs) { idx, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppleTableRowShape)
                            .background(AppleMusicSurface)
                            .clickable { onPlaySong(song, folderSongs.filter { it.id != song.id }) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("${idx + 1}", fontSize = 12.sp, color = AppleMusicTextTertiary, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextPrimary, maxLines = 1)
                            Text(song.artist, fontSize = 11.sp, color = AppleMusicTextSecondary, maxLines = 1)
                        }
                        Text(formatMs(song.duration), fontSize = 12.sp, color = AppleMusicTextTertiary)
                    }
                }
            }
        }
    }
}

// ─── 4. RADIO & EXPLORE VIEW (APPLE MUSIC RADIO & BROWSE HUB) ───────────────
@Composable
fun DesktopExploreView(
    currentSong: DesktopSong?,
    favoriteCategoryIds: Set<String> = emptySet(),
    onToggleCategoryFavorite: (String) -> Unit = {},
    favoriteStationIds: Set<String> = emptySet(),
    onToggleStationFavorite: (String) -> Unit = {},
    onPlaySong: (DesktopSong, List<DesktopSong>) -> Unit,
    onStartRadioForSong: (DesktopSong) -> Unit = {},
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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Radio & Browse",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleMusicTextPrimary
                        )
                        Text(
                            text = "Tune in to curated global radio broadcasts, moods, and endless algorithmic music sleeves",
                            fontSize = 13.sp,
                            color = AppleMusicTextSecondary
                        )
                    }

                    // Surprise Me
                    Button(
                        onClick = {
                            val randomStation = stations.random()
                            val song = randomStation.toSong()
                            val playlist = stations.map { it.toSong() }
                            onPlaySong(song, playlist)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Surprise Me", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Section 1: Live Broadcast Radio Stations
            item {
                Text(
                    text = "Live Radio Stations & Broadcasts",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleMusicTextPrimary,
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
                            isFavorite = favoriteStationIds.contains(station.id),
                            onToggleFavorite = { onToggleStationFavorite(station.id) },
                            onPlay = {
                                val song = station.toSong()
                                val playlist = stations.map { it.toSong() }
                                onPlaySong(song, playlist)
                            }
                        )
                    }
                }
            }

            // Section 2: Moods & Activities
            item {
                Text(
                    text = "Moods & Activities",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleMusicTextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodCategories) { category ->
                        DesktopMoodCategoryCard(
                            category = category,
                            isFavorite = favoriteCategoryIds.contains(category.id),
                            onToggleFavorite = { onToggleCategoryFavorite(category.id) },
                            onClick = {
                                selectedCategory = category
                                isLoadingCategory = true
                                scope.launch {
                                    val results = DesktopOnlineRepository.searchSongs("${category.title} Top Songs", limit = 25)
                                    categorySongs = results
                                    isLoadingCategory = false
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Browse by Genre
            item {
                Text(
                    text = "Browse by Genre",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleMusicTextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreCategories) { category ->
                        DesktopMoodCategoryCard(
                            category = category,
                            isFavorite = favoriteCategoryIds.contains(category.id),
                            onToggleFavorite = { onToggleCategoryFavorite(category.id) },
                            onClick = {
                                selectedCategory = category
                                isLoadingCategory = true
                                scope.launch {
                                    val results = DesktopOnlineRepository.searchSongs("${category.title} Hits", limit = 25)
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = AppleMusicRed)
                }
                DesktopArtwork(url = category.imageUrl, size = 56.dp, shape = RoundedCornerShape(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary)
                    Text("Curated station sleeve • ${categorySongs.size} tracks", fontSize = 12.sp, color = AppleMusicTextSecondary)
                }

                // Play All Button
                if (categorySongs.isNotEmpty()) {
                    Button(
                        onClick = {
                            val first = categorySongs.first()
                            onPlaySong(first, categorySongs.drop(1))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Play All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (isLoadingCategory) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppleMusicRed, strokeWidth = 2.5.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Shelf 1: Live Stations in this category (if any)
                    if (relatedStations.isNotEmpty()) {
                        item {
                            Text("Live Radio Stations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(relatedStations) { st ->
                                    val isStPlaying = isPlaying && currentSong?.path == st.streamUrl
                                    DesktopRadioStationCard(
                                        station = st,
                                        isPlaying = isStPlaying,
                                        isFavorite = favoriteStationIds.contains(st.id),
                                        onToggleFavorite = { onToggleStationFavorite(st.id) },
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
                        Text("Category Songs & Algorithmic Playlist", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    itemsIndexed(categorySongs) { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppleTableRowShape)
                                .background(AppleMusicSurface)
                                .clickable {
                                    onPlaySong(song, categorySongs.filter { it.id != song.id })
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${idx + 1}", fontSize = 12.sp, color = AppleMusicTextTertiary, modifier = Modifier.width(24.dp))
                            DesktopArtwork(url = song.artworkUrl, size = 40.dp, shape = RoundedCornerShape(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicTextPrimary, maxLines = 1)
                                Text(song.artist, fontSize = 11.sp, color = AppleMusicTextSecondary, maxLines = 1)
                            }
                            if (song.duration > 0) {
                                Text(formatMs(song.duration), fontSize = 12.sp, color = AppleMusicTextTertiary)
                            }
                            IconButton(
                                onClick = { onStartRadioForSong(song) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Podcasts, contentDescription = "Radio", tint = AppleMusicRed, modifier = Modifier.size(16.dp))
                            }
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
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artUrl = station.imageUrl.ifBlank {
        DesktopRadioData.CATEGORIES.firstOrNull { it.id == station.categoryId }?.imageUrl ?: ""
    }

    Card(
        modifier = modifier
            .width(200.dp)
            .height(200.dp)
            .clickable { onPlay() },
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = AppleMusicSurface),
        border = BorderStroke(1.dp, if (isPlaying) AppleMusicRed else AppleMusicBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    DesktopArtwork(url = artUrl, size = 68.dp, shape = RoundedCornerShape(10.dp))
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AppleMusicRed, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite Station",
                            tint = if (isFavorite) AppleMusicRed else AppleMusicTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPlaying) Color(0xFF30D158).copy(alpha = 0.2f) else AppleMusicSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "PLAYING" else "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) Color(0xFF30D158) else AppleMusicTextSecondary
                        )
                    }
                }
            }

            Column {
                Text(station.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(station.description, fontSize = 11.sp, color = AppleMusicTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(station.genre, fontSize = 10.sp, color = AppleMusicTextTertiary)
                Text(station.bitrate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AppleMusicRed)
            }
        }
    }
}

@Composable
fun DesktopMoodCategoryCard(
    category: DesktopRadioCategory,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(120.dp)
            .clickable { onClick() },
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = AppleMusicSurface),
        border = BorderStroke(1.dp, AppleMusicBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DesktopArtwork(
                url = category.imageUrl,
                size = 180.dp,
                shape = AppleCardShape,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite Category",
                    tint = if (isFavorite) AppleMusicRed else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
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
    val bands = listOf("32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val bandLevels = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) }
    val presets = listOf("Flat", "Bass Booster", "Vocal Booster", "Electronic", "Rock", "Pop", "Classical", "Acoustic")
    var activePreset by remember { mutableStateOf("Flat") }

    Column(modifier = modifier.fillMaxSize().padding(28.dp)) {
        Text("Equalizer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary)
        Text("Acoustic DSP frequency adjustment", fontSize = 13.sp, color = AppleMusicTextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

        // Preset Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            presets.forEach { preset ->
                val isSel = activePreset == preset
                val bg = if (isSel) AppleMusicRed else AppleMusicSurface
                val tint = if (isSel) Color.White else AppleMusicTextSecondary
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, if (isSel) AppleMusicRed else AppleMusicBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            activePreset = preset
                            when (preset) {
                                "Flat" -> for (i in bandLevels.indices) bandLevels[i] = 0f
                                "Bass Booster" -> { bandLevels[0] = 6f; bandLevels[1] = 5f; bandLevels[2] = 4f; bandLevels[3] = 2f; bandLevels[4] = 0f }
                                "Vocal Booster" -> { bandLevels[3] = 3f; bandLevels[4] = 5f; bandLevels[5] = 5f; bandLevels[6] = 3f }
                                "Electronic" -> { bandLevels[0] = 5f; bandLevels[1] = 4f; bandLevels[7] = 3f; bandLevels[8] = 4f; bandLevels[9] = 5f }
                                "Rock" -> { bandLevels[0] = 5f; bandLevels[1] = 3f; bandLevels[4] = -1f; bandLevels[8] = 4f; bandLevels[9] = 5f }
                                "Acoustic" -> { bandLevels[0] = 3f; bandLevels[1] = 2f; bandLevels[4] = 2f; bandLevels[7] = 3f; bandLevels[8] = 4f }
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
            shape = AppleCardShape,
            colors = CardDefaults.cardColors(containerColor = AppleMusicSurface),
            border = BorderStroke(1.dp, AppleMusicBorder)
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
                        Text("+12dB", fontSize = 10.sp, color = AppleMusicTextTertiary)
                        Slider(
                            value = bandLevels[idx],
                            onValueChange = { bandLevels[idx] = it },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AppleMusicRed,
                                inactiveTrackColor = AppleMusicSurfaceVariant
                            ),
                            modifier = Modifier.width(120.dp).padding(vertical = 4.dp)
                        )
                        Text("-12dB", fontSize = 10.sp, color = AppleMusicTextTertiary)
                        Text(freq, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextSecondary)
                    }
                }
            }
        }
    }
}

// ─── 6. SETTINGS VIEW (WITH OFFLINE STORAGE DIRECTORY PICKER) ───────────────
@Composable
fun DesktopSettingsView(
    onStorageDirChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var storageDir by remember { mutableStateOf(DesktopLocalScanner.getStoredStorageDir()) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppleMusicTextPrimary)

        // Offline Storage Directory & Folder Picker
        Card(
            shape = AppleCardShape,
            colors = CardDefaults.cardColors(containerColor = AppleMusicSurface),
            border = BorderStroke(1.dp, AppleMusicBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline Storage Directory", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppleMusicTextPrimary)
                        Text("Location for downloaded audio streams and companion artwork (.jpg):", fontSize = 12.sp, color = AppleMusicTextSecondary)
                    }

                    // Native macOS Directory Picker Button
                    Button(
                        onClick = {
                            val picked = DesktopFolderPicker.pickFolder(storageDir)
                            if (picked != null && picked.isNotBlank()) {
                                storageDir = picked
                                DesktopLocalScanner.saveStorageDir(picked)
                                onStorageDirChanged(picked)
                                updateMessage = "Storage directory updated: $picked"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleMusicRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Choose Folder...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // Current Directory Path Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppleMusicCard)
                        .border(1.dp, AppleMusicBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = storageDir,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppleMusicRed
                    )
                }

                if (updateMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(16.dp))
                        Text(updateMessage!!, fontSize = 12.sp, color = Color(0xFF30D158))
                    }
                }
            }
        }

        // Audio Engine Diagnostics Card
        Card(
            shape = AppleCardShape,
            colors = CardDefaults.cardColors(containerColor = AppleMusicSurface),
            border = BorderStroke(1.dp, AppleMusicBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Audio Engine Diagnostics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppleMusicTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backend:", fontSize = 12.sp, color = AppleMusicTextTertiary)
                    Text("Apple AVFoundation (macOS Native Media Framework)", fontSize = 12.sp, color = AppleMusicTextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hardware Acceleration:", fontSize = 12.sp, color = AppleMusicTextTertiary)
                    Text("Apple Silicon ARM64 NEON Vector DSP Enabled", fontSize = 12.sp, color = Color(0xFF30D158))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stream Formats:", fontSize = 12.sp, color = AppleMusicTextTertiary)
                    Text("AAC 256k, ALAC Lossless, MP3 320k, Live Shoutcast / Icecast", fontSize = 12.sp, color = AppleMusicTextSecondary)
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
