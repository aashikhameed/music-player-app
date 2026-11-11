package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.ui.designsystem.Icon
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

/**
 * Round glass button used by the collapsed tab bar (selected tab on the left, Search on the right).
 * Same glass as the tab bar panel; the icon uses [tint] or, when null, the adaptive luminance colour.
 */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 56.dp
) {
    val adaptive = rememberAdaptiveLuminance()
    val glass = floatingGlass(elementHeight = size)
    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    if (glass.blur.value > 0f) blur(glass.blur.toPx())
                    if (glass.lensAmount.value > 0f) lens(glass.lensHeight.toPx(), glass.lensAmount.toPx())
                },
                shadow = { null },
                innerShadow = { null },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    adaptive.layer.record { drawBackdrop() }
                },
                onDrawSurface = { drawRect(MobileTheme.panel.copy(alpha = glass.surfaceAlpha)) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: adaptive.contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
