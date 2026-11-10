package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.mobile.glass.MobileGlassSearchBar
import com.aashik.music.ui.mobile.glass.MobileGlassSegmentedPill
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

data class BrowseCategory(
    val title: String,
    val gradientColors: List<Color>
)

/**
 * 1:1 Mobile Search Screen matching designScreen/mobile/Search.png.
 * Features Large Title 'Search', Liquid Glass Capsule Search Bar,
 * Segmented scope pill ('Apple Music' / 'Library'),
 * Recently Searched history tokens with 'Clear' action,
 * and 2-Column vibrant browse categories grid.
 */
@Composable
fun MobileSearchScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedScopeIndex by remember { mutableIntStateOf(0) } // 0: Apple Music, 1: Library
    var searchHistory by remember {
        mutableStateOf(listOf("Imagine Dragons", "Alan Walker", "Coldplay", "Taylor Swift"))
    }

    val categories = remember {
        listOf(
            BrowseCategory("Spatial Audio", listOf(Color(0xFF1A2980), Color(0xFF26D0CE))),
            BrowseCategory("Hip-Hop", listOf(Color(0xFF3023AE), Color(0xFF53A0FD))),
            BrowseCategory("Pop", listOf(Color(0xFF0BA360), Color(0xFF3CBA92))),
            BrowseCategory("Rock", listOf(Color(0xFF2C3E50), Color(0xFF3498DB))),
            BrowseCategory("Dance", listOf(Color(0xFF4776E6), Color(0xFF8E54E9))),
            BrowseCategory("Chill", listOf(Color(0xFF134E5E), Color(0xFF71B280))),
            BrowseCategory("Charts", listOf(Color(0xFF0052D4), Color(0xFF4364F7))),
            BrowseCategory("Set Lists", listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)))
        )
    }

    val filteredSongs = remember(query, songs) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── LARGE TITLE 'SEARCH' ──────────────────────────────────────────────
        item {
            Text(
                text = "Search",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        // ─── LIQUID GLASS CAPSULE SEARCH BAR ───────────────────────────────────
        item {
            MobileGlassSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    if (it.isNotEmpty()) isSearchActive = true
                },
                onClear = { query = "" },
                isActive = isSearchActive,
                onActiveChange = { isSearchActive = it }
            )
        }

        // ─── SEGMENTED SCOPE PILL (Apple Music / Library) ──────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                MobileGlassSegmentedPill(
                    items = listOf("Apple Music", "Library"),
                    selectedIndex = selectedScopeIndex,
                    onItemSelected = { selectedScopeIndex = it }
                )
            }
        }

        // ─── CONDITIONAL CONTENT: SEARCH RESULTS VS IDLE BROWSE ────────────────
        if (query.isNotBlank()) {
            // Live Search Results
            item {
                Text(
                    text = "Top Results",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for \"$query\"",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                items(filteredSongs) { song ->
                    SearchResultSongRow(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        } else {
            // IDLE BROWSE STATE: RECENT SEARCHES & CATEGORY GRID

            // Recently Searched Tokens
            if (searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recently Searched",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear",
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { searchHistory = emptyList() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                items(searchHistory) { term ->
                    RecentSearchItemRow(
                        term = term,
                        onSelect = {
                            query = term
                            isSearchActive = true
                        },
                        onRemove = {
                            searchHistory = searchHistory.filter { it != term }
                        }
                    )
                }
            }

            // Browse Categories Header
            item {
                Text(
                    text = "Browse Categories",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            // 2-Column Categories Grid
            val chunkedCategories = categories.chunked(2)
            items(chunkedCategories.size) { rowIndex ->
                val rowCategories = chunkedCategories[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCategories.forEach { category ->
                        Box(modifier = Modifier.weight(1f)) {
                            BrowseCategoryCard(
                                category = category,
                                onClick = {
                                    query = category.title
                                    isSearchActive = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearchItemRow(
    term: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = term,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = "Remove",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SearchResultSongRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {},
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BrowseCategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(category.gradientColors))
            .mobileLiquidGlass(
                shape = RoundedCornerShape(12.dp),
                tintColor = Color.White.copy(alpha = 0.05f),
                borderColor = Color.White.copy(alpha = 0.12f)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = category.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp
        )
    }
}
