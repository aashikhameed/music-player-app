package com.aashik.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aashik.music.model.*
import com.aashik.music.ui.*
import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*
import com.aashik.music.viewmodel.MusicViewModel

private val PortraitSongCardShape = RoundedCornerShape(16.dp)
private val LandscapeFullSongShape = RoundedCornerShape(16.dp)
private val HeaderCapsuleShape = RoundedCornerShape(12.dp)
private val PillShape10 = RoundedCornerShape(8.dp)
private val StatusCapsuleShape = RoundedCornerShape(12.dp)
private val RadioCardShape = RoundedCornerShape(16.dp)
private val GenreChipShape = RoundedCornerShape(8.dp)
private val LiveBadgeShape = RoundedCornerShape(4.dp)
private val MoodCardShape = RoundedCornerShape(16.dp)
private val MoodStripeShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
private val BackPillShape = RoundedCornerShape(8.dp)
private val MobileMiniPlayerShape = RoundedCornerShape(16.dp)
private val MiniCodecBadgeShape = RoundedCornerShape(4.dp)
private val MobileNavBarShape = RoundedCornerShape(28.dp)
private val MobileActiveTabShape = RoundedCornerShape(16.dp)
private val ShelfArtworkShape = RoundedCornerShape(12.dp)
private val ShelfCardContainerShape = RoundedCornerShape(12.dp)
private val CircleBackButtonShape = CircleShape
private val CircleAvatarShape = CircleShape
private val MoreCardShape = RoundedCornerShape(12.dp)
private val MoreIconContainerShape = RoundedCornerShape(8.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun GenreDetailScreen(
    viewModel: MusicViewModel,
    category: RadioCategory,
    details: com.aashik.music.service.ExploreCategoryDetails?,
    categoryStations: List<RadioStation>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongLongPress: (Song) -> Unit,
    favoriteStationIds: Set<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Material 3 Standardized Top App Bar
        AppTopBar(
            title = category.title,
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        if (isLoading || (details == null && categoryStations.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Loading ${category.title}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (details?.songs.isNullOrEmpty() && details?.communityPlaylists.isNullOrEmpty() &&
                    details?.featuredPlaylists.isNullOrEmpty() && details?.albums.isNullOrEmpty() && categoryStations.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Couldn't load ${category.title}. Check your connection and try again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Shelf 1: Songs
                if (details?.songs?.isNotEmpty() == true) {
                    item {
                        ShelfHeader(title = "Songs")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = details.songs, key = { it.id }) { song ->
                                ShelfSongCard(
                                    song = song,
                                    onClick = { viewModel.play(song, playlist = details.songs) },
                                    onLongPress = { onSongLongPress(song) }
                                )
                            }
                        }
                    }
                }

                // Featured playlists
                if (details?.featuredPlaylists?.isNotEmpty() == true) {
                    item {
                        ShelfHeader(title = "Featured playlists")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items = details.featuredPlaylists, key = { it.id }) { item ->
                                CommunityPlaylistCard(
                                    item = item,
                                    onClick = { viewModel.playShelfItem(item) }
                                )
                            }
                        }
                    }
                }

                // Albums
                if (details?.albums?.isNotEmpty() == true) {
                    item {
                        ShelfHeader(title = "Albums")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items = details.albums, key = { it.id }) { item ->
                                CommunityPlaylistCard(
                                    item = item,
                                    onClick = { viewModel.playShelfItem(item) }
                                )
                            }
                        }
                    }
                }

                // Community playlists
                if (details?.communityPlaylists?.isNotEmpty() == true) {
                    item {
                        ShelfHeader(title = "Community playlists")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items = details.communityPlaylists, key = { it.id }) { item ->
                                CommunityPlaylistCard(
                                    item = item,
                                    onClick = { viewModel.playShelfItem(item) }
                                )
                            }
                        }
                    }
                }

                // Shelf 5: Live Radio Stations
                if (categoryStations.isNotEmpty()) {
                    item {
                        ShelfHeader(title = "Live Radio Stations")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = categoryStations, key = { it.id }) { station ->
                                val isFav = favoriteStationIds.contains(station.id)
                                RadioStationCard(
                                    station = station,
                                    isPlaying = false,
                                    isFavorite = isFav,
                                    onPlayClick = { viewModel.playRadioStation(station) },
                                    onFavoriteClick = { viewModel.toggleRadioFavorite(station.id) },
                                    onStartRadioClick = { viewModel.startRadioForSong(station.toSong()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
