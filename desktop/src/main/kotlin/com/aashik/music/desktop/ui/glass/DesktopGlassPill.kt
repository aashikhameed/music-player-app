package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Liquid Glass Segmented Pill Bar (e.g., Home / New / Radio / Songs).
 */
@Composable
fun DesktopGlassSegmentedPill(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    val containerShape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .height(height)
            .liquidGlassEffect(
                shape = containerShape,
                tintColor = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.10f)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val segmentShape = RoundedCornerShape((height - 8.dp) / 2)

                Box(
                    modifier = Modifier
                        .height(height - 8.dp)
                        .clip(segmentShape)
                        .background(
                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
