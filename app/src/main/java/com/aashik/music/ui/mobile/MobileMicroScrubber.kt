package com.aashik.music.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aashik.music.viewmodel.MusicViewModel

private val MicroScrubberTrackColor = Color(0x33FFFFFF)
private val MicroScrubberActiveColor = Color(0xFF00E5FF)

@Composable
fun MobileMicroScrubber(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.currentProgressFlow.collectAsState(initial = 0f)
    val durationMs by viewModel.durationFlow.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragFraction else progress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        viewModel.seekTo((dragFraction * durationMs).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        // Background track (2dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopStart)
                .background(MicroScrubberTrackColor)
        )
        // Active visual progress bar (3dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = displayProgress.coerceIn(0f, 1f))
                .height(3.dp)
                .align(Alignment.TopStart)
                .background(MicroScrubberActiveColor)
        )
    }
}

