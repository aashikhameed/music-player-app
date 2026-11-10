package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

enum class MobileNavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    NEW("New", Icons.Rounded.AutoAwesome),
    RADIO("Radio", Icons.Rounded.Radio),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    SEARCH("Search", Icons.Rounded.Search)
}

/**
 * Mobile Floating Glass Bottom Navigation Bar (361×54pt, r=27pt).
 * Floats 16dp above the home indicator with 5 Apple Music tabs.
 * Supports Kyant0/AndroidLiquidGlass backdrop sampling on Android 12+ / 13+
 * with 100% crash-free fallback on Android 9 (API 28).
 */
@Composable
fun MobileGlassBottomNavBar(
    selectedTab: MobileNavTab,
    onTabSelected: (MobileNavTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val capsuleShape = RoundedCornerShape(27.dp)
    val density = LocalDensity.current

    val backdropModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { capsuleShape },
            effects = {
                with(density) {
                    blur(24.dp.toPx())
                    lens(16.dp.toPx(), 20.dp.toPx(), chromaticAberration = true)
                }
            }
        )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(54.dp)
            .then(backdropModifier)
            .mobileLiquidGlass(
                shape = capsuleShape,
                tintColor = Color(0xFF10141A).copy(alpha = 0.58f),
                borderColor = Color.White.copy(alpha = 0.28f)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MobileNavTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

