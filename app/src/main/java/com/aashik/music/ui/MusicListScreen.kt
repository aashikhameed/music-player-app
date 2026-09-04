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

    // Handle system back button
    BackHandler(enabled = selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
        viewModel.closeFolder()
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
                            songCount = songs.size,
                            folderCount = folders.size,
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

                        val portraitSongShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
                    // ─────────────────────────────────────────────────────────────────────────
                    val fullSongShape = RoundedCornerShape(18.dp)
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
                                    .clip(fullSongShape)
                                    .background(brush = AppGradients.card(isActive = false), shape = fullSongShape)
                                    .border(border = BorderStroke(1.dp, AppGradients.border(isActive = false)), shape = fullSongShape)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                val canScroll = (selectedTab == LibraryTab.ALL_SONGS || selectedFolder != null) &&
                                        (songs.size > 5 || listState.canScrollForward || listState.canScrollBackward)
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
                                    // Header: 46dp tall — tab chips + search icon
                                    LibraryModeHeader(
                                        selectedTab = selectedTab,
                                        selectedFolder = selectedFolder,
                                        songCount = songs.size,
                                        folderCount = folders.size,
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

                                    // Song/folder grid — 3 columns fill the 1260dp width
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── Automotive Bottom Dock — 80dp tall ──
                            AndroidAutoBottomBar(
                                viewModel = viewModel,
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
    searchQuery: String,
    gridState: LazyGridState,
    columns: Int,
    onSongLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            selectedTab == LibraryTab.FOLDERS && selectedFolder == null -> {
                if (folders.isEmpty()) {
                    EmptyFoldersView(modifier = Modifier.fillMaxSize())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),   // 8dp on 1280px wide screen
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),   // 8dp on 1280px wide screen
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    songCount: Int = 0,
    folderCount: Int = 0,
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
            .height(48.dp)  // 48dp = minimum tap target per automotive UX on 160dpi
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
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("All Songs", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (songCount > 0) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Surface(
                                            color = if (selectedTab == LibraryTab.ALL_SONGS)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "$songCount",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTab == LibraryTab.ALL_SONGS)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
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
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Folders", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (folderCount > 0) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Surface(
                                            color = if (selectedTab == LibraryTab.FOLDERS)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "$folderCount",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTab == LibraryTab.FOLDERS)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
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

/**
 * Isolated recomposition boundary for the portrait seekbar.
 *
 * positionMs / durationMs / progress are high-frequency flows (update every 250 ms).
 * By reading them inside THIS composable instead of MusicListScreen, only this widget
 * recomposes on each tick — not the parent screen with its LazyVerticalGrid + header.
 */
@Composable
private fun PortraitSeekBarSection(
    viewModel: MusicViewModel,
    currentSong: com.aashik.music.model.Song?,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit
) {
    // These flows update every 250ms — isolated here so parent screen stays stable
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isPlaying by viewModel.isPlaying.collectAsState()

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
            onToggleSearch = onToggleSearch
        )
    }
}
