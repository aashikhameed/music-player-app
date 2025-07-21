package com.aashik.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashik.music.viewmodel.MusicViewModel

@Composable
fun RightControlStrip(viewModel: MusicViewModel) {
    val isPlaying = viewModel.isPlaying.collectAsState().value
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        IconButton(onClick = { viewModel.toggleTheme() }) {
            Icon(imageVector = Icons.Filled.LightMode, contentDescription = "Theme")
        }

        IconButton(onClick = { viewModel.triggerScrollToCurrentSong() }) {
            Icon(imageVector = Icons.Filled.MyLocation, contentDescription = "Theme")
        }

        IconButton(onClick = { viewModel.playPreviousSong() }) {
            Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
        }

        IconButton(onClick = { viewModel.togglePlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        IconButton(onClick = { viewModel.playNextSong() }) {
            Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
        }

        IconButton(onClick = { viewModel.toggleShuffle() }) {
            Icon(
                imageVector = if (!isShuffleOn) Icons.Filled.Shuffle else Icons.Filled.ShuffleOn,
                contentDescription = "Shuffle Songs"
            )
        }
    }
}

