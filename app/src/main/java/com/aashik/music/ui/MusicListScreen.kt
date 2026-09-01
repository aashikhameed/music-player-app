package com.aashik.music.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.MusicFolder
import com.aashik.music.model.Song
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import java.io.File

@Composable
fun MusicListScreen(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val currentSongId = currentSong?.id
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var isMapOpen by rememberSaveable { mutableStateOf(false) }
    var hasMapBeenOpened by rememberSaveable { mutableStateOf(false) }

    if (isMapOpen && !hasMapBeenOpened) {
        hasMapBeenOpened = true
    }

    val persistentNavigationMap = remember {
        movableContentOf { modifier: Modifier ->
            NavigationMapView(
                viewModel = viewModel,
                onClose = { isMapOpen = false },
                isMapOpen = isMapOpen,
                modifier = modifier
            )
        }
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val listState = rememberLazyGridState()
    val scrollToIndex by viewModel.scrollToIndex.collectAsState()

    // Handle system back button
    BackHandler(enabled = isMapOpen || (selectedTab == LibraryTab.FOLDERS && selectedFolder != null)) {
        if (isMapOpen) {
            isMapOpen = false
        } else {
            viewModel.closeFolder()
        }
    }

    LaunchedEffect(scrollToIndex) {
        scrollToIndex?.let { index ->
            if (index in songs.indices) {
                listState.animateScrollToItem(index)
            }
            viewModel.clearScrollToIndex()
        }
    }

    var songToDelete by remember { mutableStateOf<Song?>(null) }
    val activeNotification by com.aashik.music.notification.AppNotificationManager.activeNotification.collectAsState()

    // Reusable Delete Confirmation Dialog
    DeleteSongDialog(
        song = songToDelete,
        onConfirm = { song ->
            viewModel.deleteSong(song)
            songToDelete = null
        },
        onDismiss = { songToDelete = null }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ALWAYS keep the map in the composition to prevent WebView reload
        if (!isMapOpen) {
            Box(modifier = Modifier.size(1.dp).alpha(0.01f)) {
                persistentNavigationMap(Modifier.fillMaxSize())
            }
        }

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
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Header Bar with In-Place Search
                    LibraryModeHeader(
                        selectedTab = selectedTab,
                        selectedFolder = selectedFolder,
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

                    // Content & Live Map Split in Portrait (65% Map, 35% Library if map open)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isMapOpen) {
                            val portraitMapShape = RoundedCornerShape(20.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.65f)
                                    .clip(portraitMapShape)
                                    .background(brush = AppGradients.card(isActive = false), shape = portraitMapShape)
                                    .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = portraitMapShape)
                            ) {
                                persistentNavigationMap(Modifier.fillMaxSize())
                            }
                        }

                        val portraitSongShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (isMapOpen) 0.35f else 1f)
                                .clip(portraitSongShape)
                                .background(brush = AppGradients.card(isActive = false), shape = portraitSongShape)
                                .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = portraitSongShape)
                                .padding(8.dp)
                        ) {
                            MediaLibraryContent(
                                viewModel = viewModel,
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
                                folders = folders,
                                songs = songs,
                                currentSongId = currentSongId,
                                searchQuery = searchQuery,
                                gridState = listState,
                                columns = 1,
                                onSongLongPress = { songToDelete = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (selectedTab != LibraryTab.BLUETOOTH) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Waveform Seekbar & Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(brush = AppGradients.card(isActive = false), shape = RoundedCornerShape(18.dp))
                                .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = RoundedCornerShape(18.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
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
                            BottomControlStrip(
                                viewModel = viewModel,
                                isSearchVisible = isSearchOpen,
                                isMapOpen = isMapOpen,
                                onToggleSearch = {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                },
                                onToggleMap = {
                                    isMapOpen = !isMapOpen
                                }
                            )
                        }
                    }
                }
            } else {
                // Landscape Android Auto Infotainment Layout (65% Map, 35% Media & Player)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (isMapOpen) {
                        // Left: 65% Google Maps Split Screen with rounded card shape
                        val mapCardShape = RoundedCornerShape(22.dp)
                        Box(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight()
                                .clip(mapCardShape)
                                .background(brush = AppGradients.card(isActive = false), shape = mapCardShape)
                                .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = mapCardShape)
                        ) {
                            persistentNavigationMap(Modifier.fillMaxSize())
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Right: 35% Width Media Side Panel with rounded card shape
                        val songPanelShape = RoundedCornerShape(22.dp)
                        Column(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .clip(songPanelShape)
                                .background(brush = AppGradients.card(isActive = false), shape = songPanelShape)
                                .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = songPanelShape)
                                .padding(10.dp)
                        ) {
                            // Header Bar with In-Place Search
                            LibraryModeHeader(
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
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

                            // Media Library Content Grid
                            MediaLibraryContent(
                                viewModel = viewModel,
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
                                folders = folders,
                                songs = songs,
                                currentSongId = currentSongId,
                                searchQuery = searchQuery,
                                gridState = listState,
                                columns = 1,
                                onSongLongPress = { songToDelete = it },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Automotive Side Player Card
                            AutomotiveSidePlayerCard(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Full-screen Media View (Map closed)
                        val fullSongShape = RoundedCornerShape(22.dp)
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(fullSongShape)
                                    .background(brush = AppGradients.card(isActive = false), shape = fullSongShape)
                                    .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = fullSongShape)
                                    .padding(12.dp)
                            ) {
                                if (songs.isNotEmpty()) {
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
                                        .padding(start = if (songs.isNotEmpty()) 6.dp else 0.dp)
                                ) {
                                    LibraryModeHeader(
                                        selectedTab = selectedTab,
                                        selectedFolder = selectedFolder,
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

                                    MediaLibraryContent(
                                        viewModel = viewModel,
                                        selectedTab = selectedTab,
                                        selectedFolder = selectedFolder,
                                        folders = folders,
                                        songs = songs,
                                        currentSongId = currentSongId,
                                        searchQuery = searchQuery,
                                        gridState = listState,
                                        columns = 3,
                                        onSongLongPress = { songToDelete = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (selectedTab != LibraryTab.BLUETOOTH) {
                                Spacer(modifier = Modifier.height(12.dp))

                                AndroidAutoBottomBar(
                                    viewModel = viewModel,
                                    isMapOpen = false,
                                    onToggleMap = { isMapOpen = true },
                                    isSearchVisible = isSearchOpen,
                                    onToggleSearch = {
                                        isSearchOpen = !isSearchOpen
                                        if (!isSearchOpen) viewModel.onSearchQueryChanged("")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Automotive Floating Heads-Up Notification Banner (Top Overlay)
        HeadsUpNotificationBanner(
            notification = activeNotification,
            onDismiss = { com.aashik.music.notification.AppNotificationManager.dismissActive() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Reusable Media Library Content Component.
 * Dynamically displays Bluetooth Screen, Folders Grid, or Songs Grid with zero code duplication.
 */
@Composable
fun MediaLibraryContent(
    viewModel: MusicViewModel,
    selectedTab: LibraryTab,
    selectedFolder: String?,
    folders: List<MusicFolder>,
    songs: List<Song>,
    currentSongId: String?,
    searchQuery: String,
    gridState: LazyGridState,
    columns: Int,
    onSongLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            selectedTab == LibraryTab.BLUETOOTH -> {
                BluetoothScreen(
                    viewModel = viewModel,
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(items = folders, key = { it.path }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { viewModel.openFolder(folder.path) }
                            )
                        }
                    }
                }
            }
            else -> {
                if (songs.isEmpty()) {
                    EmptySongsView(
                        isSearching = searchQuery.isNotBlank(),
                        onScanClick = { viewModel.loadSongs() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(items = songs, key = { it.id }) { song ->
                            val isPlaying = song.id == currentSongId
                            SongCard(
                                song = song,
                                isPlaying = isPlaying,
                                onClick = { viewModel.play(song) },
                                onLongPress = { onSongLongPress(song) }
                            )
                        }
                    }
                }
            }
        }
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
    selectedTab: LibraryTab,
    selectedFolder: String?,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBackFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerShape = RoundedCornerShape(14.dp)
    val capsuleGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)
    val activeBorder = AppGradients.border(isActive = true)

    Box(
        modifier = modifier
            .height(44.dp)
            .then(
                if (isSearchOpen) Modifier
                    .clip(headerShape)
                    .background(brush = capsuleGrad, shape = headerShape)
                    .border(border = BorderStroke(1.dp, activeBorder), shape = headerShape)
                else Modifier
            )
    ) {
        if (isSearchOpen) {
            // Inline Search Text Field (opens directly in the header at the same place)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
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
                    onSearchQueryChanged,
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
                        val pillShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(pillShape)
                                .background(brush = capsuleGrad, shape = pillShape)
                                .border(border = BorderStroke(1.dp, borderBrush), shape = pillShape)
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
                                .clip(pillShape)
                                .background(brush = capsuleGrad, shape = pillShape)
                                .border(border = BorderStroke(1.dp, borderBrush), shape = pillShape)
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTab == LibraryTab.ALL_SONGS,
                            onClick = { onTabSelected(LibraryTab.ALL_SONGS) },
                            label = { Text("All Songs", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        FilterChip(
                            selected = selectedTab == LibraryTab.FOLDERS,
                            onClick = { onTabSelected(LibraryTab.FOLDERS) },
                            label = { Text("Folders", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        FilterChip(
                            selected = selectedTab == LibraryTab.BLUETOOTH,
                            onClick = { onTabSelected(LibraryTab.BLUETOOTH) },
                            label = { Text("Bluetooth", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Top Right: Search Button (opens search text in place)
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
