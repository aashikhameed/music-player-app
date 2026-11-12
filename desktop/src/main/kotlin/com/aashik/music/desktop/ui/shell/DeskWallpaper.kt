package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Window background: the theme colour (Album Art theme: a colour picked from the playing artwork) with a soft accent glow. */
@Composable
internal fun DeskWallpaper(modifier: Modifier = Modifier) {
    val c = Desk.c
    val bg by animateColorAsState(c.background, tween(500))
    Box(modifier.fillMaxSize().background(bg)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.18f), Color.Transparent), radius = 900f)))
    }
}
