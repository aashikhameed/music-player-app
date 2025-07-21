package com.aashik.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aashik.music.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    progress: Float = 0f,
    cardWidth: Dp
) {
    val borderRadius = if (isPlaying) 25.dp else 10.dp
    val backgroundColor = if (isPlaying) MaterialTheme.colorScheme.outlineVariant
    else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .padding(2.dp)
            .width(cardWidth)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(borderRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(path = song.path, borderRadius = borderRadius, spinning = isPlaying)

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

