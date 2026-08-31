package com.aashik.music.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var isMapOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val columns = if (isPortrait) 1 else if (isMapOpen) 2 else 3

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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    if (showDeleteDialog && songToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Are you sure you want to delete \"${songToDelete!!.title}\" from storage?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSong(songToDelete!!)
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SongLoadingBar(viewModel)
            }
        } else {
            if (isPortrait) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Navigation / Mode Selector Bar
                    LibraryModeHeader(
                        selectedTab = selectedTab,
                        selectedFolder = selectedFolder,
                        onTabSelected = { viewModel.selectTab(it) },
                        onBackFolder = { viewModel.closeFolder() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    // Collapsible Search Bar
                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SearchHeader(
                            query = searchQuery,
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onClose = {
                                isSearchOpen = false
                                viewModel.onSearchQueryChanged("")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Content & Live Map Split in Portrait
                    Column(modifier = Modifier.weight(1f)) {
                        if (isMapOpen) {
                            NavigationMapView(
                                viewModel = viewModel,
                                onClose = { isMapOpen = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Box(modifier = Modifier.weight(if (isMapOpen) 1f else 2f)) {
                            if (selectedTab == LibraryTab.BLUETOOTH) {
                                BluetoothScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (selectedTab == LibraryTab.FOLDERS && selectedFolder == null) {
                                if (folders.isEmpty()) {
                                    EmptyFoldersView(modifier = Modifier.fillMaxSize())
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(1),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                            } else {
                                if (songs.isEmpty()) {
                                    EmptySongsView(
                                        isSearching = searchQuery.isNotBlank(),
                                        onScanClick = { viewModel.loadSongs() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(1),
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(items = songs, key = { it.id }) { song ->
                                            val isPlaying = song.id == currentSongId
                                            SongCard(
                                                song = song,
                                                isPlaying = isPlaying,
                                                onClick = { viewModel.play(song) },
                                                onLongPress = {
                                                    showDeleteDialog = true
                                                    songToDelete = song
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Seekbar & Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CustomHorizontalSeekBar(
                            progress = progress,
                            onProgressChanged = { viewModel.seekToFraction(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
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
            } else {
                // Landscape Android Auto Infotainment Layout (Permanent Modern Design)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Split Main Content Area
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Left: Modern Automotive Scrollbar (Shown when Map is closed)
                        if (!isMapOpen && songs.isNotEmpty()) {
                            ModernVerticalScrollbar(
                                gridState = listState,
                                totalItemCount = songs.size,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 6.dp)
                            )
                        }

                        // Left: Android Auto Google Maps Split Screen (When Map is Active)
                        if (isMapOpen) {
                            NavigationMapView(
                                viewModel = viewModel,
                                onClose = { isMapOpen = false },
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight()
                                    .padding(top = 6.dp, start = 8.dp, bottom = 4.dp, end = 4.dp)
                            )
                        }

                        // Right / Center: Music Library Area
                        Column(
                            modifier = Modifier
                                .weight(if (isMapOpen) 1f else 1f)
                                .fillMaxHeight()
                                .padding(top = 6.dp, start = 6.dp, end = 8.dp)
                        ) {
                            // Header Tabs
                            LibraryModeHeader(
                                selectedTab = selectedTab,
                                selectedFolder = selectedFolder,
                                onTabSelected = { viewModel.selectTab(it) },
                                onBackFolder = { viewModel.closeFolder() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )

                            // Collapsible Search
                            AnimatedVisibility(
                                visible = isSearchOpen,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                SearchHeader(
                                    query = searchQuery,
                                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                    onClose = {
                                        isSearchOpen = false
                                        viewModel.onSearchQueryChanged("")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                )
                            }

                            // Songs / Folders Grid
                            Box(modifier = Modifier.weight(1f)) {
                                if (selectedTab == LibraryTab.BLUETOOTH) {
                                    BluetoothScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (selectedTab == LibraryTab.FOLDERS && selectedFolder == null) {
                                    if (folders.isEmpty()) {
                                        EmptyFoldersView(modifier = Modifier.fillMaxSize())
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(if (isMapOpen) 2 else 3),
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(bottom = 6.dp)
                                        ) {
                                            items(items = folders, key = { it.path }) { folder ->
                                                FolderCard(
                                                    folder = folder,
                                                    onClick = { viewModel.openFolder(folder.path) }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    if (songs.isEmpty()) {
                                        EmptySongsView(
                                            isSearching = searchQuery.isNotBlank(),
                                            onScanClick = { viewModel.loadSongs() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(if (isMapOpen) 2 else 3),
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(bottom = 6.dp)
                                        ) {
                                            items(items = songs, key = { it.id }) { song ->
                                                val isPlaying = song.id == currentSongId
                                                SongCard(
                                                    song = song,
                                                    isPlaying = isPlaying,
                                                    onClick = { viewModel.play(song) },
                                                    onLongPress = {
                                                        showDeleteDialog = true
                                                        songToDelete = song
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom: Android Auto Permanent Taskbar & Mini Media Bar
                    AndroidAutoBottomBar(
                        viewModel = viewModel,
                        isMapOpen = isMapOpen,
                        onToggleMap = { isMapOpen = !isMapOpen },
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

@Composable
fun LibraryModeHeader(
    selectedTab: LibraryTab,
    selectedFolder: String?,
    onTabSelected: (LibraryTab) -> Unit,
    onBackFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .height(44.dp)
                        .clickable(onClick = onBackFolder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .height(44.dp)
                        .weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTab == LibraryTab.ALL_SONGS,
                    onClick = { onTabSelected(LibraryTab.ALL_SONGS) },
                    label = { Text("All Songs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
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
                    label = { Text("Folders", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
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
                    label = { Text("Bluetooth", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
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
fun SearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search songs, artists...", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                if (query.isNotEmpty()) {
                    onQueryChanged("")
                } else {
                    onClose()
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear/Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier.height(50.dp)
    )
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
