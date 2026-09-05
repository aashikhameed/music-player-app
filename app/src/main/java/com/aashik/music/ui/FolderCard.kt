package com.aashik.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.MusicFolder
import com.aashik.music.theme.AppGradients

private val FolderCardShape = RoundedCornerShape(14.dp)
private val FolderIconShape = RoundedCornerShape(10.dp)
private val CountPillShape = RoundedCornerShape(6.dp)

private val DarkFolderBg = Brush.linearGradient(listOf(Color(0xFF2B2113), Color(0xFF1B140A)))
private val LightFolderBg = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
private val DarkFolderBorder = Brush.linearGradient(listOf(Color(0xFFFFB300).copy(alpha = 0.5f), Color(0xFFFF8F00).copy(alpha = 0.2f)))
private val LightFolderBorder = Brush.linearGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.5f), Color(0xFFD97706).copy(alpha = 0.25f)))
private val DarkFolderIconTint = Color(0xFFFFB300)
private val LightFolderIconTint = Color(0xFFD97706)
private val DarkCountPillBg = Color(0xFF1E2638)
private val LightCountPillBg = Color(0xFFE2E8F0)
private val DarkCountPillText = Color(0xFF94A3B8)
private val LightCountPillText = Color(0xFF475569)

@Composable
fun FolderCard(
    folder: MusicFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardGradient = AppGradients.card(isActive = false)
    val borderBrush = AppGradients.border(isActive = false)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp) // Matched with SongCard height, tuned for 1280×720 @ 160dpi
            .clip(FolderCardShape)
            .background(brush = cardGradient, shape = FolderCardShape)
            .border(width = 1.dp, brush = borderBrush, shape = FolderCardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

            // Warm Amber/Gold Automotive Folder Icon Capsule
            val folderBg = if (isDark) DarkFolderBg else LightFolderBg
            val folderBorder = if (isDark) DarkFolderBorder else LightFolderBorder
            val folderIconTint = if (isDark) DarkFolderIconTint else LightFolderIconTint

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(FolderIconShape)
                    .background(folderBg)
                    .border(1.dp, folderBorder, FolderIconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = folderIconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countPillBg = if (isDark) DarkCountPillBg else LightCountPillBg
                    val countPillText = if (isDark) DarkCountPillText else LightCountPillText

                    Box(
                        modifier = Modifier
                            .clip(CountPillShape)
                            .background(countPillBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${folder.songCount} ${if (folder.songCount == 1) "TRACK" else "TRACKS"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = countPillText,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
