package com.aashik.music.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SliderTrackShape = RoundedCornerShape(2.dp)

@Immutable
data class SliderColors(
    val thumbColor: Color,
    val activeTrackColor: Color,
    val activeTickColor: Color = Color.Transparent,
    val inactiveTrackColor: Color,
    val inactiveTickColor: Color = Color.Transparent,
    val disabledThumbColor: Color = Color.Gray,
    val disabledActiveTrackColor: Color = Color.Gray,
    val disabledActiveTickColor: Color = Color.Transparent,
    val disabledInactiveTrackColor: Color = Color.DarkGray,
    val disabledInactiveTickColor: Color = Color.Transparent
)

object SliderDefaults {
    @Composable
    fun colors(
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        activeTrackColor: Color = MaterialTheme.colorScheme.primary,
        activeTickColor: Color = Color.Transparent,
        inactiveTrackColor: Color = MaterialTheme.colorScheme.outlineVariant,
        inactiveTickColor: Color = Color.Transparent,
        disabledThumbColor: Color = MaterialTheme.colorScheme.outline,
        disabledActiveTrackColor: Color = MaterialTheme.colorScheme.outline,
        disabledActiveTickColor: Color = Color.Transparent,
        disabledInactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        disabledInactiveTickColor: Color = Color.Transparent
    ): SliderColors = SliderColors(
        thumbColor = thumbColor,
        activeTrackColor = activeTrackColor,
        activeTickColor = activeTickColor,
        inactiveTrackColor = inactiveTrackColor,
        inactiveTickColor = inactiveTickColor,
        disabledThumbColor = disabledThumbColor,
        disabledActiveTrackColor = disabledActiveTrackColor,
        disabledActiveTickColor = disabledActiveTickColor,
        disabledInactiveTrackColor = disabledInactiveTrackColor,
        disabledInactiveTickColor = disabledInactiveTickColor
    )
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished = rememberUpdatedState(onValueChangeFinished)

    val thumbRadius: Dp = 9.dp
    val trackHeight: Dp = 4.dp

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val thumbColor = if (enabled) colors.thumbColor else colors.disabledThumbColor
    val activeTrackColor = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
    val inactiveTrackColor = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbRadiusPx = with(LocalDensity.current) { thumbRadius.toPx() }
        val usableWidthPx = (widthPx - 2 * thumbRadiusPx).coerceAtLeast(1f)

        fun updateFromX(x: Float) {
            val clampedX = (x - thumbRadiusPx).coerceIn(0f, usableWidthPx)
            val newFraction = clampedX / usableWidthPx
            val rawValue = valueRange.start + newFraction * rangeSpan
            val steppedValue = if (steps > 0) {
                val stepSize = rangeSpan / (steps + 1)
                val stepIndex = ((rawValue - valueRange.start) / stepSize).roundToInt()
                (valueRange.start + stepIndex * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
            } else {
                rawValue.coerceIn(valueRange.start, valueRange.endInclusive)
            }
            currentOnValueChange.value(steppedValue)
        }

        val gestureModifier = if (enabled) {
            Modifier
                .pointerInput(usableWidthPx, valueRange, steps) {
                    detectTapGestures(
                        onPress = { offset ->
                            updateFromX(offset.x)
                            tryAwaitRelease()
                            currentOnValueChangeFinished.value?.invoke()
                        }
                    )
                }
                .pointerInput(usableWidthPx, valueRange, steps) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            updateFromX(change.position.x)
                        },
                        onDragEnd = {
                            currentOnValueChangeFinished.value?.invoke()
                        },
                        onDragCancel = {
                            currentOnValueChangeFinished.value?.invoke()
                        }
                    )
                }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .then(gestureModifier),
            contentAlignment = Alignment.CenterStart
        ) {
            // Inactive Track (Full Width)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(SliderTrackShape)
                    .background(inactiveTrackColor)
            )

            // Active Track
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(SliderTrackShape)
                    .background(activeTrackColor)
            )

            // Thumb
            val thumbOffsetPx = fraction * usableWidthPx
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                    .size(thumbRadius * 2)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}
