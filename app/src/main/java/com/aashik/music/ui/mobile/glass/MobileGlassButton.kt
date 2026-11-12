package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultButtonShape = RoundedCornerShape(12.dp)

/**
 * Reusable Mobile Liquid Glass modifier.
 * Applies a translucent frosted acrylic surface with specular reflection ramp and rim highlight.
 */
fun Modifier.mobileLiquidGlass(
    shape: Shape,
    tintColor: Color = MobileTheme.panel.copy(alpha = 0.55f)
): Modifier {
    val specularReflection = Brush.verticalGradient(
        colors = listOf(
            MobileTheme.text.copy(alpha = 0.16f),
            MobileTheme.text.copy(alpha = 0.04f),
            MobileTheme.accent.copy(alpha = 0.06f)
        )
    )

    return this
        .clip(shape)
        .background(tintColor, shape)
        .background(specularReflection, shape)
}

/**
 * Interactive Liquid Glass Button with press/hover animation.
 * Powered by Kyant0/AndroidLiquidGlass drawBackdrop with optical lens refraction.
 */
@Composable
fun MobileGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = DefaultButtonShape,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (isPrimary) {
        LiquidPrimaryButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = content
        )
    } else {
        LiquidButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            enabled = enabled,
            content = content
        )
    }
}

/**
 * Circular Action Icon Glass Button (36×36pt / 40×40pt).
 * Used for Back ‹, Favorite Star ★, Download ⬇, Share ↗, Options •••.
 * Powered by Kyant0/AndroidLiquidGlass drawBackdrop with optical lens refraction.
 */
@Composable
fun MobileGlassIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isActive: Boolean = false,
    contentDescription: String? = null
) {
    LiquidIconButton(
        onClick = onClick,
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else modifier,
        size = size,
        isActive = isActive
    ) {
        icon()
    }
}

/**
 * Elevated Circular Cyan Play Button (64pt / 42pt).
 * Uses solid Ice-Cyan fill with black glyph and ambient cyan glow.
 * Powered by Kyant0/AndroidLiquidGlass drawBackdrop with optical lens refraction.
 */
@Composable
fun MobileGlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    useCyan: Boolean = true
) {
    LiquidPlayButton(
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier,
        size = size,
        useCyan = useCyan
    )
}
