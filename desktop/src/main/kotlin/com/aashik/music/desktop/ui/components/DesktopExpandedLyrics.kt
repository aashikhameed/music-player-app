package com.aashik.music.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.service.DesktopLyricLine

@Composable
fun DesktopExpandedLyrics(
    lyrics: List<DesktopLyricLine>,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00E5FF),
    textColor: Color = Color(0xFFF1F5F9),
    secondaryTextColor: Color = Color(0xFF94A3B8)
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Subtitles,
                        contentDescription = null,
                        tint = secondaryTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No synchronized lyrics available",
                        fontSize = 15.sp,
                        color = secondaryTextColor
                    )
                }
            }
        } else {
            val activeIndex = lyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
            val listState = rememberLazyListState()

            LaunchedEffect(activeIndex) {
                if (activeIndex in lyrics.indices) {
                    listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    Text(
                        text = line.text,
                        fontSize = if (isActive) 20.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) primaryColor else secondaryTextColor.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeek(line.timeMs) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Top Right Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .background(Color(0xFF1B202A).copy(alpha = 0.8f), shape = CircleShape)
                .clip(CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Lyrics",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
