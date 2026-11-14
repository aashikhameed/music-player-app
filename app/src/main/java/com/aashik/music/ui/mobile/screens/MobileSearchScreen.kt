package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.components.GenreDetailScreen
import com.aashik.music.viewmodel.MusicViewModel
import com.aashik.music.ui.mobile.glass.LiquidButton
import com.aashik.music.ui.mobile.glass.LiquidIconButton
import com.aashik.music.ui.mobile.glass.MobileGlassSearchBar
import com.aashik.music.ui.mobile.glass.MobileGlassSegmentedPill

private val ResultArtShape = RoundedCornerShape(6.dp)


/**
 * 1:1 Mobile Search Screen matching designScreen/mobile/Search.png.
 * Features Large Title 'Search', Liquid Glass Capsule Search Bar,
 * Segmented scope pill ('All' / 'Library'),
 * Recently Searched history tokens with 'Clear' action,
 * and 2-Column vibrant browse categories grid.
 */
@Composable
fun MobileSearchScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedRadioCategory.collectAsState()
    val radioCategories by viewModel.radioCategories.collectAsState()
    val favoriteCategoryIds by viewModel.favoriteCategoryIds.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val isLoadingCategorySongs by viewModel.isLoadingCategorySongs.collectAsState()
    val exploreCategoryDetails by viewModel.exploreCategoryDetails.collectAsState()

    androidx.activity.compose.BackHandler(enabled = selectedCategory != null) {
        viewModel.closeRadioCategory()
    }

    selectedCategory?.let { category ->
        val categoryStations = remember(radioStations, category.id) {
            radioStations.filter { it.categoryId == category.id }
        }
        GenreDetailScreen(
            viewModel = viewModel,
            category = category,
            details = exploreCategoryDetails,
            categoryStations = categoryStations,
            isLoading = isLoadingCategorySongs,
            onBack = { viewModel.closeRadioCategory() },
            onSongLongPress = { },
            favoriteStationIds = favoriteStationIds,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Same search pipeline as the landscape headunit (MusicViewModel.onSearchQueryChanged):
    // library + radio stations instantly, online streams debounced.
    val query by viewModel.searchQuery.collectAsState()
    val setQuery: (String) -> Unit = { viewModel.onSearchQueryChanged(it) }
    val localResults by viewModel.searchSongs.collectAsState()
    val onlineResults by viewModel.onlineSearchSongs.collectAsState()
    val stationResults by viewModel.searchStations.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    var isSearchActive by remember { mutableStateOf(query.isNotEmpty()) }
    var selectedScopeIndex by remember { mutableIntStateOf(0) } // 0: All (online + library), 1: Library
    // Leaving Search clears the query so the shared song list is not left filtered.
    DisposableEffect(Unit) { onDispose { viewModel.onSearchQueryChanged("") } }
    // Real search history: a query is remembered when one of its results is played.
    val context = androidx.compose.ui.platform.LocalContext.current
    val historyPrefs = remember { context.getSharedPreferences("mobile_search", android.content.Context.MODE_PRIVATE) }
    var searchHistory by remember {
        mutableStateOf(historyPrefs.getString("recent", "").orEmpty().split('\n').filter { it.isNotBlank() })
    }
    fun saveHistory(list: List<String>) {
        searchHistory = list
        historyPrefs.edit().putString("recent", list.joinToString("\n")).apply()
    }
    val rememberQuery: () -> Unit = {
        val term = query.trim()
        if (term.isNotEmpty()) saveHistory((listOf(term) + searchHistory.filterNot { it.equals(term, ignoreCase = true) }).take(8))
    }

    // Favorited categories float to the top of the grid
    val categories = remember(radioCategories, favoriteCategoryIds) {
        radioCategories.sortedByDescending { favoriteCategoryIds.contains(it.id) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── LARGE TITLE 'SEARCH' ──────────────────────────────────────────────
        item {
            Text(
                text = "Search",
                color = MobileTheme.text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        // ─── LIQUID GLASS CAPSULE SEARCH BAR ───────────────────────────────────
        item {
            MobileGlassSearchBar(
                query = query,
                onQueryChange = {
                    setQuery(it)
                    if (it.isNotEmpty()) isSearchActive = true
                },
                onClear = { setQuery("") },
                placeholderText = "Search songs, artists, stations",
                isActive = isSearchActive,
                onActiveChange = { isSearchActive = it }
            )
        }

        // ─── SEGMENTED SCOPE PILL (All / Library) ─────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                MobileGlassSegmentedPill(
                    items = listOf("All", "Library"),
                    selectedIndex = selectedScopeIndex,
                    onItemSelected = { selectedScopeIndex = it }
                )
            }
        }

        // ─── CONDITIONAL CONTENT: SEARCH RESULTS VS IDLE BROWSE ────────────────
        if (query.isNotBlank()) {
            val showOnline = selectedScopeIndex == 0
            val nothingYet = localResults.isEmpty() && (!showOnline || (onlineResults.isEmpty() && stationResults.isEmpty()))

            if (showOnline && (isSearchingOnline || onlineResults.isNotEmpty())) {
                item { SearchSectionHeader("Songs") }
                if (isSearchingOnline && onlineResults.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MobileTheme.accent,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
                items(onlineResults, key = { "online_" + it.id }) { song ->
                    MobileSongRow(
                        song = song,
                        isCurrent = song.path == currentSong?.path,
                        // Seed-only playlist: the app then starts similar-song radio instead of queueing the
                        // other search hits (same title / same movie).
                        onClick = { rememberQuery(); viewModel.play(song, playlist = listOf(song)) }
                    )
                }
            }

            if (localResults.isNotEmpty()) {
                item { SearchSectionHeader("In Your Library") }
                items(localResults, key = { "local_" + it.path }) { song ->
                    MobileSongRow(
                        song = song,
                        subtitle = "${song.artist} • ${song.album}",
                        isCurrent = song.path == currentSong?.path,
                        onClick = { rememberQuery(); viewModel.play(song, playlist = localResults) }
                    )
                }
            }

            if (showOnline && stationResults.isNotEmpty()) {
                item { SearchSectionHeader("Radio Stations") }
                items(stationResults, key = { "station_" + it.id }) { station ->
                    val stationSong = remember(station.id) { station.toSong() }
                    MobileSongRow(
                        song = stationSong,
                        subtitle = "${station.genre} • ${station.country}",
                        isCurrent = currentSong?.path == station.streamUrl,
                        onClick = { rememberQuery(); viewModel.playRadioStation(station) }
                    )
                }
            }

            if (nothingYet && !(showOnline && isSearchingOnline)) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results for \"$query\"",
                            color = MobileTheme.textTertiary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            // IDLE BROWSE STATE: RECENT SEARCHES & CATEGORY GRID

            // Recently Searched Tokens
            if (searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recently Searched",
                            color = MobileTheme.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LiquidButton(
                            onClick = { saveHistory(emptyList()) },
                            surfaceColor = MobileTheme.text.copy(alpha = 0.10f),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Clear",
                                color = MobileTheme.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                items(searchHistory) { term ->
                    RecentSearchItemRow(
                        term = term,
                        onSelect = {
                            setQuery(term)
                            isSearchActive = true
                        },
                        onRemove = {
                            saveHistory(searchHistory.filter { it != term })
                        }
                    )
                }
            }

            // Browse Categories Header
            item {
                Text(
                    text = "Radio & Genres",
                    color = MobileTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            // 2-Column Categories Grid
            val chunkedCategories = categories.chunked(2)
            items(chunkedCategories.size) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunkedCategories[rowIndex].forEachIndexed { col, category ->
                        RadioCategoryTile(
                            category = category,
                            index = rowIndex * 2 + col,
                            isFavorite = favoriteCategoryIds.contains(category.id),
                            onFavoriteClick = { viewModel.toggleCategoryFavorite(category.id) },
                            onClick = { viewModel.openRadioCategory(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (chunkedCategories[rowIndex].size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentSearchItemRow(
    term: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MobileTheme.textTertiary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = term,
            color = MobileTheme.text,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        LiquidIconButton(
            onClick = onRemove,
            size = 30.dp
        ) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = "Remove",
                tint = MobileTheme.text,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        color = MobileTheme.text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)
    )
}
