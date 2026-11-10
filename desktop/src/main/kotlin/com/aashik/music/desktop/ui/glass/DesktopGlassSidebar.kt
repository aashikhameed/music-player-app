package com.aashik.music.desktop.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.ui.DesktopScreen

/**
 * Left Navigation Sidebar with Backdrop liquid glass material (width 280dp).
 */
@Composable
fun DesktopGlassSidebar(
    currentScreen: DesktopScreen,
    onScreenChange: (DesktopScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .liquidGlassEffect(
                shape = RoundedCornerShape(0.dp),
                tintColor = Color(0xFF101216).copy(alpha = 0.85f),
                borderColor = Color.White.copy(alpha = 0.08f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top Header: Edit button & Sidebar icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "◫",
                    color = Color(0xFF94A3B8),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .liquidGlassEffect(
                        shape = RoundedCornerShape(10.dp),
                        tintColor = Color.White.copy(alpha = 0.06f),
                        borderColor = Color.White.copy(alpha = 0.08f)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("Search", fontSize = 13.5.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Navigation List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                GlassSidebarRow("🏠", "Home", currentScreen == DesktopScreen.HOME) {
                    onScreenChange(DesktopScreen.HOME)
                }
                GlassSidebarRow("✨", "New", currentScreen == DesktopScreen.NEW_RELEASES) {
                    onScreenChange(DesktopScreen.NEW_RELEASES)
                }
                GlassSidebarRow("📻", "Radio", currentScreen == DesktopScreen.RADIO) {
                    onScreenChange(DesktopScreen.RADIO)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Header: Library
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text("⌄", color = Color(0xFF64748B), fontSize = 12.sp)
                }

                GlassSidebarRow("🎧", "Now Playing", currentScreen == DesktopScreen.NOW_PLAYING) {
                    onScreenChange(DesktopScreen.NOW_PLAYING)
                }
                GlassSidebarRow("🕒", "Recently Added", currentScreen == DesktopScreen.RECENTLY_ADDED) {
                    onScreenChange(DesktopScreen.RECENTLY_ADDED)
                }
                GlassSidebarRow("👤", "Artists", currentScreen == DesktopScreen.ARTISTS) {
                    onScreenChange(DesktopScreen.ARTISTS)
                }
                GlassSidebarRow("💿", "Albums", currentScreen == DesktopScreen.ALBUMS) {
                    onScreenChange(DesktopScreen.ALBUMS)
                }
                GlassSidebarRow("🎵", "Songs", currentScreen == DesktopScreen.SONGS) {
                    onScreenChange(DesktopScreen.SONGS)
                }
                GlassSidebarRow("📁", "Folders", currentScreen == DesktopScreen.FOLDERS) {
                    onScreenChange(DesktopScreen.FOLDERS)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Header: Playlists
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlists",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text("⌄", color = Color(0xFF64748B), fontSize = 12.sp)
                }

                GlassSidebarRow("📑", "All Playlists", currentScreen == DesktopScreen.PLAYLISTS) {
                    onScreenChange(DesktopScreen.PLAYLISTS)
                }
                GlassSidebarRow("❤️", "Favorites", currentScreen == DesktopScreen.FAVORITES) {
                    onScreenChange(DesktopScreen.FAVORITES)
                }
            }

            // Bottom User Profile Capsule (248dp × 48dp, r=24dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .liquidGlassEffect(
                        shape = RoundedCornerShape(24.dp),
                        tintColor = Color.White.copy(alpha = 0.06f),
                        borderColor = Color.White.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .liquidGlassEffect(
                                shape = CircleShape,
                                tintColor = Color.White.copy(alpha = 0.15f),
                                borderColor = Color.White.copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text("John Smith", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Apple Music", color = Color(0xFF64748B), fontSize = 10.5.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun GlassSidebarRow(
    icon: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val tint = when {
        isSelected -> Color(0xFF00E5FF).copy(alpha = 0.15f)
        isHovered -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val border = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.30f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .liquidGlassEffect(
                shape = RoundedCornerShape(19.dp),
                tintColor = tint,
                borderColor = border
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 15.sp,
                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}
