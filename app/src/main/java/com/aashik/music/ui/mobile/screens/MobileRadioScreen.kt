package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import com.aashik.music.ui.mobile.glass.MobileGlassBadgePill
import com.aashik.music.ui.mobile.glass.MobileGlassCard
import com.aashik.music.ui.mobile.glass.mobileLiquidGlass

private val RadioCardShape = RoundedCornerShape(14.dp)

data class RadioStation(
    val name: String,
    val showName: String,
    val host: String,
    val isLive: Boolean = true,
    val colors: List<Color>
)

/**
 * 1:1 Mobile Radio Screen.
 * Features Apple Music 1, Apple Music Hits, Apple Music Country live radio stations,
 * broadcaster cards, and genre broadcast shelves.
 */
@Composable
fun MobileRadioScreen(
    onStationClick: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveStations = listOf(
        RadioStation(
            name = "Apple Music 1",
            showName = "The Zane Lowe Show",
            host = "Zane Lowe",
            isLive = true,
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        ),
        RadioStation(
            name = "Apple Music Hits",
            showName = "Top Hits Spotlight",
            host = "Estelle",
            isLive = true,
            colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE))
        ),
        RadioStation(
            name = "Apple Music Country",
            showName = "Today's Country",
            host = "Kelleigh Bannen",
            isLive = true,
            colors = listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── TOP HEADER ────────────────────────────────────────────────────────
        item {
            Text(
                text = "Radio",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            )
        }

        // ─── FEATURED LIVE STATION BANNER (Apple Music 1) ──────────────────────
        item {
            MobileGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(230.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    Color(0xFF020617)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            MobileGlassBadgePill(
                                text = "LIVE NOW",
                                useCyan = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Apple Music 1",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "The Zane Lowe Show",
                                color = Color(0xFF00E5FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "The world's biggest artists, breaking music, and culture stories.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
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
                                text = "Broadcasting globally",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                                    .clickable { onStationClick(liveStations.first()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Listen Live",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── STATIONS CAROUSEL ─────────────────────────────────────────────────
        item {
            Text(
                text = "Live Stations",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp)
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(liveStations) { station ->
                    Column(
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { onStationClick(station) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RadioCardShape)
                                .background(SolidColor(station.colors.first()))
                                .mobileLiquidGlass(RadioCardShape)
                                .padding(14.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Rounded.Radio,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = station.name,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = station.showName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = station.host,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

