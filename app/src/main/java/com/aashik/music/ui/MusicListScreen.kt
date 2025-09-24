package com.aashik.music.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.aashik.music.model.Song
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@Composable
fun MusicListScreen(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val scrollToIndex by viewModel.scrollToIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val columns = if (isPortrait) 1 else 3
    val cardWidth = remember(configuration.screenWidthDp, columns) {
        (configuration.screenWidthDp / columns.toFloat()).dp
    }

    val listState = rememberLazyGridState()


    // Scroll to current song or scrollToIndex
    LaunchedEffect(current?.id, scrollToIndex) {
        val index = scrollToIndex ?: songs.indexOfFirst { it.id == current?.id }
        if (index in songs.indices) {
            delay(100) // let layout stabilize
            listState.scrollToItem(index)
            viewModel.clearScrollToIndex()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    if (showDeleteDialog && songToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to delete \"${songToDelete!!.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSong(songToDelete!!)
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) { SongLoadingBar(viewModel) }
        } else {
            if (isPortrait) {
                // Portrait layout: controls at bottom
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(items = songs, key = { it.id }) { song ->
                            val isPlaying = song.id == current?.id
                            SongCard(
                                song = song,
                                isPlaying = isPlaying,
                                progress = if (isPlaying) progress else 0f,
                                onClick = rememberUpdatedState { viewModel.play(song) }.value,
                                onLongPress = {
                                    showDeleteDialog = true
                                    songToDelete = song
                                },
                                cardWidth = cardWidth
                            )
                        }
                    }

                    // Bottom control strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CustomHorizontalSeekBar(
                            progress = progress,
                            onProgressChanged = { viewModel.seekToFraction(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        BottomControlStrip(viewModel)
                    }
                }
            } else {
                // Landscape layout: controls at right
                Row(modifier = Modifier.fillMaxSize()) {
                    val onLetterClick = remember(songs) {
                        { letter: Char ->
                            val index = songs.indexOfFirst {
                                it.title.firstOrNull()?.uppercaseChar() == letter
                            }
                            if (index >= 0) {
                                viewModel.triggerScrollToSong(index)
                            }
                        }
                    }

                    AlphaScrollBar(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(24.dp),
                        onLetterClick = onLetterClick
                    )
                    // Grid of songs
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(items = songs, key = { it.id }) { song ->
                            val isPlaying = song.id == current?.id
                            SongCard(
                                song = song,
                                isPlaying = isPlaying,
                                progress = if (isPlaying) progress else 0f,
                                onClick = rememberUpdatedState { viewModel.play(song) }.value,
                                onLongPress = {
                                    showDeleteDialog = true
                                    songToDelete = song
                                },
                                cardWidth = cardWidth
                            )
                        }
                    }

                    // Right-side controls
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(70.dp) // total width for seekbar + controls
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Vertical SeekBar on the left
                        CustomVerticalSeekBar(
                            progress = progress,
                            onProgressChanged = { viewModel.seekToFraction(it) },
                            modifier = Modifier
                                .fillMaxHeight() // full height of the row
                                .weight(1f)     // take remaining horizontal space
                        )

                        // Control strip on the right
                        RightControlStrip(viewModel)
                    }

                }

            }
        }
    }
}
