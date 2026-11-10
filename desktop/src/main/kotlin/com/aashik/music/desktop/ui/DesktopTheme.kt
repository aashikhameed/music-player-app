package com.aashik.music.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DesktopBg = Color(0xFF0B0D13)
val DesktopSurface = Color(0xFF131720)
val DesktopSurfaceVariant = Color(0xFF1A202C)
val DesktopBorder = Color(0xFF252D3D)
val DesktopPrimary = Color(0xFF00E5FF)
val DesktopSecondary = Color(0xFF7C4DFF)
val DesktopTextPrimary = Color(0xFFF1F5F9)
val DesktopTextSecondary = Color(0xFF94A3B8)
val DesktopCardShape = RoundedCornerShape(16.dp)
val DesktopArtShape = RoundedCornerShape(20.dp)
val DesktopPillShape = RoundedCornerShape(24.dp)

private val DesktopDarkScheme = darkColorScheme(
    primary = DesktopPrimary,
    secondary = DesktopSecondary,
    background = DesktopBg,
    surface = DesktopSurface,
    surfaceVariant = DesktopSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = DesktopTextPrimary,
    onSurface = DesktopTextPrimary
)

@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopDarkScheme,
        content = content
    )
}
