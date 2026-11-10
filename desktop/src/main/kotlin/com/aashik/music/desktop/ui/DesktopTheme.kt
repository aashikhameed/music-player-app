package com.aashik.music.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Apple Music macOS Design Tokens
val AppleMusicRed = Color(0xFFFA2D48)
val AppleMusicRedLight = Color(0xFFFF3B56)
val AppleMusicPink = Color(0xFFFF2D55)

val AppleMusicBg = Color(0xFF141416)
val AppleMusicSidebar = Color(0xFF1B1B1F)
val AppleMusicSurface = Color(0xFF222226)
val AppleMusicSurfaceVariant = Color(0xFF28282D)
val AppleMusicCard = Color(0xFF26262B)
val AppleMusicBorder = Color(0xFF333339)
val AppleMusicBorderSubtle = Color(0xFF2A2A30)

val AppleMusicTextPrimary = Color(0xFFFFFFFF)
val AppleMusicTextSecondary = Color(0xFF98989D)
val AppleMusicTextTertiary = Color(0xFF636366)

// Backwards compatibility tokens
val DesktopBg = AppleMusicBg
val DesktopSurface = AppleMusicSurface
val DesktopSurfaceVariant = AppleMusicSurfaceVariant
val DesktopBorder = AppleMusicBorder
val DesktopPrimary = AppleMusicRed
val DesktopSecondary = AppleMusicPink
val DesktopTextPrimary = AppleMusicTextPrimary
val DesktopTextSecondary = AppleMusicTextSecondary

val DesktopCardShape = RoundedCornerShape(12.dp)
val DesktopArtShape = RoundedCornerShape(14.dp)
val DesktopPillShape = RoundedCornerShape(20.dp)

private val AppleMusicDarkScheme = darkColorScheme(
    primary = AppleMusicRed,
    secondary = AppleMusicPink,
    background = AppleMusicBg,
    surface = AppleMusicSurface,
    surfaceVariant = AppleMusicSurfaceVariant,
    onPrimary = Color.White,
    onBackground = AppleMusicTextPrimary,
    onSurface = AppleMusicTextPrimary
)

@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppleMusicDarkScheme,
        content = content
    )
}
