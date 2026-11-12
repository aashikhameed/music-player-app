package com.aashik.music.ui.mobile.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Library
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

enum class MobileNavTab(val title: String, val icon: ImageVector) {
    MUSIC("Music", Lucide.Music),
    LIBRARY("Library", Lucide.Library),
    SEARCH("Search", Lucide.Search)
}

private val AllNavTabs = MobileNavTab.entries

/**
 * Mobile Floating Bottom Navigation Bar.
 * Single liquid-glass capsule (Music, Library, Search) per designScreen reference:
 * one draggable lens indicator slides across every tab, including Search.
 * Strictly NEVER uses border color or border width.
 */
@Composable
fun MobileGlassBottomNavBar(
    selectedTab: MobileNavTab,
    onTabSelected: (MobileNavTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val nonNullBackdrop = backdrop ?: rememberLayerBackdrop()
    LiquidBottomBar(
        tabs = AllNavTabs,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier
            .fillMaxWidth()
            // Full-width (incl. the 16dp side margins) so dragging the lens to the screen edge
            // is not stolen by the system back gesture, which would cancel the press highlight.
            .systemGestureExclusion()
            .padding(horizontal = DockGutter),
        backdrop = nonNullBackdrop,
        accentColor = MobileTheme.accent,
        inactiveColor = MobileTheme.textSecondary
    )
}
