package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

private val DefaultCardShape = RoundedCornerShape(16.dp)

/**
 * Reusable Mobile Liquid Glass Card for Albums, Playlists, and Editorial Modules.
 * Powered by Kyant0/AndroidLiquidGlass drawBackdrop with optical lens refraction.
 * Strictly NEVER uses border color or border width.
 */
@Composable
fun MobileGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = DefaultCardShape,
    tintColor: Color = MobileTheme.text.copy(alpha = 0.06f),
    backdrop: Backdrop? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = shape,
        tintColor = tintColor,
        backdrop = backdrop,
        exportedBackdrop = exportedBackdrop,
        onClick = onClick,
        content = content
    )
}
