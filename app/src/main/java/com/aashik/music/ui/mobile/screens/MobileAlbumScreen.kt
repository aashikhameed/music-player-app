package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.ui.mobile.glass.MobileGlassBadgePill
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

/**
 * 1:1 Mobile Album Screen matching designScreen/mobile/Album.png.
 * Features 240x240pt centered artwork, format badge pills,
 * Solid White Play and Frosted Glass Shuffle pill buttons,
 * expandable editorial review, and clean numbered tracklist without thumbnails.
 */
@Composable
fun MobileAlbumScreen(
    album: DomainAlbum,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isReviewExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── TOP BAR WITH BACK BUTTON & CONTEXT ACTIONS ────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ─── 240x240pt HERO ARTWORK CARD ───────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .mobileLiquidGlass(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.6f)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.35f)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.4f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Album Title
                Text(
                    text = album.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Artist Link in Cyan
                Text(
                    text = album.artist,
                    color = Color(0xFF00E5FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onArtistClick(album.artist) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Format Badges Row (Pop • 2019 • Dolby Atmos • Lossless)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${album.genre} • ${album.year}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    if (album.isDolbyAtmos) {
                        MobileGlassBadgePill(text = "DOLBY ATMOS")
                    }
                    if (album.isLossless) {
                        MobileGlassBadgePill(text = "LOSSLESS", useCyan = true)
                    }
                }
            }
        }

        // ─── ACTION BUTTONS: PLAY & SHUFFLE CAPSULES (160x44pt) ────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Solid White Play Capsule
                Button(
                    onClick = { album.tracks.firstOrNull()?.let(onSongClick) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Play",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Frosted Glass Shuffle Capsule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .mobileLiquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            tintColor = Color.White.copy(alpha = 0.10f),
                            borderColor = Color.White.copy(alpha = 0.15f)
                        )
                        .clickable {
                            val shuffled = album.tracks.shuffled()
                            shuffled.firstOrNull()?.let(onSongClick)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Shuffle",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // ─── EDITORIAL REVIEW EXPANDABLE CARD ──────────────────────────────────
        if (album.editorialReview.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = album.editorialReview,
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (isReviewExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isReviewExpanded) "LESS" else "MORE",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isReviewExpanded = !isReviewExpanded }
                    )
                }
            }
        }

        // ─── CLEAN NUMBERED TRACKLIST ──────────────────────────────────────────
        itemsIndexed(album.tracks) { index, song ->
            AlbumNumberedTrackRow(
                index = index + 1,
                song = song,
                onClick = { onSongClick(song) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 20.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.05f)
            )
        }

        // ─── ALBUM FOOTER SUMMARY ──────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "${album.tracks.size} Songs, ${calculateTotalRuntime(album.tracks)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Released ${album.year} • ℗ ${album.year} Republic Records",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Apple Digital Master",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AlbumNumberedTrackRow(
    index: Int,
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Index Number
        Text(
            text = "$index",
            color = Color(0xFF64748B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp)
        )

        // Track Title & Featured Artists
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song.artist.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration (mm:ss)
        Text(
            text = formatDuration(song.duration),
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // More Options
        IconButton(
            onClick = {},
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun calculateTotalRuntime(tracks: List<Song>): String {
    val totalSeconds = tracks.sumOf { it.duration } / 1000
    val minutes = totalSeconds / 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "$hours hour $remainingMinutes minutes"
    } else {
        "$minutes minutes"
    }
}

