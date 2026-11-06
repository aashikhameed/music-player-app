package com.aashik.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashik.music.ui.CustomHorizontalSeekBar
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Isolated leaf composable for the high-contrast automotive seekbar.
 * Ensures 250ms playback ticks only recompose the seekbar and not the outer HUD screen.
 */
@Composable
fun DriveHudSeekBar(
    viewModel: MusicViewModel,
    showTimeLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isPlayerLoading by viewModel.isPlayerLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    CustomHorizontalSeekBar(
        progress = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying = isPlaying,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        waveSeed = currentSong?.id?.hashCode() ?: 42,
        showTimeLabels = showTimeLabels,
        isLoading = isPlayerLoading,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

