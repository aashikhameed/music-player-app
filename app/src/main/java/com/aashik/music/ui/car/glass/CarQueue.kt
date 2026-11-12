package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.PlaybackMode
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Playing Next list (legacy QueueDrawer features): the user queue with tap-to-play,
 * move up / down, remove and Clear; otherwise the upcoming songs of the active playlist.
 */
@Composable
internal fun CarQueueList(viewModel: MusicViewModel, modifier: Modifier = Modifier, horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    val userQueue by viewModel.userQueue.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val mode by viewModel.playbackMode.collectAsState()
    val fromUserQueue = userQueue.isNotEmpty()
    val upcoming = remember(userQueue, currentSong, mode) {
        if (fromUserQueue) userQueue else viewModel.getUpcomingSongs(40)
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = horizontalPadding, end = horizontalPadding - 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Playing Next", color = MobileTheme.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = when {
                        fromUserQueue -> if (userQueue.size == 1) "1 song in your queue" else "${userQueue.size} songs in your queue"
                        mode == PlaybackMode.SHUFFLE -> "Shuffling the playlist"
                        else -> "Continuing in order"
                    },
                    color = MobileTheme.textSecondary,
                    fontSize = 13.sp
                )
            }
            if (fromUserQueue) {
                CarTextPillButton("Clear", accent = false) { viewModel.clearQueue() }
            }
        }

        if (upcoming.isEmpty()) {
            Text(
                "Nothing up next.",
                color = MobileTheme.textSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp)
            )
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(upcoming, key = { i, s -> "${s.id}-$i" }) { i, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CarDims.RowHeight)
                        .clickable { if (fromUserQueue) viewModel.playFromQueue(i) else viewModel.play(song, direction = 1) }
                        .padding(start = horizontalPadding, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImage(path = song.path, artworkUrl = song.artworkUrl, size = 44.dp, borderRadius = 6.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title.ifBlank { "Untitled" }, color = MobileTheme.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist.ifBlank { "Unknown Artist" }, color = MobileTheme.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (fromUserQueue) {
                        CarIconButton(Icons.Rounded.ArrowUpward, "Move up", onClick = { if (i > 0) viewModel.moveQueueItem(i, i - 1) },
                            size = 48.dp, glyph = 20.dp, tint = MobileTheme.textSecondary, enabled = i > 0)
                        CarIconButton(Icons.Rounded.ArrowDownward, "Move down", onClick = { if (i < upcoming.lastIndex) viewModel.moveQueueItem(i, i + 1) },
                            size = 48.dp, glyph = 20.dp, tint = MobileTheme.textSecondary, enabled = i < upcoming.lastIndex)
                        CarIconButton(Icons.Rounded.RemoveCircleOutline, "Remove from queue", onClick = { viewModel.removeFromQueue(i) },
                            size = 48.dp, glyph = 22.dp, tint = MobileTheme.textSecondary)
                    }
                }
            }
        }
    }
}

/** Queue sheet on the driver's side, opened from the playback capsule (replaces the overlay drawer). */
@Composable
internal fun CarQueueSheet(visible: Boolean, viewModel: MusicViewModel, onClose: () -> Unit) {
    // The sheet slides in from the driver's side so its controls are in reach.
    val fromRight = LocalDriveSide.current == com.aashik.music.model.DriveSide.RIGHT
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f)) { if (fromRight) it else -it },
            exit = slideOutHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f)) { if (fromRight) it else -it },
            modifier = Modifier.align(if (fromRight) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Column(
                Modifier
                    .width(480.dp)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .carGlass(24.dp, CarSheetShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarGlyph(Icons.AutoMirrored.Rounded.QueueMusic, null, MobileTheme.accent, 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Queue", color = MobileTheme.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    CarIconButton(Icons.Rounded.Close, "Close queue", onClick = onClose, filled = true, size = 48.dp, glyph = 22.dp)
                }
                CarQueueList(viewModel, Modifier.weight(1f).fillMaxWidth(), horizontalPadding = 20.dp)
            }
        }
    }
}
