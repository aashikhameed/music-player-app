package com.aashik.music.ui.mobile.glass

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global configuration data class for Kyant0/AndroidLiquidGlass components.
 * Drives real-time optical lens refraction, blur radius, chromatic aberration,
 * and obsidian glass surface opacity.
 */
data class LiquidGlassConfig(
    val blurRadius: Dp = 8.dp,
    val refractionHeight: Dp = 24.dp,
    val refractionAmount: Dp = 24.dp,
    val chromaticAberration: Boolean = true,
    val surfaceAlpha: Float = 0.4f
)

val LocalGlassConfig = compositionLocalOf { LiquidGlassConfig() }


/**
 * Glass parameters for floating chrome, driven by the Settings "Glass Style" preset.
 * The lens band is capped at the catalog proportion (24dp on the 64dp tab bar = 37.5% of the
 * element height); a taller band refracts the whole surface and mirrors the content ("inverted blur").
 */
@androidx.compose.runtime.Composable
internal fun floatingGlass(elementHeight: androidx.compose.ui.unit.Dp): FloatingGlass {
    val c = LocalGlassConfig.current
    val maxBand = elementHeight * 0.375f
    return FloatingGlass(
        blur = c.blurRadius,
        lensHeight = minOf(c.refractionHeight, maxBand),
        lensAmount = c.refractionAmount,
        chromatic = c.chromaticAberration,
        surfaceAlpha = c.surfaceAlpha
    )
}

internal data class FloatingGlass(
    val blur: androidx.compose.ui.unit.Dp,
    val lensHeight: androidx.compose.ui.unit.Dp,
    val lensAmount: androidx.compose.ui.unit.Dp,
    val chromatic: Boolean,
    val surfaceAlpha: Float
)
