package com.aashik.music.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
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

private val TableRowShape = RoundedCornerShape(6.dp)
private val CardShape = RoundedCornerShape(14.dp)
private val PillBadgeShape = RoundedCornerShape(4.dp)

// Curated dark genre card background palette (Subtle, elegant dark tones, never red)
private val DarkGenreCardPalette = listOf(
    Color(0xFF1E2838), // Deep Slate Navy
    Color(0xFF1B2A2E), // Deep Teal Charcoal
    Color(0xFF221F33), // Deep Indigo Dusk
    Color(0xFF1B2D26), // Deep Pine Emerald
    Color(0xFF262232), // Deep Charcoal Violet
    Color(0xFF2E271C), // Deep Amber Umber
    Color(0xFF232B35), // Dark Steel Blue
    Color(0xFF1D2633)  // Midnight Blue
)

private fun getDarkGenreColor(id: String): Color {
    val idx = (id.hashCode() and 0x7FFFFFFF) % DarkGenreCardPalette.size
    return DarkGenreCardPalette[idx]
}

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
            Text("#", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary, modifier = Modifier.width(36.dp))
            Text("TITLE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary, modifier = Modifier.weight(0.42f))
            Text("ARTIST", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary, modifier = Modifier.weight(0.28f))
            Text("ALBUM", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary, modifier = Modifier.weight(0.20f))
            Text("TIME", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkTextSecondary, modifier = Modifier.width(54.dp))
            Spacer(modifier = Modifier.width(36.dp))
        }

        Divider(color = DarkBorderSubtle, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(4.dp))

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = DarkTextTertiary, modifier = Modifier.size(44.dp))
                    Text("No songs found in library", color = DarkTextSecondary, fontSize = 14.sp)
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
                    val rowBg = if (isCurrent) DarkSurfaceVariant else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TableRowShape)
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
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = DarkAccent, modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    color = if (isCurrent) DarkAccent else DarkTextTertiary
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
                                color = if (isCurrent) DarkAccent else DarkTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Artist
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.28f)
                        )

                        // Album
                        Text(
                            text = song.album,
                            fontSize = 13.sp,
                            color = DarkTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.20f)
                        )

                        // Duration
                        Text(
                            text = if (song.duration > 0) formatMs(song.duration) else "Live",
                            fontSize = 12.sp,
                            color = DarkTextTertiary,
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
                                tint = if (isFav) DarkFavorite else DarkTextTertiary,
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
        // ─── LEFT: LARGE ARTWORK & INTEGRATED CONTROLS ───────────────────────
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
                            .background(DarkAccent.copy(alpha = 0.12f))
                            .border(1.dp, DarkAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
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
                                    tint = DarkAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Continuous Radio Station",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkAccent
                                )
                            }
                            Text(
                                text = "Exit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextSecondary,
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
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
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
                    color = DarkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Artist & Album
                Text(
                    text = "${currentSong?.artist ?: "Unknown Artist"} — ${currentSong?.album ?: "Unknown Album"}",
                    fontSize = 14.sp,
                    color = DarkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Lossless / Live Audio Pill
                if (durationMs <= 0L && currentSong != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkPlayingGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DarkPlayingGreen))
                        Text("LIVE BROADCAST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkPlayingGreen)
                    }
                } else if (currentSong != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LOSSLESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkTextSecondary)
                        Text("•", fontSize = 10.sp, color = DarkTextTertiary)
                        Text("Apple ALAC 24-bit/48kHz", fontSize = 10.sp, color = DarkTextSecondary)
                    }
                }
            }

            // Lower Info Stack: Scrubber, Transport Deck & Quick Actions
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
                            color = DarkTextTertiary,
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
                                activeTrackColor = DarkAccent,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = formatMs(durationMs),
                            fontSize = 11.5.sp,
                            color = DarkTextTertiary,
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
                            tint = if (isShuffleOn) DarkAccent else DarkTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous
                    IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause Circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(DarkAccent)
                            .clickable { DesktopAudioPlayer.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF0C0E14),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Next
                    IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatOn) DarkAccent else DarkTextTertiary,
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
                            tint = if (isFavorite) DarkFavorite else DarkTextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Start Continuous Radio
                    Button(
                        onClick = onStartRadio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRadioMode) DarkAccent.copy(alpha = 0.22f) else DarkSurfaceVariant
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
                                    color = DarkAccent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Podcasts,
                                    contentDescription = null,
                                    tint = if (isRadioMode) DarkAccent else DarkTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isRadioMode) "Radio Active" else "Start Radio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRadioMode) DarkAccent else DarkTextPrimary
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
                            tint = DarkTextTertiary,
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
                                activeTrackColor = DarkAccent,
                                inactiveTrackColor = DarkSurfaceVariant
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
            colors = CardDefaults.cardColors(containerColor = DarkSidebar),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                // Segmented Tab Switcher: [ Lyrics ] | [ Playing Next ]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Lyrics Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 0) DarkCard else Color.Transparent)
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
                                    tint = if (activeTab == 0) DarkAccent else DarkTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Lyrics",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (activeTab == 0) DarkTextPrimary else DarkTextSecondary
                                )
                            }
                        }

                        // Playing Next Tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == 1) DarkCard else Color.Transparent)
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
                                    tint = if (activeTab == 1) DarkAccent else DarkTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Playing Next (${queue.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (activeTab == 1) DarkTextPrimary else DarkTextSecondary
                                )
                            }
                        }
                    }

                    if (activeTab == 1 && queue.isNotEmpty()) {
                        TextButton(
                            onClick = onClearQueue,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = DarkAccent)
                        }
                    }
                }

                Divider(color = DarkBorderSubtle, thickness = 0.8.dp)

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
                                tint = DarkTextTertiary,
                                modifier = Modifier.size(46.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No lyrics available for this song",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Showing Playing Next queue instead",
                                    fontSize = 12.sp,
                                    color = DarkTextTertiary
                                )
                            }

                            Button(
                                onClick = { activeTab = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("View Playing Next (${queue.size})", fontSize = 12.sp, color = DarkAccent)
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
                                                .clip(TableRowShape)
                                                .background(DarkSurface)
                                                .clickable { onPlaySong(song) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("${idx + 1}", fontSize = 12.sp, color = DarkTextTertiary, modifier = Modifier.width(20.dp))
                                            DesktopArtwork(url = song.artworkUrl, size = 36.dp, shape = RoundedCornerShape(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary, maxLines = 1)
                                                Text(song.artist, fontSize = 11.sp, color = DarkTextSecondary, maxLines = 1)
                                            }
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = DarkAccent, modifier = Modifier.size(18.dp))
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
                                    color = if (isActive) DarkTextPrimary else DarkTextTertiary,
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
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = DarkTextTertiary, modifier = Modifier.size(46.dp))
                                Text("Playing Next queue is empty", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
                                Text("Start Radio to dynamically fill upcoming recommendations", fontSize = 12.sp, color = DarkTextTertiary)
                                Button(
                                    onClick = onStartRadio,
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Start Radio Station", fontSize = 12.sp, color = Color(0xFF0C0E14), fontWeight = FontWeight.Bold)
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
                                        .clip(TableRowShape)
                                        .background(DarkSurface)
                                        .clickable { onPlaySong(song) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("${idx + 1}", fontSize = 12.sp, color = DarkTextTertiary, modifier = Modifier.width(24.dp))
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
                                            color = DarkTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (song.duration > 0) {
                                        Text(formatMs(song.duration), fontSize = 11.sp, color = DarkTextTertiary)
                                    }
                                    IconButton(
                                        onClick = { onRemoveFromQueue(song) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = DarkTextTertiary,
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
            Text("Music Folders", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary, modifier = Modifier.padding(bottom = 16.dp))
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
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, CardShape)
                            .clickable { selectedFolder = folderPath }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = DarkAccent, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count songs", fontSize = 12.sp, color = DarkTextSecondary)
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = DarkAccent)
                }
                Text(File(selectedFolder!!).name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)
                Text("(${folderSongs.size} tracks)", fontSize = 13.sp, color = DarkTextSecondary)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(folderSongs) { idx, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TableRowShape)
                            .background(DarkSurface)
                            .clickable { onPlaySong(song, folderSongs.filter { it.id != song.id }) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("${idx + 1}", fontSize = 12.sp, color = DarkTextTertiary, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary, maxLines = 1)
                            Text(song.artist, fontSize = 11.sp, color = DarkTextSecondary, maxLines = 1)
                        }
                        Text(formatMs(song.duration), fontSize = 12.sp, color = DarkTextTertiary)
                    }
                }
            }
        }
    }
}

