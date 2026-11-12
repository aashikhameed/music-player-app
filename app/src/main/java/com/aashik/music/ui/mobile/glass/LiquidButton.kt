package com.aashik.music.ui.mobile.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.aashik.music.ui.designsystem.Icon
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

// Pre-allocated static shapes to prevent GC allocations on low-spec automotive hardware
private val LiquidCapsuleShape = Capsule()
private val LiquidCircleShape = CircleShape
private val LiquidDefaultRoundedShape = RoundedRectangle(16f.dp)

/**
 * 1:1 Implementation of LiquidButton from Kyant0/AndroidLiquidGlass.
 * 
 * References:
 * - aHR0cHM6Ly9reWFudC5naXRib29rLmlvL2JhY2tkcm9wL3R1dG9yaWFscy9nbGFzcy1ib3R0b20tYmFy
 * - app/src/commonMain/kotlin/com/kyant/backdrop/catalog/destinations/GlassPlaygroundContent.kt
 * - app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt
 * 
 * Strict architectural rule: NEVER uses border color or border width (border = null).
 * Uses interactive touch-follow spring physics via InteractiveHighlight,
 * optical lens refraction, specular highlights, and backdrop vibrancy.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    shape: Shape = LiquidCapsuleShape,
    content: @Composable RowScope.() -> Unit
) {
    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    val progress = interactiveHighlight.pressProgress
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(
                        refractionHeight = 12f.dp.toPx() * (1f + 0.2f * progress),
                        refractionAmount = 24f.dp.toPx() * (1f + 0.2f * progress),
                        depthEffect = true,
                        chromaticAberration = progress > 0.05f
                    )
                },
                highlight = {
                    val progress = interactiveHighlight.pressProgress
                    Highlight.Default.copy(alpha = lerp(0.35f, 1f, progress))
                },
                shadow = { null },
                innerShadow = { null },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    val progress = interactiveHighlight.pressProgress
                    // Base frosted acrylic for AMOLED contrast
                    drawRect(MobileTheme.panel.copy(alpha = 0.50f))
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = lerp(0.40f, 0.75f, progress)))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor, alpha = 1f - 0.25f * progress)
                    } else {
                        drawRect(MobileTheme.text.copy(alpha = lerp(0.06f, 0.14f, progress)))
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Elevated Primary Liquid Glass Button (e.g. Play, Subscribe, Download).
 * Tinted with Ice-Cyan, vibrant optical lens refraction, and spring physics.
 */
@Composable
fun LiquidPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    LiquidButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = MobileTheme.accent,
        surfaceColor = MobileTheme.text.copy(alpha = 0.16f),
        enabled = enabled,
        content = content
    )
}

/**
 * Liquid Glass Circular Icon Button.
 * Replicates Kyant0/AndroidLiquidGlass optical physics on a circle shape.
 * Used for Back ‹, Favorite Star ★, Download ⬇, Share ↗, Options •••.
 * NEVER uses border color or border width.
 */
@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    size: Dp = 40.dp,
    isActive: Boolean = false,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { LiquidCircleShape },
                effects = {
                    val progress = interactiveHighlight.pressProgress
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(
                        refractionHeight = (size.value * 0.25f).dp.toPx(),
                        refractionAmount = (size.value * 0.50f).dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = progress > 0.05f
                    )
                },
                highlight = {
                    val progress = interactiveHighlight.pressProgress
                    Highlight.Default.copy(alpha = if (isActive) 0.85f else lerp(0.35f, 1f, progress))
                },
                shadow = { null },
                innerShadow = { null },
                layerBlock = {
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 3f.dp.toPx() / this.size.height, progress)
                    val maxOffset = this.size.minDimension * 0.4f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    val progress = interactiveHighlight.pressProgress
                    // Base frosted acrylic for AMOLED contrast
                    drawRect(MobileTheme.panel.copy(alpha = 0.65f))
                    if (isActive) {
                        drawRect(MobileTheme.accent.copy(alpha = 0.22f))
                    }
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = lerp(0.35f, 0.75f, progress)))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor, alpha = 1f - 0.25f * progress)
                    } else {
                        drawRect(MobileTheme.text.copy(alpha = lerp(0.08f, 0.16f, progress)))
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Elevated Circular Cyan Play Button (64pt / 42pt).
 * Optical lens refraction, Lucide Play/Pause glyphs, and spring physics.
 */
@Composable
fun LiquidPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    size: Dp = 64.dp,
    useCyan: Boolean = true,
    iconSize: Dp = if (size >= 56.dp) 28.dp else 20.dp
) {
    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    val pressProgress = remember { Animatable(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressProgress.animateTo(1f, spring(stiffness = 1000f, dampingRatio = 0.6f))
        } else {
            pressProgress.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.75f))
        }
    }

    val iconColor = if (useCyan) MobileTheme.background else MobileTheme.text

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { LiquidCircleShape },
                effects = {
                    val progress = pressProgress.value
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(
                        refractionHeight = (size.value * (0.3f + 0.1f * progress)).dp.toPx(),
                        refractionAmount = (size.value * (0.6f + 0.1f * progress)).dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                },
                highlight = {
                    val progress = pressProgress.value
                    Highlight.Default.copy(alpha = lerp(0.45f, 1f, progress))
                },
                shadow = { null },
                innerShadow = { null },
                layerBlock = {
                    val scale = lerp(1f, 0.94f, pressProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    val progress = pressProgress.value
                    if (useCyan) {
                        drawRect(MobileTheme.accent.copy(alpha = lerp(0.92f, 1f, progress)))
                        drawRect(MobileTheme.text.copy(alpha = lerp(0.15f, 0.28f, progress)))
                    } else {
                        drawRect(MobileTheme.panel.copy(alpha = 0.85f))
                        drawRect(MobileTheme.text.copy(alpha = lerp(0.12f, 0.25f, progress)))
                    }
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Liquid Glass Rounded Button with custom corner radius.
 * Replicates Kyant0/AndroidLiquidGlass optical physics on a RoundedRectangle shape.
 * NEVER uses border color or border width.
 */
@Composable
fun LiquidRoundedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    cornerRadiusDp: Dp = 16.dp,
    shape: Shape? = null,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val resolvedShape = shape ?: when (cornerRadiusDp) {
        16.dp -> LiquidDefaultRoundedShape
        else -> remember(cornerRadiusDp) { RoundedRectangle(cornerRadiusDp) }
    }
    LiquidButton(
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        tint = tint,
        surfaceColor = surfaceColor,
        enabled = enabled,
        shape = resolvedShape,
        content = content
    )
}
