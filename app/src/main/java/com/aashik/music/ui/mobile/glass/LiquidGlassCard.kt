package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle

// Pre-allocated static shapes to prevent GC allocations on low-spec hardware
private val CardDefaultCornerShape = RoundedRectangle(18f.dp)
private val CardSmallCornerShape = RoundedRectangle(12f.dp)
private val CardLargeCornerShape = RoundedRectangle(24f.dp)

/**
 * Liquid Glass Card implementation following Kyant0/AndroidLiquidGlass.
 * 
 * References:
 * - https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-bar
 * - app/src/commonMain/kotlin/com/kyant/backdrop/catalog/destinations/GlassPlaygroundContent.kt
 * 
 * Features:
 * - Real-time backdrop sampling with vibrancy, blur, and optical lens refraction
 * - Specular rim highlight and ambient shadow
 * - Nested backdrop support via `exportedBackdrop` (enables child sliders/buttons to sample card glass)
 * - Interactive spring scale and highlight brightening on click
 * - Strictly NEVER uses border color or border width (border = null)
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    cornerRadius: Dp = 18.dp,
    shape: Shape? = null,
    blurRadius: Dp? = null,
    refractionHeight: Dp? = null,
    refractionAmount: Dp? = null,
    depthEffect: Boolean = true,
    chromaticAberration: Boolean? = null,
    surfaceColor: Color = Color.Unspecified,
    tintColor: Color = Color.Unspecified,
    highlightAlpha: Float = 0.35f,
    shadowRadius: Dp = 12.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.35f),
    exportedBackdrop: LayerBackdrop? = null,
    onClick: (() -> Unit)? = null,
    isInteractive: Boolean = onClick != null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassConfig = LocalGlassConfig.current
    val effectiveBlur = blurRadius ?: glassConfig.blurRadius
    val effectiveRefractionHeight = refractionHeight ?: glassConfig.refractionHeight
    val effectiveRefractionAmount = refractionAmount ?: glassConfig.refractionAmount
    val effectiveChromatic = chromaticAberration ?: glassConfig.chromaticAberration
    val effectiveSurfaceColor = if (surfaceColor.isSpecified) surfaceColor else MobileTheme.panel.copy(alpha = glassConfig.surfaceAlpha)

    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    val resolvedShape = shape ?: when (cornerRadius) {
        12.dp -> CardSmallCornerShape
        18.dp -> CardDefaultCornerShape
        24.dp -> CardLargeCornerShape
        else -> remember(cornerRadius) { RoundedRectangle(cornerRadius) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (isInteractive) {
            if (isPressed) {
                pressProgress.animateTo(1f, spring(stiffness = 1000f, dampingRatio = 0.7f))
            } else {
                pressProgress.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.75f))
            }
        }
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { resolvedShape },
                effects = {
                    val progress = pressProgress.value
                    vibrancy()
                    if (effectiveBlur > 0.dp) {
                        blur(effectiveBlur.toPx())
                    }
                    if (effectiveRefractionHeight > 0.dp && effectiveRefractionAmount > 0.dp) {
                        lens(
                            refractionHeight = effectiveRefractionHeight.toPx() * (1f + 0.1f * progress),
                            refractionAmount = effectiveRefractionAmount.toPx() * (1f + 0.1f * progress),
                            depthEffect = depthEffect,
                            chromaticAberration = effectiveChromatic || (isInteractive && progress > 0.1f)
                        )
                    }
                },
                highlight = {
                    val progress = pressProgress.value
                    Highlight.Default.copy(alpha = lerp(highlightAlpha, 0.75f, progress))
                },
                shadow = { null },
                innerShadow = { null },
                layerBlock = if (isInteractive) {
                    {
                        val scale = lerp(1f, 0.985f, pressProgress.value)
                        scaleX = scale
                        scaleY = scale
                    }
                } else null,
                exportedBackdrop = exportedBackdrop,
                onDrawSurface = {
                    val progress = pressProgress.value
                    // Obsidian frosted acrylic base for AMOLED dark theme contrast
                    drawRect(effectiveSurfaceColor)
                    // Specular white sheen
                    drawRect(MobileTheme.text.copy(alpha = lerp(0.06f, 0.14f, progress)))
                    // Accent tint if specified
                    if (tintColor.isSpecified) {
                        drawRect(tintColor, blendMode = BlendMode.Hue)
                        drawRect(tintColor.copy(alpha = lerp(0.12f, 0.24f, progress)))
                    }
                }
            )
            .then(clickableModifier),
        content = content
    )
}

