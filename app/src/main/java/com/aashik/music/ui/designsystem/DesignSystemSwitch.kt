package com.aashik.music.ui.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val SwitchTrackShape = RoundedCornerShape(14.dp)

@Immutable
data class SwitchColors(
    val checkedThumbColor: Color,
    val checkedTrackColor: Color,
    val checkedBorderColor: Color = Color.Transparent,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedBorderColor: Color = Color.Transparent,
    val disabledCheckedThumbColor: Color = Color.Gray,
    val disabledCheckedTrackColor: Color = Color.DarkGray,
    val disabledUncheckedThumbColor: Color = Color.Gray,
    val disabledUncheckedTrackColor: Color = Color.DarkGray
)

object SwitchDefaults {
    @Composable
    fun colors(
        checkedThumbColor: Color = Color.White,
        checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
        checkedBorderColor: Color = Color.Transparent,
        uncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
        disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant
    ): SwitchColors = SwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor,
        checkedBorderColor = checkedBorderColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp

    val trackColor = when {
        !enabled && checked -> colors.disabledCheckedTrackColor
        !enabled && !checked -> colors.disabledUncheckedTrackColor
        checked -> colors.checkedTrackColor
        else -> colors.uncheckedTrackColor
    }

    val thumbColor = when {
        !enabled && checked -> colors.disabledCheckedThumbColor
        !enabled && !checked -> colors.disabledUncheckedThumbColor
        checked -> colors.checkedThumbColor
        else -> colors.uncheckedThumbColor
    }

    val borderColor = if (checked) colors.checkedBorderColor else colors.uncheckedBorderColor

    val targetOffset = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 150),
        label = "SwitchThumbOffset"
    )

    val shape = SwitchTrackShape

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clip(shape)
            .background(trackColor)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                onClick = { onCheckedChange?.invoke(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            thumbContent?.invoke()
        }
    }
}
