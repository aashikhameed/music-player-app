package com.aashik.music.wear.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.aashik.music.wear.model.WearSong
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.storage.WearStorageHelper
import com.aashik.music.wear.theme.WearBackgroundBlack
import com.aashik.music.wear.theme.WearOnSurfaceDim
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSecondaryAmber
import com.aashik.music.wear.theme.WearSurfaceCard

private val SquareChipShape = RoundedCornerShape(12.dp)

@Composable
fun WearLibraryScreen(
    songs: List<WearSong>,
    onSongSelected: (WearSong, List<WearSong>) -> Unit,
    onScanStorageClicked: () -> Unit,
    onQueueClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onDownloadsClicked: (() -> Unit)? = null,
    downloadedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val isRound = isScreenRound()
    val listState = rememberScalingLazyListState()
    val storageInfo = remember { WearStorageHelper.getWatchStorageInfo() }
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
            // Header
            item {
                ListHeader {
                    Text(
                        text = "Music Library",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Radio & Genres Chip
            item {
                Chip(
                    onClick = onSearchClicked,
                    label = {
                        Text(
                            text = "Radio & Genres",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Live stations & moods",
                            fontSize = 10.sp,
                            color = WearPrimaryCyan
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio",
                            tint = WearPrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                    shape = chipShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Offline Downloads Chip
            if (onDownloadsClicked != null) {
                item {
                    Chip(
                        onClick = onDownloadsClicked,
                        label = {
                            Text(
                                text = "Downloads",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "$downloadedCount saved offline",
                                fontSize = 10.sp,
                                color = Color(0xFF30D158)
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Downloads",
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Current Queue Chip
            item {
                Chip(
                    onClick = onQueueClicked,
                    label = {
                        Text(
                            text = "Playback Queue",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "${songs.size} songs queued",
                            fontSize = 10.sp,
                            color = WearSecondaryAmber
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = WearSecondaryAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                    shape = chipShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Storage Scan Chip
            item {
                Chip(
                    onClick = onScanStorageClicked,
                    label = {
                        Text(
                            text = "Scan Watch Storage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WearPrimaryCyan
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "${storageInfo.formattedFree} free storage",
                            fontSize = 10.sp,
                            color = WearOnSurfaceDim
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Storage",
                            tint = WearPrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(backgroundColor = WearSurfaceCard),
                    shape = chipShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Place audio files into the watch's /Music folder and tap Scan Storage.",
                            fontSize = 10.sp,
                            color = WearOnSurfaceDim,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(songs) { song ->
                    Chip(
                        onClick = { onSongSelected(song, songs) },
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
                                text = "${song.artist} • ${song.formattedDuration()}",
                                fontSize = 10.sp,
                                color = WearOnSurfaceDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(WearPrimaryCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = WearPrimaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
