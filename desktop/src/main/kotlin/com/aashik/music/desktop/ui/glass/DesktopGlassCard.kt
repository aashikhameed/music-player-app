package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Liquid Glass Card for albums, tracklist cards, and dashboard panels.
 */
@Composable
fun DesktopGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    blurRadius: Dp = 24.dp,
    tintColor: Color = Color.White.copy(alpha = 0.06f),
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backdrop = LocalBackdrop.current

    val effectiveTint = if (onClick != null && isHovered) {
        tintColor.copy(alpha = (tintColor.alpha + 0.05f).coerceAtMost(1f))
    } else {
        tintColor
    }

    val effectiveBorder = if (onClick != null && isHovered) {
        Color.White.copy(alpha = 0.20f)
    } else {
        borderColor
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .liquidGlassEffect(
                shape = shape,
                tintColor = effectiveTint,
                borderColor = effectiveBorder
            )
            .then(clickableModifier),
        content = content
    )
}
