package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.RadioCategory
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.components.GenreDetailScreen
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.LiquidButton
import com.aashik.music.viewmodel.MusicViewModel

// Pre-allocated static shapes hoisted to top-level constants for zero GC churn on automotive hardware
private val RadioTileShape = RoundedCornerShape(16.dp)
private val RadioArtworkRightShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)

// Curated vibrant Apple Music-style palette for category tiles
private val CuratedTilePalette = listOf(
    Color(0xFF880E4F), // Deep Magenta / Plum
    Color(0xFF1E293B), // Dark Slate
    Color(0xFFB71C1C), // Deep Crimson Red
    Color(0xFFE11D48), // Bright Hot Red
    Color(0xFFE63946), // Coral Crimson
    Color(0xFF2563EB), // Vibrant Royal Blue
    Color(0xFFD97706), // Warm Amber / Country Gold
    Color(0xFFEC4899), // Bubblegum Pop Pink
    Color(0xFFDB2777), // Deep Rose Pink
    Color(0xFF4D7C0F), // Olive Gold Green
    Color(0xFF7C3AED), // Royal Violet
    Color(0xFF0D9488), // Deep Teal
    Color(0xFFEA580C), // Saturated Orange
    Color(0xFF4338CA)  // Indigo
)

internal fun getTileBackgroundColor(category: RadioCategory, index: Int): Color {
    val raw = category.stripeColor
    val isNeutral = raw == 0xFF606060L || raw == 0xFF8C8C8CL || raw == 0xFFCCCCCCL || raw == 0L
    val base = if (isNeutral) {
        val paletteIndex = (category.id.hashCode().let { if (it < 0) -it else it } + index) % CuratedTilePalette.size
        CuratedTilePalette[paletteIndex]
    } else {
        Color(raw)
    }

    // Ensure text legibility: darken overly bright colors
    val lum = base.luminance()
    return when {
        lum > 0.60f -> base.copy(red = base.red * 0.45f, green = base.green * 0.45f, blue = base.blue * 0.45f)
        lum > 0.40f -> base.copy(red = base.red * 0.65f, green = base.green * 0.65f, blue = base.blue * 0.65f)
        else -> base
    }
}

/** Radio genre/mood tile used in Search browse grid: sharp artwork on the right, favorite heart top-left. */
@Composable
internal fun RadioCategoryTile(
    category: RadioCategory,
    index: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileBg = remember(category.id) { getTileBackgroundColor(category, index) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RadioTileShape)
            .background(tileBg)
            .clickable(onClick = onClick)
    ) {
        if (category.imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(92.dp)
                    .clip(RadioArtworkRightShape)
            ) {
                AlbumArtImage(
                    path = category.imageUrl,
                    artworkUrl = category.imageUrl,
                    size = 104.dp,
                    borderRadius = 0.dp,
                    sharpSize = 256,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = category.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp, end = 82.dp)
        )
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) Color(0xFFFF2D55) else Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RadioTileShape)
                .clickable(onClick = onFavoriteClick)
                .padding(10.dp)
                .size(20.dp)
        )
    }
}
