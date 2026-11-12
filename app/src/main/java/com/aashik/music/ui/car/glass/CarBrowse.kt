package com.aashik.music.ui.car.glass

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.RadioStation
import com.aashik.music.model.Song
import com.aashik.music.service.ExploreShelfItem
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.ui.mobile.screens.RadioCategoryTile
import com.aashik.music.viewmodel.MusicViewModel

private val StationFavoriteRed = Color(0xFFFF375F)

// ─── Search ──────────────────────────────────────────────────────────────────

/**
 * Unified search: online songs (tapping one seeds a similar-song radio, v0.4.2),
 * local library tracks, and radio / community streams.
 */
@Composable
internal fun CarSearchResults(
    viewModel: MusicViewModel,
    currentSongId: String?,
    favoritePaths: Set<String>,
    bottomInset: Dp,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val onlineSongs by viewModel.onlineSearchSongs.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    val localSongs by viewModel.searchSongs.collectAsState()
    val stations by viewModel.searchStations.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    if (onlineSongs.isEmpty() && localSongs.isEmpty() && stations.isEmpty() && !isSearchingOnline) {
        CarEmptyState(
            icon = Icons.Rounded.SearchOff,
            title = "No results",
            message = "Nothing matched in your library, online songs or radio stations.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = CarDims.ContentGutter - 8.dp, end = CarDims.ContentGutter, bottom = bottomInset)
    ) {
        if (localSongs.isNotEmpty()) {
            item(key = "h_local") { SectionHeader("On this device", "${localSongs.size}") }
            items(localSongs, key = { "l_${it.id}" }) { song ->
                CarSongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    isFavorite = favoritePaths.contains(song.path),
                    onClick = { viewModel.play(song, playlist = localSongs) },
                    onLongPress = { onSongActions(song) },
                    onFavorite = { viewModel.toggleFavorite(song.path) },
                    onMore = { onSongActions(song) }
                )
            }
        }
        if (isSearchingOnline || onlineSongs.isNotEmpty()) {
            item(key = "h_online") {
                SectionHeader("Online songs", if (isSearchingOnline) null else "${onlineSongs.size}", loading = isSearchingOnline)
            }
            items(onlineSongs, key = { "o_${it.id}" }) { song ->
                CarSongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    isFavorite = favoritePaths.contains(song.path),
                    // Seed-only playlist: the player builds a similar-song radio from this track.
                    onClick = { viewModel.play(song, playlist = listOf(song)) },
                    onLongPress = { onSongActions(song) },
                    onFavorite = { viewModel.toggleFavorite(song.path) },
                    onMore = { onSongActions(song) }
                )
            }
        }
        if (stations.isNotEmpty()) {
            item(key = "h_stations") { SectionHeader("Radio & streams", "${stations.size}") }
            items(stations, key = { "s_${it.id}" }) { station ->
                CarStationRow(
                    station = station,
                    isPlaying = station.streamUrl == currentSong?.path,
                    isFavorite = favoriteStationIds.contains(station.id),
                    onPlay = { viewModel.playRadioStation(station) },
                    onFavorite = { viewModel.toggleRadioFavorite(station.id) },
                    onStartRadio = { viewModel.startRadioForSong(station.toSong()) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?, loading: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MobileTheme.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (loading) {
            CircularProgressIndicator(color = MobileTheme.textSecondary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Searching online…", color = MobileTheme.textSecondary, fontSize = 13.sp)
        } else if (trailing != null) {
            Text(trailing, color = MobileTheme.textTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Radio & Genres ──────────────────────────────────────────────────────────

/** Genre grid with favourites pinned on top; a genre opens its shelves + live stations. */
@Composable
internal fun CarRadioContent(
    viewModel: MusicViewModel,
    bottomInset: Dp,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedRadioCategory.collectAsState()
    if (selectedCategory != null) {
        CarGenreDetail(viewModel = viewModel, bottomInset = bottomInset, onSongActions = onSongActions, modifier = modifier)
        return
    }

    val radioStations by viewModel.radioStations.collectAsState()
    val genreCategories by viewModel.genreCategories.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()
    val favoriteCategoryIds by viewModel.favoriteCategoryIds.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val favCategories = remember(genreCategories, favoriteCategoryIds) { genreCategories.filter { favoriteCategoryIds.contains(it.id) } }
    val favStations = remember(radioStations, favoriteStationIds) { radioStations.filter { favoriteStationIds.contains(it.id) } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(start = CarDims.ContentGutter, end = CarDims.ContentGutter, top = 4.dp, bottom = bottomInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (favCategories.isNotEmpty() || favStations.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_fav") {
                GridHeader("Favorites", "${favCategories.size + favStations.size} saved")
            }
            itemsIndexed(favCategories, key = { _, it -> "fav_${it.id}" }) { index, genre ->
                RadioCategoryTile(
                    category = genre,
                    index = index,
                    isFavorite = true,
                    onFavoriteClick = { viewModel.toggleCategoryFavorite(genre.id) },
                    onClick = { viewModel.openRadioCategory(genre) }
                )
            }
            itemsIndexed(favStations, key = { _, it -> "fav_st_${it.id}" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { _, station ->
                CarStationRow(
                    station = station,
                    isPlaying = station.streamUrl == currentSong?.path,
                    isFavorite = true,
                    onPlay = { viewModel.playRadioStation(station) },
                    onFavorite = { viewModel.toggleRadioFavorite(station.id) },
                    onStartRadio = { viewModel.startRadioForSong(station.toSong()) }
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "h_all") { GridHeader("All genres & moods", null) }
        itemsIndexed(genreCategories, key = { _, it -> it.id }) { index, genre ->
            RadioCategoryTile(
                category = genre,
                index = index,
                isFavorite = favoriteCategoryIds.contains(genre.id),
                onFavoriteClick = { viewModel.toggleCategoryFavorite(genre.id) },
                onClick = { viewModel.openRadioCategory(genre) }
            )
        }
    }
}

@Composable
private fun GridHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MobileTheme.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = MobileTheme.textTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Genre drill-down: Songs, Featured playlists, Albums, Community playlists and Live stations shelves. */
@Composable
private fun CarGenreDetail(
    viewModel: MusicViewModel,
    bottomInset: Dp,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = viewModel.selectedRadioCategory.collectAsState().value ?: return
    val details by viewModel.exploreCategoryDetails.collectAsState()
    val isLoading by viewModel.isLoadingCategorySongs.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val stations = remember(radioStations, category.id) { radioStations.filter { it.categoryId == category.id } }

    if (isLoading || (details == null && stations.isEmpty())) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MobileTheme.accent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text("Loading ${category.title}…", color = MobileTheme.textSecondary, fontSize = 15.sp)
            }
        }
        return
    }
    val d = details
    val nothing = d?.songs.isNullOrEmpty() && d?.featuredPlaylists.isNullOrEmpty() &&
        d?.albums.isNullOrEmpty() && d?.communityPlaylists.isNullOrEmpty() && stations.isEmpty()
    if (nothing) {
        CarEmptyState(
            icon = Icons.Rounded.CloudOff,
            title = "Couldn't load ${category.title}",
            message = "Check your connection and try again.",
            actionLabel = "Retry",
            onAction = { viewModel.openRadioCategory(category) },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!d?.songs.isNullOrEmpty()) {
            val songs = d!!.songs
            item(key = "songs") {
                Shelf("Songs") {
                    items(songs, key = { it.id }) { song ->
                        ShelfCard(
                            title = song.title,
                            subtitle = song.artist,
                            art = song.artworkUrl ?: song.path,
                            onClick = { viewModel.play(song, playlist = songs) },
                            onLongPress = { onSongActions(song) }
                        )
                    }
                }
            }
        }
        listOf(
            "Featured playlists" to d?.featuredPlaylists,
            "Albums" to d?.albums,
            "Community playlists" to d?.communityPlaylists
        ).forEach { (title, list) ->
            if (!list.isNullOrEmpty()) {
                item(key = title) {
                    Shelf(title) {
                        items(list, key = { it.id }) { item: ExploreShelfItem ->
                            ShelfCard(
                                title = item.title,
                                subtitle = item.subtitle,
                                art = item.artworkUrl,
                                onClick = { viewModel.playShelfItem(item) }
                            )
                        }
                    }
                }
            }
        }
        if (stations.isNotEmpty()) {
            item(key = "h_live") {
                Text(
                    "Live radio stations",
                    color = MobileTheme.text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = CarDims.ContentGutter, top = 10.dp, bottom = 2.dp)
                )
            }
            items(stations, key = { "live_${it.id}" }) { station ->
                CarStationRow(
                    station = station,
                    isPlaying = station.streamUrl == currentSong?.path,
                    isFavorite = favoriteStationIds.contains(station.id),
                    onPlay = { viewModel.playRadioStation(station) },
                    onFavorite = { viewModel.toggleRadioFavorite(station.id) },
                    onStartRadio = { viewModel.startRadioForSong(station.toSong()) },
                    modifier = Modifier.padding(horizontal = CarDims.ContentGutter)
                )
            }
        }
    }
}

@Composable
private fun Shelf(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            color = MobileTheme.text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = CarDims.ContentGutter, top = 10.dp, bottom = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = CarDims.ContentGutter),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfCard(title: String, subtitle: String, art: String, onClick: () -> Unit, onLongPress: (() -> Unit)? = null) {
    Column(
        Modifier
            .width(168.dp)
            .clip(CarArtShape8)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        AlbumArtImage(path = art, artworkUrl = art, size = 168.dp, borderRadius = 8.dp, sharpSize = 256)
        Spacer(Modifier.height(8.dp))
        Text(title.ifBlank { "Untitled" }, color = MobileTheme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = MobileTheme.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Radio / stream row: artwork, name, genre • bitrate, play, start-radio and favourite. */
@Composable
internal fun CarStationRow(
    station: RadioStation,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onStartRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(CarCardShape)
            .background(if (isPlaying) carSelectedFill else MobileTheme.panel)
            .clickable(onClick = onPlay)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val art = station.imageUrl.ifBlank { RadioStation.CATEGORIES.firstOrNull { it.id == station.categoryId }?.imageUrl.orEmpty() }
        AlbumArtImage(path = art.ifBlank { station.streamUrl }, artworkUrl = art.ifBlank { null }, size = 52.dp, borderRadius = 8.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, color = if (isPlaying) MobileTheme.accent else MobileTheme.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${station.genre} • ${station.bitrate}", color = MobileTheme.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CarIconButton(Icons.Rounded.Podcasts, "Start radio from this station", onClick = onStartRadio, size = 48.dp, glyph = 22.dp, tint = MobileTheme.textSecondary)
        CarIconButton(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (isFavorite) "Remove from favorites" else "Add to favorites",
            onClick = onFavorite,
            size = 48.dp,
            glyph = 22.dp,
            tint = if (isFavorite) StationFavoriteRed else MobileTheme.textTertiary
        )
        CarIconButton(Icons.Rounded.PlayArrow, "Play station", onClick = onPlay, size = 48.dp, glyph = 26.dp, tint = MobileTheme.accent)
    }
}
