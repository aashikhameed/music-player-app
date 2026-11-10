package com.aashik.music.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.*
import com.aashik.music.service.DownloadStatus
import com.aashik.music.theme.AppGradients
import com.aashik.music.ui.*
import com.aashik.music.ui.components.*
import com.aashik.music.ui.mobile.*
import com.aashik.music.ui.car.*
import com.aashik.music.utils.formatTime
import com.aashik.music.utils.getCurrentTimeWithAmPm
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import java.io.File
import kotlinx.coroutines.isActive

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategorySongRowCard(
    song: Song,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = when {
        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.12f)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }

    Surface(
        shape = RadioCardShape,
        color = cardBg,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(FocusBorderWidth, MaterialTheme.colorScheme.primary, RadioCardShape)
                else Modifier
            )
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail
            AlbumArtImage(
                path = song.path,
                artworkUrl = song.artworkUrl,
                size = 46.dp,
                borderRadius = 8.dp,
                isPlaying = isPlaying
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Track Title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.duration > 0L) {
                        Text(
                            text = "• ${formatTime(song.duration)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
