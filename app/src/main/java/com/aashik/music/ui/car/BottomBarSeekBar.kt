package com.aashik.music.ui.car

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aashik.music.ui.CustomHorizontalSeekBar
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Isolated Seekbar — limits progress recompositions strictly to the canvas.
 */
@Composable
fun BottomBarSeekBar(viewModel: MusicViewModel, isPlaying: Boolean) {
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    val isPlayerLoading by viewModel.isPlayerLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    CustomHorizontalSeekBar(
        progress          = progress,
        onProgressChanged = { viewModel.seekToFraction(it) },
        isPlaying         = isPlaying,
        currentPositionMs = positionMs,
        durationMs        = durationMs,
        waveSeed          = currentSong?.id?.hashCode() ?: currentSong?.title?.hashCode() ?: 42,
        showTimeLabels    = false,
        isLoading         = isPlayerLoading,
        modifier          = Modifier.fillMaxWidth()
    )
}

