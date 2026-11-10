package com.aashik.music.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DesktopScreen {
    NOW_PLAYING,
    LIBRARY,
    FOLDERS,
    FAVORITES,
    EXPLORE,
    EQUALIZER,
    SETTINGS
}

private val NavItemShape = RoundedCornerShape(8.dp)

@Composable
fun DesktopSidebar(
    currentScreen: DesktopScreen,
    onSelectScreen: (DesktopScreen) -> Unit,
    songCount: Int,
    favoriteCount: Int,
    folderCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(230.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F1218))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // App Branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Music Player",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                    Text(
                        text = "Desktop Edition",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: PLAYBACK & LIBRARY
            SidebarSectionHeader(title = "LIBRARY")
            SidebarNavItem(
                label = "Now Playing",
                icon = Icons.Rounded.PlayCircleOutline,
                isSelected = currentScreen == DesktopScreen.NOW_PLAYING,
                onClick = { onSelectScreen(DesktopScreen.NOW_PLAYING) }
            )
            SidebarNavItem(
                label = "All Songs",
                icon = Icons.Rounded.MusicNote,
                badge = if (songCount > 0) "$songCount" else null,
                isSelected = currentScreen == DesktopScreen.LIBRARY,
                onClick = { onSelectScreen(DesktopScreen.LIBRARY) }
            )
            SidebarNavItem(
                label = "Folders",
                icon = Icons.Rounded.FolderOpen,
                badge = if (folderCount > 0) "$folderCount" else null,
                isSelected = currentScreen == DesktopScreen.FOLDERS,
                onClick = { onSelectScreen(DesktopScreen.FOLDERS) }
            )
            SidebarNavItem(
                label = "Favorites",
                icon = Icons.Rounded.FavoriteBorder,
                badge = if (favoriteCount > 0) "$favoriteCount" else null,
                isSelected = currentScreen == DesktopScreen.FAVORITES,
                onClick = { onSelectScreen(DesktopScreen.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: DISCOVER
            SidebarSectionHeader(title = "DISCOVER")
            SidebarNavItem(
                label = "Radio & Explore",
                icon = Icons.Rounded.Podcasts,
                isSelected = currentScreen == DesktopScreen.EXPLORE,
                onClick = { onSelectScreen(DesktopScreen.EXPLORE) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: AUDIO & TOOLS
            SidebarSectionHeader(title = "TOOLS")
            SidebarNavItem(
                label = "10-Band Equalizer",
                icon = Icons.Rounded.Tune,
                isSelected = currentScreen == DesktopScreen.EQUALIZER,
                onClick = { onSelectScreen(DesktopScreen.EQUALIZER) }
            )
            SidebarNavItem(
                label = "Settings",
                icon = Icons.Rounded.Settings,
                isSelected = currentScreen == DesktopScreen.SETTINGS,
                onClick = { onSelectScreen(DesktopScreen.SETTINGS) }
            )
        }

        // Footer info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Divider(color = Color(0xFF1E2638), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "macOS v0.3.2",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun SidebarSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: String? = null
) {
    val bg = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color.Transparent
    val tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8)
    val textColor = if (isSelected) Color(0xFFF1F5F9) else Color(0xFFCBD5E1)
    val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NavItemShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = weight,
                color = textColor
            )
        }
        if (badge != null) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}
