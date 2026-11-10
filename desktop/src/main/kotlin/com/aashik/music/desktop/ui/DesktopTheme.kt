package com.aashik.music.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// ─── APPLE MUSIC PURE DARK PALETTE (STRICTLY ZERO RED, ELEGANT SLATE & ICE CYAN) ───
val DarkAccent = Color(0xFF00E5FF)       // Crisp Apple Ice Cyan
val DarkAccentSubtle = Color(0xFF00B4D8)
val DarkBg = Color(0xFF07090E)           // Deep Obsidian Canvas
val DarkSidebar = Color(0xFF0E121A)      // Translucent Charcoal Glass
val DarkSurface = Color(0xFF141824)      // Translucent Acrylic Surface
val DarkSurfaceVariant = Color(0xFF1B2232)
val DarkCard = Color(0xFF171E2C)
val DarkBorder = Color(0xFF222B3D)
val DarkBorderSubtle = Color(0xFF1B2332)

val DarkTextPrimary = Color(0xFFF8FAFC)  // Pure White
val DarkTextSecondary = Color(0xFF94A3B8)// Slate Gray
val DarkTextTertiary = Color(0xFF64748B) // Dimmed Slate
val DarkFavorite = Color(0xFF00E5FF)     // Favorite Accent (Ice Cyan, NEVER red)
val DarkPlayingGreen = Color(0xFF00E676) // On Air / Live Green

// ─── APPLE MACOS LIQUID GLASS DESIGN TOKENS ─────────────────────────────────
val GlassBgCanvas = Color(0xFF07090E)
val GlassSurface = Color(0xCC141824)       // 80% opacity dark glass
val GlassSurfaceVariant = Color(0x991B2232)// 60% opacity frosted glass
val GlassCard = Color(0xB3171E2C)          // 70% opacity card glass
val GlassSidebar = Color(0xF20E121A)       // 95% opacity frosted sidebar
val GlassDock = Color(0xF00D1119)          // 94% opacity floating bottom player dock

// Specular light reflection on physical glass edges
val GlassBorderBrush = Brush.verticalGradient(
    listOf(
        Color(0x45FFFFFF), // Specular highlight reflection on top edge
        Color(0x16FFFFFF), // Subtle translucent side refraction
        Color(0x06FFFFFF)  // Dimmed bottom edge
    )
)

val GlassBorderSubtle = BorderStroke(1.dp, Color(0x1FFFFFFF))
val GlassBorderSpecular = BorderStroke(1.dp, GlassBorderBrush)
val GlassBorderActive = BorderStroke(1.dp, DarkAccent.copy(alpha = 0.65f))

// Standard Apple macOS shapes
val DesktopCardShape = RoundedCornerShape(16.dp)
val DesktopArtShape = RoundedCornerShape(16.dp)
val DesktopPillShape = RoundedCornerShape(24.dp)
val DesktopCapsuleShape = RoundedCornerShape(32.dp)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    secondary = DarkAccentSubtle,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color(0xFF07090E),
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

/**
 * Animated Liquid Glass Canvas Container:
 * Features smoothly animated organic fluid vector shapes moving behind the glass layer
 * with subtle chromatic aberration simulation and frosted translucent absorption.
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    blurRadius: Float = 30f,
    chromaticAberration: Float = 1.2f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 1. Organic Fluid Lava-Lamp Background Layer
        LiquidLavaLampCanvas()

        // 2. Chromatic Aberration & Frosted Diffusion Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    if (chromaticAberration > 0f) {
                        drawRect(
                            color = Color(0x0700E5FF),
                            topLeft = Offset(-chromaticAberration, 0f),
                            blendMode = BlendMode.Screen
                        )
                        drawRect(
                            color = Color(0x067928CA),
                            topLeft = Offset(chromaticAberration, 0f),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
                .background(Color(0x4007090E)) // Frosted dark ambient absorption
        )

        // 3. Foreground Glass UI
        content()
    }
}

@Composable
private fun LiquidLavaLampCanvas() {
    val transition = rememberInfiniteTransition(label = "LiquidLavaTransition")

    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Blob1"
    )

    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Blob2"
    )

    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Blob3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Blob 1: Deep Emerald / Dark Cyan Glow
        val c1X = w * 0.35f + (cos(phase1) * w * 0.20f)
        val c1Y = h * 0.30f + (sin(phase1) * h * 0.15f)
        val r1 = w * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x5500E5FF), Color(0x22005F73), Color.Transparent),
                center = Offset(c1X, c1Y),
                radius = r1
            ),
            center = Offset(c1X, c1Y),
            radius = r1
        )

        // Blob 2: Dark Royal Indigo / Violet
        val c2X = w * 0.70f + (sin(phase2) * w * 0.18f)
        val c2Y = h * 0.65f + (cos(phase2) * h * 0.16f)
        val r2 = w * 0.58f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x444361EE), Color(0x181F005C), Color.Transparent),
                center = Offset(c2X, c2Y),
                radius = r2
            ),
            center = Offset(c2X, c2Y),
            radius = r2
        )

        // Blob 3: Deep Teal / Sea Glass
        val c3X = w * 0.50f + (cos(phase3) * w * 0.22f)
        val c3Y = h * 0.85f + (sin(phase3) * h * 0.12f)
        val r3 = w * 0.50f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3800F5D4), Color(0x140B3C49), Color.Transparent),
                center = Offset(c3X, c3Y),
                radius = r3
            ),
            center = Offset(c3X, c3Y),
            radius = r3
        )
    }
}