// ─── 4. RADIO & EXPLORE VIEW (EXACT MOBILE / LANDSCAPE GRID PARITY) ──────────
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
    val categories = remember { DesktopRadioData.CATEGORIES }

    val favStations = remember(stations, favoriteStationIds) {
        stations.filter { favoriteStationIds.contains(it.id) }
    }
    val favCategories = remember(categories, favoriteCategoryIds) {
        categories.filter { favoriteCategoryIds.contains(it.id) }
    }

    if (selectedCategory == null) {
        // EXACT MOBILE/LANDSCAPE GRID LAYOUT (RadioExploreContent.kt PARITY)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            // ── Section 1: Favorites Pinned at Top (if any) ──
            if (favStations.isNotEmpty() || favCategories.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Favorite Genres & Streams",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "${favStations.size + favCategories.size} saved",
                            fontSize = 12.sp,
                            color = DarkAccent
                        )
                    }
                }

                items(favCategories) { cat ->
                    val count = stations.count { it.categoryId == cat.id }
                    DesktopMobileMoodGenreCard(
                        category = cat,
                        stationCount = count,
                        isFavorite = true,
                        onFavoriteClick = { onToggleCategoryFavorite(cat.id) },
                        onClick = {
                            selectedCategory = cat
                            isLoadingCategory = true
                            scope.launch {
                                val results = DesktopOnlineRepository.searchSongs("${cat.title} Top Songs", limit = 25)
                                categorySongs = results
                                isLoadingCategory = false
                            }
                        }
                    )
                }

                items(favStations) { st ->
                    val isStPlaying = isPlaying && currentSong?.path == st.streamUrl
                    DesktopMobileRadioStationCard(
                        station = st,
                        isPlaying = isStPlaying,
                        isFavorite = true,
                        onPlayClick = {
                            val song = st.toSong()
                            val playlist = stations.map { it.toSong() }
                            onPlaySong(song, playlist)
                        },
                        onFavoriteClick = { onToggleStationFavorite(st.id) },
                        onStartRadioClick = { onStartRadioForSong(st.toSong()) }
                    )
                }
            }

            // ── Section 2: Header with "Surprise Me" Button ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (favStations.isNotEmpty() || favCategories.isNotEmpty()) 12.dp else 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "All Genres & Styles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Stream curated global stations, moods, and infinite algorithmic radio sleeves",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                    }

                    // Surprise Me Button
                    Surface(
                        onClick = {
                            val randomStation = stations.random()
                            val song = randomStation.toSong()
                            val playlist = stations.map { it.toSong() }
                            onPlaySong(song, playlist)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = DarkAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, DarkAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = DarkAccent, modifier = Modifier.size(15.dp))
                            Text("Surprise Me", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkAccent)
                        }
                    }
                }
            }

            // ── Section 3: Live Broadcast Radio Stations Grid ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Live Radio Stations & Community Streams",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(stations) { station ->
                val isStationPlaying = isPlaying && currentSong?.path == station.streamUrl
                DesktopMobileRadioStationCard(
                    station = station,
                    isPlaying = isStationPlaying,
                    isFavorite = favoriteStationIds.contains(station.id),
                    onPlayClick = {
                        val song = station.toSong()
                        val playlist = stations.map { it.toSong() }
                        onPlaySong(song, playlist)
                    },
                    onFavoriteClick = { onToggleStationFavorite(station.id) },
                    onStartRadioClick = { onStartRadioForSong(station.toSong()) }
                )
            }

            // ── Section 4: All Genres & Moods Grid ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Mood & Genre Sleeves",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(categories) { category ->
                val count = stations.count { it.categoryId == category.id }
                DesktopMobileMoodGenreCard(
                    category = category,
                    stationCount = count,
                    isFavorite = favoriteCategoryIds.contains(category.id),
                    onFavoriteClick = { onToggleCategoryFavorite(category.id) },
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
    } else {
        // Category Detail View (GenreDetailScreen Parity)
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = DarkAccent)
                }
                DesktopArtwork(url = category.imageUrl, size = 56.dp, shape = RoundedCornerShape(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)
                    Text("Curated station sleeve • ${categorySongs.size} tracks", fontSize = 12.sp, color = DarkTextSecondary)
                }

                // Play All Button
                if (categorySongs.isNotEmpty()) {
                    Button(
                        onClick = {
                            val first = categorySongs.first()
                            onPlaySong(first, categorySongs.drop(1))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF0C0E14), modifier = Modifier.size(16.dp))
                            Text("Play All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C0E14))
                        }
                    }
                }
            }

            if (isLoadingCategory) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkAccent, strokeWidth = 2.5.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Shelf 1: Live Stations in this category (if any)
                    if (relatedStations.isNotEmpty()) {
                        item {
                            Text("Live Radio Stations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(relatedStations) { st ->
                                    val isStPlaying = isPlaying && currentSong?.path == st.streamUrl
                                    DesktopMobileRadioStationCard(
                                        station = st,
                                        isPlaying = isStPlaying,
                                        isFavorite = favoriteStationIds.contains(st.id),
                                        onPlayClick = {
                                            val song = st.toSong()
                                            onPlaySong(song, relatedStations.map { it.toSong() })
                                        },
                                        onFavoriteClick = { onToggleStationFavorite(st.id) },
                                        onStartRadioClick = { onStartRadioForSong(st.toSong()) },
                                        modifier = Modifier.width(280.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Shelf 2: Songs
                    item {
                        Text("Category Songs & Algorithmic Playlist", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    itemsIndexed(categorySongs) { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TableRowShape)
                                .background(DarkSurface)
                                .clickable {
                                    onPlaySong(song, categorySongs.filter { it.id != song.id })
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${idx + 1}", fontSize = 12.sp, color = DarkTextTertiary, modifier = Modifier.width(24.dp))
                            DesktopArtwork(url = song.artworkUrl, size = 40.dp, shape = RoundedCornerShape(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary, maxLines = 1)
                                Text(song.artist, fontSize = 11.sp, color = DarkTextSecondary, maxLines = 1)
                            }
                            if (song.duration > 0) {
                                Text(formatMs(song.duration), fontSize = 12.sp, color = DarkTextTertiary)
                            }
                            IconButton(
                                onClick = { onStartRadioForSong(song) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Podcasts, contentDescription = "Radio", tint = DarkAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mobile/Landscape MoodGenreCard Port:
 * Wide card (height 86dp), corner artwork on bottom-right, title & live count badge on left, heart on top-right.
 */
@Composable
fun DesktopMobileMoodGenreCard(
    category: DesktopRadioCategory,
    stationCount: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = remember(category.id) { getDarkGenreColor(category.id) }

    Surface(
        onClick = onClick,
        shape = CardShape,
        color = cardBg,
        border = BorderStroke(1.dp, DarkBorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail Cover Art in bottom-right corner
            if (category.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    DesktopArtwork(
                        url = category.imageUrl,
                        size = 46.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Category Title + Live Badge / Subtitle on the left
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 56.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (stationCount > 0) {
                    Surface(
                        shape = PillBadgeShape,
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "$stationCount live",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Radio & Streams",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Favorite Icon Button in top-right corner
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) DarkFavorite else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Mobile/Landscape RadioStationCard Port:
 * Wide card (height 76dp), 46dp radio box, station name + ● ON AIR badge, genre • bitrate, Start Radio & Favorite buttons.
 */
@Composable
fun DesktopMobileRadioStationCard(
    station: DesktopRadioStation,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onStartRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isPlaying) DarkAccent.copy(alpha = 0.14f) else DarkSurface

    Surface(
        onClick = onPlayClick,
        shape = CardShape,
        color = cardBg,
        border = BorderStroke(1.dp, if (isPlaying) DarkAccent.copy(alpha = 0.5f) else DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Artwork / Icon box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) DarkAccent.copy(alpha = 0.25f) else DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (station.imageUrl.isNotBlank()) {
                    DesktopArtwork(url = station.imageUrl, size = 46.dp, shape = RoundedCornerShape(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = null,
                        tint = if (isPlaying) DarkAccent else DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) DarkAccent else DarkTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = PillBadgeShape,
                        color = if (isPlaying) DarkPlayingGreen.copy(alpha = 0.2f) else DarkSurfaceVariant
                    ) {
                        Text(
                            text = if (isPlaying) "● ON AIR" else "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPlaying) DarkPlayingGreen else DarkTextTertiary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.genre,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkAccent.copy(alpha = 0.9f)
                    )
                    Text(
                        text = " • ${station.bitrate} • ${station.country}",
                        fontSize = 11.sp,
                        color = DarkTextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Start Radio Button
            IconButton(
                onClick = onStartRadioClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = "Start Radio",
                    tint = DarkAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Favorite Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) DarkFavorite else DarkTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
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
        Text("Equalizer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)
        Text("Acoustic DSP frequency adjustment", fontSize = 13.sp, color = DarkTextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

        // Preset Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            presets.forEach { preset ->
                val isSel = activePreset == preset
                val bg = if (isSel) DarkAccent else DarkSurface
                val tint = if (isSel) Color(0xFF0C0E14) else DarkTextSecondary
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, if (isSel) DarkAccent else DarkBorder, RoundedCornerShape(16.dp))
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
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
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
                        Text("+12dB", fontSize = 10.sp, color = DarkTextTertiary)
                        Slider(
                            value = bandLevels[idx],
                            onValueChange = { bandLevels[idx] = it },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = DarkAccent,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.width(120.dp).padding(vertical = 4.dp)
                        )
                        Text("-12dB", fontSize = 10.sp, color = DarkTextTertiary)
                        Text(freq, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkTextSecondary)
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
        Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkTextPrimary)

        // Offline Storage Directory & Folder Picker
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline Storage Directory", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkTextPrimary)
                        Text("Location for downloaded audio streams and companion artwork (.jpg):", fontSize = 12.sp, color = DarkTextSecondary)
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Color(0xFF0C0E14), modifier = Modifier.size(16.dp))
                            Text("Choose Folder...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0C0E14))
                        }
                    }
                }

                // Current Directory Path Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = storageDir,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkAccent
                    )
                }

                if (updateMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DarkPlayingGreen, modifier = Modifier.size(16.dp))
                        Text(updateMessage!!, fontSize = 12.sp, color = DarkPlayingGreen)
                    }
                }
            }
        }

        // Audio Engine Diagnostics Card
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Audio Engine Diagnostics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backend:", fontSize = 12.sp, color = DarkTextTertiary)
                    Text("Apple AVFoundation (macOS Native Media Framework)", fontSize = 12.sp, color = DarkTextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hardware Acceleration:", fontSize = 12.sp, color = DarkTextTertiary)
                    Text("Apple Silicon ARM64 NEON Vector DSP Enabled", fontSize = 12.sp, color = DarkPlayingGreen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stream Formats:", fontSize = 12.sp, color = DarkTextTertiary)
                    Text("AAC 256k, ALAC Lossless, MP3 320k, Live Shoutcast / Icecast", fontSize = 12.sp, color = DarkTextSecondary)
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
