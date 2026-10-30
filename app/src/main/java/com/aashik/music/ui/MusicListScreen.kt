package com.aashik.music.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.aashik.music.theme.AppGradients
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import com.aashik.music.model.CategorySection
import com.aashik.music.service.DownloadStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.RadioCategory
import com.aashik.music.model.RadioStation
import com.aashik.music.model.Song
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.utils.formatTime
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import java.io.File
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.rounded.Shuffle

private val PortraitSongCardShape = RoundedCornerShape(20.dp)
private val LandscapeFullSongShape = RoundedCornerShape(18.dp)
private val HeaderCapsuleShape = RoundedCornerShape(14.dp)
private val PillShape10 = RoundedCornerShape(10.dp)
private val PeekPillShape = RoundedCornerShape(12.dp)
private val StatusCapsuleShape = RoundedCornerShape(12.dp)
private val RadioCardShape = RoundedCornerShape(16.dp)
private val GenreChipShape = RoundedCornerShape(10.dp)
private val LiveBadgeShape = RoundedCornerShape(6.dp)
private val MoodCardShape = RoundedCornerShape(16.dp)
private val MoodStripeShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
private val BackPillShape = RoundedCornerShape(10.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun MusicListScreen(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentSongId = currentSong?.id
    // NOTE: isPlaying is NOT collected here — each SongCard derives its own
    // isPlaying = (song.id == currentSongId), avoiding a full-screen recompose
    // on every play/pause toggle.
    // NOTE: positionMs / durationMs / progress are intentionally NOT collected here.
    // Reading them in this scope would recompose the entire screen (3 song cards + header)
    // every 250ms. They are read inside PortraitSeekBarSection below, which scopes
    // recompositions to only that small widget.
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val listState = rememberLazyGridState()
    val scrollToIndex by viewModel.scrollToIndex.collectAsState()

    // Scroll to the playing/highlighted song on initial load
    var hasScrolledToInitialSong by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isLoading, songs, currentSongId) {
        if (!isLoading && songs.isNotEmpty() && currentSongId != null && !hasScrolledToInitialSong) {
            val index = songs.indexOfFirst { it.id == currentSongId }
            if (index >= 0) {
                listState.scrollToItem(index)
                hasScrolledToInitialSong = true
            }
        }
    }

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isDriveModeOpen by viewModel.isDriveModeOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val hideBottomBar by viewModel.hideBottomBar.collectAsState()
    var isBottomDockPeeked by rememberSaveable { mutableStateOf(false) }

    val userInteractionChannel = remember { kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED) }

    AutoHudInactivityWatcher(
        viewModel = viewModel,
        userInteractionChannel = userInteractionChannel
    )

    // Handle system back button
    BackHandler(enabled = isQueueOpen || isEqualizerOpen || isDriveModeOpen || isSettingsOpen || (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) || selectedTab == LibraryTab.RADIO) {
        if (isQueueOpen) {
            viewModel.closeQueue()
        } else if (isEqualizerOpen) {
            viewModel.closeEqualizer()
        } else if (isDriveModeOpen) {
            viewModel.closeDriveMode()
        } else if (isSettingsOpen) {
            viewModel.closeSettings()
        } else if (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
            viewModel.closeFolder()
        } else if (selectedTab == LibraryTab.RADIO) {
            val cat = viewModel.selectedRadioCategory.value
            if (cat != null) {
                viewModel.closeRadioCategory()
            } else {
                viewModel.selectTab(LibraryTab.ALL_SONGS)
            }
        }
    }

    if (isEqualizerOpen) {
        EqualizerScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeEqualizer() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (isDriveModeOpen) {
        DriveModeScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeDriveMode() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeSettings() },
            onOpenEqualizer = { viewModel.openEqualizer() },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LaunchedEffect(scrollToIndex) {
        scrollToIndex?.let { index ->
            if (index in songs.indices) {
                listState.scrollToItem(index)
            }
            viewModel.clearScrollToIndex()
        }
    }

    var songForActionMenu by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val isActionSongDownloaded = songForActionMenu?.let { viewModel.isSongDownloaded(it.id) } == true

    // Song Actions Dialog ("Play Next", "Add to Queue", "Favorite", "Download", "Delete")
    SongActionDialog(
        song = songForActionMenu,
        isFavorite = songForActionMenu?.let { favoritePaths.contains(it.path) } == true,
        isDownloaded = isActionSongDownloaded,
        onPlayNext = { song -> viewModel.playNext(song) },
        onAddToQueue = { song -> viewModel.addToQueue(song) },
        onToggleFavorite = { path -> viewModel.toggleFavorite(path) },
        onStartRadio = { song -> viewModel.startRadioForSong(song) },
        onDownload = { song -> viewModel.downloadSong(song) },
        onDeleteRequest = { song -> songToDelete = song },
        onDismiss = { songForActionMenu = null }
    )

    // Reusable Delete Confirmation Dialog
    DeleteSongDialog(
        song = songToDelete,
        onConfirm = { song ->
            viewModel.deleteSong(song)
            songToDelete = null
        },
        onDismiss = { songToDelete = null }
    )

    // Automotive Equalizer & Acoustic Separation DSP Dialog
    val isEqOpen by viewModel.isEqualizerDialogOpen.collectAsState()
    if (isEqOpen) {
        EqualizerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeEqualizerDialog() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.isNotEmpty()) {
                            userInteractionChannel.trySend(Unit)
                        }
                    }
                }
            }
    ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SongLoadingBar(viewModel)
                }
            } else {
                if (isPortrait) {
                    // Portrait Mobile Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Header Bar with In-Place Search
                        LibraryModeHeader(
                            selectedTab = selectedTab,
                            selectedFolder = selectedFolder,
                            songCount = songs.size,
                            folderCount = folders.size,
                            favoriteCount = favoritePaths.size,
                            radioCount = radioStations.size,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            isSearchOpen = isSearchOpen,
                            onToggleSearch = {
                                isSearchOpen = !isSearchOpen
                                if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                            },
                            onTabSelected = { viewModel.selectTab(it) },
                            onBackFolder = { viewModel.closeFolder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(brush = AppGradients.card(isActive = false), shape = PortraitSongCardShape)
                                .border(width = 1.dp, brush = AppGradients.border(isActive = false), shape = PortraitSongCardShape)
                                .padding(8.dp)
                        ) {
                            MediaLibraryContent(
                                viewModel = viewModel,
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
                                folders = folders,
                                songs = songs,
                                currentSongId = currentSongId,
                                favoritePaths = favoritePaths,
                                searchQuery = searchQuery,
                                gridState = listState,
                                columns = 1,
                                onSongLongPress = { songForActionMenu = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (!hideBottomBar || isBottomDockPeeked) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Portrait Seekbar + Controls — isolated composable so that
                            // 250ms position ticks only recompose this widget, not the song list.
                            PortraitSeekBarSection(
                                viewModel = viewModel,
                                currentSong = currentSong,
                                isSearchOpen = isSearchOpen,
                                onToggleSearch = {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))

                            BottomDockPeekPill(
                                viewModel = viewModel,
                                onExpand = { isBottomDockPeeked = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // ─────────────────────────────────────────────────────────────────────────
                    // Landscape Infotainment Layout — optimised for 1280×720 @ 160 dpi
                    // 1dp = 1px; full canvas available for automotive touch targets
                    // Layout budget: 720dp height, 1280dp width
                    //   Outer pad: h=10dp, v=8dp → inner: 1260dp × 704dp
                    //   Bottom bar: 80dp
                    //   Spacer:     8dp
                    //   Library:    704 - 80 - 8 = 616dp
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // ── Library Panel (songs + header) ──
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(brush = AppGradients.card(isActive = false), shape = LandscapeFullSongShape)
                                    .border(width = 1.dp, brush = AppGradients.border(isActive = false), shape = LandscapeFullSongShape)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                val canScroll = (selectedTab == LibraryTab.ALL_SONGS || selectedFolder != null) && songs.size > 5
                                if (canScroll) {
                                    ModernVerticalScrollbar(
                                        gridState = listState,
                                        totalItemCount = songs.size,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(start = if (canScroll) 6.dp else 0.dp)
                                ) {
                                    // Header: 46dp tall — tab chips + search icon + time/status
                                    LibraryModeHeader(
                                        viewModel = viewModel,
                                        selectedTab = selectedTab,
                                        selectedFolder = selectedFolder,
                                        songCount = songs.size,
                                        folderCount = folders.size,
                                        favoriteCount = favoritePaths.size,
                                        radioCount = radioStations.size,
                                        searchQuery = searchQuery,
                                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                        isSearchOpen = isSearchOpen,
                                        onToggleSearch = {
                                            isSearchOpen = !isSearchOpen
                                            if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                        },
                                        onTabSelected = { viewModel.selectTab(it) },
                                        onBackFolder = { viewModel.closeFolder() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                    )

                                    val gridColumns by viewModel.gridColumns.collectAsState()

                                    // Song/folder grid — optimized for automotive touch targets & weak GPUs
                                    MediaLibraryContent(
                                        viewModel = viewModel,
                                        selectedTab = selectedTab,
                                        selectedFolder = selectedFolder,
                                        folders = folders,
                                        songs = songs,
                                        currentSongId = currentSongId,
                                        favoritePaths = favoritePaths,
                                        searchQuery = searchQuery,
                                        gridState = listState,
                                        columns = gridColumns,
                                        onSongLongPress = { songForActionMenu = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (!hideBottomBar || isBottomDockPeeked) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // ── Automotive Bottom Dock — 80dp tall ──
                                AndroidAutoBottomBar(
                                    viewModel = viewModel,
                                    isSearchVisible = isSearchOpen,
                                    onToggleSearch = {
                                        isSearchOpen = !isSearchOpen
                                        if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                    },
                                    onCollapse = if (hideBottomBar) { { isBottomDockPeeked = false } } else null
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))

                                BottomDockPeekPill(
                                    viewModel = viewModel,
                                    onExpand = { isBottomDockPeeked = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay Slide-over Playback Queue Drawer
        if (isQueueOpen) {
            QueueDrawer(
                viewModel = viewModel,
                onClose = { viewModel.closeQueue() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Reusable Media Library Content Component.
 * Dynamically displays Folders Grid or Songs Grid with zero code duplication.
 */
@Composable
fun MediaLibraryContent(
    viewModel: MusicViewModel,
    selectedTab: LibraryTab,
    selectedFolder: String?,
    folders: List<MusicFolder>,
    songs: List<Song>,
    currentSongId: String?,
    favoritePaths: Set<String>,
    searchQuery: String,
    gridState: LazyGridState,
    columns: Int,
    onSongLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            searchQuery.isNotBlank() -> {
                UnifiedGlobalSearchResults(
                    viewModel = viewModel,
                    columns = columns,
                    currentSongId = currentSongId,
                    favoritePaths = favoritePaths,
                    onSongLongPress = onSongLongPress,
                    modifier = Modifier.fillMaxSize()
                )
            }
            selectedTab == LibraryTab.RADIO -> {
                RadioExploreContent(
                    viewModel = viewModel,
                    columns = columns,
                    onSongLongPress = onSongLongPress,
                    modifier = Modifier.fillMaxSize()
                )
            }
            selectedTab == LibraryTab.FOLDERS && selectedFolder == null -> {
                if (folders.isEmpty()) {
                    EmptyFoldersView(modifier = Modifier.fillMaxSize())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(
                            items = folders,
                            key = { it.path },
                            contentType = { "FolderCard" }
                        ) { folder ->
                            val onFolderClick = remember(folder.path) { { viewModel.openFolder(folder.path) } }
                            FolderCard(
                                folder = folder,
                                onClick = onFolderClick
                            )
                        }
                    }
                }
            }
            selectedTab == LibraryTab.FAVORITES && songs.isEmpty() -> {
                EmptyFavoritesView(modifier = Modifier.fillMaxSize())
            }
            else -> {
                if (songs.isEmpty()) {
                    EmptySongsView(
                        isSearching = searchQuery.isNotBlank(),
                        onScanClick = { viewModel.loadSongs() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(
                                items = songs,
                                key = { it.id },
                                contentType = { "SongCard" }
                            ) { song ->
                                val isPlaying = song.id == currentSongId
                                val onCardClick = remember(song.id) { { viewModel.play(song, playlist = songs) } }
                                val onCardLongPress = remember(song.id) { { onSongLongPress(song) } }
                                val isFav = favoritePaths.contains(song.path)
                                val onFavClick = remember(song.path) { { viewModel.toggleFavorite(song.path) } }
                                SongCard(
                                    song = song,
                                    isPlaying = isPlaying,
                                    onClick = onCardClick,
                                    onLongPress = onCardLongPress,
                                    isFavorite = isFav,
                                    onFavoriteClick = onFavClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Community Streams & Radio Exploration View (Built like BitChord Explore Screen).
 * Features BitChord-style Mood & Genre category sleeves, instant Favorites filter toggle,
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

    val categorySongs by viewModel.categorySongs.collectAsState()
    val isLoadingCategorySongs by viewModel.isLoadingCategorySongs.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Favorites Only" Quick Filter Toggle Button
                val totalFavCount = favoriteCategoryIds.size + favoriteStationIds.size
                Surface(
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    shape = PillShape10,
                    color = if (showFavoritesOnly) FavoriteHeartRed.copy(alpha = 0.22f)
                            else if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Filter Favorites",
                            tint = if (showFavoritesOnly) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showFavoritesOnly) "Favorites ($totalFavCount)" else "Favorite Streams",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showFavoritesOnly) FavoriteHeartRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Random Radio Quick-Play Button
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
                // Genres & Styles Grid (Text-only cards, no images, with favorite icon)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.coerceAtLeast(2)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
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
                                    color = FavoriteHeartRed
                                )
                                Text(
                                    text = "${favCategories.size + favStations.size} saved",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FavoriteHeartRed
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
                                Text(
                                    text = "${genreCategories.size} styles",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
            // ── Category Detail Screen (Live Streams, Stations & Downloads) ──
            val category = selectedCategory!!
            val stripeColor = Color(category.stripeColor)
            val activeRadioStationName by viewModel.activeRadioStationName.collectAsState()
            val categoryStations = remember(radioStations, category.id, favoriteStationIds, showFavoritesOnly) {
                radioStations.filter { station ->
                    val matchesCategory = station.categoryId == category.id
                    val matchesFav = !showFavoritesOnly || favoriteStationIds.contains(station.id)
                    matchesCategory && matchesFav
                }
            }

            // Category Header Toolbar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Back to Categories Pill
                    Surface(
                        onClick = { viewModel.closeRadioCategory() },
                        shape = BackPillShape,
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Categories",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Explore",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Dot / Stripe Indicator & Category Title
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = stripeColor, shape = PillShape10)
                    )
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Total items badge
                Surface(
                    shape = PillShape10,
                    color = stripeColor.copy(alpha = 0.16f),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isLoadingCategorySongs) "Loading..." else "${categoryStations.size + categorySongs.size} tracks",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = stripeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            if (isLoadingCategorySongs) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = stripeColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Loading ${category.title} streams...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (categoryStations.isEmpty() && categorySongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No songs or stations found in ${category.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Section 1: Live Radio Stations (if any)
                    if (categoryStations.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Live Radio Stations (${categoryStations.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(items = categoryStations, key = { it.id }) { station ->
                            val isPlaying = station.streamUrl == currentSongPath
                            val isFav = favoriteStationIds.contains(station.id)
                            RadioStationCard(
                                station = station,
                                isPlaying = isPlaying,
                                isFavorite = isFav,
                                onPlayClick = { viewModel.playRadioStation(station) },
                                onFavoriteClick = { viewModel.toggleRadioFavorite(station.id) },
                                onStartRadioClick = { viewModel.startRadioForSong(station.toSong()) }
                            )
                        }
                    }

                    // Section 2: Online Stream Songs for this Mood/Genre
                    if (categorySongs.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val sectionHeader = if (activeRadioStationName != null && activeRadioStationName?.contains("Radio:", ignoreCase = true) == true) {
                                "$activeRadioStationName (${categorySongs.size})"
                            } else {
                                "${category.title} Songs & Community Streams (${categorySongs.size})"
                            }
                            Text(
                                text = sectionHeader,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = if (categoryStations.isNotEmpty()) 12.dp else 4.dp, bottom = 2.dp)
                            )
                        }

                        items(items = categorySongs, key = { it.id }) { song ->
                            val isPlaying = song.id == currentSong?.id
                            val isFav = favoritePaths.contains(song.path)
                            val downloadStatus = downloadStates[song.id] ?: DownloadStatus.Idle

                            CategorySongRowCard(
                                song = song,
                                isPlaying = isPlaying,
                                isFavorite = isFav,
                                downloadStatus = downloadStatus,
                                onPlayClick = { viewModel.play(song, playlist = categorySongs) },
                                onLongPress = { onSongLongPress(song) },
                                onFavoriteClick = { viewModel.toggleFavorite(song.path) },
                                onStartRadioClick = { viewModel.startRadioForSong(song) },
                                onDownloadClick = { viewModel.downloadSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Genre Category Card Component (BitChord Sleeve Style).
 * Features solid colored left accent stripe, text-only styling (zero images),
 * and quick favorite toggle icon button instead of right arrow.
 */
@Composable
fun MoodGenreCard(
    category: RadioCategory,
    stationCount: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val stripeColor = Color(category.stripeColor)

    Surface(
        onClick = onClick,
        shape = MoodCardShape,
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Stripe (BitChord sleeve styling)
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(color = stripeColor, shape = MoodStripeShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Category Details (Text-only - zero images per user request)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Genre • Radio & Streams",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (stationCount > 0) {
                        Surface(
                            shape = LiveBadgeShape,
                            color = stripeColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "$stationCount live",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = stripeColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Favorite Icon Button instead of right arrow per user request
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Streaming song row card inside category drill-down view.
 * Features instant play, "Start Radio" queue generation, and direct download.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategorySongRowCard(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    downloadStatus: DownloadStatus,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: () -> Unit,
    onStartRadioClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.12f)
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }

    Surface(
        shape = RadioCardShape,
        color = cardBg,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail
            AlbumArtImage(
                path = song.path,
                artworkUrl = song.artworkUrl,
                size = 46.dp,
                borderRadius = 8.dp,
                isPlaying = isPlaying
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Track Title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.duration > 0L) {
                        Text(
                            text = "• ${formatTime(song.duration)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action 1: Start Radio button (generate algorithmic queue from this song)
            IconButton(
                onClick = onStartRadioClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = "Start Radio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Action 2: Download Offline button
            IconButton(
                onClick = onDownloadClick,
                enabled = downloadStatus !is DownloadStatus.Downloading && downloadStatus !is DownloadStatus.Downloaded,
                modifier = Modifier.size(34.dp)
            ) {
                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is DownloadStatus.Downloaded -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download Offline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Action 3: Favorite toggle
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Community Radio Station Card Component.
 * Optimized for Mali GPU with solid color styling and zero dynamic shape allocations.
 */
@Composable
fun RadioStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onStartRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.12f)
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }

    Surface(
        onClick = onPlayClick,
        shape = RadioCardShape,
        color = cardBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = PillShape10
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = LiveBadgeShape,
                        color = if (isPlaying) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isPlaying) "● ON AIR" else "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPlaying) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.genre,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                    Text(
                        text = " • ${station.bitrate} • ${station.country}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onStartRadioClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = "Start Radio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Unified Global Search Results Component.
 * Integrates Online Streaming Tracks, Local Songs, and Radio Stations in grouped sections.
 */
@Composable
fun UnifiedGlobalSearchResults(
    viewModel: MusicViewModel,
    @Suppress("UNUSED_PARAMETER") columns: Int,
    currentSongId: String?,
    favoritePaths: Set<String>,
    onSongLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val onlineSongs by viewModel.onlineSearchSongs.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    val localSongs by viewModel.searchSongs.collectAsState()
    val stations by viewModel.searchStations.collectAsState()
    val favoriteStationIds by viewModel.favoriteStationIds.collectAsState()

    val hasResults = onlineSongs.isNotEmpty() || localSongs.isNotEmpty() || stations.isNotEmpty()

    if (!hasResults && !isSearchingOnline) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No matches found across library and online streams",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Section 1: Online Streaming Songs (from BitChord engine)
        if (isSearchingOnline || onlineSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Online Stream Songs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isSearchingOnline) {
                        Text(
                            text = "Searching online...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "${onlineSongs.size} tracks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                items = onlineSongs,
                key = { it.id }
            ) { song ->
                val isPlaying = song.id == currentSongId
                SongCard(
                    song = song,
                    isPlaying = isPlaying,
                    onClick = { viewModel.play(song, playlist = onlineSongs) },
                    onLongPress = { onSongLongPress(song) },
                    isFavorite = favoritePaths.contains(song.path),
                    onFavoriteClick = { viewModel.toggleFavorite(song.path) }
                )
            }
        }

        // Section 2: Local Library Tracks
        if (localSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Local Library Tracks (${localSongs.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            items(
                items = localSongs,
                key = { it.id }
            ) { song ->
                val isPlaying = song.id == currentSongId
                SongCard(
                    song = song,
                    isPlaying = isPlaying,
                    onClick = { viewModel.play(song, playlist = localSongs) },
                    onLongPress = { onSongLongPress(song) },
                    isFavorite = favoritePaths.contains(song.path),
                    onFavoriteClick = { viewModel.toggleFavorite(song.path) }
                )
            }
        }

        // Section 3: Radio & Community Streams
        if (stations.isNotEmpty()) {
            item {
                Text(
                    text = "Community Radio & Streams (${stations.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            items(
                items = stations,
                key = { it.id }
            ) { station ->
                RadioStationCard(
                    station = station,
                    isPlaying = false,
                    isFavorite = favoriteStationIds.contains(station.id),
                    onPlayClick = { viewModel.playRadioStation(station) },
                    onFavoriteClick = { viewModel.toggleRadioFavorite(station.id) },
                    onStartRadioClick = { viewModel.startRadioForSong(station.toSong()) }
                )
            }
        }
    }
}

/**
 * Contextual Song Actions Modal Dialog ("Play Next", "Add to Queue", "Favorite", "Delete").
 */
@Composable
fun SongActionDialog(
    song: Song?,
    isFavorite: Boolean,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onStartRadio: (Song) -> Unit = {},
    isDownloaded: Boolean = false,
    onDownload: (Song) -> Unit = {},
    onDeleteRequest: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    if (song != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column {
                    Text(
                        text = song.title.ifEmpty { "Unknown Title" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play Next Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayNext(song)
                                onDismiss()
                            },
                        shape = PillShape10,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Play Next (Top of Queue)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Add to Queue Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddToQueue(song)
                                onDismiss()
                            },
                        shape = PillShape10,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Add to End of Queue",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Favorite Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleFavorite(song.path)
                                onDismiss()
                            },
                        shape = PillShape10,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Start Radio Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStartRadio(song)
                                onDismiss()
                            },
                        shape = PillShape10,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Podcasts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Start Radio (Related Streams)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Download Song Button (for Streams)
                    if (song.isStream() && song.duration > 0) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDownload(song)
                                    onDismiss()
                                },
                            shape = PillShape10,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = if (isDownloaded) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isDownloaded) "Downloaded (Offline Available)" else "Download Song for Offline",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDownloaded) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Delete Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onDeleteRequest(song)
                            },
                        shape = PillShape10,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Delete Song from Storage",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Reusable Confirmation Dialog for Deleting Songs.
 */
@Composable
fun DeleteSongDialog(
    song: Song?,
    onConfirm: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    if (song != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Song", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Are you sure you want to delete \"${song.title}\" from storage?") },
            confirmButton = {
                TextButton(onClick = { onConfirm(song) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LibraryModeHeader(
    viewModel: MusicViewModel? = null,
    selectedTab: LibraryTab,
    selectedFolder: String?,
    songCount: Int = 0,
    folderCount: Int = 0,
    favoriteCount: Int = 0,
    radioCount: Int = 0,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBackFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)
    val activeBorder = AppGradients.border(isActive = true)

    Box(
        modifier = modifier.height(48.dp)
    ) {
        if (isSearchOpen) {
            // Inline Search Text Field with Top-Right Status Clock
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(brush = capsuleGrad, shape = HeaderCapsuleShape)
                        .border(width = 1.dp, brush = activeBorder, shape = HeaderCapsuleShape)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs, artists, folders...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChanged("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onToggleSearch()
                                onSearchQueryChanged("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (viewModel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TopRightStatusClock(viewModel = viewModel)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
                    val folderName = try {
                        File(selectedFolder).name.ifBlank { selectedFolder }
                    } catch (_: Exception) {
                        selectedFolder
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .background(brush = capsuleGrad, shape = PillShape10)
                                .border(width = 1.dp, brush = borderBrush, shape = PillShape10)
                                .clickable(onClick = onBackFolder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Back",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .weight(1f)
                                .background(brush = capsuleGrad, shape = PillShape10)
                                .border(width = 1.dp, brush = borderBrush, shape = PillShape10)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Left Section Title & Count Badge
                    val (sectionTitle, sectionCount) = when (selectedTab) {
                        LibraryTab.ALL_SONGS -> "All Songs" to songCount
                        LibraryTab.FOLDERS -> "Folders" to folderCount
                        LibraryTab.FAVORITES -> "Favorites" to favoriteCount
                        LibraryTab.RADIO -> "Community Streams" to radioCount
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (sectionCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = PillShape10
                            ) {
                                Text(
                                    text = "$sectionCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Top Right Action Cluster: Navigation Mode Icons + Search + Status Clock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                    // Compact Navigation & Search Capsule
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .background(brush = capsuleGrad, shape = PillShape10)
                            .border(width = 0.8.dp, brush = borderBrush, shape = PillShape10)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // 1. All Songs Icon Button
                        val isAllSongs = selectedTab == LibraryTab.ALL_SONGS
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAllSongs)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.ALL_SONGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "All Songs",
                                tint = if (isAllSongs) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 2. Folders Icon Button
                        val isFolders = selectedTab == LibraryTab.FOLDERS
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFolders)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.FOLDERS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Folders",
                                tint = if (isFolders) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 3. Favorites Heart Icon Button
                        val isFavorites = selectedTab == LibraryTab.FAVORITES
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFavorites)
                                        FavoriteHeartRed.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.FAVORITES) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = if (isFavorites) FavoriteHeartRed
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 4. Stream / Radio Icon Button next to Fav icon
                        val isRadio = selectedTab == LibraryTab.RADIO
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRadio)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.RADIO) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radio,
                                contentDescription = "Radio & Streams",
                                tint = if (isRadio) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(18.dp)
                                .width(0.6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )

                        // 4. Search Icon Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleSearch),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (viewModel != null) {
                        TopRightStatusClock(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

/**
 * Isolated background inactivity watcher for Auto HUD mode.
 * Collects high-frequency/state triggers without triggering recompositions of the main music library.
 */
@Composable
private fun AutoHudInactivityWatcher(
    viewModel: MusicViewModel,
    userInteractionChannel: kotlinx.coroutines.channels.Channel<Unit>
) {
    val autoHudEnabled by viewModel.autoHudEnabled.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isDriveModeOpen by viewModel.isDriveModeOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()

    LaunchedEffect(autoHudEnabled, isPlaying, isDriveModeOpen, isSettingsOpen, isEqualizerOpen, isQueueOpen) {
        if (!autoHudEnabled || !isPlaying || isDriveModeOpen || isSettingsOpen || isEqualizerOpen || isQueueOpen) return@LaunchedEffect

        while (isActive) {
            val interaction = kotlinx.coroutines.withTimeoutOrNull(20_000L) {
                userInteractionChannel.receive()
            }
            if (interaction == null) {
                if (autoHudEnabled && isPlaying && !isDriveModeOpen && !isSettingsOpen && !isEqualizerOpen && !isQueueOpen) {
                    viewModel.openDriveMode()
                }
                return@LaunchedEffect
            }
        }
    }
}

/**
 * Ultra-compact peek pill displayed when bottom bar is hidden.
 * Highly optimized for Mali GPU with flat solid colors and leaf recomposition.
 */
@Composable
private fun BottomDockPeekPill(
    viewModel: MusicViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerBg = if (isDark) Color(0xFF131924) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isDark) Color(0xFF232D3F) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .height(36.dp)
            .background(color = containerBg, shape = PeekPillShape)
            .border(width = 0.8.dp, color = borderColor, shape = PeekPillShape)
            .clickable { onExpand() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = currentSong?.let { "${it.title} • ${it.artist}" } ?: "Tap to expand Player Dock",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Dock",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Expand Player Dock",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Top-Right Infotainment Status Cluster (Theme Toggle + 12-Hour Clock with Live State Dot).
 * Isolated composable so 1-second clock updates only recompose this small widget.
 */
@Composable
fun TopRightStatusClock(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeWithAmPm()
            kotlinx.coroutines.delay(20000)
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val capsuleGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)

    Row(
        modifier = modifier
            .height(38.dp)
            .background(brush = capsuleGrad, shape = StatusCapsuleShape)
            .border(width = 0.8.dp, brush = borderBrush, shape = StatusCapsuleShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Drive Mode HUD Button
        IconButton(
            onClick = { viewModel.openDriveMode() },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = "Drive HUD",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }

        // Dedicated Equalizer DSP Button
        IconButton(
            onClick = { viewModel.openEqualizer() },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Equalizer DSP",
                tint = if (isEqualizerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(18.dp).width(0.6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )

        // 12-hour clock
        val timeParts = currentTime.split(" ")
        val timeDigits = timeParts.getOrElse(0) { currentTime }
        val amPmLabel = timeParts.getOrElse(1) { "" }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) ClockPlayingGreen
                        else if (isDark) ClockDarkGray
                        else ClockLightGray
                    )
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.3.sp
                        )
                    ) { append(text = timeDigits) }
                    append("\u202F")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            baselineShift = BaselineShift.Superscript,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { append(text = amPmLabel) }
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        VerticalDivider(
            modifier = Modifier.height(18.dp).width(0.6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )

        // Settings Screen Button
        IconButton(
            onClick = { viewModel.toggleSettings() },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun EmptyFavoritesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = FavoriteHeartRed.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No favorite tracks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap the heart icon on any song card to pin your favorite tracks here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyFoldersView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No music folders found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Audio files located in storage folders will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptySongsView(
    isSearching: Boolean,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSearching) "No matches found" else "No music found on device",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSearching) "Try a different search term" else "Connect USB drive or copy audio files to storage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onScanClick) {
                    Text("Scan Library")
                }
            }
        }
    }
}

/**
 * Isolated recomposition boundary for the portrait seekbar.
 *
 * positionMs / durationMs / progress are high-frequency flows (update every 250 ms).
 * By reading them inside IsolatedPortraitSeekBar, only the seekbar widget
 * recomposes on each tick — BottomControlStrip remains static.
 */
@Composable
private fun IsolatedPortraitSeekBar(
    viewModel: MusicViewModel,
    currentSong: com.aashik.music.model.Song?
) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isPlaying by viewModel.isPlaying.collectAsState()

    CustomHorizontalSeekBar(
        progress = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying = isPlaying,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        waveSeed = currentSong?.id?.hashCode() ?: currentSong?.title?.hashCode() ?: 42,
        showTimeLabels = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

private val PortraitDockShape = RoundedCornerShape(18.dp)

@Composable
private fun PortraitSeekBarSection(
    viewModel: MusicViewModel,
    currentSong: com.aashik.music.model.Song?,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = AppGradients.card(isActive = false), shape = PortraitDockShape)
            .border(width = 1.dp, brush = AppGradients.border(isActive = false), shape = PortraitDockShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IsolatedPortraitSeekBar(
            viewModel = viewModel,
            currentSong = currentSong
        )
        BottomControlStrip(
            viewModel = viewModel,
            isSearchVisible = isSearchOpen,
            onToggleSearch = onToggleSearch
        )
    }
}
