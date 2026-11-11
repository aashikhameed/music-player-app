package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.components.LiveAnimatedEqualizer
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.LiquidAlertDialog
import com.aashik.music.ui.mobile.glass.LiquidButton
import com.aashik.music.ui.mobile.glass.LiquidGlassCard
import com.aashik.music.ui.mobile.glass.LiquidIconButton
import com.aashik.music.ui.mobile.glass.LiquidPrimaryButton
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

// Pre-allocated static shapes to prevent GC allocations on low-spec automotive hardware
private val SongThumbnailShape = RoundedCornerShape(10.dp)
private val SongThumbnailLiquidShape = RoundedRectangle(10f.dp)
private val ActionButtonLiquidShape = Capsule()

/**
 * Mobile Music Screen (replaces Home).
 * Displays local available songs library with quick playback controls (Play All, Shuffle)
 * and detailed tracklist with live playing status indicators.
 * Fully powered by Kyant0/AndroidLiquidGlass components.
 */
@Composable
fun MobileMusicScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRescanClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onDeleteSong: (Song) -> Unit = {},
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 150.dp) // Room for floating dock & nav bar
        ) {
        // ─── TOP LARGE TITLE & SETTINGS ICON ───────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Music",
                        color = MobileTheme.text,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (songs.isNotEmpty()) "${songs.size} local songs available" else "No local songs",
                        color = MobileTheme.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Settings Button using LiquidIconButton
                LiquidIconButton(
                    onClick = onSettingsClick,
                    size = 36.dp,
                    surfaceColor = MobileTheme.text.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MobileTheme.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ─── ACTION BUTTONS: PLAY ALL & SHUFFLE ────────────────────────────────
        if (songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play All: tinted liquid glass button (catalog LiquidButton `tint`), accent hue
                    LiquidButton(
                        onClick = onPlayAllClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = ActionButtonLiquidShape,
                        tint = MobileTheme.accent
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play All",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Shuffle Button (Frosted Liquid Glass Button)
                    LiquidButton(
                        onClick = onShuffleClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = ActionButtonLiquidShape,
                        surfaceColor = MobileTheme.text.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = MobileTheme.text,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shuffle",
                            color = MobileTheme.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── TRACKLIST OF ALL LOCAL SONGS ──────────────────────────────────
            itemsIndexed(
                items = songs,
                key = { _, song -> song.path }
            ) { _, song ->
                MobileSongRow(
                    song = song,
                    isCurrent = song.path == currentSong?.path,
                    onClick = { onSongClick(song) },
                    showMoreButton = false,
                    onLongClick = { songToDelete = song }
                )
            }
        } else {
            // ─── EMPTY STATE WHEN NO SONGS FOUND ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, start = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidIconButton(
                        onClick = {},
                        size = 80.dp,
                        surfaceColor = MobileTheme.text.copy(alpha = 0.08f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MobileTheme.accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Local Music Found",
                        color = MobileTheme.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Place audio files in your device storage or grant media audio permissions to listen to your local library.",
                        color = MobileTheme.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LiquidPrimaryButton(
                        onClick = onRescanClick,
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Rescan",
                            tint = MobileTheme.onText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Media",
                            color = MobileTheme.onText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal Liquid Glass Alert Dialog rendered strictly on top of the list
    LiquidAlertDialog(
        visible = songToDelete != null,
        onDismissRequest = { songToDelete = null },
        title = "Delete Song",
        message = "Are you sure you want to delete \"${songToDelete?.title ?: ""}\" from storage?",
        confirmButtonText = "Delete",
        confirmTint = Color(0xFFEF4444),
        onConfirm = {
            songToDelete?.let { onDeleteSong(it) }
            songToDelete = null
        },
        dismissButtonText = "Cancel",
        onDismissButton = { songToDelete = null },
        backdrop = backdrop
    )
}
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
