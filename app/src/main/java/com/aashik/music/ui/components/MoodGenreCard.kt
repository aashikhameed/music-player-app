package com.aashik.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.*
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

private val MoodCardShape = RoundedCornerShape(14.dp)
private val LiveBadgeShape = RoundedCornerShape(4.dp)
private val ArtworkThumbShape = RoundedCornerShape(8.dp)
private val FavoriteHeartRed = Color(0xFFFF5252)

// Curated vibrant fallback palette for categories without custom accent
private val CuratedGenrePalette = listOf(
    Color(0xFFE24B00), // Saturated Orange
    Color(0xFF7C3AED), // Royal Violet
    Color(0xFF0D9488), // Deep Teal
    Color(0xFF2563EB), // Vibrant Blue
    Color(0xFFDC2626), // Crimson Red
    Color(0xFFDB2777), // Hot Pink
    Color(0xFF16A34A), // Emerald Green
    Color(0xFFD97706), // Amber Gold
    Color(0xFF4F46E5), // Indigo
    Color(0xFF0891B2)  // Cyan
)

private fun getGenreCardColor(category: RadioCategory): Color {
    val rawColor = category.stripeColor
    if (rawColor == 0xFF606060L || rawColor == 0xFFCCCCCCL || rawColor == 0xFF8C8C8CL || rawColor == 0L) {
        val index = (category.id.hashCode() and 0x7FFFFFFF) % CuratedGenrePalette.size
        return CuratedGenrePalette[index]
    }
    return Color(rawColor)
}

private fun getContrastingCardBackground(category: RadioCategory): Color {
    val base = getGenreCardColor(category)
    val lum = base.luminance()
    return when {
        lum > 0.65f -> base.copy(red = base.red * 0.45f, green = base.green * 0.45f, blue = base.blue * 0.45f)
        lum > 0.40f -> base.copy(red = base.red * 0.65f, green = base.green * 0.65f, blue = base.blue * 0.65f)
        else -> base
    }
}

/**
 * Genre Category Card Component (Spotify / Apple Music Browse Style).
 * Features rich vibrant card background, bold typography, corner cover artwork,
 * live station count badge, and quick favorite toggle.
 */
@Composable
fun MoodGenreCard(
    category: RadioCategory,
    stationCount: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = remember(category) { getContrastingCardBackground(category) }

    Surface(
        onClick = onClick,
        shape = MoodCardShape,
        color = cardBg,
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail Cover Art in bottom-right corner
            if (category.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .size(46.dp)
                        .clip(ArtworkThumbShape)
                ) {
                    AlbumArtImage(
                        path = category.imageUrl,
                        artworkUrl = category.imageUrl,
                        size = 46.dp,
                        borderRadius = 8.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Podcasts,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Category Title + Live Badge / Subtitle on the left
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 56.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        letterSpacing = 0.15.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (stationCount > 0) {
                    Surface(
                        shape = LiveBadgeShape,
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "$stationCount live",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Radio & Streams",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Favorite Icon Button in top-right corner
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) FavoriteHeartRed else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
