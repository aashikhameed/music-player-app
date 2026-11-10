package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Reusable Mobile Liquid Glass Card for Albums, Playlists, and Editorial Modules.
 */
@Composable
fun MobileGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tintColor: Color = Color.White.copy(alpha = 0.06f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveTint = if (onClick != null && isPressed) {
        Color.White.copy(alpha = (tintColor.alpha + 0.08f).coerceAtMost(1f))
    } else {
        tintColor
    }

    val effectiveBorder = if (onClick != null && isPressed) {
        Color.White.copy(alpha = 0.22f)
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
            .mobileLiquidGlass(
                shape = shape,
                tintColor = effectiveTint,
                borderColor = effectiveBorder
            )
            .then(clickableModifier),
        content = content
    )
}
