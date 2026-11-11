package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.model.domain.DomainPlaylist
import androidx.compose.ui.graphics.SolidColor
import com.aashik.music.ui.mobile.glass.MobileGlassBadgePill
import com.aashik.music.ui.mobile.glass.MobileGlassCard
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

private val ChartCardShape = RoundedCornerShape(14.dp)

/**
 * 1:1 Mobile New & Charts Screen matching designScreen/mobile/New.png.
 * Features Large Title 'New', Exclusive Editorial Hero Banner,
 * 'Daily Top 100' horizontal carousel, 4-Row chunked 'Latest Songs',
 * and 'City Charts' horizontal carousel.
 */
@Composable
fun MobileNewScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (DomainPlaylist) -> Unit,
    onSeeAllCharts: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── HEADER: TITLE & USER AVATAR ────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New",
                    color = MobileTheme.text,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                // Profile Avatar Pill
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF263238)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JS",
                        color = Color(0xFFECEFF1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ─── EXCLUSIVE EDITORIAL HERO BANNER (361x240pt) ────────────────────────
        item {
            ExclusiveEditorialHeroBanner(
                onPlayClick = {
                    songs.firstOrNull()?.let(onSongClick)
                }
            )
        }

        // ─── DAILY TOP 100 SECTION ──────────────────────────────────────────────
        item {
            SectionHeader(
                title = "Daily Top 100",
                onSeeAll = onSeeAllCharts
            )
        }

        item {
            DailyTop100Row(
                onChartClick = { chartName ->
                    val playlist = DomainPlaylist(
                        id = chartName,
                        title = chartName,
                        curator = "Apple Music",
                        description = "The most played songs right now, updated every day.",
                        songs = songs.take(20)
                    )
                    onPlaylistClick(playlist)
                }
            )
        }

        // ─── LATEST SONGS (4-ROW CHUNKED CAROUSEL) ──────────────────────────────
        item {
            SectionHeader(
                title = "Latest Songs",
                onSeeAll = onSeeAllCharts
            )
        }

        item {
            LatestSongsChunkCarousel(
                songs = songs,
                onSongClick = onSongClick
            )
        }

        // ─── CITY CHARTS SECTION ────────────────────────────────────────────────
        item {
            SectionHeader(
                title = "City Charts",
                onSeeAll = onSeeAllCharts
            )
        }

        item {
            CityChartsRow(
                onCityClick = { cityName ->
                    val playlist = DomainPlaylist(
                        id = cityName,
                        title = "Top 25: $cityName",
                        curator = "Apple Music",
                        description = "The most played tracks in $cityName this week.",
                        songs = songs.take(25)
                    )
                    onPlaylistClick(playlist)
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSeeAll
            )
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MobileTheme.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "See All",
            tint = MobileTheme.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ExclusiveEditorialHeroBanner(
    onPlayClick: () -> Unit
) {
    MobileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(240.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MobileTheme.surface,
                            MobileTheme.surfaceDeep,
                            Color(0xFF020617)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    MobileGlassBadgePill(
                        text = "EXCLUSIVE PLAYLIST",
                        useCyan = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Best Songs of 2025",
                        color = MobileTheme.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "The songs that brought us from reflection to release in 2025.",
                        color = MobileTheme.textSecondary,
                        fontSize = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Apple Music Pop",
                        color = MobileTheme.textTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Play Button Circle in Ice-Cyan
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MobileTheme.accent)
                            .clickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = MobileTheme.onText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTop100Row(
    onChartClick: (String) -> Unit
) {
    val charts = listOf(
        Pair("Top 100: Global", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
        Pair("Top 100: USA", listOf(Color(0xFF141E30), Color(0xFF243B55))),
        Pair("Top 100: UK", listOf(Color(0xFF1E3C72), Color(0xFF2A5298))),
        Pair("Top 100: Japan", listOf(Color(0xFF232526), Color(0xFF414345)))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(charts) { (chartName, gradientColors) ->
            Column(
                modifier = Modifier
                    .width(155.dp)
                    .clickable { onChartClick(chartName) }
            ) {
                Box(
                    modifier = Modifier
                        .size(155.dp)
                        .clip(ChartCardShape)
                        .background(SolidColor(gradientColors.first()))
                        .mobileLiquidGlass(ChartCardShape)
                        .padding(14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            text = "TOP",
                            color = MobileTheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "100",
                            color = MobileTheme.text,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 34.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = chartName,
                    color = MobileTheme.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Apple Music",
                    color = MobileTheme.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LatestSongsChunkCarousel(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    // Chunks of 4 songs per column block
    val displaySongs = if (songs.isEmpty()) {
        List(8) { Song(id = it.toString(), title = "Sample Track ${it + 1}", artist = "Featured Artist", album = "New Hits", duration = 210000L, path = "sample_$it") }
    } else songs

    val chunkedSongs = displaySongs.chunked(4)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(chunkedSongs) { columnSongs ->
            Column(
                modifier = Modifier.width(320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                columnSongs.forEach { song ->
                    LatestSongItemRow(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestSongItemRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Artwork
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MobileTheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MobileTheme.accent.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title.ifBlank { "Untitled" },
                color = MobileTheme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist.ifBlank { "Unknown Artist" },
                color = MobileTheme.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More Options
        IconButton(
            onClick = {},
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = MobileTheme.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CityChartsRow(
    onCityClick: (String) -> Unit
) {
    val cities = listOf(
        Pair("New York City", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
        Pair("Los Angeles", listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))),
        Pair("Tokyo", listOf(Color(0xFF3A6073), Color(0xFF3A7BD5))),
        Pair("London", listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(cities) { (city, colors) ->
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onCityClick(city) }
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(ChartCardShape)
                        .background(SolidColor(colors.first().copy(alpha = 0.85f)))
                        .mobileLiquidGlass(ChartCardShape)
                        .padding(14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            text = "TOP 25",
                            color = MobileTheme.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = city,
                            color = MobileTheme.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Top 25: $city",
                    color = MobileTheme.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Apple Music",
                    color = MobileTheme.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
