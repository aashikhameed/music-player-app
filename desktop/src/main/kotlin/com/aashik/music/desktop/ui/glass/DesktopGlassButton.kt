package com.aashik.music.desktop.ui.glass

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

/**
 * Reusable Liquid Glass modifier applying frosted acrylic surfaces with specular borders
 * as specified in https://kyant.gitbook.io/backdrop.
 */
fun Modifier.liquidGlassEffect(
    shape: Shape,
    tintColor: Color = Color.White.copy(alpha = 0.07f),
    borderColor: Color = Color.White.copy(alpha = 0.12f)
): Modifier {
    return this
        .clip(shape)
        .background(tintColor, shape)
        .border(BorderStroke(1.dp, borderColor), shape)
}

/**
 * Interactive Liquid Glass Button with hover and press feedback.
 */
@Composable
fun DesktopGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseAlpha = when {
        isPressed -> 0.22f
        isHovered -> 0.14f
        else -> 0.07f
    }
    val tintColor = if (isPrimary) {
        Color(0xFF00E5FF).copy(alpha = if (isHovered) 0.35f else 0.22f)
    } else {
        Color.White.copy(alpha = baseAlpha)
    }
    val borderColor = if (isPrimary) {
        Color(0xFF00E5FF).copy(alpha = if (isHovered) 0.60f else 0.40f)
    } else {
        Color.White.copy(alpha = if (isHovered) 0.22f else 0.10f)
    }

    Box(
        modifier = modifier
            .liquidGlassEffect(
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
 * Circular Liquid Glass Play/Pause Button.
 */
@Composable
fun DesktopGlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    useCyan: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = if (useCyan) {
        if (isHovered) Color(0xFF33EBFF) else Color(0xFF00E5FF)
    } else {
        if (isHovered) Color(0xFFF0F0F0) else Color(0xFFFFFFFF)
    }
    val iconColor = Color(0xFF0D0F12)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isHovered) 12.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f),
                spotColor = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
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
