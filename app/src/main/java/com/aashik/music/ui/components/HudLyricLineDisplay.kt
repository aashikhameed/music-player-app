package com.aashik.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.aashik.music.viewmodel.MusicViewModel

private val LyricLineShape = RoundedCornerShape(8.dp)

private data class LyricTickerState(val index: Int, val text: String)

/**
 * Isolated dynamic synchronized lyrics line ticker composable.
 * Recomposition-scoped to playback ticks without triggering recompositions of the outer HUD stage.
 * Features smooth slide + crossfade animation when moving between lines.
 */
@Composable
fun HudLyricLineDisplay(
    viewModel: MusicViewModel,
    textColor: Color,
    fontSize: TextUnit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    if (lyrics.isEmpty() && !isLoadingLyrics) return

    val tickerState = remember(lyrics, positionMs, isLoadingLyrics) {
        if (lyrics.isEmpty() && isLoadingLyrics) {
            LyricTickerState(-1, "♪ Loading lyrics...")
        } else {
            val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
            if (idx >= 0) {
                val line = lyrics[idx]
                val nextLineTime = lyrics.getOrNull(idx + 1)?.timeMs
                val text = if (nextLineTime != null && positionMs >= nextLineTime && line.text.isBlank()) {
                    "♪ ♪ ♪"
                } else {
                    line.text.ifBlank { "♪ ♪ ♪" }
                }
                LyricTickerState(idx, text)
            } else {
                LyricTickerState(-1, "♪ Lyrics")
            }
        }
    }

    AnimatedContent(
        targetState = tickerState,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)) { height -> height } +
             fadeIn(animationSpec = tween(durationMillis = 260)))
                .togetherWith(
                    slideOutVertically(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { height -> -height } +
                    fadeOut(animationSpec = tween(durationMillis = 180))
                )
        },
        label = "HudLyricLineAnimation",
        modifier = modifier
    ) { state ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = LyricLineShape
                )
                .clip(LyricLineShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Lyrics",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = state.text,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

