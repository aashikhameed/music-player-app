package com.aashik.music.desktop.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale

/** Window wallpaper the glass refracts: the playing artwork, heavily blurred and dimmed (accent glow without art). */
@Composable
internal fun DeskWallpaper(art: ImageBitmap?, modifier: Modifier = Modifier) {
    val c = Desk.c
    val dark = c.text == Color.White
    Box(modifier.fillMaxSize().background(c.background)) {
        if (art != null) {
            Image(
                art, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    renderEffect = BlurEffect(60f, 60f, TileMode.Clamp); scaleX = 1.25f; scaleY = 1.25f
                }
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.35f), Color.Transparent), radius = 900f)))
        }
        Box(Modifier.fillMaxSize().background(if (dark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.40f)))
    }
}
