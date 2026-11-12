package com.aashik.music.desktop.ui.shell

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.aashik.music.desktop.audio.DesktopAudioPlayer
import com.aashik.music.desktop.model.DesktopSong
import com.aashik.music.desktop.service.DesktopLocalScanner
import com.aashik.music.desktop.service.DesktopLyricLine
import com.aashik.music.desktop.service.DesktopLyricsRepository
import com.aashik.music.desktop.service.DesktopOnlineRepository
import com.aashik.music.desktop.ui.DesktopImageCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Optional preset used by the off-screen snapshot tool. */
internal class DeskPreview(
    val songs: List<DesktopSong>, val lyrics: List<DesktopLyricLine>, val dest: DeskDest = DeskDest.SONGS,
    val playing: Boolean = true, val panel: RightPanel = RightPanel.LYRICS, val sheet: Boolean = false, val query: String = "", val suggestions: Boolean = false
)

private val BarInset = 100.dp

/** macOS app root: macOS Music layout (sidebar, table, lyrics panel) with a frosted floating player bar. */
@Composable
internal fun DeskApp(preview: DeskPreview? = null) {
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf(preview?.songs ?: emptyList()) }
    var current by remember { mutableStateOf<DesktopSong?>(if (preview?.playing == true) preview.songs.firstOrNull() else null) }
    val queue = remember { mutableStateListOf<DesktopSong>().also { q -> if (preview?.playing == true) q.addAll(preview.songs.drop(1)) } }
    val history = remember { mutableStateListOf<DesktopSong>() }
    var context by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var lyrics by remember { mutableStateOf(preview?.lyrics ?: emptyList()) }
    val favorites = remember { mutableStateListOf<String>().also { it.addAll(if (preview == null) DeskPrefs.loadFavorites() else listOf(preview.songs.getOrNull(2)?.id ?: "")) } }
    val plays = remember { mutableStateMapOf<String, Int>() }
    var art by remember { mutableStateOf<ImageBitmap?>(null) }
    var dest by remember { mutableStateOf(preview?.dest ?: DeskDest.SONGS) }
    var group by remember { mutableStateOf<DeskGroup?>(null) }
    var filter by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf(preview?.query ?: "") }
    var searchScope by remember { mutableStateOf(SearchScope.LIBRARY) }
    var searchFocused by remember { mutableStateOf(false) }
    var suggestDismissed by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SortKey.TITLE) }
    var sortAsc by remember { mutableStateOf(true) }
    var panelOpen by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(preview?.panel ?: RightPanel.LYRICS) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(DeskRepeat.OFF) }
    var themeMode by remember { mutableStateOf(DeskPrefs.theme()) }
    var sheetSong by remember { mutableStateOf<DesktopSong?>(if (preview?.sheet == true) preview.songs.getOrNull(1) else null) }
    var deleteSong by remember { mutableStateOf<DesktopSong?>(null) }
    var onlineResults by remember { mutableStateOf<List<DesktopSong>>(emptyList()) }
    var onlineLoading by remember { mutableStateOf(false) }
    val isPlaying by DesktopAudioPlayer.isPlaying.collectAsState()
    val downloads by com.aashik.music.desktop.service.DesktopDownloader.progress.collectAsState()
    var radioLoading by remember { mutableStateOf(false) }
    val tableState = rememberLazyListState()
    val userName = remember { System.getProperty("user.name")?.replaceFirstChar { it.uppercase() } ?: "You" }

    LaunchedEffect(Unit) {
        if (preview == null) {
            val loaded = DesktopLocalScanner.scanLocalMusic(DesktopLocalScanner.getStoredStorageDir())
            songs = loaded
            loaded.forEach { s -> DeskPrefs.plays(s.id).takeIf { it > 0 }?.let { plays[s.id] = it } }
        }
    }
    LaunchedEffect(current?.id) {
        val s = current ?: return@LaunchedEffect
        art = DesktopImageCache.load(s.artworkUrl ?: s.path)
        if (preview == null) lyrics = DesktopLyricsRepository.fetchLyrics(s)
    }
    LaunchedEffect(dest, group) { tableState.scrollToItem(0) }
    LaunchedEffect(dest, searchQuery, searchScope) {
        suggestDismissed = false
        if (dest == DeskDest.SEARCH && searchScope == SearchScope.ONLINE && searchQuery.trim().length >= 2 && preview == null) {
            onlineLoading = true; delay(350)
            onlineResults = runCatching { DesktopOnlineRepository.searchSongs(searchQuery, 24) }.getOrDefault(emptyList())
            onlineLoading = false
        } else onlineResults = emptyList()
    }

    fun play(song: DesktopSong, list: List<DesktopSong>? = null) {
        current?.let { if (it.id != song.id) history.add(it) }
        current = song
        if (list != null) {
            context = list
            val after = list.dropWhile { it.id != song.id }.drop(1)
            queue.clear(); queue.addAll(after)
        }
        plays[song.id] = (plays[song.id] ?: 0) + 1
        DeskPrefs.addPlay(song.id)
        DesktopAudioPlayer.play(song)
    }
    fun next(auto: Boolean = false) {
        if (auto && repeat == DeskRepeat.ONE) { current?.let { DesktopAudioPlayer.play(it) }; return }
        if (queue.isEmpty()) {
            if (repeat == DeskRepeat.ALL && context.isNotEmpty()) { play(context.first(), context) }
            return
        }
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
    LaunchedEffect(Unit) { DesktopAudioPlayer.setOnTrackFinished { scope.launch { next(auto = true) } } }

    val actions = DeskTransportActions(
        onPlayPause = { if (isPlaying) DesktopAudioPlayer.pause() else current?.let { if (DesktopAudioPlayer.durationMs.value > 0) DesktopAudioPlayer.resume() else DesktopAudioPlayer.play(it) } },
        onPrevious = { previous() }, onNext = { next() },
        onToggleShuffle = { shuffle = !shuffle },
        onCycleRepeat = { repeat = when (repeat) { DeskRepeat.OFF -> DeskRepeat.ALL; DeskRepeat.ALL -> DeskRepeat.ONE; DeskRepeat.ONE -> DeskRepeat.OFF } },
        onSeekFraction = { f -> val d = DesktopAudioPlayer.durationMs.value.takeIf { it > 0 } ?: current?.duration ?: 0L; DesktopAudioPlayer.seekTo((f * d).toLong()) }
    )
    // Radio: replaces Up Next with songs similar to [seed] and keeps the current song playing.
    fun startRadio(seed: DesktopSong) {
        if (radioLoading) return
        radioLoading = true
        scope.launch {
            val r = runCatching { DesktopOnlineRepository.fetchRadioQueue(seed, 20) }.getOrDefault(emptyList())
            if (r.isNotEmpty()) { queue.clear(); queue.addAll(r.filter { it.id != seed.id }); context = listOf(seed) + r; if (current?.id != seed.id) play(seed) }
            radioLoading = false
        }
    }
    fun download(song: DesktopSong) {
        val dir = DesktopLocalScanner.getStoredStorageDir()
        scope.launch {
            val f = com.aashik.music.desktop.service.DesktopDownloader.download(song, dir)
            if (f != null && preview == null) songs = DesktopLocalScanner.scanLocalMusic(dir)
        }
    }
    val downloadState: DownloadState = current.let { s ->
        val dir = DesktopLocalScanner.getStoredStorageDir()
        when {
            s == null -> DownloadState.None
            downloads[s.id]?.let { it < 1f } == true -> DownloadState.Running(downloads[s.id] ?: 0f)
            com.aashik.music.desktop.service.DesktopDownloader.isDownloaded(s, dir) -> DownloadState.Done
            else -> DownloadState.Available
        }
    }

    val toggleFavorite: (DesktopSong) -> Unit = { s ->
        if (favorites.contains(s.id)) favorites.remove(s.id) else favorites.add(s.id)
        if (preview == null) DeskPrefs.saveFavorites(favorites.toList())
    }
    val palette = deskPalette(themeMode, art)
    val favSet = favorites.toSet()

    fun sorted(list: List<DesktopSong>): List<DesktopSong> {
        val cmp: Comparator<DesktopSong> = when (sort) {
            SortKey.TITLE -> compareBy { it.title.lowercase() }
            SortKey.TIME -> compareBy { it.duration }
            SortKey.ARTIST -> compareBy { it.artist.lowercase() }
            SortKey.ALBUM -> compareBy { it.album.lowercase() }
            SortKey.PLAYS -> compareBy { plays[it.id] ?: 0 }
        }
        return list.sortedWith(if (sortAsc) cmp else cmp.reversed())
    }
    fun matches(s: DesktopSong) = filter.isBlank() || s.title.contains(filter, true) || s.artist.contains(filter, true) || s.album.contains(filter, true)

    val recent = remember(songs) { songs.sortedByDescending { runCatching { File(it.path).lastModified() }.getOrDefault(0L) } }
    val artistGroups = remember(songs) { songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }.map { (k, v) -> DeskGroup("ar-$k", k, if (v.size == 1) "1 song" else "${v.size} songs", v.first(), v) }.sortedBy { it.title.lowercase() } }
    val albumGroups = remember(songs) { songs.groupBy { it.album to it.artist }.map { (k, v) -> DeskGroup("al-${k.first}-${k.second}", k.first, k.second, v.first(), v) }.sortedBy { it.title.lowercase() } }
    val playlistGroups = remember(songs, favorites.size) {
        val fav = songs.filter { favSet.contains(it.id) }
        listOfNotNull(DeskGroup("pl-fav", "Favorites", if (fav.size == 1) "1 song" else "${fav.size} songs", fav.firstOrNull(), fav)) +
            songs.groupBy { File(it.path).parent ?: "Local Music" }.map { (k, v) -> DeskGroup("pl-$k", File(k).name.ifBlank { k }, "${v.size} songs", v.first(), v) }
    }

    val searchResults: List<DesktopSong> = if (searchScope == SearchScope.ONLINE) onlineResults
        else if (searchQuery.isBlank()) emptyList()
        else songs.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) || it.album.contains(searchQuery, true) }

    @Composable
    fun table(title: String, list: List<DesktopSong>, placeholder: String, onBack: (() -> Unit)? = null, withSort: Boolean = true) {
        val shown = sorted(list.filter(::matches))
        Column(Modifier.fillMaxSize()) {
            DeskToolbar(title, filter, placeholder, if (withSort) sort else null, if (withSort) { k -> if (k == sort) sortAsc = !sortAsc else { sort = k; sortAsc = true } } else null, { filter = it }, onBack = onBack)
            DeskSongTable(
                shown, current, isPlaying, plays, favSet, Modifier.fillMaxSize(), state = tableState, bottomInset = BarInset, sort = sort, sortAsc = sortAsc,
                onHeader = { k -> if (k == sort) sortAsc = !sortAsc else { sort = k; sortAsc = true } },
                onPlay = { s -> play(s, shown) }, onMore = { sheetSong = it }, onToggleFavorite = toggleFavorite
            )
        }
    }

    @Composable
    fun mainContent() {
        Crossfade(if (group != null) "group" else dest.name, animationSpec = tween(160)) { key ->
            val g = group
            when {
                key == "group" && g != null -> table(g.title, g.songs, "Find in ${g.title}", onBack = { group = null; filter = "" })
                key == DeskDest.SONGS.name -> table("Songs", songs, "Find in Songs")
                key == DeskDest.RECENT.name -> table("Recently Added", recent, "Find in Recently Added", withSort = false)
                key == DeskDest.HOME.name -> DeskHome(recent.take(12), songs.filter { (plays[it.id] ?: 0) > 0 }.sortedByDescending { plays[it.id] }.take(12), BarInset, { s, l -> play(s, listOf(s) + l) })
                key == DeskDest.ARTISTS.name -> DeskGroupGrid("Artists", artistGroups, BarInset, { group = it; filter = "" }, round = true)
                key == DeskDest.ALBUMS.name -> DeskGroupGrid("Albums", albumGroups, BarInset, { group = it; filter = "" })
                key == DeskDest.PLAYLISTS.name -> DeskGroupGrid("All Playlists", playlistGroups, BarInset, { group = it; filter = "" })
                key == DeskDest.RADIO.name -> DeskRadio(current, BarInset, { s, l -> play(s, listOf(s) + l) }, Modifier.fillMaxSize())
                key == DeskDest.EQUALIZER.name -> DeskEqualizer(Modifier.fillMaxSize())
                key == DeskDest.SEARCH.name -> DeskSearchPage(
                    query = searchQuery, onQuery = { searchQuery = it }, scope = searchScope, onScope = { searchScope = it },
                    focused = searchFocused, onFocus = { searchFocused = it },
                    onSubmit = { searchResults.firstOrNull()?.let { play(it, searchResults) }; suggestDismissed = true },
                    onEscape = { suggestDismissed = true },
                    results = searchResults, loading = onlineLoading, bottomInset = BarInset,
                    onPlay = { s, l -> play(s, l); suggestDismissed = true }, onMore = { sheetSong = it },
                    onOpenArtist = { name -> val l = searchResults.filter { it.artist.contains(name, true) }; group = DeskGroup("sa-$name", name, "", l.firstOrNull(), l) },
                    onOpenAlbum = { album, artist -> val l = searchResults.filter { it.album == album }; group = DeskGroup("sb-$album", album, artist, l.firstOrNull(), l) },
                    modifier = Modifier.fillMaxSize()
                )
                else -> DeskSettings(
                    themeMode, { themeMode = it; DeskPrefs.setTheme(it) },
                    { dir -> DesktopLocalScanner.saveStorageDir(dir); scope.launch { songs = DesktopLocalScanner.scanLocalMusic(dir) } },
                    { dest = DeskDest.EQUALIZER }, Modifier.fillMaxSize()
                )
            }
        }
    }

    val backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    CompositionLocalProvider(LocalDesk provides palette, LocalDeskBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize().background(palette.background)) {
                DeskSidebar(dest, userName, { dest = it; group = null; filter = "" }, Modifier.width(220.dp).fillMaxHeight())
                DeskContentWithBar(
                    song = current, isPlaying = isPlaying, shuffle = shuffle, repeat = repeat,
                    lyricsOpen = panelOpen && panel == RightPanel.LYRICS, queueOpen = panelOpen && panel == RightPanel.QUEUE, actions = actions,
                    onLyrics = { if (panelOpen && panel == RightPanel.LYRICS) panelOpen = false else { panel = RightPanel.LYRICS; panelOpen = true } },
                    onQueue = { if (panelOpen && panel == RightPanel.QUEUE) panelOpen = false else { panel = RightPanel.QUEUE; panelOpen = true } },
                    onMore = { current?.let { sheetSong = it } },
                    radioLoading = radioLoading, onRadio = { current?.let { startRadio(it) } },
                    download = downloadState, onDownload = { current?.let { download(it) } },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    overlay = {
                        val show = dest == DeskDest.SEARCH && group == null && searchQuery.isNotBlank() && searchResults.isNotEmpty() &&
                            !suggestDismissed && (searchFocused || preview?.suggestions == true)
                        if (show) DeskSearchSuggestions(searchQuery, searchResults) { s -> play(s, searchResults); suggestDismissed = true }
                    }
                ) { mainContent() }
                if (panelOpen) DeskRightPanel(
                    panel, current, lyrics, queue, { s -> play(s, queue.toList().dropWhile { it.id != s.id }) }, { queue.remove(it) }, { queue.clear() },
                    Modifier.width(280.dp).fillMaxHeight()
                )
            }
            val s = sheetSong
            DeskSongActionSheet(
                s,
                listOfNotNull(
                    s?.let { x -> DeskAction(Icons.Rounded.PlaylistPlay, "Play next") { queue.add(0, x) } },
                    s?.let { x -> DeskAction(Icons.Rounded.PlaylistAdd, "Add to queue") { queue.add(x) } },
                    s?.let { x -> DeskAction(if (favSet.contains(x.id)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (favSet.contains(x.id)) "Remove from favorites" else "Add to favorites") { toggleFavorite(x) } },
                    s?.let { x -> DeskAction(Icons.Rounded.Podcasts, "Start radio") { if (current?.id != x.id) play(x); startRadio(x) } },
                    s?.takeIf { it.isStream() && downloads[it.id] == null }?.let { x -> DeskAction(Icons.Rounded.Download, "Download") { download(x) } },
                    s?.takeIf { !it.isStream() }?.let { x -> DeskAction(Icons.Rounded.Delete, "Delete from storage", DeskDestructive) { deleteSong = x } }
                ),
                onDismiss = { sheetSong = null }
            )
            DeskConfirmDialog(
                visible = deleteSong != null, title = "Delete song?",
                message = deleteSong?.let { "\"${it.title.ifBlank { "Untitled" }}\" will be permanently removed from this Mac." } ?: "",
                confirmLabel = "Delete",
                onConfirm = { deleteSong?.let { d -> runCatching { File(d.path).delete() }; songs = songs.filter { it.id != d.id }; queue.removeAll { it.id == d.id } }; deleteSong = null },
                onDismiss = { deleteSong = null }
            )
        }
    }
}
