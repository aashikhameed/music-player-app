package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.Surface
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.designsystem.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.theme.AppGradients
import com.aashik.music.ui.components.LiveAnimatedEqualizer
import com.aashik.music.ui.components.QueueItemRow
import com.aashik.music.viewmodel.MusicViewModel

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

// Precomputed top-level shapes matching Material 3 specifications
private val DrawerShapeLandscape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
private val DrawerShapePortrait = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val DrawerItemShape = RoundedCornerShape(12.dp)
private val DrawerBadgeShape = RoundedCornerShape(8.dp)
private val ReorderBtnShape = RoundedCornerShape(8.dp)

private val BackdropColor = Color.Black.copy(alpha = 0.55f)
private val GreenBadgeColor = Color(0xFF00E676)
private val PlayingBarColor = Color(0xFF00E676)

/**
 * Slide-over Queue Management Drawer.
 * Displays "Now Playing", prioritized user queue ("Play Next" & "Add to Queue"),
 * and playlist upcoming preview with reorder/remove controls.
 */
@Composable
fun QueueDrawer(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userQueue by viewModel.userQueue.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val nextFromPlaylist = viewModel.getNextSong()

    val cardBorder = AppGradients.border(isActive = false)

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackdropColor)
            .clickable(onClick = onClose)
    ) {
        val panelModifier = if (isPortrait) {
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        } else {
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(420.dp)
        }
        val panelShape = if (isPortrait) DrawerShapePortrait else DrawerShapeLandscape

        // Drawer Content Panel (Adaptive: Bottom Sheet on Phone, Side Drawer on Car)
        Surface(
            modifier = panelModifier
                .clickable(enabled = false) {}, // Consume clicks so backdrop doesn't trigger
            shape = panelShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Playback Queue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (userQueue.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        shape = DrawerBadgeShape
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${userQueue.size} Up Next",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (userQueue.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearQueue() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Clear", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (userQueue.isEmpty()) {
                    // Empty Queue State
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), shape = DrawerItemShape),
                        shape = DrawerItemShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Manual queue is empty",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Long press any song to select \"Play Next\" or \"Add to Queue\".",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // Playlist Upcoming Fallback Preview
                    if (nextFromPlaylist != null && nextFromPlaylist.id != currentSong?.id) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "FOLLOWING FROM PLAYLIST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playNextSong() }
                                .border(width = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), shape = DrawerItemShape),
                            shape = DrawerItemShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtImage(
                                    path = nextFromPlaylist.path,
                                    artworkUrl = nextFromPlaylist.artworkUrl,
                                    size = 38.dp,
                                    borderRadius = 6.dp,
                                    isPlaying = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextFromPlaylist.title.ifEmpty { "Unknown Title" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = nextFromPlaylist.artist.ifEmpty { "Unknown Artist" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = DrawerBadgeShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Queued Songs List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = userQueue,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            QueueItemRow(
                                index = index,
                                totalCount = userQueue.size,
                                song = song,
                                onPlayNow = {
                                    viewModel.playFromQueue(index)
                                },
                                onMoveUp = {
                                    if (index > 0) viewModel.moveQueueItem(index, index - 1)
                                },
                                onMoveDown = {
                                    if (index < userQueue.size - 1) viewModel.moveQueueItem(index, index + 1)
                                },
                                onRemove = {
                                    viewModel.removeFromQueue(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
