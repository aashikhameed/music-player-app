package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.ModernVerticalScrollbar
import com.aashik.music.ui.components.LiveAnimatedEqualizer
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel

private val FavoriteRed = Color(0xFFFF375F)

/**
 * Content pane: large title row (with inline search / drill-down back), then the songs,
 * favourites, folders, radio or search results for the current sidebar destination.
 */
@Composable
internal fun CarLibraryPane(
    viewModel: MusicViewModel,
    songs: List<Song>,
    selectedTab: LibraryTab,
    selectedFolder: String?,
    searchQuery: String,
    isSearchOpen: Boolean,
    onCloseSearch: () -> Unit,
    favoritePaths: Set<String>,
    currentSongId: String?,
    bottomInset: Dp,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val selectedRadioCategory by viewModel.selectedRadioCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gridState = rememberLazyGridState()

    // ── Scroll to the playing song on first load, and whenever the capsule asks for it ──
    var hasScrolledToInitialSong by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isLoading, songs, currentSongId) {
        if (!isLoading && songs.isNotEmpty() && currentSongId != null && !hasScrolledToInitialSong) {
            val index = songs.indexOfFirst { it.id == currentSongId }
            if (index >= 0) {
                gridState.scrollToItem(index)
                hasScrolledToInitialSong = true
            }
        }
    }
    val scrollToIndex by viewModel.scrollToIndex.collectAsState()
    LaunchedEffect(scrollToIndex) {
        scrollToIndex?.let { index ->
            if (index in songs.indices) gridState.scrollToItem(index)
            viewModel.clearScrollToIndex()
        }
    }

    val activeTab = if (selectedTab == LibraryTab.MORE) LibraryTab.ALL_SONGS else selectedTab
    val showSearch = isSearchOpen || searchQuery.isNotBlank()
    val folderName = selectedFolder?.let { path -> folders.firstOrNull { it.path == path }?.name ?: path.substringAfterLast('/') }

    val (title, subtitle) = when {
        showSearch -> "Search" to "Your library, online songs and radio"
        activeTab == LibraryTab.FAVORITES -> "Favorites" to countLabel(songs.size, "song")
        activeTab == LibraryTab.FOLDERS && folderName != null -> folderName to countLabel(songs.size, "song")
        activeTab == LibraryTab.FOLDERS -> "Folders" to countLabel(folders.size, "folder")
        activeTab == LibraryTab.RADIO && selectedRadioCategory != null -> selectedRadioCategory!!.title to "Genre"
        activeTab == LibraryTab.RADIO -> "Radio & Genres" to "Live stations, moods and genres"
        else -> "Songs" to countLabel(songs.size, "song")
    }
    val onBack: (() -> Unit)? = when {
        showSearch -> null
        activeTab == LibraryTab.FOLDERS && selectedFolder != null -> { { viewModel.closeFolder() } }
        activeTab == LibraryTab.RADIO && selectedRadioCategory != null -> { { viewModel.closeRadioCategory() } }
        else -> null
    }

    Column(modifier.statusBarsPadding()) {
        TitleRow(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            isSearchOpen = showSearch,
            searchQuery = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onCloseSearch = onCloseSearch,
            trailing = if (!showSearch && activeTab == LibraryTab.RADIO && selectedRadioCategory == null) {
                { CarTextPillButton("Surprise Me", Icons.Rounded.Shuffle) { viewModel.playRandomOrNextRadioStation() } }
            } else null
        )

        val contentKey = when {
            showSearch && searchQuery.isNotBlank() -> "search"
            showSearch -> "searchHint"
            activeTab == LibraryTab.RADIO -> "radio"
            activeTab == LibraryTab.FOLDERS && selectedFolder == null -> "folders"
            else -> "songs:$activeTab:$selectedFolder"
        }
        AnimatedContent(
            targetState = contentKey,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "carContent",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { key ->
            when {
                key == "search" -> CarSearchResults(
                    viewModel = viewModel,
                    currentSongId = currentSongId,
                    favoritePaths = favoritePaths,
                    bottomInset = bottomInset,
                    onSongActions = onSongActions,
                    modifier = Modifier.fillMaxSize()
                )
                key == "searchHint" -> CarEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Search your music",
                    message = "Find songs on this device, online tracks and radio stations.",
                    modifier = Modifier.fillMaxSize()
                )
                key == "radio" -> CarRadioContent(
                    viewModel = viewModel,
                    bottomInset = bottomInset,
                    onSongActions = onSongActions,
                    modifier = Modifier.fillMaxSize()
                )
                key == "folders" -> FolderGrid(
                    folders = folders,
                    columns = gridColumns,
                    bottomInset = bottomInset,
                    onOpen = { viewModel.openFolder(it.path) },
                    onScan = { viewModel.loadSongs() },
                    modifier = Modifier.fillMaxSize()
                )
                else -> SongGrid(
                    viewModel = viewModel,
                    songs = songs,
                    columns = gridColumns,
                    gridState = gridState,
                    currentSongId = currentSongId,
                    favoritePaths = favoritePaths,
                    isFavoritesTab = activeTab == LibraryTab.FAVORITES,
                    bottomInset = bottomInset,
                    onSongActions = onSongActions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun countLabel(n: Int, noun: String): String = if (n == 1) "1 $noun" else "$n ${noun}s"

// ─── Title row ───────────────────────────────────────────────────────────────

@Composable
private fun TitleRow(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    isSearchOpen: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    trailing: (@Composable () -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(start = if (onBack != null) 12.dp else CarDims.ContentGutter, end = CarDims.ContentGutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack, glyph = 26.dp)
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MobileTheme.text,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, color = MobileTheme.textSecondary, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1)
        }
        Spacer(Modifier.width(16.dp))
        if (isSearchOpen) {
            SearchField(query = searchQuery, onQueryChange = onQueryChange, onClose = onCloseSearch)
        } else {
            trailing?.invoke()
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier
            .width(460.dp)
            .height(CarDims.Target)
            .background(carIdleFill, CarSearchShape)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarGlyph(Icons.Rounded.Search, null, MobileTheme.textSecondary, 22.dp)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Songs, artists, stations", color = MobileTheme.textTertiary, fontSize = 17.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = MobileTheme.text, fontSize = 17.sp),
                cursorBrush = SolidColor(MobileTheme.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        CarIconButton(
            Icons.Rounded.Close,
            if (query.isEmpty()) "Close search" else "Clear search",
            onClick = { if (query.isEmpty()) onClose() else onQueryChange("") },
            size = 48.dp,
            glyph = 20.dp,
            tint = MobileTheme.textSecondary
        )
    }
}

/** Capsule text button with optional leading glyph (e.g. "Surprise Me"). */
@Composable
internal fun CarTextPillButton(label: String, icon: ImageVector? = null, accent: Boolean = true, onClick: () -> Unit) {
    val tint = if (accent) MobileTheme.accent else MobileTheme.text
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CarPillShape)
            .background(if (accent) carSelectedFill else carIdleFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            CarGlyph(icon, null, tint, 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ─── Songs ───────────────────────────────────────────────────────────────────

@Composable
private fun SongGrid(
    viewModel: MusicViewModel,
    songs: List<Song>,
    columns: Int,
    gridState: LazyGridState,
    currentSongId: String?,
    favoritePaths: Set<String>,
    isFavoritesTab: Boolean,
    bottomInset: Dp,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        if (isFavoritesTab) {
            CarEmptyState(
                icon = Icons.Rounded.FavoriteBorder,
                title = "No favorites yet",
                message = "Tap the heart on any song to keep it here.",
                modifier = modifier
            )
        } else {
            CarScanEmptyState(viewModel = viewModel, modifier = modifier)
        }
        return
    }
    Row(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.coerceIn(1, 3)),
            state = gridState,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = CarDims.ContentGutter - 8.dp, end = 4.dp, bottom = bottomInset),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = songs, key = { it.id }, contentType = { "carSong" }) { song ->
                val isCurrent = song.id == currentSongId
                CarSongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isFavorite = favoritePaths.contains(song.path),
                    compact = columns >= 3,
                    playingIndicator = if (isCurrent) { { CurrentSongEq(viewModel) } } else null,
                    onClick = { viewModel.play(song, playlist = songs) },
                    onLongPress = { onSongActions(song) },
                    onFavorite = { viewModel.toggleFavorite(song.path) },
                    onMore = { onSongActions(song) }
                )
            }
        }
        if (songs.size > 5) {
            ModernVerticalScrollbar(
                gridState = gridState,
                totalItemCount = songs.size,
                trackColor = MobileTheme.text.copy(alpha = 0.06f),
                thumbColor = MobileTheme.textTertiary,
                modifier = Modifier.fillMaxHeight().padding(top = 4.dp, bottom = bottomInset, end = 4.dp)
            )
        }
    }
}

