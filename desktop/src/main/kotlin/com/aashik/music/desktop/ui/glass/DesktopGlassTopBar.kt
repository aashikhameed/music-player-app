package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.ui.DesktopScreen

/**
 * Top Segmented Navigation Bar with liquid glass material.
 * Houses back/forward controls, central segmented switcher, and clock/profile indicator.
 */
@Composable
fun DesktopGlassTopBar(
    currentScreen: DesktopScreen,
    onScreenChange: (DesktopScreen) -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        "Home" to DesktopScreen.HOME,
        "New" to DesktopScreen.NEW_RELEASES,
        "Radio" to DesktopScreen.RADIO,
        "Songs" to DesktopScreen.SONGS
    )
    val selectedIndex = navItems.indexOfFirst { it.second == currentScreen }.coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: History Navigation controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .liquidGlassEffect(
                        shape = RoundedCornerShape(8.dp),
                        tintColor = Color.White.copy(alpha = if (canGoBack) 0.08f else 0.03f),
                        borderColor = Color.White.copy(alpha = if (canGoBack) 0.12f else 0.05f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canGoBack,
                        onClick = onBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = if (canGoBack) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .liquidGlassEffect(
                        shape = RoundedCornerShape(8.dp),
                        tintColor = Color.White.copy(alpha = if (canGoForward) 0.08f else 0.03f),
                        borderColor = Color.White.copy(alpha = if (canGoForward) 0.12f else 0.05f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canGoForward,
                        onClick = onForwardClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    color = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Segmented Navigation Pill
        DesktopGlassSegmentedPill(
            items = navItems.map { it.first },
            selectedIndex = selectedIndex,
            onItemSelected = { idx -> onScreenChange(navItems[idx].second) },
            height = 40.dp
        )

        // Right: Status / User Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Music Player",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .liquidGlassEffect(
                        shape = CircleShape,
                        tintColor = Color.White.copy(alpha = 0.12f),
                        borderColor = Color.White.copy(alpha = 0.20f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
