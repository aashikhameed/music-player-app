package com.aashik.music.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.*
import com.aashik.music.service.DownloadStatus
import com.aashik.music.theme.AppGradients
import com.aashik.music.ui.*
import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*
import com.aashik.music.utils.formatTime
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import java.io.File
import kotlinx.coroutines.isActive

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

/**
 * Community Streams & Radio Exploration View.
 * Features curated Mood & Genre category sleeves, instant Favorites filter toggle,
 * and category-level community playlists / radio stations.
 */
@Composable
fun RadioExploreContent(
    viewModel: MusicViewModel,
    columns: Int,
    onSongLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val radioStations by viewModel.radioStations.collectAsState()
    val genreCategories by viewModel.genreCategories.collectAsState()
    val selectedCategory by viewModel.selectedRadioCategory.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()
    val favoriteCategoryIds by viewModel.favoriteCategoryIds.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentSongPath = currentSong?.path

    val isLoadingCategorySongs by viewModel.isLoadingCategorySongs.collectAsState()

    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        if (selectedCategory == null) {
            // ── Main Explore Screen (Genres & Styles) ──
            // Top Toolbar: Favorites Quick Filter + Random Station Trigger
            val favCategories = remember(genreCategories, favoriteCategoryIds) {
                genreCategories.filter { favoriteCategoryIds.contains(it.id) }
            }
            val favStations = remember(radioStations, favoriteStationIds) {
                radioStations.filter { favoriteStationIds.contains(it.id) }
            }

            if (showFavoritesOnly && favCategories.isEmpty() && favStations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No favorite genres or radio stations saved yet.\nTap the heart icon on any genre or stream to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Genres & Styles Grid (Vibrant streaming cards with cover thumbnails)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.coerceAtLeast(2)),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── 1. Favorites Pinned at the Top (when favorites exist) ──
                    if (favCategories.isNotEmpty() || favStations.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 6.dp, start = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Favorite Genres & Streams",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                )
                                Text(
                                    text = "${favCategories.size + favStations.size} saved",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // Favorite Genre Cards
                        items(items = favCategories, key = { "fav_${it.id}" }) { genre ->
                            val count = remember(radioStations, genre.id) {
                                radioStations.count { it.categoryId == genre.id }
                            }
                            MoodGenreCard(
                                category = genre,
                                stationCount = count,
                                isFavorite = true,
                                onFavoriteClick = { viewModel.toggleCategoryFavorite(genre.id) },
                                onClick = { viewModel.openRadioCategory(genre) }
                            )
                        }

                        // Favorite Station Cards
                        items(items = favStations, key = { "fav_st_${it.id}" }) { station ->
                            val isPlaying = station.streamUrl == currentSongPath
                            RadioStationCard(
                                station = station,
                                isPlaying = isPlaying,
                                isFavorite = true,
                                onPlayClick = { viewModel.playRadioStation(station) },
                                onFavoriteClick = { viewModel.toggleRadioFavorite(station.id) },
                                onStartRadioClick = { viewModel.startRadioForSong(station.toSong()) }
                            )
                        }
                    }

                    // ── 2. All Genres & Styles Section (only when not in favorites-only filter) ──
                    if (!showFavoritesOnly) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (favCategories.isNotEmpty() || favStations.isNotEmpty()) 12.dp else 4.dp, bottom = 6.dp, start = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "All Genres & Styles",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                    onClick = { viewModel.playRandomOrNextRadioStation() },
                    shape = PillShape10,
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Random Stream",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Surprise Me",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                            }
                        }

                        items(items = genreCategories, key = { it.id }) { genre ->
                            val count = remember(radioStations, genre.id) {
                                radioStations.count { it.categoryId == genre.id }
                            }
                            val isFav = favoriteCategoryIds.contains(genre.id)
                            MoodGenreCard(
                                category = genre,
                                stationCount = count,
                                isFavorite = isFav,
                                onFavoriteClick = { viewModel.toggleCategoryFavorite(genre.id) },
                                onClick = { viewModel.openRadioCategory(genre) }
                            )
                        }
                    }
                }
            }
        } else {
            // ── Category Detail Screen (Drill-Down: Songs, Playlists, Albums) ──
            val category = selectedCategory!!
            val details by viewModel.exploreCategoryDetails.collectAsState()
            val categoryStations = remember(radioStations, category.id, favoriteStationIds, showFavoritesOnly) {
                radioStations.filter { station ->
                    val matchesCategory = station.categoryId == category.id
                    val matchesFav = !showFavoritesOnly || favoriteStationIds.contains(station.id)
                    matchesCategory && matchesFav
                }
            }

            GenreDetailScreen(
                viewModel = viewModel,
                category = category,
                details = details,
                categoryStations = categoryStations,
                isLoading = isLoadingCategorySongs,
                onBack = { viewModel.closeRadioCategory() },
                onSongLongPress = onSongLongPress,
                favoriteStationIds = favoriteStationIds
            )
        }
    }
}
