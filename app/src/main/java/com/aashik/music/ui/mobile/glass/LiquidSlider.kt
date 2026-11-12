package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest

private val SliderCapsuleShape = Capsule()

/**
 * 1:1 Implementation of LiquidSlider from Kyant0/AndroidLiquidGlass.
 * 
 * Canonical Reference:
 * /Users/aashikhameed/Documents/aashik/projects/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog
 * 
 * Strict architectural rule: NEVER uses border color or border width.
 * Uses real-time track backdrop sampling, elastic thumb knob, and velocity squashing.
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MobileTheme.accent,
    trackHeight: Dp = 6.dp,
    thumbWidth: Dp = 40.dp,
    thumbHeight: Dp = 24.dp,
    onValueChangeFinished: (() -> Unit)? = null,
    trackColorOverride: Color? = null,
    /** Fluid scrubber: no thumb at rest; it fades in and the track swells while seeking. */
    thumbOnlyWhileSeeking: Boolean = false
) {
    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    // Follow the app theme (Light / Dark / Album Art), not the system setting.
    val isLightTheme = !MobileTheme.isDark
    val trackColor = trackColorOverride
        ?: if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()
    val viewConfiguration = LocalViewConfiguration.current
    val touchSlop = viewConfiguration.touchSlop
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = value(),
            valueRange = valueRange,
            visibilityThreshold = visibilityThreshold,
            initialScale = 1f,
            pressedScale = 1.5f
        )
    }

    // The caller's value lambda changes whenever it recomposes; always read the latest one.
    val currentValue by rememberUpdatedState(value)
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentValue() }
            .collectLatest { v ->
                if (!dampedDragAnimation.isDragging && dampedDragAnimation.targetValue != v) {
                    dampedDragAnimation.updateValue(v)
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbHeight.coerceAtLeast(44.dp))
            .pointerInput(isLtr, valueRange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    val pointerId = down.id
                    var hasDragged = false
                    var currentX = downX
                    val rangeSpan = valueRange.endInclusive - valueRange.start
                    val currentTrackWidth = size.width.toFloat()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        currentX = change.position.x
                        if (!change.pressed) {
                            break
                        }

                        val dragDistance = kotlin.math.abs(currentX - downX)

                        if (!hasDragged && dragDistance > touchSlop) {
                            hasDragged = true
                            dampedDragAnimation.press()
                        }

                        if (hasDragged) {
                            change.consume()
                            val fraction = if (currentTrackWidth > 0f) (currentX / currentTrackWidth).coerceIn(0f, 1f) else 0f
                            val currentVal = if (isLtr) {
                                valueRange.start + fraction * rangeSpan
                            } else {
                                valueRange.endInclusive - fraction * rangeSpan
                            }
                            dampedDragAnimation.snapToValue(currentVal)
                            currentOnValueChange(currentVal)
                        }
                    }

                    if (hasDragged) {
                        val fraction = if (currentTrackWidth > 0f) (currentX / currentTrackWidth).coerceIn(0f, 1f) else 0f
                        val finalVal = if (isLtr) {
                            valueRange.start + fraction * rangeSpan
                        } else {
                            valueRange.endInclusive - fraction * rangeSpan
                        }
                        currentOnValueChange(finalVal)
                        dampedDragAnimation.release()
                        currentOnValueChangeFinished?.invoke() ?: currentOnValueChange(finalVal)
                    } else {
                        // Direct tap-to-seek anywhere along the track
                        val fraction = if (currentTrackWidth > 0f) (downX / currentTrackWidth).coerceIn(0f, 1f) else 0f
                        val targetVal = if (isLtr) {
                            valueRange.start + fraction * rangeSpan
                        } else {
                            valueRange.endInclusive - fraction * rangeSpan
                        }
                        currentOnValueChange(targetVal)
                        dampedDragAnimation.animateToValue(targetVal) {
                            currentOnValueChangeFinished?.invoke() ?: currentOnValueChange(targetVal)
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        // Visual Track Layer
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                // layerBackdrop must wrap the scale so the lens samples the swollen track the
                // user sees, not the unscaled one (which showed a misaligned bar in the glass).
                .layerBackdrop(trackBackdrop)
                .graphicsLayer {
                    if (thumbOnlyWhileSeeking) {
                        val p = dampedDragAnimation.pressProgress
                        scaleY = 1f + 0.5f * p
                    }
                }
        ) {
            Box(
                Modifier
                    .clip(SliderCapsuleShape)
                    .background(trackColor)
                    .height(trackHeight)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(SliderCapsuleShape)
                    .background(accentColor)
                    .height(trackHeight)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // Visual Liquid Glass Thumb Knob
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        activeBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { SliderCapsuleShape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = { null },
                    innerShadow = { null },
                    layerBlock = {
                        if (thumbOnlyWhileSeeking) alpha = dampedDragAnimation.pressProgress
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress)) // catalog: white thumb in every theme
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    }
}

/**
 * Convenient overload for value/progress state with optional backdrop.
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    accentColor: Color = MobileTheme.accent,
    trackHeight: Dp = 6.dp,
    thumbWidth: Dp = 36.dp,
    thumbHeight: Dp = 22.dp,
    backdrop: Backdrop? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val activeBackdrop = backdrop ?: LocalBackdrop.current ?: rememberLayerBackdrop()
    LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        visibilityThreshold = 0.001f,
        backdrop = activeBackdrop,
        modifier = modifier,
        accentColor = accentColor,
        trackHeight = trackHeight,
        thumbWidth = thumbWidth,
        thumbHeight = thumbHeight,
        onValueChangeFinished = onValueChangeFinished
    )
}
