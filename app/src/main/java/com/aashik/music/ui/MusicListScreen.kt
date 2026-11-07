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
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
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
import androidx.compose.material3.HorizontalDivider
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

import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*

import java.io.File
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.rounded.Shuffle

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
private val MobileDockShape = RoundedCornerShape(22.dp)
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

    val userInteractionChannel = remember { kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED) }

    AutoHudInactivityWatcher(
        viewModel = viewModel,
        userInteractionChannel = userInteractionChannel
    )

    // Handle system back button
    BackHandler(
        enabled = isQueueOpen || isEqualizerOpen || isDriveModeOpen || isSettingsOpen ||
                (isPortrait && selectedTab != LibraryTab.ALL_SONGS) ||
                (!isPortrait && ((selectedTab == LibraryTab.FOLDERS && selectedFolder != null) || selectedTab == LibraryTab.RADIO))
    ) {
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
            } else if (isPortrait) {
                viewModel.selectTab(LibraryTab.MORE)
            } else {
                viewModel.selectTab(LibraryTab.ALL_SONGS)
            }
        } else if (isPortrait && (selectedTab == LibraryTab.FOLDERS || selectedTab == LibraryTab.FAVORITES)) {
            viewModel.selectTab(LibraryTab.MORE)
        } else if (isPortrait && selectedTab == LibraryTab.MORE) {
            viewModel.selectTab(LibraryTab.ALL_SONGS)
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
        onRedownload = { song -> viewModel.redownloadSong(song) },
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
                    // Mobile Portrait Layout
                    val selectedRadioCat by viewModel.selectedRadioCategory.collectAsState()
                    val isDrillDown = selectedTab == LibraryTab.RADIO && selectedRadioCat != null

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (!isDrillDown) {
                            MobileHeader(
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
                                isSearchOpen = isSearchOpen,
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                onToggleSearch = {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                },
                                onBack = {
                                    if (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
                                        viewModel.closeFolder()
                                    } else {
                                        viewModel.selectTab(LibraryTab.MORE)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (selectedTab == LibraryTab.MORE) {
                                MobileMoreContent(
                                    foldersCount = folders.size,
                                    onNavigateToFolders = { viewModel.selectTab(LibraryTab.FOLDERS) },
                                    onNavigateToFavorites = { viewModel.selectTab(LibraryTab.FAVORITES) },
                                    onNavigateToExplore = { viewModel.selectTab(LibraryTab.RADIO) },
                                    onOpenDriveMode = { viewModel.openDriveMode() },
                                    onOpenEqualizer = { viewModel.openEqualizer() },
                                    onOpenSettings = { viewModel.openSettings() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
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
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ── Unified Mobile Bottom Dock (Mini-Player + Bottom Nav Bar merged with 0 gap) ──
                        if (currentSong != null) {
                            val dockBg = MaterialTheme.colorScheme.surfaceContainer
                            val dockBorder = MaterialTheme.colorScheme.outlineVariant

                            Surface(
                                shape = MobileDockShape,
                                color = dockBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 0.8.dp,
                                        color = dockBorder,
                                        shape = MobileDockShape
                                    )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    MobileMiniPlayer(
                                        viewModel = viewModel,
                                        song = currentSong!!,
                                        onClick = { viewModel.openDriveMode() },
                                        isDocked = true
                                    )
                                    HorizontalDivider(
                                        color = dockBorder,
                                        thickness = 0.8.dp
                                    )
                                    MobileBottomNavBar(
                                        selectedTab = selectedTab,
                                        onTabSelected = { tab ->
                                            viewModel.selectTab(tab)
                                        },
                                        isDocked = true
                                    )
                                }
                            }
                        } else {
                            // Standalone Bottom Navigation Bar (no song loaded)
                            MobileBottomNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    viewModel.selectTab(tab)
                                },
                                isDocked = false
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
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                val canScroll = (selectedTab == LibraryTab.ALL_SONGS || selectedTab == LibraryTab.MORE || selectedFolder != null) && songs.size > 5
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

                            if (!hideBottomBar) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // ── Automotive Bottom Dock — 80dp tall ──
                                AndroidAutoBottomBar(
                                    viewModel = viewModel,
                                    isSearchVisible = isSearchOpen,
                                    onToggleSearch = {
                                        isSearchOpen = !isSearchOpen
                                        if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                    },
                                    onCollapse = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay Slide-over Playback Queue Drawer (Left-aligned)
        if (isQueueOpen) {
            QueueDrawer(
                viewModel = viewModel,
                onClose = { viewModel.closeQueue() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
