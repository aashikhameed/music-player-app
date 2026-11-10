package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Controller configuration for the mobile liquid glass backing canvas.
 * Implements the architecture defined in https://kyant.gitbook.io/backdrop.
 */
class MobileLiquidBackdropController(
    val backgroundColor: Color = Color(0xFF0D0F12)
)

val LocalMobileBackdrop = compositionLocalOf<MobileLiquidBackdropController?> { null }

/**
 * Root Liquid Glass Container for Android Mobile portrait views.
 * Hosts an animated morphing fluid lava-lamp canvas behind the UI.
 * 
 * Powered by https://kyant.gitbook.io/backdrop architecture.
 */
@Composable
fun MobileLiquidContainer(
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00E5FF),     // Ice-Cyan primary highlight
    secondaryColor: Color = Color(0xFF1E3A8A),   // Deep Indigo secondary
    tertiaryColor: Color = Color(0xFF0F172A),    // Obsidian Midnight tertiary
    backgroundColor: Color = Color(0xFF0D0F12),  // Root canvas background
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidMeshAnim")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeFloat"
    )

    val controller = remember { MobileLiquidBackdropController(backgroundColor) }

    CompositionLocalProvider(LocalMobileBackdrop provides controller) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Blob 1: Ice-Cyan Wave
                val x1 = w * (0.35f + 0.20f * sin(time))
                val y1 = h * (0.40f + 0.18f * cos(time))
                val r1 = (w.coerceAtLeast(h)) * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.32f),
                            primaryColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(x1, y1),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(x1, y1)
                )

                // Blob 2: Deep Indigo Wave
                val x2 = w * (0.70f + 0.22f * cos(time * 0.8f))
                val y2 = h * (0.55f + 0.22f * sin(time * 0.8f))
                val r2 = (w.coerceAtLeast(h)) * 0.50f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.35f),
                            secondaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(x2, y2),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(x2, y2)
                )

                // Blob 3: Obsidian / Teal shadow wave
                val x3 = w * (0.50f + 0.25f * sin(time * 1.2f + 1f))
                val y3 = h * (0.22f + 0.15f * cos(time * 1.2f))
                val r3 = (w.coerceAtLeast(h)) * 0.40f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(x3, y3),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(x3, y3)
                )

                // Blob 4: Bottom Floating Glass Backlight Wave (Ice-Cyan & Deep Ocean)
                // Shines directly behind the floating mini-player and bottom navigation bar
                val x4 = w * (0.50f + 0.28f * sin(time * 0.7f + 2.5f))
                val y4 = h * (0.90f + 0.08f * cos(time * 0.7f))
                val r4 = (w.coerceAtLeast(h)) * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.42f),
                            secondaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(x4, y4),
                        radius = r4
                    ),
                    radius = r4,
                    center = Offset(x4, y4)
                )
            }

            content()
        }
    }
}

