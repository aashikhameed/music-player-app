package com.aashik.music.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QueueMusic
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

private val QueueCardShape = RoundedCornerShape(12.dp)

@Composable
fun DesktopQueueDrawer(
    queue: List<DesktopSong>,
    onPlayTrack: (DesktopSong, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF131720),
    primaryColor: Color = Color(0xFF00E5FF),
    textColor: Color = Color(0xFFF1F5F9),
    secondaryColor: Color = Color(0xFF94A3B8)
) {
    Column(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(backgroundColor)
            .border(width = 1.dp, color = Color(0xFF252F42))
            .padding(16.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Upcoming Queue (${queue.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1B202A), shape = CircleShape)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = secondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Queue Items
        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Queue is empty", color = secondaryColor, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(QueueCardShape)
                            .background(Color(0xFF1A202C))
                            .clickable { onPlayTrack(song, index) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.width(20.dp)
                        )
                        DesktopArtwork(url = song.artworkUrl, size = 36.dp, shape = RoundedCornerShape(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 11.sp,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
