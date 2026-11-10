package com.aashik.music.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.utils.DesktopFolderPicker
import java.io.File

enum class DesktopScreen {
    NOW_PLAYING,
    LIBRARY,
    FOLDERS,
    FAVORITES,
    EXPLORE,
    EQUALIZER,
    SETTINGS
}

private val DarkNavItemShape = RoundedCornerShape(10.dp)

/**
 * Apple Music Liquid Glass Sidebar Navigation.
 * Matches 1:1 with designScreen/tablet/Home.png & Album.png:
 * - Frosted acrylic glass sidebar with specular right border.
 * - Library categories, Now Playing indicator, Live Radio.
 * - Bottom user profile card with macOS folder picker shortcut.
 */
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
            .background(GlassSidebar)
            .border(width = 1.dp, brush = GlassBorderBrush, shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Apple Music Icon + Branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkAccent.copy(alpha = 0.16f))
                        .border(GlassBorderSubtle, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = DarkAccent,
                        modifier = Modifier.size(20.dp)
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
                        text = "Desktop Edition",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: APPLE MUSIC STAGE
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

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: LIBRARY
            DarkSidebarSectionHeader(title = "LIBRARY")
            DarkSidebarNavItem(
                label = "All Songs",
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

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: TOOLS & AUDIO
            DarkSidebarSectionHeader(title = "SETTINGS & AUDIO")
            DarkSidebarNavItem(
                label = "10-Band Equalizer",
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

        // ─── BOTTOM PROFILE & OFFLINE DIRECTORY PICKER PILL ──────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(GlassBorderSpecular, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DarkAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkAccent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("macOS Library", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
                    Text(
                        text = File(DesktopLocalScanner.getStoredStorageDir()).name.ifBlank { "Default" },
                        fontSize = 10.sp,
                        color = DarkTextTertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .clickable {
                        val chosen = DesktopFolderPicker.pickFolder()
                        if (!chosen.isNullOrBlank()) {
                            DesktopLocalScanner.saveStorageDir(chosen)
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = DarkAccent, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Select Folder", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DarkTextSecondary)
            }
        }
    }
}

@Composable
private fun DarkSidebarSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = DarkTextTertiary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
    val bgModifier = if (isSelected) {
        Modifier
            .background(DarkAccent.copy(alpha = 0.16f), DarkNavItemShape)
            .border(1.dp, DarkAccent.copy(alpha = 0.35f), DarkNavItemShape)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(DarkNavItemShape)
            .then(bgModifier)
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
                tint = if (isSelected) DarkAccent else DarkTextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) DarkTextPrimary else DarkTextSecondary
            )
        }

        if (badge != null) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) DarkAccent else DarkTextTertiary
            )
        }
    }
}
