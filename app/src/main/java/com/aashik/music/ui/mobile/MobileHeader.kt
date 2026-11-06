package com.aashik.music.ui.mobile

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
private val SearchBarShape = RoundedCornerShape(28.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun MobileHeader(
    selectedTab: LibraryTab,
    selectedFolder: String?,
    isSearchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearchOpen) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = SearchBarShape)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = SearchBarShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search songs, artists, albums...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.weight(1f)
            )
            // Single clean action: clear text if present, otherwise close search mode
            IconButton(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChanged("")
                    } else {
                        onToggleSearch()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    } else {
        val isSubPage = selectedTab == LibraryTab.FOLDERS ||
                selectedTab == LibraryTab.FAVORITES ||
                selectedTab == LibraryTab.RADIO

        val title = if (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
            try {
                File(selectedFolder).name.ifBlank { selectedFolder }
            } catch (_: Exception) {
                selectedFolder
            }
        } else when (selectedTab) {
            LibraryTab.FOLDERS -> "Folders"
            LibraryTab.FAVORITES -> "Favorites"
            LibraryTab.RADIO -> "Explore"
            LibraryTab.MORE -> "More"
            LibraryTab.ALL_SONGS -> "Music"
        }

        AppTopBar(
            title = title,
            onBack = if (isSubPage) onBack else null,
            actions = {
                if (selectedTab != LibraryTab.MORE) {
                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            modifier = modifier.fillMaxWidth()
        )
    }
}

private fun getMobileAudioCodecBadge(song: Song?): String {
    if (song == null) return ""
    if (song.isStream()) return "320K"
    val path = song.path.lowercase()
    return when {
        path.endsWith(".flac") -> "FLAC"
        path.endsWith(".wav") -> "WAV"
        path.endsWith(".m4a") || path.endsWith(".aac") -> "AAC"
        path.endsWith(".ogg") -> "OGG"
        path.endsWith(".mp3") -> "MP3"
        else -> song.getFileExtension().uppercase()
    }
}
