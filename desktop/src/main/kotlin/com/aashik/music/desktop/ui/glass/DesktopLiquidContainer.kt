package com.aashik.music.desktop.ui.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Controller configuration for the liquid glass backing canvas.
 * Implements the architecture defined in https://kyant.gitbook.io/backdrop.
 */
class LiquidBackdropController(
    val backgroundColor: Color = Color(0xFF0D0F12)
)

val LocalBackdrop = compositionLocalOf<LiquidBackdropController?> { null }

/**
 * Root liquid container hosting the animated fluid lava-lamp canvas.
 * Implements the backdrop sampling architecture defined in https://kyant.gitbook.io/backdrop.
 */
@Composable
fun DesktopLiquidContainer(
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00E5FF),     // Ice-Cyan primary highlight
    secondaryColor: Color = Color(0xFF1E3A8A),   // Deep Indigo/Ocean secondary
    tertiaryColor: Color = Color(0xFF0F172A),    // Obsidian Midnight tertiary
    backgroundColor: Color = Color(0xFF0D0F12),  // Root obsidian backing canvas
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Smooth organic morphing animation for 3 lava-lamp fluid vector spheres
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val controller = remember { LiquidBackdropController(backgroundColor) }

    CompositionLocalProvider(LocalBackdrop provides controller) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Animated Lava-Lamp canvas drawing 3 morphing fluid blobs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Blob 1: Cyan organic wave
                val x1 = w * (0.35f + 0.20f * sin(time))
                val y1 = h * (0.45f + 0.18f * cos(time))
                val r1 = (w.coerceAtLeast(h)) * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(x1, y1),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(x1, y1)
                )

                // Blob 2: Deep Indigo wave
                val x2 = w * (0.70f + 0.22f * cos(time * 0.8f))
                val y2 = h * (0.60f + 0.22f * sin(time * 0.8f))
                val r2 = (w.coerceAtLeast(h)) * 0.50f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.40f),
                            secondaryColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(x2, y2),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(x2, y2)
                )

                // Blob 3: Obsidian / Midnight shadow wave
                val x3 = w * (0.50f + 0.25f * sin(time * 1.2f + 1f))
                val y3 = h * (0.25f + 0.15f * cos(time * 1.2f))
                val r3 = (w.coerceAtLeast(h)) * 0.40f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.50f),
                            Color.Transparent
                        ),
                        center = Offset(x3, y3),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(x3, y3)
                )
            }

            // Foreground content rendering over the liquid canvas
            content()
        }
    }
}
