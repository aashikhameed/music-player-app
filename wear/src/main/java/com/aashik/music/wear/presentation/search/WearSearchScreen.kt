package com.aashik.music.wear.presentation.search

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.aashik.music.wear.model.WearSong
import com.aashik.music.wear.online.WearRadioRepository
import com.aashik.music.wear.presentation.util.isScreenRound
import com.aashik.music.wear.theme.WearBackgroundBlack
import com.aashik.music.wear.theme.WearOnSurfaceDim
import com.aashik.music.wear.theme.WearPrimaryCyan
import com.aashik.music.wear.theme.WearSecondaryAmber
import com.aashik.music.wear.theme.WearSurfaceCard
import kotlinx.coroutines.launch

private val SquareChipShape = RoundedCornerShape(12.dp)

@Composable
fun WearSearchScreen(
    onSongSelected: (WearSong, List<WearSong>, WearRadioRepository.RadioCategory?) -> Unit,
    onPlayLiveStation: ((WearSong) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isRound = isScreenRound()
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isLoadingStation by remember { mutableStateOf(false) }
    var activeStationTitle by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf<WearRadioRepository.RadioCategory?>(null) }
    var stationTracks by remember { mutableStateOf<List<WearSong>>(emptyList()) }
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
            item {
                ListHeader {
                    Text(
                        text = "Radio & Genres",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (isLoadingStation) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                indicatorColor = WearPrimaryCyan,
                                trackColor = WearSurfaceCard,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tuning to $activeStationTitle...",
                                fontSize = 11.sp,
                                color = WearOnSurfaceDim,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (stationTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Station: $activeStationTitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WearSecondaryAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                items(stationTracks) { song ->
                    Chip(
                        onClick = { onSongSelected(song, stationTracks, activeCategory) },
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
                                text = "${song.artist} • LIVE STREAM",
                                fontSize = 10.sp,
                                color = WearPrimaryCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // ─── LIVE 24/7 RADIO STATIONS ───────────────────────────────────────
                item {
                    Text(
                        text = "Live Radio Stations",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearPrimaryCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                items(WearRadioRepository.LIVE_RADIO_STATIONS) { station ->
                    Chip(
                        onClick = {
                            val liveSong = with(WearRadioRepository) { station.toWearSong() }
                            if (onPlayLiveStation != null) {
                                onPlayLiveStation(liveSong)
                            } else {
                                onSongSelected(liveSong, listOf(liveSong), null)
                            }
                        },
                        label = {
                            Text(
                                text = station.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "LIVE • ${station.description}",
                                fontSize = 10.sp,
                                color = WearPrimaryCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── MOODS & MOMENTS (Matching Mobile Radio) ─────────────────────────
                item {
                    Text(
                        text = "Moods & Moments",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearSecondaryAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                items(WearRadioRepository.MOOD_CATEGORIES) { mood ->
                    Chip(
                        onClick = {
                            activeStationTitle = mood.title
                            activeCategory = mood
                            isLoadingStation = true
                            coroutineScope.launch {
                                val tracks = WearRadioRepository.getCategoryTracks(mood)
                                stationTracks = tracks
                                isLoadingStation = false
                                if (tracks.isNotEmpty()) {
                                    onSongSelected(tracks.first(), tracks, mood)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = mood.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = mood.subtitle,
                                fontSize = 10.sp,
                                color = WearOnSurfaceDim
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = WearPrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── GENRES (Matching Mobile Radio) ─────────────────────────────────
                item {
                    Text(
                        text = "Popular Genres",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearSecondaryAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                items(WearRadioRepository.GENRE_CATEGORIES) { genre ->
                    Chip(
                        onClick = {
                            activeStationTitle = genre.title
                            activeCategory = genre
                            isLoadingStation = true
                            coroutineScope.launch {
                                val tracks = WearRadioRepository.getCategoryTracks(genre)
                                stationTracks = tracks
                                isLoadingStation = false
                                if (tracks.isNotEmpty()) {
                                    onSongSelected(tracks.first(), tracks, genre)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = genre.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = genre.subtitle,
                                fontSize = 10.sp,
                                color = WearOnSurfaceDim
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = WearSecondaryAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(backgroundColor = WearSurfaceCard),
                        shape = chipShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
