package com.aashik.music.ui.mobile.screens

import com.aashik.music.ui.mobile.glass.MobileTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.Text

data class BrowseListItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val artSong: com.aashik.music.model.Song? = null,
    val roundArt: Boolean = false,
    val onClick: () -> Unit
)

/** Simple large-title list used for Albums, Artists and Folders drill-downs. */
@Composable
fun MobileBrowseListScreen(
    title: String,
    items: List<BrowseListItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.statusBarsPadding().height(com.aashik.music.ui.mobile.glass.MobileToolbarHeight))
        Text(title, color = MobileTheme.text, fontSize = 30.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter) {
                Text("Nothing here yet", color = MobileTheme.textTertiary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 150.dp)) {
                itemsIndexed(items, key = { i, it -> "${it.key}#$i" }) { _, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                            .clickable(onClick = item.onClick).padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.artSong?.let { s ->
                            com.aashik.music.ui.AlbumArtImage(
                                path = s.path,
                                artworkUrl = s.artworkUrl,
                                size = 48.dp,
                                borderRadius = if (item.roundArt) 24.dp else 6.dp
                            )
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = MobileTheme.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = MobileTheme.textSecondary, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
                            tint = MobileTheme.textTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
