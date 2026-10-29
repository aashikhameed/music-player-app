package com.aashik.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.theme.AppGradients

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

private val SongCardShape = RoundedCornerShape(14.dp)
private val ArtVignetteShape = RoundedCornerShape(10.dp)
private val BadgeShape = RoundedCornerShape(6.dp)

private val BadgeDarkBg = Color(0xFF1E2638)
private val BadgeLightBg = Color(0xFFE2E8F0)
private val BadgeDarkText = Color(0xFF94A3B8)
private val BadgeLightText = Color(0xFF475569)
private val VignetteBlack = Color.Black.copy(alpha = 0.45f)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun LiveAnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val h1 = if (isPlaying) 0.40f else 0.35f
    val h2 = if (isPlaying) 0.95f else 0.45f
    val h3 = if (isPlaying) 0.60f else 0.30f
    val h4 = if (isPlaying) 0.85f else 0.40f

    Canvas(modifier = modifier.size(18.dp)) {
        val totalHeight = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        val heights = floatArrayOf(h1, h2, h3, h4)

        for (i in 0 until 4) {
            val barH = totalHeight * heights[i]
            val left = i * (barWidth + spacing)
            val top = totalHeight - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = null,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null
) {
    val widthModifier = if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
    val cardGradient = AppGradients.card(isActive = isPlaying)
    val borderBrush = AppGradients.border(isActive = isPlaying)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(76.dp) // 1280×720 @ 160dpi: 76dp card = clear automotive touch target
            .clip(SongCardShape)
            .background(brush = cardGradient, shape = SongCardShape)
            .border(
                width = if (isPlaying) 1.6.dp else 1.dp,
                brush = borderBrush,
                shape = SongCardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork with active indicator
            Box(contentAlignment = Alignment.Center) {
                AlbumArtImage(
                    path = song.path,
                    size = 56.dp,  // 56dp — fills the 76dp card nicely at 160dpi
                    borderRadius = 10.dp,
                    isPlaying = isPlaying
                )

                if (isPlaying) {
                    // Semi-translucent dark vignette over art with live equalizer overlay
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(ArtVignetteShape)
                            .background(VignetteBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveAnimatedEqualizer(
                            isPlaying = true,
                            barColor = if (isDark) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title.ifEmpty { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp  // 15sp for glanceable at automotive distance on 160dpi
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = song.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), // bumped for 160dpi
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Codec & Duration Micro-Pill
                    val badgeBg = if (isPlaying) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                    } else {
                        if (isDark) BadgeDarkBg else BadgeLightBg
                    }
                    val badgeTextColor = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) BadgeDarkText else BadgeLightText
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBg, shape = BadgeShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.getBadgeLabel(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            letterSpacing = 0.4.sp
                        )
                    }

                    if (onFavoriteClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorite" else "Not favorite",
                            tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { onFavoriteClick() }
                        )
                    }
                }
            }
        }
    }
}
