package com.aashik.music.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.audio.DesktopAudioPlayer

@Composable
fun DesktopSeekBar(
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00E5FF),
    inactiveColor: Color = Color(0xFF252F42),
    textColor: Color = Color(0xFF94A3B8)
) {
    val positionMs by DesktopAudioPlayer.currentPositionMs.collectAsState()
    val durationMs by DesktopAudioPlayer.durationMs.collectAsState()

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val effectiveFraction = if (isDragging) {
        dragFraction
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Elapsed Time
        Text(
            text = formatTime(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
            fontSize = 12.sp,
            color = textColor,
            modifier = Modifier.width(36.dp)
        )

        // Draggable Slider
        Slider(
            value = effectiveFraction,
            onValueChange = {
                isDragging = true
                dragFraction = it
            },
            onValueChangeFinished = {
                val targetMs = (dragFraction * durationMs).toLong()
                DesktopAudioPlayer.seekTo(targetMs)
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = inactiveColor
            ),
            modifier = Modifier.weight(1f)
        )

        // Remaining or Total Time
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
        Text(
            text = "-${formatTime(remainingMs)}",
            fontSize = 12.sp,
            color = textColor,
            modifier = Modifier.width(42.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}

