package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Mobile Segmented Pill Bar (e.g. Apple Music / Library scope switcher).
 */
@Composable
fun MobileGlassSegmentedPill(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp
) {
    val containerShape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .height(height)
            .mobileLiquidGlass(
                shape = containerShape,
                tintColor = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.12f)
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val segmentShape = RoundedCornerShape((height - 6.dp) / 2)

                Box(
                    modifier = Modifier
                        .height(height - 6.dp)
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
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Format Badge Pill (e.g. LOSSLESS, DOLBY ATMOS).
 */
@Composable
fun MobileGlassBadgePill(
    text: String,
    modifier: Modifier = Modifier,
    useCyan: Boolean = false
) {
    val shape = RoundedCornerShape(4.dp)
    val tint = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f)
    val border = if (useCyan) Color(0xFF00E5FF).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
    val textColor = if (useCyan) Color(0xFF00E5FF) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .mobileLiquidGlass(shape = shape, tintColor = tint, borderColor = border)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}
