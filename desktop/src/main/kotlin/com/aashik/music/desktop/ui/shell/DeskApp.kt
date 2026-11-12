package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.ui.DesktopEqualizerView
import com.aashik.music.desktop.ui.DesktopExploreView
import com.aashik.music.desktop.ui.DesktopFolderBrowser
import com.aashik.music.desktop.ui.DesktopImageCache
import com.aashik.music.desktop.ui.DesktopSettingsView
import kotlinx.coroutines.launch

/** Optional preset used by the off-screen snapshot tool. */
internal class DeskPreview(val songs: List<DesktopSong>, val lyrics: List<DesktopLyricLine>, val dest: DeskDest = DeskDest.SONGS, val nowPlaying: Boolean = false, val pane: DeskPane = DeskPane.NONE, val search: Boolean = false)

/** macOS app root: the headunit (landscape) design on desktop: sidebar, library, floating capsule, Now Playing. */
@Composable
internal fun DeskApp(preview: DeskPreview? = null) {
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf(preview?.songs ?: emptyList()) }
    var current by remember { mutableStateOf<DesktopSong?>(preview?.songs?.firstOrNull()) }
    val queue = remember { mutableStateListOf<DesktopSong>().also { q -> preview?.songs?.drop(1)?.let(q::addAll) } }
    val history = remember { mutableStateListOf<DesktopSong>() }
    var lyrics by remember { mutableStateOf(preview?.lyrics ?: emptyList()) }
    val favorites = remember { mutableStateListOf<String>() }
    var art by remember { mutableStateOf<ImageBitmap?>(null) }
    var dest by remember { mutableStateOf(preview?.dest ?: DeskDest.SONGS) }
    var searchOpen by remember { mutableStateOf(preview?.search ?: false) }
    var query by remember { mutableStateOf(if (preview?.search == true) "a" else "") }
    var nowPlaying by remember { mutableStateOf(preview?.nowPlaying ?: false) }
    var pane by remember { mutableStateOf(preview?.pane ?: DeskPane.NONE) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()

    LaunchedEffect(Unit) {
        if (preview == null) {
            val loaded = DesktopLocalScanner.scanLocalMusic(DesktopLocalScanner.getStoredStorageDir())
            songs = loaded
            if (loaded.isNotEmpty() && current == null) { current = loaded.first(); queue.clear(); queue.addAll(loaded.drop(1)) }
        }
    }
    LaunchedEffect(current?.id) {
        val s = current ?: return@LaunchedEffect
        art = DesktopImageCache.load(s.artworkUrl ?: s.path)
        if (preview == null) lyrics = DesktopLyricsRepository.fetchLyrics(s)
    }

    fun play(song: DesktopSong, newQueue: List<DesktopSong>? = null) {
        current?.let { if (it.id != song.id) history.add(it) }
        current = song
        if (newQueue != null) { queue.clear(); queue.addAll(newQueue.filter { it.id != song.id }) }
        DesktopAudioPlayer.play(song)
    }
    fun next() {
        if (queue.isEmpty()) { if (repeat) current?.let { DesktopAudioPlayer.play(it) }; return }
        val n = if (shuffle) queue.random() else queue.first()
        queue.remove(n)
        play(n)
    }
    fun previous() {
        if (DesktopAudioPlayer.currentPositionMs.value > 3000 || history.isEmpty()) { DesktopAudioPlayer.seekTo(0); return }
        val p = history.removeAt(history.lastIndex)
        current?.let { queue.add(0, it) }
        current = p
        DesktopAudioPlayer.play(p)
    }
    LaunchedEffect(Unit) { DesktopAudioPlayer.setOnTrackFinished { scope.launch { next() } } }

    val actions = DeskPlaybackActions(
        onPlayPause = { if (isPlaying) DesktopAudioPlayer.pause() else current?.let { if (DesktopAudioPlayer.durationMs.value > 0) DesktopAudioPlayer.resume() else DesktopAudioPlayer.play(it) } },
        onPrevious = { previous() },
        onNext = { next() },
        onToggleShuffle = { shuffle = !shuffle },
        onToggleRepeat = { repeat = !repeat },
        onSeekFraction = { f -> val d = DesktopAudioPlayer.durationMs.value.takeIf { it > 0 } ?: current?.duration ?: 0L; DesktopAudioPlayer.seekTo((f * d).toLong()) }
    )
    val toggleFavorite: (DesktopSong) -> Unit = { s -> if (favorites.contains(s.id)) favorites.remove(s.id) else favorites.add(s.id) }
    val palette = deskPaletteFromArt(art)

    CompositionLocalProvider(LocalDesk provides palette) {
        Row(Modifier.fillMaxSize().background(palette.background)) {
            DeskSidebar(
                selected = dest, isSearchOpen = searchOpen, isPlaying = isPlaying, favoriteCount = favorites.size,
                onSearch = { searchOpen = true; nowPlaying = false },
                onSelect = { dest = it; searchOpen = false; query = ""; nowPlaying = false },
                modifier = Modifier.width(240.dp).fillMaxHeight()
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Crossfade(if (nowPlaying) "np" else if (searchOpen) "search" else dest.name, animationSpec = tween(200)) { key ->
                    when (key) {
                        "np" -> DeskNowPlaying(
                            song = current, art = art, isPlaying = isPlaying, isShuffle = shuffle, isRepeat = repeat,
                            isFavorite = current?.let { favorites.contains(it.id) } == true,
                            lyrics = lyrics, queue = queue, pane = pane, onPane = { pane = it }, actions = actions,
                            onToggleFavorite = { current?.let(toggleFavorite) },
                            onPlayFromQueue = { s -> play(s, queue.toList().dropWhile { it.id != s.id }) },
                            onRemoveFromQueue = { queue.remove(it) }, onClearQueue = { queue.clear() },
                            modifier = Modifier.fillMaxSize()
                        )
                        "search" -> library("Songs", songs, current, isPlaying, favorites, true, query, { query = it }, { searchOpen = false; query = "" }, toggleFavorite, { s, q -> play(s, q) }, current != null)
                        DeskDest.SONGS.name -> library("Songs", songs, current, isPlaying, favorites, false, "", {}, {}, toggleFavorite, { s, q -> play(s, q) }, current != null)
                        DeskDest.FAVORITES.name -> library("Favorites", songs.filter { favorites.contains(it.id) }, current, isPlaying, favorites, false, "", {}, {}, toggleFavorite, { s, q -> play(s, q) }, current != null)
                        DeskDest.FOLDERS.name -> DesktopFolderBrowser(songs = songs, onPlaySong = { s, q -> play(s, q) })
                        DeskDest.RADIO.name -> DesktopExploreView(currentSong = current, onPlaySong = { s, q -> play(s, q) })
                        DeskDest.EQUALIZER.name -> DesktopEqualizerView()
                        else -> DesktopSettingsView(onStorageDirChanged = { dir -> DesktopLocalScanner.saveStorageDir(dir); scope.launch { songs = DesktopLocalScanner.scanLocalMusic(dir) } })
                    }
                }
                val cur = current
                if (cur != null && !nowPlaying) {
                    DeskCapsule(
                        song = cur, isPlaying = isPlaying, isShuffle = shuffle, isRepeat = repeat, actions = actions,
                        onOpenNowPlaying = { nowPlaying = true }, onOpenQueue = { pane = DeskPane.UP_NEXT; nowPlaying = true },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun library(
    title: String, songs: List<DesktopSong>, current: DesktopSong?, isPlaying: Boolean, favorites: List<String>, search: Boolean, query: String,
    onQuery: (String) -> Unit, onCloseSearch: () -> Unit, onFav: (DesktopSong) -> Unit, onPlay: (DesktopSong, List<DesktopSong>) -> Unit, hasCapsule: Boolean
) {
    DeskLibrary(
        title = title, songs = songs, currentSong = current, isPlaying = isPlaying, favoriteIds = favorites.toSet(),
        isSearchOpen = search, query = query, bottomInset = if (hasCapsule) 100.dp else 16.dp,
        onQuery = onQuery, onCloseSearch = onCloseSearch, onToggleFavorite = onFav, onPlay = onPlay,
        onShuffle = { l -> if (l.isNotEmpty()) onPlay(l.random(), l) },
        modifier = Modifier.fillMaxSize()
    )
}
