package com.aashik.music.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearPrimaryCyan = Color(0xFF00E5FF)
val WearPrimaryVariant = Color(0xFF00B0FF)
val WearSecondaryAmber = Color(0xFFFF9800)
val WearBackgroundBlack = Color(0xFF000000)
val WearSurfaceDark = Color(0xFF141414)
val WearSurfaceCard = Color(0xFF202020)
val WearOnSurfaceWhite = Color(0xFFFFFFFF)
val WearOnSurfaceDim = Color(0xFFAAAAAA)

private val WearColorPalette = Colors(
    primary = WearPrimaryCyan,
    primaryVariant = WearPrimaryVariant,
    secondary = WearSecondaryAmber,
    background = WearBackgroundBlack,
    surface = WearSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = WearOnSurfaceWhite,
    onSurface = WearOnSurfaceWhite,
    onSurfaceVariant = WearOnSurfaceDim
)

@Composable
fun WearMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
