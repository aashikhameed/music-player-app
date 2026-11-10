package com.aashik.music.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.DesktopArtwork
import kotlinx.coroutines.launch

private val DialogShape = RoundedCornerShape(20.dp)
private val SongCardShape = RoundedCornerShape(12.dp)

@Composable
fun DesktopSearchDialog(
    onPlaySong: (DesktopSong) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00E5FF),
    backgroundColor: Color = Color(0xFF131720),
    textColor: Color = Color(0xFFF1F5F9),
    secondaryColor: Color = Color(0xFF94A3B8)
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    fun doSearch() {
        if (query.isNotBlank()) {
            isSearching = true
            scope.launch {
                results = DesktopOnlineRepository.searchSongs(query)
                isSearching = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 520.dp)
                .clip(DialogShape)
                .background(backgroundColor)
                .border(width = 1.dp, color = Color(0xFF252F42), shape = DialogShape)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            // Header with search bar and close button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search songs, artists, soundtracks...", color = secondaryColor, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryColor, strokeWidth = 2.dp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A202C),
                        unfocusedContainerColor = Color(0xFF1A202C),
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF252F42),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1B202A), shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = secondaryColor, modifier = Modifier.size(18.dp))
                }
            }

            // Results list
            if (results.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "Type a song name and press Enter to search" else "No matching songs found",
                        color = secondaryColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SongCardShape)
                                .background(Color(0xFF1A202C))
                                .clickable {
                                    onPlaySong(song)
                                    onClose()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DesktopArtwork(url = song.artworkUrl, size = 42.dp, shape = RoundedCornerShape(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 12.sp,
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
}