/** Leaf: only the playing row collects play/pause state. */
@Composable
private fun CurrentSongEq(viewModel: MusicViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    LiveAnimatedEqualizer(isPlaying = isPlaying, barColor = MobileTheme.accent)
}

/**
 * 64 dp song row (tablet/mobile list style): 48 dp artwork, title over artist • album,
 * duration, favourite heart and "⋯" (song actions). Hairline divider starts under the text.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CarSongRow(
    song: Song,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavorite: (() -> Unit)?,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    subtitle: String = listOf(song.artist.ifBlank { "Unknown Artist" }, song.album).filter { it.isNotBlank() }.joinToString(" • "),
    playingIndicator: (@Composable () -> Unit)? = null
) {
    Box(modifier.fillMaxWidth().height(CarDims.RowHeight)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(CarArtShape8)
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(start = 8.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(path = song.path, artworkUrl = song.artworkUrl, size = CarDims.RowArt, borderRadius = 8.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title.ifBlank { "Untitled" },
                    color = if (isCurrent) MobileTheme.accent else MobileTheme.text,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = MobileTheme.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (playingIndicator != null) {
                Spacer(Modifier.width(8.dp))
                playingIndicator()
            }
            if (!compact && song.duration > 0) {
                Spacer(Modifier.width(12.dp))
                Text(carFormatTime(song.duration), color = MobileTheme.textTertiary, fontSize = 13.sp, maxLines = 1)
            }
            if (onFavorite != null) {
                CarIconButton(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFavorite) "Remove from favorites" else "Add to favorites",
                    onClick = onFavorite,
                    size = 48.dp,
                    glyph = 22.dp,
                    tint = if (isFavorite) FavoriteRed else MobileTheme.textTertiary
                )
            }
            if (onMore != null) {
                CarIconButton(Icons.Rounded.MoreHoriz, "More actions", onClick = onMore, size = 48.dp, glyph = 22.dp, tint = MobileTheme.textSecondary)
            }
        }
        // Hairline under the text, not under the artwork
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp + CarDims.RowArt + 14.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(carHairline)
        )
    }
}

// ─── Folders ─────────────────────────────────────────────────────────────────

@Composable
private fun FolderGrid(
    folders: List<MusicFolder>,
    columns: Int,
    bottomInset: Dp,
    onOpen: (MusicFolder) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        CarEmptyState(
            icon = Icons.Rounded.Folder,
            title = "No folders found",
            message = "Rescan the device to index your music folders.",
            actionLabel = "Rescan",
            onAction = onScan,
            modifier = modifier
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceIn(2, 3)),
        modifier = modifier,
        contentPadding = PaddingValues(start = CarDims.ContentGutter, end = CarDims.ContentGutter, top = 4.dp, bottom = bottomInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = folders, key = { it.path }, contentType = { "carFolder" }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(CarCardShape)
                    .background(MobileTheme.panel)
                    .clickable { onOpen(folder) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(52.dp).background(carSelectedFill, CarArtShape8), contentAlignment = Alignment.Center) {
                    CarGlyph(Icons.Rounded.Folder, null, MobileTheme.accent, 28.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, color = MobileTheme.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(countLabel(folder.songCount, "song"), color = MobileTheme.textSecondary, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

// ─── Empty / permission states ───────────────────────────────────────────────

@Composable
internal fun CarEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(420.dp)) {
            Box(Modifier.size(72.dp).background(carIdleFill, CarCircle), contentAlignment = Alignment.Center) {
                CarGlyph(icon, null, MobileTheme.textSecondary, 34.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = MobileTheme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MobileTheme.textSecondary, fontSize = 15.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                CarTextPillButton(actionLabel, onClick = onAction)
            }
        }
    }
}

/** Empty library: rescan, or request the audio permission first if it was never granted. */
@Composable
private fun CarScanEmptyState(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioPermission = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_AUDIO
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, audioPermission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.loadSongs()
    }
    CarEmptyState(
        icon = Icons.Rounded.MusicOff,
        title = if (hasPermission) "No songs found" else "Allow access to music",
        message = if (hasPermission) "Add music to this device, then rescan the library."
        else "Grant audio access so the player can find songs on this device.",
        actionLabel = if (hasPermission) "Rescan library" else "Allow access",
        onAction = { if (hasPermission) viewModel.loadSongs() else launcher.launch(audioPermission) },
        modifier = modifier
    )
}
