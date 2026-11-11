package com.aashik.music.ui.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.DropdownMenu
import com.aashik.music.ui.designsystem.DropdownMenuItem
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme

/** Actions offered by every song's "•••" menu; provided once by the app shell. */
@Immutable
class SongActions(
    val playNext: (Song) -> Unit,
    val addToQueue: (Song) -> Unit,
    val toggleFavorite: (Song) -> Unit,
    val isFavorite: (Song) -> Boolean,
    val goToAlbum: (Song) -> Unit,
    val goToArtist: (Song) -> Unit
)

val LocalSongActions = staticCompositionLocalOf<SongActions?> { null }

/** "•••" button that opens Play Next / Add to Queue / Favorite / Go to Album / Go to Artist. */
@Composable
fun SongMoreButton(song: Song, tint: Color = MobileTheme.text, modifier: Modifier = Modifier) {
    val actions = LocalSongActions.current ?: return
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More options for ${song.title}", tint = tint, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Play Next") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                onClick = { open = false; actions.playNext(song) }
            )
            DropdownMenuItem(
                text = { Text("Add to Queue") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                onClick = { open = false; actions.addToQueue(song) }
            )
            if (!song.isStream()) {
                val fav = actions.isFavorite(song)
                DropdownMenuItem(
                    text = { Text(if (fav) "Remove from Favorites" else "Add to Favorites") },
                    leadingIcon = { Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null) },
                    onClick = { open = false; actions.toggleFavorite(song) }
                )
            }
            if (song.album.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Go to Album") },
                    leadingIcon = { Icon(Icons.Rounded.Album, contentDescription = null) },
                    onClick = { open = false; actions.goToAlbum(song) }
                )
            }
            if (song.artist.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Go to Artist") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    onClick = { open = false; actions.goToArtist(song) }
                )
            }
        }
    }
}

/** Large cover for a collection: the first track's artwork, decoded at [sharpSize] px. */
@Composable
fun CollectionCover(tracks: List<Song>, size: Dp, cornerRadius: Dp, sharpSize: Int = 512) {
    val first = tracks.firstOrNull()
    if (first != null) {
        AlbumArtImage(path = first.path, artworkUrl = first.artworkUrl, size = size, borderRadius = cornerRadius, sharpSize = sharpSize)
    } else {
        val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
        Box(Modifier.size(size).clip(shape).background(MobileTheme.surface))
    }
}

/** 2×2 mosaic of the first four distinct album covers; a single cover when there are fewer. */
@Composable
fun CollectionMosaic(tracks: List<Song>, size: Dp, cornerRadius: Dp) {
    val distinct = remember(tracks) { tracks.distinctBy { it.album.ifBlank { it.path } }.take(4) }
    if (distinct.size < 4) {
        CollectionCover(tracks, size, cornerRadius)
        return
    }
    val tile = (size - 2.dp) / 2
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Column(
        modifier = Modifier.size(size).clip(shape),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        distinct.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { s -> AlbumArtImage(path = s.path, artworkUrl = s.artworkUrl, size = tile, borderRadius = 4.dp, sharpSize = 256) }
            }
        }
    }
}

/** Body text clamped to [collapsedLines]; the MORE/LESS toggle only appears when text overflows. */
@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier, collapsedLines: Int = 3) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = text,
            color = MobileTheme.text.copy(alpha = 0.8f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
        )
        if (overflows || expanded) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (expanded) "LESS" else "MORE",
                color = MobileTheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

/** Honest quality label for a set of local files: "Lossless" only when every track is lossless. */
fun qualityLabel(tracks: List<Song>): String? {
    if (tracks.isEmpty() || tracks.any { it.isStream() }) return null
    val lossless = setOf("flac", "alac", "wav", "aiff", "aif")
    val exts = tracks.map { it.path.substringAfterLast('.', "").lowercase() }.toSet()
    return when {
        exts.all { it in lossless } -> "LOSSLESS"
        exts.size == 1 && exts.first().isNotBlank() -> if (exts.first() == "m4a") "AAC" else exts.first().uppercase()
        else -> null
    }
}

fun runtimeLabel(tracks: List<Song>): String {
    val minutes = tracks.sumOf { it.duration } / 60_000
    val count = if (tracks.size == 1) "1 Song" else "${tracks.size} Songs"
    return when {
        minutes >= 60 -> "$count, ${minutes / 60} hr ${minutes % 60} min"
        else -> "$count, $minutes min"
    }
}

/** Album artist: the shared artist when every track agrees, otherwise "Various Artists". */
fun albumArtistOf(tracks: List<Song>): String {
    val artists = tracks.map { it.artist.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
    return when (artists.size) {
        0 -> "Unknown Artist"
        1 -> artists.first()
        else -> "Various Artists"
    }
}
