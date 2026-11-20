package com.aashik.music.wear.presentation.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.aashik.music.wear.model.WearSong
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.theme.WearBackgroundBlack
import com.aashik.music.wear.theme.WearOnSurfaceDim
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSurfaceCard

private val SquareChipShape = RoundedCornerShape(12.dp)

@Composable
fun WearQueueScreen(
    playlist: List<WearSong>,
    currentIndex: Int,
    isPlaying: Boolean,
    onSongSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = isScreenRound()
    val initialItem = remember(currentIndex, playlist.size) {
        if (currentIndex in playlist.indices) (currentIndex + 1) else 0
    }
    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = initialItem
    )
    val chipShape = remember(isRound) {
        if (isRound) CircleShape else SquareChipShape
    }

    LaunchedEffect(currentIndex, playlist.size) {
        if (currentIndex in playlist.indices) {
            // Scroll to center the currently playing item (item 0 is ListHeader, so item is currentIndex + 1)
            listState.animateScrollToItem(currentIndex + 1)
        }
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
            item {
                ListHeader {
                    Text(
                        text = "Playback Queue",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (playlist.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Queue is empty",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select a track from the library to start.",
                            fontSize = 10.sp,
                            color = WearOnSurfaceDim,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(playlist) { index, song ->
                    val isCurrent = index == currentIndex
                    val backgroundColor = if (isCurrent) WearPrimaryCyan.copy(alpha = 0.22f) else WearSurfaceCard
                    val titleColor = if (isCurrent) WearPrimaryCyan else Color.White

                    Chip(
                        onClick = { onSongSelected(index) },
                        label = {
                            Text(
                                text = song.title,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (isCurrent && isPlaying) "NOW PLAYING • ${song.formattedDuration()}" else "${song.artist} • ${song.formattedDuration()}",
                                fontSize = 10.sp,
                                color = if (isCurrent) WearPrimaryCyan.copy(alpha = 0.8f) else WearOnSurfaceDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) WearPrimaryCyan else WearSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.Black else WearPrimaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = backgroundColor),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
