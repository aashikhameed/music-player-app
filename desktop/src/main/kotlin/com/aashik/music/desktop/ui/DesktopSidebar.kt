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

private val AppleNavItemShape = RoundedCornerShape(6.dp)

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
            .background(AppleMusicSidebar)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Apple Music Logo / Header
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
                        .background(AppleMusicRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Apple Music",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleMusicTextPrimary
                    )
                    Text(
                        text = "macOS Edition",
                        fontSize = 11.sp,
                        color = AppleMusicTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: APPLE MUSIC
            AppleSidebarSectionHeader(title = "APPLE MUSIC")
            AppleSidebarNavItem(
                label = "Now Playing",
                icon = Icons.Rounded.PlayCircle,
                isSelected = currentScreen == DesktopScreen.NOW_PLAYING,
                onClick = { onSelectScreen(DesktopScreen.NOW_PLAYING) }
            )
            AppleSidebarNavItem(
                label = "Radio & Browse",
                icon = Icons.Rounded.Podcasts,
                isSelected = currentScreen == DesktopScreen.EXPLORE,
                onClick = { onSelectScreen(DesktopScreen.EXPLORE) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: LIBRARY
            AppleSidebarSectionHeader(title = "LIBRARY")
            AppleSidebarNavItem(
                label = "Songs",
                icon = Icons.Rounded.MusicNote,
                isSelected = currentScreen == DesktopScreen.LIBRARY,
                badge = if (songCount > 0) "$songCount" else null,
                onClick = { onSelectScreen(DesktopScreen.LIBRARY) }
            )
            AppleSidebarNavItem(
                label = "Folders",
                icon = Icons.Rounded.Folder,
                isSelected = currentScreen == DesktopScreen.FOLDERS,
                badge = if (folderCount > 0) "$folderCount" else null,
                onClick = { onSelectScreen(DesktopScreen.FOLDERS) }
            )
            AppleSidebarNavItem(
                label = "Favorites",
                icon = Icons.Rounded.Favorite,
                isSelected = currentScreen == DesktopScreen.FAVORITES,
                badge = if (favoriteCount > 0) "$favoriteCount" else null,
                onClick = { onSelectScreen(DesktopScreen.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: TOOLS
            AppleSidebarSectionHeader(title = "SETTINGS & TOOLS")
            AppleSidebarNavItem(
                label = "Equalizer",
                icon = Icons.Rounded.Tune,
                isSelected = currentScreen == DesktopScreen.EQUALIZER,
                onClick = { onSelectScreen(DesktopScreen.EQUALIZER) }
            )
            AppleSidebarNavItem(
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
            Divider(color = AppleMusicBorderSubtle, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Apple Silicon Native",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppleMusicTextTertiary
            )
        }
    }
}

@Composable
private fun AppleSidebarSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AppleMusicTextTertiary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun AppleSidebarNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) AppleMusicCard else Color.Transparent
    val tint = if (isSelected) AppleMusicRed else AppleMusicTextSecondary
    val textStyle = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(AppleNavItemShape)
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
                color = if (isSelected) AppleMusicTextPrimary else AppleMusicTextSecondary
            )
        }

        if (badge != null) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppleMusicTextTertiary
            )
        }
    }
}
