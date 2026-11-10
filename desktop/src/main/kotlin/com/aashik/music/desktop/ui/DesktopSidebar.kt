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

private val DarkNavItemShape = RoundedCornerShape(8.dp)

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
            .width(220.dp)
            .fillMaxHeight()
            .background(DarkSidebar)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Apple Music Logo / Header in Dark Titanium
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = DarkAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Music Player",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "Apple macOS Edition",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: APPLE MUSIC
            DarkSidebarSectionHeader(title = "APPLE MUSIC")
            DarkSidebarNavItem(
                label = "Now Playing",
                icon = Icons.Rounded.PlayCircle,
                isSelected = currentScreen == DesktopScreen.NOW_PLAYING,
                onClick = { onSelectScreen(DesktopScreen.NOW_PLAYING) }
            )
            DarkSidebarNavItem(
                label = "Radio & Explore",
                icon = Icons.Rounded.Podcasts,
                isSelected = currentScreen == DesktopScreen.EXPLORE,
                onClick = { onSelectScreen(DesktopScreen.EXPLORE) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: LIBRARY
            DarkSidebarSectionHeader(title = "LIBRARY")
            DarkSidebarNavItem(
                label = "Songs",
                icon = Icons.Rounded.MusicNote,
                isSelected = currentScreen == DesktopScreen.LIBRARY,
                badge = if (songCount > 0) "$songCount" else null,
                onClick = { onSelectScreen(DesktopScreen.LIBRARY) }
            )
            DarkSidebarNavItem(
                label = "Folders",
                icon = Icons.Rounded.Folder,
                isSelected = currentScreen == DesktopScreen.FOLDERS,
                badge = if (folderCount > 0) "$folderCount" else null,
                onClick = { onSelectScreen(DesktopScreen.FOLDERS) }
            )
            DarkSidebarNavItem(
                label = "Favorites",
                icon = Icons.Rounded.Favorite,
                isSelected = currentScreen == DesktopScreen.FAVORITES,
                badge = if (favoriteCount > 0) "$favoriteCount" else null,
                onClick = { onSelectScreen(DesktopScreen.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: TOOLS
            DarkSidebarSectionHeader(title = "SETTINGS & TOOLS")
            DarkSidebarNavItem(
                label = "Equalizer",
                icon = Icons.Rounded.Tune,
                isSelected = currentScreen == DesktopScreen.EQUALIZER,
                onClick = { onSelectScreen(DesktopScreen.EQUALIZER) }
            )
            DarkSidebarNavItem(
                label = "Preferences",
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
            Divider(color = DarkBorderSubtle, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Apple Silicon Native (AVFoundation)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTextTertiary
            )
        }
    }
}

@Composable
private fun DarkSidebarSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = DarkTextTertiary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun DarkSidebarNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) DarkCard else Color.Transparent
    val tint = if (isSelected) DarkAccent else DarkTextSecondary
    val textStyle = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(DarkNavItemShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = textStyle,
                color = if (isSelected) DarkTextPrimary else DarkTextSecondary
            )
        }

        if (badge != null) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTextTertiary
            )
        }
    }
}
