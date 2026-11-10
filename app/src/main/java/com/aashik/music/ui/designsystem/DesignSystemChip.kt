package com.aashik.music.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val DefaultChipShape = RoundedCornerShape(8.dp)

@Immutable
data class FilterChipColors(
    val containerColor: Color,
    val labelColor: Color,
    val iconColor: Color,
    val selectedContainerColor: Color,
    val selectedLabelColor: Color,
    val selectedLeadingIconColor: Color,
    val disabledContainerColor: Color = Color.Transparent,
    val disabledLabelColor: Color = Color.Gray
)

@Immutable
data class FilterChipElevation(
    val elevation: Float = 0f
)

object FilterChipDefaults {
    @Composable
    fun filterChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = MaterialTheme.colorScheme.onSurface,
        iconColor: Color = MaterialTheme.colorScheme.primary,
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = MaterialTheme.colorScheme.outline,
        disabledLeadingIconColor: Color = MaterialTheme.colorScheme.outline,
        selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        disabledSelectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        selectedLabelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTrailingIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
    ): FilterChipColors = FilterChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        iconColor = iconColor,
        selectedContainerColor = selectedContainerColor,
        selectedLabelColor = selectedLabelColor,
        selectedLeadingIconColor = selectedLeadingIconColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor
    )
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = DefaultChipShape,
    colors: FilterChipColors = FilterChipDefaults.filterChipColors(),
    elevation: FilterChipElevation? = null,
    border: BorderStroke? = null
) {
    val containerColor = if (selected) colors.selectedContainerColor else colors.containerColor
    val labelColor = if (selected) colors.selectedLabelColor else colors.labelColor

    Row(
        modifier = modifier
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .defaultMinSize(minHeight = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        CompositionLocalProvider(LocalContentColor provides labelColor) {
            label()
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(6.dp))
            trailingIcon()
        }
    }
}
