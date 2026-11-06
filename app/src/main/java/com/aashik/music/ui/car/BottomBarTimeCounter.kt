package com.aashik.music.ui.car

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aashik.music.utils.formatTime
import com.aashik.music.viewmodel.MusicViewModel

/**
 * Isolated Time Counter — limits 250ms recompositions strictly to this Text widget.
 */
@Composable
fun BottomBarTimeCounter(viewModel: MusicViewModel) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()

    Text(
        text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
