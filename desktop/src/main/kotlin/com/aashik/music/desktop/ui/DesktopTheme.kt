package com.aashik.music.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─── PURE DARK TOKENS (NEVER RED, ONLY ELEGANT DARK CHARCOAL & ICE CYAN) ───
val DarkAccent = Color(0xFF00E5FF)       // Crisp Ice Cyan
val DarkAccentSubtle = Color(0xFF00B4D8)
val DarkBg = Color(0xFF090B10)           // Deep Obsidian
val DarkSidebar = Color(0xFF10131B)      // Charcoal Glass Sidebar
val DarkSurface = Color(0xFF151924)      // Surface Card
val DarkSurfaceVariant = Color(0xFF1C2230)
val DarkCard = Color(0xFF181D2A)
val DarkBorder = Color(0xFF242C3D)
val DarkBorderSubtle = Color(0xFF1C2230)

val DarkTextPrimary = Color(0xFFF8FAFC)  // Pure White
val DarkTextSecondary = Color(0xFF94A3B8)// Slate Gray
val DarkTextTertiary = Color(0xFF64748B) // Dimmed Slate

val DarkFavorite = Color(0xFF00E5FF)     // Favorite Accent (Ice Cyan, NEVER red)
val DarkPlayingGreen = Color(0xFF00E676) // On Air / Live Green

// ─── APPLE MACOS LIQUID GLASS DESIGN TOKENS ─────────────────────────────────
// Translucent acrylic glass backgrounds
val GlassBgCanvas = Color(0xFF090B10)
val GlassSurface = Color(0xCC161B26)       // 80% opacity dark glass
val GlassSurfaceVariant = Color(0x991D2332)// 60% opacity frosted glass
val GlassCard = Color(0xB3181E2B)          // 70% opacity card glass
val GlassSidebar = Color(0xF210131B)       // 95% opacity frosted sidebar
val GlassDock = Color(0xF20F121A)          // 95% opacity bottom dock

// Specular light reflection on glass edges
val GlassBorderBrush = Brush.verticalGradient(
    listOf(
        Color(0x38FFFFFF), // Specular highlight on top edge
        Color(0x14FFFFFF), // Translucent side reflection
        Color(0x06FFFFFF)  // Dimmed bottom edge
    )
)

val GlassBorderSubtle = BorderStroke(1.dp, Color(0x22FFFFFF))
val GlassBorderSpecular = BorderStroke(1.dp, GlassBorderBrush)
val GlassBorderActive = BorderStroke(1.dp, DarkAccent.copy(alpha = 0.55f))

// Semantic tokens
val DesktopBg = DarkBg
val DesktopSurface = DarkSurface
val DesktopSurfaceVariant = DarkSurfaceVariant
val DesktopBorder = DarkBorder
val DesktopPrimary = DarkAccent
val DesktopSecondary = DarkAccentSubtle
val DesktopTextPrimary = DarkTextPrimary
val DesktopTextSecondary = DarkTextSecondary

val DesktopCardShape = RoundedCornerShape(14.dp)
val DesktopArtShape = RoundedCornerShape(16.dp)
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
