package com.aashik.music.ui.car

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

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val ClockPlayingGreen = Color(0xFF00E676)
private val ClockDarkGray = Color(0xFF455A64)
private val ClockLightGray = Color(0xFFB0BEC5)
private val FavoriteHeartRed = Color(0xFFFF5252)

@Composable
fun LibraryModeHeader(
    viewModel: MusicViewModel? = null,
    selectedTab: LibraryTab,
    selectedFolder: String?,
    songCount: Int = 0,
    folderCount: Int = 0,
    favoriteCount: Int = 0,
    radioCount: Int = 0,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBackFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleGrad = AppGradients.capsule(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)
    val activeBorder = AppGradients.border(isActive = true)

    Box(
        modifier = modifier.height(48.dp)
    ) {
        if (isSearchOpen) {
            // Inline Search Text Field with Top-Right Status Clock
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(brush = capsuleGrad, shape = HeaderCapsuleShape)
                        .border(width = 1.dp, brush = activeBorder, shape = HeaderCapsuleShape)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs, artists, folders...",
                                        style = MaterialTheme.typography.bodyMedium,
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (viewModel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TopRightStatusClock(viewModel = viewModel)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) {
                    val folderName = try {
                        File(selectedFolder).name.ifBlank { selectedFolder }
                    } catch (_: Exception) {
                        selectedFolder
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .background(brush = capsuleGrad, shape = PillShape10)
                                .border(width = 1.dp, brush = borderBrush, shape = PillShape10)
                                .clickable(onClick = onBackFolder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Back",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .weight(1f)
                                .background(brush = capsuleGrad, shape = PillShape10)
                                .border(width = 1.dp, brush = borderBrush, shape = PillShape10)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Left Section Title & Count Badge
                    val (sectionTitle, sectionCount) = when (selectedTab) {
                        LibraryTab.ALL_SONGS, LibraryTab.MORE -> "All Songs" to songCount
                        LibraryTab.FOLDERS -> "Folders" to folderCount
                        LibraryTab.FAVORITES -> "Favorites" to favoriteCount
                        LibraryTab.RADIO -> "Explore" to radioCount
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (sectionCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = PillShape10
                            ) {
                                Text(
                                    text = "$sectionCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Top Right Action Cluster: Navigation Mode Icons + Search + Status Clock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                    // Compact Navigation & Search Capsule
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .background(brush = capsuleGrad, shape = PillShape10)
                            .border(width = 0.8.dp, brush = borderBrush, shape = PillShape10)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // 1. All Songs Icon Button
                        val isAllSongs = selectedTab == LibraryTab.ALL_SONGS
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAllSongs)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.ALL_SONGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "All Songs",
                                tint = if (isAllSongs) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 2. Folders Icon Button
                        val isFolders = selectedTab == LibraryTab.FOLDERS
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFolders)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.FOLDERS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Folders",
                                tint = if (isFolders) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 3. Favorites Heart Icon Button
                        val isFavorites = selectedTab == LibraryTab.FAVORITES
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFavorites)
                                        FavoriteHeartRed.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.FAVORITES) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = if (isFavorites) FavoriteHeartRed
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 4. Stream / Radio Icon Button next to Fav icon
                        val isRadio = selectedTab == LibraryTab.RADIO
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRadio)
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { onTabSelected(LibraryTab.RADIO) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = "Explore",
                                tint = if (isRadio) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(18.dp)
                                .width(0.6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )

                        // 4. Search Icon Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleSearch),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (viewModel != null) {
                        TopRightStatusClock(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
