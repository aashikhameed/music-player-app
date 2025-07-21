package com.aashik.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@Composable
fun MusicListScreen(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    val current by viewModel.currentSong.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val scrollToIndex by viewModel.scrollToIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val cardWidth = remember(screenWidthDp) { (screenWidthDp / 3f).dp }

    val listState = rememberLazyGridState()

    LaunchedEffect(current?.id) {
        current?.let { song ->
            val index = songs.indexOfFirst { it.id == song.id }
            if (index >= 0) {
                delay(100) // Let the layout stabilize before scrolling
                listState.scrollToItem(index, scrollOffset = -200)
                viewModel.clearScrollToIndex()
            }
        }
    }

    LaunchedEffect(scrollToIndex) {
        scrollToIndex?.let { index ->
            if (index in songs.indices) {
                delay(100) // Let composition settle
                listState.scrollToItem(index)
                viewModel.clearScrollToIndex()
            }
        }
    }


    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                SongLoadingBar(viewModel = viewModel)
            }
        } else {
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .graphicsLayer {},
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(
                        items = songs,
                        key = { it.id }
                    ) { song ->
                        val isPlaying = song.id == current?.id
                        SongCard(
                            song = song,
                            isPlaying = isPlaying,
                            progress = if (isPlaying) progress else 0f,
                            onClick = rememberUpdatedState { viewModel.play(song) }.value,
                            onLongPress = {},
                            cardWidth=cardWidth// replace with actual handler
                        )
                    }
                }

                CustomVerticalSeekBar(
                    progress = progress,
                    onProgressChanged = { viewModel.seekToFraction(it) },
                    modifier = Modifier
//                        .height(150.dp)
                        .padding(1.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RightControlStrip(viewModel)
                }
            }
        }
    }
}
