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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@Composable
fun RightControlStrip(viewModel: MusicViewModel) {
    val isPlaying = viewModel.isPlaying.collectAsState().value
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()

    // --- Real-time clock ---
    var currentTime by remember { mutableStateOf(getCurrentTimeWithAmPm()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeWithAmPm()
            kotlinx.coroutines.delay(1000) // update every second
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Display live time

        Text(
            text = currentTime,
            fontSize = 16.sp // adjust size here
        )


        // Theme toggle
        IconButton(onClick = { viewModel.toggleTheme() }) {
            Icon(imageVector = Icons.Filled.LightMode, contentDescription = "Theme")
        }

        // Scroll to current song
        IconButton(onClick = { viewModel.triggerScrollToCurrentSong() }) {
            Icon(imageVector = Icons.Filled.MyLocation, contentDescription = "Scroll to current song")
        }

        // Previous
        IconButton(onClick = { viewModel.playPreviousSong() }) {
            Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
        }

        // Play/Pause
        IconButton(onClick = { viewModel.togglePlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        // Next
        IconButton(onClick = { viewModel.playNextSong() }) {
            Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
        }

        // Shuffle
        IconButton(onClick = { viewModel.toggleShuffle() }) {
            Icon(
                imageVector = if (!isShuffleOn) Icons.Filled.Shuffle else Icons.Filled.ShuffleOn,
                contentDescription = "Shuffle Songs"
            )
        }
    }
}

// --- Helper function ---
fun getCurrentTimeWithAmPm(): String {
    val sdf = SimpleDateFormat("hh:mm", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}