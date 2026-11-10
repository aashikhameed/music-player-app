package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Brush

/**
 * Reusable Mobile Liquid Glass modifier.
 * Applies a translucent frosted acrylic surface with specular reflection ramp and rim highlight.
 */
fun Modifier.mobileLiquidGlass(
    shape: Shape,
    tintColor: Color = Color(0xFF141A22).copy(alpha = 0.55f),
    borderColor: Color = Color.White.copy(alpha = 0.22f)
): Modifier {
    val specularReflection = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f),
            Color(0xFF00E5FF).copy(alpha = 0.06f)
        )
    )
    val specularRim = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.40f),
            borderColor,
            Color(0xFF00E5FF).copy(alpha = 0.25f)
        )
    )

    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.45f),
            spotColor = Color(0xFF00E5FF).copy(alpha = 0.18f)
        )
        .clip(shape)
        .background(tintColor, shape)
        .background(specularReflection, shape)
        .border(BorderStroke(1.2.dp, specularRim), shape)
}

/**
 * Interactive Liquid Glass Button with press/hover animation.
 */
@Composable
fun MobileGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val baseAlpha = when {
        isPressed -> 0.22f
        isHovered -> 0.15f
        else -> 0.08f
    }
    val tintColor = if (isPrimary) {
        Color(0xFF00E5FF).copy(alpha = if (isPressed) 0.35f else 0.22f)
    } else {
        Color.White.copy(alpha = baseAlpha)
    }
    val borderColor = if (isPrimary) {
        Color(0xFF00E5FF).copy(alpha = if (isPressed) 0.65f else 0.45f)
    } else {
        Color.White.copy(alpha = if (isPressed) 0.25f else 0.14f)
    }

    Box(
        modifier = modifier
            .mobileLiquidGlass(
                shape = shape,
                tintColor = tintColor,
                borderColor = borderColor
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Circular Action Icon Glass Button (36×36pt / 40×40pt).
 * Used for Back ‹, Favorite Star ★, Download ⬇, Share ↗, Options •••.
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tintColor = if (isActive) {
        Color(0xFF00E5FF).copy(alpha = 0.20f)
    } else {
        Color.White.copy(alpha = if (isPressed) 0.18f else 0.08f)
    }

    val borderColor = if (isActive) {
        Color(0xFF00E5FF).copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = if (isPressed) 0.25f else 0.12f)
    }

    Box(
        modifier = modifier
            .size(size)
            .mobileLiquidGlass(
                shape = CircleShape,
                tintColor = tintColor,
                borderColor = borderColor
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Elevated Circular Cyan Play Button (64pt / 42pt).
 * Uses solid Ice-Cyan fill with black glyph and ambient cyan glow.
 */
@Composable
fun MobileGlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    useCyan: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = if (useCyan) {
        if (isPressed) Color(0xFF00B8CC) else Color(0xFF00E5FF)
    } else {
        if (isPressed) Color(0xFFE2E8F0) else Color(0xFFFFFFFF)
    }
    val iconColor = Color(0xFF0D0F12)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isPressed) 6.dp else 16.dp,
                shape = CircleShape,
                ambientColor = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.30f),
                spotColor = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.40f)
            )
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPlaying) "⏸" else "▶",
            color = iconColor,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

