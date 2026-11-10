package com.aashik.music.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Pure Dark Apple Desktop Tokens (Never red, only elegant dark slate / obsidian & ice cyan)
val DarkAccent = Color(0xFF00E5FF)       // Crisp Ice Cyan
val DarkAccentSubtle = Color(0xFF00B4D8)
val DarkBg = Color(0xFF0C0E14)           // Deep Obsidian Canvas
val DarkSidebar = Color(0xFF12151E)      // Charcoal Sidebar
val DarkSurface = Color(0xFF171B26)      // Surface Card
val DarkSurfaceVariant = Color(0xFF1E2332)
val DarkCard = Color(0xFF1B202D)
val DarkBorder = Color(0xFF262D3D)
val DarkBorderSubtle = Color(0xFF1F2533)

val DarkTextPrimary = Color(0xFFF8FAFC)  // Pure White
val DarkTextSecondary = Color(0xFF94A3B8)// Slate Gray
val DarkTextTertiary = Color(0xFF64748B) // Dimmed Slate

val DarkFavorite = Color(0xFF00E5FF)     // Favorite Accent (Ice Cyan, NEVER red)
val DarkPlayingGreen = Color(0xFF00E676) // On Air / Live Green

// Semantic tokens
val DesktopBg = DarkBg
val DesktopSurface = DarkSurface
val DesktopSurfaceVariant = DarkSurfaceVariant
val DesktopBorder = DarkBorder
val DesktopPrimary = DarkAccent
val DesktopSecondary = DarkAccentSubtle
val DesktopTextPrimary = DarkTextPrimary
val DesktopTextSecondary = DarkTextSecondary

val DesktopCardShape = RoundedCornerShape(12.dp)
val DesktopArtShape = RoundedCornerShape(14.dp)
val DesktopPillShape = RoundedCornerShape(20.dp)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    secondary = DarkAccentSubtle,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
