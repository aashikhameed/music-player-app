package com.aashik.music.ui.car.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel

private val DestructiveRed = Color(0xFFFF453A)
private val ActionFavoriteRed = Color(0xFFFF375F)

private data class SongAction(val icon: ImageVector, val label: String, val tint: Color? = null, val run: () -> Unit)

/**
 * Song actions (legacy SongActionDialog, same actions and conditions): play next, add to queue,
 * favourite, start radio, download / re-download / delete download (streams), delete from storage.
 * Solid panel sheet with scrim; tiles are 2-up 64 dp targets.
 */
@Composable
internal fun CarSongActionSheet(
    song: Song?,
    viewModel: MusicViewModel,
    isFavorite: Boolean,
    onDeleteRequest: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    // Keep showing the last song while the exit animation runs (updated after composition,
    // never written during it).
    var lastSong by remember { mutableStateOf<Song?>(null) }
    SideEffect { if (song != null) lastSong = song }
    val shown = song ?: lastSong
    val visible = song != null
    // The search keyboard would otherwise cover the sheet.
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        if (visible) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)) + fadeIn(),
            exit = scaleOut(targetScale = 0.92f, animationSpec = tween(140)) + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (shown != null) SheetContent(shown, viewModel, isFavorite, onDeleteRequest, onDismiss)
        }
    }
}

@Composable
private fun SheetContent(
    s: Song,
    viewModel: MusicViewModel,
    isFavorite: Boolean,
    onDeleteRequest: (Song) -> Unit,
    onDismiss: () -> Unit
) {
            val isDownloaded = viewModel.isSongDownloaded(s.id) || s.id.startsWith("download_") || s.path.contains("MusicPlayer")
            val canDownload = (s.isStream() && s.duration > 0) || isDownloaded
            val actions = buildList {
                add(SongAction(Icons.AutoMirrored.Rounded.PlaylistPlay, "Play next") { viewModel.playNext(s) })
                add(SongAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to queue") { viewModel.addToQueue(s) })
                add(
                    SongAction(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (isFavorite) "Remove favorite" else "Add to favorites",
                        if (isFavorite) ActionFavoriteRed else null
                    ) { viewModel.toggleFavorite(s.path) }
                )
                add(SongAction(Icons.Rounded.Podcasts, "Start radio") { viewModel.startRadioForSong(s) })
                if (canDownload) {
                    if (isDownloaded) {
                        add(SongAction(Icons.Rounded.Refresh, "Re-download") { viewModel.redownloadSong(s) })
                        add(SongAction(Icons.Rounded.DeleteSweep, "Delete download") { viewModel.deleteDownload(s) })
                    } else {
                        add(SongAction(Icons.Rounded.Download, "Download for offline") { viewModel.downloadSong(s) })
                    }
                }
                add(SongAction(Icons.Rounded.Delete, "Delete from storage", DestructiveRed) { onDeleteRequest(s) })
            }

            Column(
                Modifier
                    .width(600.dp)
                    .carGlass(24.dp, CarSheetShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtImage(path = s.path, artworkUrl = s.artworkUrl, size = 64.dp, borderRadius = 8.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title.ifBlank { "Untitled" }, color = MobileTheme.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist.ifBlank { "Unknown Artist" }, color = MobileTheme.textSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CarIconButton(Icons.Rounded.Close, "Close", onClick = onDismiss, filled = true, size = 48.dp, glyph = 22.dp)
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(((actions.size + 1) / 2 * 74).dp)
                ) {
                    items(actions, key = { it.label }) { action ->
                        val tint = action.tint ?: MobileTheme.text
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(CarCardShape)
                                .background(if (action.tint == DestructiveRed) DestructiveRed.copy(alpha = 0.12f) else carIdleFill)
                                .clickable { onDismiss(); action.run() }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CarGlyph(action.icon, null, tint, 24.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(action.label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
}
