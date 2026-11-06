package com.aashik.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Isolated phase animator that runs only during active audio stream loading / buffering.
 * Returns 0f immediately and completely disposes the animation when not loading,
 * ensuring 0 CPU / GPU overhead and zero frame ticks on automotive headunits.
 */
@Composable
fun rememberLoadingPhase(isLoading: Boolean): Float {
    if (!isLoading) return 0f
    val transition = rememberInfiniteTransition(label = "SeekerLoadingTransition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SeekerLoadingPhase"
    )
    return phase
}

