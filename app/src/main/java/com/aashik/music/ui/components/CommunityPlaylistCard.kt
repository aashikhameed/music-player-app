package com.aashik.music.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.aashik.music.ui.designsystem.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.*
import com.aashik.music.ui.*
import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*

private val PortraitSongCardShape = RoundedCornerShape(16.dp)
private val LandscapeFullSongShape = RoundedCornerShape(16.dp)
private val HeaderCapsuleShape = RoundedCornerShape(12.dp)
private val PillShape10 = RoundedCornerShape(8.dp)
private val StatusCapsuleShape = RoundedCornerShape(12.dp)
private val RadioCardShape = RoundedCornerShape(16.dp)
private val GenreChipShape = RoundedCornerShape(8.dp)
private val LiveBadgeShape = RoundedCornerShape(4.dp)
private val MoodCardShape = RoundedCornerShape(16.dp)
private val MoodStripeShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
private val BackPillShape = RoundedCornerShape(8.dp)
private val MobileMiniPlayerShape = RoundedCornerShape(16.dp)
private val MiniCodecBadgeShape = RoundedCornerShape(4.dp)
private val MobileNavBarShape = RoundedCornerShape(28.dp)
private val MobileActiveTabShape = RoundedCornerShape(16.dp)
private val ShelfArtworkShape = RoundedCornerShape(12.dp)
private val ShelfCardContainerShape = RoundedCornerShape(12.dp)
private val CircleBackButtonShape = CircleShape
private val CircleAvatarShape = CircleShape
private val MoreCardShape = RoundedCornerShape(12.dp)
private val MoreIconContainerShape = RoundedCornerShape(8.dp)
private val FocusBorderWidth = 1.8.dp

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun CommunityPlaylistCard(
    item: com.aashik.music.service.ExploreShelfItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(140.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(FocusBorderWidth, MaterialTheme.colorScheme.primary, ShelfCardContainerShape)
                else Modifier
            )
            .clip(ShelfCardContainerShape)
            .clickable(onClick = onClick)
    ) {
        // Artwork: 2x2 collage if 4 multi-thumbnails available, otherwise single artwork
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(ShelfCardContainerShape)
        ) {
            if (item.thumbnailUrls.size >= 4) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumArtImage(
                                path = item.thumbnailUrls[0],
                                artworkUrl = item.thumbnailUrls[0],
                                size = 70.dp,
                                borderRadius = 0.dp,
                                isHighRes = true
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumArtImage(
                                path = item.thumbnailUrls[1],
                                artworkUrl = item.thumbnailUrls[1],
                                size = 70.dp,
                                borderRadius = 0.dp,
                                isHighRes = true
                            )
                        }
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumArtImage(
                                path = item.thumbnailUrls[2],
                                artworkUrl = item.thumbnailUrls[2],
                                size = 70.dp,
                                borderRadius = 0.dp,
                                isHighRes = true
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AlbumArtImage(
                                path = item.thumbnailUrls[3],
                                artworkUrl = item.thumbnailUrls[3],
                                size = 70.dp,
                                borderRadius = 0.dp,
                                isHighRes = true
                            )
                        }
                    }
                }
            } else {
                AlbumArtImage(
                    path = item.artworkUrl,
                    artworkUrl = item.artworkUrl,
                    size = 140.dp,
                    borderRadius = 14.dp,
                    isHighRes = true
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Title
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle formatted as: playlist • [Creator] • [details]
        val subText = if (item.subtitle.isNotBlank() && !item.subtitle.startsWith("playlist", ignoreCase = true)) {
            "playlist • ${item.subtitle}"
        } else if (item.subtitle.isNotBlank()) {
            item.subtitle
        } else {
            "playlist"
        }

        Text(
            text = subText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
