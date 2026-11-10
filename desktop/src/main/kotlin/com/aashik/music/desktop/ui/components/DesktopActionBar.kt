package com.aashik.music.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.ui.DesktopArtwork

private val NextSongActionBarShape = RoundedCornerShape(12.dp)
private val FavoriteRedColor = Color(0xFFFF4081)

@Composable
fun DesktopActionBar(
    currentSong: DesktopSong?,
    upcomingSongs: List<DesktopSong>,
    isQueueOpen: Boolean,
    onToggleQueue: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    isRadioActive: Boolean,
    isRadioLoading: Boolean,
    onStartRadio: () -> Unit,
    onPlayNextSong: (DesktopSong) -> Unit,
    modifier: Modifier = Modifier,
    actionButtonBg: Color = Color(0xFF1A222F),
    controlInactiveColor: Color = Color(0xFF94A3B8),
    primaryColor: Color = Color(0xFF00E5FF)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        // 1. Queue Drawer Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isQueueOpen) primaryColor.copy(alpha = 0.22f) else actionButtonBg,
                    shape = CircleShape
                )
                .clickable { onToggleQueue() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = "Upcoming Queue Drawer",
                tint = if (isQueueOpen) primaryColor else controlInactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Favorite Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isFavorite) FavoriteRedColor.copy(alpha = 0.22f) else actionButtonBg,
                    shape = CircleShape
                )
                .clickable { onToggleFavorite() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) FavoriteRedColor else controlInactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Start Radio Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isRadioActive) primaryColor.copy(alpha = 0.22f) else actionButtonBg,
                    shape = CircleShape
                )
                .clickable { onStartRadio() },
            contentAlignment = Alignment.Center
        ) {
            if (isRadioLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = primaryColor
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = "Start Radio",
                    tint = if (isRadioActive) primaryColor else controlInactiveColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. Next Song Container Card
        val nextSong = upcomingSongs.firstOrNull()
        if (nextSong != null) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .background(color = actionButtonBg, shape = NextSongActionBarShape)
                    .border(width = 1.dp, color = Color(0xFF283445), shape = NextSongActionBarShape)
                    .clickable { onPlayNextSong(nextSong) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopArtwork(
                    url = nextSong.artworkUrl,
                    size = 32.dp,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier.widthIn(min = 80.dp, max = 220.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = nextSong.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NEXT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor
                        )
                    }
                    Text(
                        text = nextSong.artist,
                        fontSize = 10.sp,
                        color = controlInactiveColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
