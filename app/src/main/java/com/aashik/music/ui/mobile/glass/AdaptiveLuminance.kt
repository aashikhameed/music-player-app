package com.aashik.music.ui.mobile.glass

import android.graphics.Bitmap
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adaptive content colour for glass surfaces, ported from the AndroidLiquidGlass catalog
 * (`AdaptiveLuminanceGlassContent`): the glass records what it samples into [layer]
 * (via `onDrawBackdrop`), a 5×5 thumbnail is averaged for luminance about once a second,
 * and [contentColor] animates to dark text on bright backdrops and white text on dark ones.
 */
@Stable
class AdaptiveLuminance internal constructor(
    val layer: GraphicsLayer,
    private val colorAnimation: androidx.compose.animation.core.Animatable<Color, androidx.compose.animation.core.AnimationVector4D>
) {
    val contentColor: Color get() = colorAnimation.value
    val secondaryColor: Color get() = colorAnimation.value.copy(alpha = 0.62f)
}

private val DarkContent = Color(0xFF0F172A)
private val LightContent = Color.White

@Composable
fun rememberAdaptiveLuminance(
    /** false when the sampled layer is the visible background itself (no glass tint on top). */
    blendGlassSurface: Boolean = true,
    /** Colour to start from before the first sample; defaults to the app theme. */
    startDark: Boolean = !MobileTheme.isDark
): AdaptiveLuminance {
    val layer = rememberGraphicsLayer()
    val initial = if (startDark) LightContent else DarkContent
    val colorAnimation = remember { Animatable(initial) }
    val adaptive = remember(layer) { AdaptiveLuminance(layer, colorAnimation) }
    // The glass draws its surface tint (theme panel at surfaceAlpha) over the sampled backdrop,
    // so judge contrast against what is actually visible: backdrop blended with that tint.
    val surfaceAlpha = if (blendGlassSurface) LocalGlassConfig.current.surfaceAlpha.coerceIn(0f, 1f) else 0f
    val panel = MobileTheme.panel
    // Same (gamma-space) weighting as the backdrop sample below.
    val panelLuminance = 0.2126 * panel.red + 0.7152 * panel.green + 0.0722 * panel.blue
    val currentSurfaceAlpha by rememberUpdatedState(surfaceAlpha)
    val currentPanelLuminance by rememberUpdatedState(panelLuminance)

    LaunchedEffect(layer) {
        val buffer = IntArray(25)
        while (isActive) {
            delay(500)
            val luminance = runCatching {
                val image = layer.toImageBitmap()
                withContext(Dispatchers.Default) {
                    val thumb = Bitmap.createScaledBitmap(image.asAndroidBitmap(), 5, 5, false)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    thumb.getPixels(buffer, 0, 5, 0, 0, 5, 5)
                    buffer.sumOf { argb ->
                        val r = (argb shr 16 and 0xFF) / 255.0
                        val g = (argb shr 8 and 0xFF) / 255.0
                        val b = (argb and 0xFF) / 255.0
                        0.2126 * r + 0.7152 * g + 0.0722 * b
                    } / buffer.size
                }
            }.getOrNull() ?: continue
            val visible = luminance * (1.0 - currentSurfaceAlpha) + currentPanelLuminance * currentSurfaceAlpha
            launch {
                colorAnimation.animateTo(if (visible > 0.5) DarkContent else LightContent, tween(1000))
            }
        }
    }
    return adaptive
}
