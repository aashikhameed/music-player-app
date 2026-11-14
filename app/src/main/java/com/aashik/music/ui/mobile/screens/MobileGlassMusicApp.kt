package com.aashik.music.ui.mobile.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.aashik.music.model.Song
import com.aashik.music.model.domain.DomainAlbum
import com.aashik.music.model.domain.DomainArtist
import com.aashik.music.model.domain.DomainPlaylist
import com.aashik.music.ui.mobile.glass.*
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class DetailStack {
    val items = mutableStateListOf<MobileDetailDestination>()
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): MobileDetailDestination? = items.lastOrNull()
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: MobileDetailDestination?) {
        if (value == null) {
            if (items.isNotEmpty()) items.removeAt(items.lastIndex)
        } else items.add(value)
    }
}

sealed interface MobileDetailDestination {
    data class Album(val album: DomainAlbum) : MobileDetailDestination
    data class Artist(val artist: DomainArtist) : MobileDetailDestination
    data class Playlist(val playlist: DomainPlaylist) : MobileDetailDestination
    data object Settings : MobileDetailDestination
    data object AlbumList : MobileDetailDestination
    data object ArtistList : MobileDetailDestination
    data object FolderList : MobileDetailDestination
}

/**
 * Mobile Root Shell for Portrait Android Devices.
 * Implements modern fluid liquid glass mobile portrait architecture:
 * - Fluid animated canvas (MobileLiquidContainer)
 * - 5-tab floating glass bottom bar (Home, New, Radio, Library, Search)
 * - Detail screen navigation stack (Album, Artist, Playlist)
 * - Floating mini-player capsule dock (361x56pt, r=28pt)
 * - Smooth spring modal presentation of MobileGlassNowPlaying
 * - Audio routing modal sheet
 */
@Composable
fun MobileGlassMusicApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val glassBlur by viewModel.glassBlurRadius.collectAsState()
    val glassRefractionHeight by viewModel.glassRefractionHeight.collectAsState()
    val glassRefractionAmount by viewModel.glassRefractionAmount.collectAsState()
    val glassChromatic by viewModel.glassChromaticAberration.collectAsState()
    val glassSurfaceAlpha by viewModel.glassSurfaceAlpha.collectAsState()

    val glassConfig = remember(glassBlur, glassRefractionHeight, glassRefractionAmount, glassChromatic, glassSurfaceAlpha) {
        LiquidGlassConfig(
            blurRadius = glassBlur.dp,
            refractionHeight = glassRefractionHeight.dp,
            refractionAmount = glassRefractionAmount.dp,
            chromaticAberration = glassChromatic,
            surfaceAlpha = glassSurfaceAlpha
        )
    }

    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isDriveModeOpen by viewModel.isDriveModeOpen.collectAsState()
    // ─── THEME: Light / Dark / Album Art ──────────────────────────────────────────
    val themeMode by viewModel.themeMode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var artColor by remember { mutableStateOf<Color?>(null) }
    var thumbArt by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var largeArt by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(themeMode, currentSong?.path, currentSong?.artworkUrl) {
        val song = currentSong
        if (song == null) {
            artColor = null; thumbArt = null; largeArt = null
        } else {
            val (thumb, large) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val t = com.aashik.music.cache.AlbumArtCache.getOrLoad(context, song.path) {
                    com.aashik.music.utils.loadAlbumArt(it, artworkUrl = song.artworkUrl)
                }
                val l = com.aashik.music.cache.AlbumArtCache.getOrLoadHighRes(song.path, "_s512") {
                    com.aashik.music.utils.loadAlbumArtHighRes(it, artworkUrl = song.artworkUrl, targetSize = 512)
                }
                t to l
            }
            thumbArt = thumb?.asImageBitmap()
            largeArt = (large ?: thumb)?.asImageBitmap()
            artColor = if (themeMode == AppThemeMode.ALBUM_ART && thumb != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { MobilePalettes.dominantColor(thumb) }
            } else null
        }
    }
    val targetPalette = remember(themeMode, artColor) {
        when (themeMode) {
            AppThemeMode.LIGHT -> MobilePalettes.Light
            AppThemeMode.DARK -> MobilePalettes.Dark
            AppThemeMode.ALBUM_ART -> artColor?.let { MobilePalettes.fromArt(it) } ?: MobilePalettes.Dark
        }
    }
    val themeAnim = androidx.compose.animation.core.tween<Color>(500)
    val animBg by animateColorAsState(targetPalette.background, themeAnim, label = "bg")
    val animSurface by animateColorAsState(targetPalette.surface, themeAnim, label = "surface")
    val animDeep by animateColorAsState(targetPalette.surfaceDeep, themeAnim, label = "deep")
    val animPanel by animateColorAsState(targetPalette.panel, themeAnim, label = "panel")
    val animAccent by animateColorAsState(targetPalette.accent, themeAnim, label = "accent")
    val animatedPalette = targetPalette.copy(
        background = animBg, surface = animSurface, surfaceDeep = animDeep, panel = animPanel, accent = animAccent
    )
    SideEffect { MobileTheme.palette = animatedPalette }

    var barCollapsed by remember { mutableStateOf(false) }
    // How far the current detail screen has scrolled; drives the toolbar title + top scrim.
    var detailScrollPx by remember { mutableFloatStateOf(0f) }
    val dockScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                detailScrollPx = (detailScrollPx - consumed.y).coerceAtLeast(0f)
                // Collapse only when content really scrolled down; any upward scroll (or pull at
                // the top) restores the full dock. Short, non-scrolling lists never collapse it.
                if (consumed.y < -6f) barCollapsed = true
                else if (consumed.y > 6f || available.y > 6f) barCollapsed = false
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    var selectedTab by remember { mutableStateOf(MobileNavTab.MUSIC) }
    // Detail navigation is a stack: assigning a destination pushes, assigning null pops one level,
    // so Back from an album opened via the Albums list returns to that list.
    val detailStack = remember { DetailStack() }
    var activeDetail by detailStack
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var showAudioRouteSheet by remember { mutableStateOf(false) }
    // Hero transition: 0 = mini-player, 1 = full-screen Now Playing. Bounds are in root coordinates.
    // One linear clock drives both directions, so closing is the exact reverse of opening.
    val npClock = remember { androidx.compose.animation.core.Animatable(0f) }
    val npProgress = remember {
        object { val value: Float get() = HeroEasing.transform(npClock.value) }
    }
    var miniCapsuleRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var miniArtRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val npScope = rememberCoroutineScope()
    val npDragRangePx = with(androidx.compose.ui.platform.LocalDensity.current) { 420.dp.toPx() }
    LaunchedEffect(isNowPlayingExpanded) {
        npClock.animateTo(
            if (isNowPlayingExpanded) 1f else 0f,
            tween(520, easing = androidx.compose.animation.core.LinearEasing)
        )
    }
    val npVisible by remember { derivedStateOf { npClock.value > 0.001f || isNowPlayingExpanded } }
    // The portrait UI has no separate HUD: anything that asks for Drive Mode (restoring a live
    // session, a legacy entry point) opens the Now Playing modal instead.
    LaunchedEffect(isDriveModeOpen) {
        if (isDriveModeOpen) {
            isNowPlayingExpanded = true
            viewModel.closeDriveMode()
        }
    }
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()

    // Kyant0/AndroidLiquidGlass backdrop layer recorder
    val backdrop = rememberLayerBackdrop()
    val navBackdrop = rememberLayerBackdrop()

    // Navigating to another tab or screen always restores the full tab bar.
    LaunchedEffect(selectedTab, activeDetail) {
        barCollapsed = false
        detailScrollPx = 0f
    }

    // Status-bar icons follow the theme (dark icons on Light, light icons otherwise).
    val view = androidx.compose.ui.platform.LocalView.current
    val lightStatusIcons = !MobileTheme.isDark
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightStatusIcons
            isAppearanceLightNavigationBars = lightStatusIcons
        }
    }

    // System Back Handler
    BackHandler(enabled = isQueueOpen || isEqualizerOpen || isNowPlayingExpanded || showAudioRouteSheet || activeDetail != null) {
        when {
            isQueueOpen -> viewModel.closeQueue()
            isEqualizerOpen -> viewModel.closeEqualizer()
            showAudioRouteSheet -> showAudioRouteSheet = false
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            activeDetail != null -> activeDetail = null
        }
    }

    // ── Collection playback: songs keep playing within the list they were started from ──
    val playIn: (List<Song>, Song) -> Unit = { list, song -> viewModel.play(song, playlist = list) }
    val playAll: (List<Song>) -> Unit = { list ->
        list.firstOrNull()?.let {
            if (viewModel.playbackMode.value == com.aashik.music.model.PlaybackMode.SHUFFLE) {
                viewModel.setPlaybackMode(com.aashik.music.model.PlaybackMode.QUEUE)
            }
            viewModel.play(it, playlist = list)
        }
    }
    val shuffleAll: (List<Song>) -> Unit = { list ->
        list.randomOrNull()?.let {
            viewModel.setPlaybackMode(com.aashik.music.model.PlaybackMode.SHUFFLE)
            viewModel.play(it, playlist = list)
        }
    }
    fun albumFor(title: String): DomainAlbum {
        val tracks = songs.filter { it.album.ifBlank { "Unknown Album" } == title }
        return DomainAlbum(title = title, artist = albumArtistOf(tracks), tracks = tracks)
    }
    fun artistFor(name: String): DomainArtist {
        val tracks = songs.filter { it.artist.ifBlank { "Unknown Artist" }.equals(name, ignoreCase = true) }
        val albums = tracks.groupBy { it.album.ifBlank { "Unknown Album" } }
            .map { (t, l) -> DomainAlbum(title = t, artist = name, tracks = l) }
        return DomainArtist(name = name, topSongs = tracks, albums = albums)
    }
    val songActions = remember(viewModel, songs, favoritePaths) {
        SongActions(
            playNext = { viewModel.playNext(it) },
            addToQueue = { viewModel.addToQueue(it) },
            toggleFavorite = { viewModel.toggleFavorite(it.path) },
            isFavorite = { favoritePaths.contains(it.path) },
            goToAlbum = { isNowPlayingExpanded = false; activeDetail = MobileDetailDestination.Album(albumFor(it.album.ifBlank { "Unknown Album" })) },
            goToArtist = { isNowPlayingExpanded = false; activeDetail = MobileDetailDestination.Artist(artistFor(it.artist.ifBlank { "Unknown Artist" })) }
        )
    }

    CompositionLocalProvider(LocalGlassConfig provides glassConfig, LocalSongActions provides songActions) {
        MobileLiquidContainer(
            backdrop = backdrop,
            modifier = modifier
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Records background + screen content so the floating glass (mini-player, nav bar)
            // genuinely refracts what scrolls behind it. In-content glass keeps using `backdrop`.
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(navBackdrop)) {
            // ─── BACKGROUND CANVAS (CAPTURED TO BACKDROP) ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MobileTheme.background)
                    .layerBackdrop(backdrop)
            )

            // ─── MAIN CONTENT HOST (CAN FREELY USE LIQUID GLASS COMPONENTS) ──────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (activeDetail == null) Modifier.statusBarsPadding() else Modifier)
                    .nestedScroll(dockScrollConnection)
            ) {
                if (activeDetail != null) {
                    when (val dest = activeDetail!!) {
                        is MobileDetailDestination.Album -> {
                            MobileAlbumScreen(
                                album = dest.album,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> playIn(dest.album.tracks, song) },
                                onArtistClick = { artistName -> activeDetail = MobileDetailDestination.Artist(artistFor(artistName)) },
                                onPlay = { playAll(dest.album.tracks) },
                                onShuffle = { shuffleAll(dest.album.tracks) }
                            )
                        }
                        is MobileDetailDestination.Artist -> {
                            MobileArtistScreen(
                                artist = dest.artist,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> playIn(dest.artist.topSongs, song) },
                                onAlbumClick = { album ->
                                    activeDetail = MobileDetailDestination.Album(album)
                                },
                                onPlay = { playAll(dest.artist.topSongs) },
                                onShuffle = { shuffleAll(dest.artist.topSongs) }
                            )
                        }
                        is MobileDetailDestination.Playlist -> {
                            MobilePlaylistScreen(
                                playlist = dest.playlist,
                                onBack = { activeDetail = null },
                                onSongClick = { song -> playIn(dest.playlist.songs, song) },
                                onArtistClick = { artist ->
                                    activeDetail = MobileDetailDestination.Artist(artist)
                                },
                                onPlay = { playAll(dest.playlist.songs) },
                                onShuffle = { shuffleAll(dest.playlist.songs) }
                            )
                        }
                        is MobileDetailDestination.AlbumList -> {
                            val items = remember(songs) {
                                songs.groupBy { it.album.ifBlank { "Unknown Album" } }.map { (title, list) ->
                                    BrowseListItem(title, title, "${albumArtistOf(list)} • ${if (list.size == 1) "1 song" else "${list.size} songs"}", artSong = list.first()) {
                                        activeDetail = MobileDetailDestination.Album(
                                            DomainAlbum(title = title, artist = albumArtistOf(list), tracks = list))
                                    }
                                }.sortedBy { it.title.lowercase() }
                            }
                            MobileBrowseListScreen("Albums", items, onBack = { activeDetail = null })
                        }
                        is MobileDetailDestination.ArtistList -> {
                            val items = remember(songs) {
                                songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }.map { (name, list) ->
                                    BrowseListItem(name, name, if (list.size == 1) "1 song" else "${list.size} songs", artSong = list.first(), roundArt = true) {
                                        activeDetail = MobileDetailDestination.Artist(artistFor(name))
                                    }
                                }.sortedBy { it.title.lowercase() }
                            }
                            MobileBrowseListScreen("Artists", items, onBack = { activeDetail = null })
                        }
                        is MobileDetailDestination.FolderList -> {
                            val folders by viewModel.folders.collectAsState()
                            val items = remember(folders, songs) {
                                folders.map { f ->
                                    val list = songs.filter { java.io.File(it.path).parent == f.path }
                                    BrowseListItem(f.path, f.name, if (f.songCount == 1) "1 song" else "${f.songCount} songs", artSong = list.firstOrNull()) {
                                        activeDetail = MobileDetailDestination.Playlist(
                                            DomainPlaylist(id = "folder_${f.path}", title = f.name, curator = "Folder",
                                                description = f.path, songs = list))
                                    }
                                }
                            }
                            MobileBrowseListScreen("Folders", items, onBack = { activeDetail = null })
                        }
                        is MobileDetailDestination.Settings -> {
                            MobileSettingsScreen(
                                viewModel = viewModel,
                                backdrop = backdrop,
                                onBack = { activeDetail = null }
                            )
                        }
                    }
                } else {
                    // TAB DESTINATIONS
                    when (selectedTab) {
                        MobileNavTab.MUSIC -> {
                            MobileMusicScreen(
                                songs = songs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onSongClick = { song -> playIn(songs, song) },
                                onPlayAllClick = { playAll(songs) },
                                onShuffleClick = { shuffleAll(songs) },
                                onRescanClick = { viewModel.loadSongs() },
                                onSettingsClick = { activeDetail = MobileDetailDestination.Settings },
                                onDeleteSong = { song -> viewModel.deleteSong(song) },
                                backdrop = backdrop
                            )
                        }
                        MobileNavTab.LIBRARY -> {
                            MobileLibraryScreen(
                                songs = songs,
                                onCategoryClick = { category ->
                                    when (category) {
                                        LibraryCategory.FAVORITES -> {
                                            val fav = songs.filter { favoritePaths.contains(it.path) }
                                            activeDetail = MobileDetailDestination.Playlist(
                                                DomainPlaylist(id = "favorites", title = "Favorites", curator = "Your library",
                                                    description = "Songs you've liked", songs = fav))
                                        }
                                        LibraryCategory.TRACKS -> {
                                            activeDetail = MobileDetailDestination.Playlist(
                                                DomainPlaylist(id = "tracks", title = "Tracks", curator = "Your library",
                                                    description = "All songs on this device", songs = songs))
                                        }
                                        LibraryCategory.ALBUMS -> activeDetail = MobileDetailDestination.AlbumList
                                        LibraryCategory.ARTISTS -> activeDetail = MobileDetailDestination.ArtistList
                                        LibraryCategory.FOLDERS -> activeDetail = MobileDetailDestination.FolderList
                                        LibraryCategory.GENRES -> selectedTab = MobileNavTab.SEARCH
                                    }
                                },
                                onAlbumClick = { album ->
                                    activeDetail = MobileDetailDestination.Album(album)
                                }
                            )
                        }
                        MobileNavTab.SEARCH -> {
                            MobileSearchScreen(
                                songs = songs,
                                onSongClick = { song -> viewModel.play(song) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            }

            // ─── FLOATING GLASS TOOLBAR (detail screens) + TOP SAFE-AREA SCRIM ───
            val detailForToolbar = activeDetail
            if (detailForToolbar != null && detailForToolbar !is MobileDetailDestination.Settings && !isNowPlayingExpanded) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val revealPx = with(density) { 220.dp.toPx() }
                val titleVisibility = { (detailScrollPx / revealPx).coerceIn(0f, 1f) }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(MobileToolbarHeight)
                        .graphicsLayer { alpha = titleVisibility() }
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(MobileTheme.background.copy(alpha = 0.92f), MobileTheme.background.copy(alpha = 0f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .graphicsLayer { alpha = titleVisibility() }
                        .background(MobileTheme.background.copy(alpha = 0.92f))
                )
                val (toolbarTitle, collection) = when (detailForToolbar) {
                    is MobileDetailDestination.Album -> detailForToolbar.album.title to detailForToolbar.album.tracks
                    is MobileDetailDestination.Artist -> detailForToolbar.artist.name to detailForToolbar.artist.topSongs
                    is MobileDetailDestination.Playlist -> detailForToolbar.playlist.title to detailForToolbar.playlist.songs
                    MobileDetailDestination.AlbumList -> "Albums" to emptyList()
                    MobileDetailDestination.ArtistList -> "Artists" to emptyList()
                    MobileDetailDestination.FolderList -> "Folders" to emptyList()
                    else -> "" to emptyList()
                }
                val toolbarContext = androidx.compose.ui.platform.LocalContext.current
                MobileGlassToolbar(
                    title = toolbarTitle,
                    titleVisibility = titleVisibility,
                    onBack = { activeDetail = null },
                    backdrop = navBackdrop,
                    actions = if (collection.isNotEmpty()) listOf(
                        ToolbarAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add all to Queue") {
                            collection.forEach { viewModel.addToQueue(it) }
                            android.widget.Toast.makeText(
                                toolbarContext,
                                if (collection.size == 1) "Added 1 song to the queue" else "Added ${collection.size} songs to the queue",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) else emptyList(),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // ─── BOTTOM DOCK: MINI-PLAYER + LIQUID TAB BAR ──────────────────────
            // Scrolling down collapses the tab bar into two glass circles (current tab, Search)
            // and the mini-player drops into the same row between them (Playlist.png reference);
            // scrolling up restores the full bar with the mini-player above it.
            val collapsedBottom = 16.dp + (64.dp - CollapsedDockHeight) / 2
            val collapse by animateFloatAsState(
                targetValue = if (barCollapsed) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f),
                label = "dockCollapse"
            )
            AnimatedVisibility(
                visible = !isNowPlayingExpanded,
                enter = fadeIn(tween(220, delayMillis = 120)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(160.dp)) {
                    // Full tab bar (fades and shrinks toward the left circle as it collapses)
                    if (collapse < 0.999f) {
                        MobileGlassBottomNavBar(
                            selectedTab = selectedTab,
                            backdrop = navBackdrop,
                            onTabSelected = {
                                detailStack.items.clear() // Return to root tab view
                                selectedTab = it
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .graphicsLayer {
                                    alpha = 1f - collapse
                                    val scale = 1f - 0.12f * collapse
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                                }
                        )
                    }

                    // Collapsed circles
                    if (collapse > 0.001f) {
                        val leftTab = if (selectedTab == MobileNavTab.SEARCH) MobileNavTab.MUSIC else selectedTab
                        GlassCircleButton(
                            icon = leftTab.icon,
                            contentDescription = "Show tabs",
                            backdrop = navBackdrop,
                            tint = if (selectedTab != MobileNavTab.SEARCH) MobileTheme.accent else null,
                            onClick = { barCollapsed = false },
                            size = CollapsedDockHeight,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = DockGutter, bottom = collapsedBottom)
                                .graphicsLayer {
                                    alpha = collapse
                                    scaleX = 0.6f + 0.4f * collapse
                                    scaleY = 0.6f + 0.4f * collapse
                                }
                        )
                        GlassCircleButton(
                            icon = MobileNavTab.SEARCH.icon,
                            contentDescription = "Search",
                            backdrop = navBackdrop,
                            tint = if (selectedTab == MobileNavTab.SEARCH) MobileTheme.accent else null,
                            onClick = {
                                detailStack.items.clear()
                                selectedTab = MobileNavTab.SEARCH
                                barCollapsed = false
                            },
                            size = CollapsedDockHeight,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = DockGutter, bottom = collapsedBottom)
                                .graphicsLayer {
                                    alpha = collapse
                                    scaleX = 0.6f + 0.4f * collapse
                                    scaleY = 0.6f + 0.4f * collapse
                                }
                        )
                    }

                    // Mini-player: above the bar when expanded, between the circles when collapsed
                    if (currentSong != null) {
                        // Same 24dp gutter as the tab bar when expanded; 12dp from each 48dp circle when collapsed.
                        val side = DockGutter + (CollapsedDockHeight + 12.dp) * collapse
                        // Expanded: 6dp above the 64dp bar (16 + 64 + 6). Collapsed: same row as the circles.
                        val miniBottom = 86.dp + (collapsedBottom - 86.dp) * collapse
                        val miniHeight = MiniPlayerHeight + (CollapsedDockHeight - MiniPlayerHeight) * collapse
                        MobileGlassMiniPlayer(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            albumArt = thumbArt,
                            backdrop = navBackdrop,
                            compact = barCollapsed,
                            height = miniHeight,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNextSong() },
                            onOpenNowPlaying = { isNowPlayingExpanded = true },
                            onBounds = { capsule, art -> miniCapsuleRect = capsule; miniArtRect = art },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = side, end = side, bottom = miniBottom)
                        )
                    }
                }
            }

            // ─── NOW PLAYING: CONTAINER TRANSFORM FROM THE MINI-PLAYER ──────────
            // The sheet grows out of the mini-player capsule while the cover art flies from the
            // mini-player thumbnail to its place in the sheet (and back when closing).
            if (npVisible) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = npProgress.value
                                val cap = miniCapsuleRect
                                if (cap != null && p < 1f) {
                                    clip = true
                                    shape = HeroRevealShape(cap, p)
                                } else if (cap == null) {
                                    alpha = p
                                }
                            }
                    ) {
                        MobileGlassNowPlaying(
                            viewModel = viewModel,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            albumArt = largeArt,
                            isFavorite = currentSong?.path?.let { favoritePaths.contains(it) } == true,
                            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.path) } },
                            onClose = { isNowPlayingExpanded = false },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onPrevious = { viewModel.playPreviousSong() },
                            onNext = { viewModel.playNextSong() },
                            contentAlpha = { ((npProgress.value - 0.25f) / 0.6f).coerceIn(0f, 1f) },
                            entry = { npProgress.value },
                            miniArt = miniArtRect,
                            onDrag = { d -> npScope.launch { npClock.snapTo((npClock.value - d / npDragRangePx).coerceIn(0f, 1f)) } },
                            onDragEnd = { v ->
                                if (npClock.value < 0.8f || v > 2000f) isNowPlayingExpanded = false
                                else npScope.launch { npClock.animateTo(1f, tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                            }
                        )
                    }
                }
            }

            // ─── AIRPLAY AUDIO ROUTE MODAL SHEET ────────────────────────────────
            AnimatedVisibility(
                visible = showAudioRouteSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MobileGlassAudioRouteSheet(
                    devices = listOf(
                        AudioOutputDevice("1", "AirPods Max", Icons.Rounded.Radio, 95, true),
                        AudioOutputDevice("2", "Phone Speaker", Icons.Rounded.GraphicEq, null, false),
                        AudioOutputDevice("3", "MacBook Pro", Icons.Rounded.Settings, null, false)
                    ),
                    selectedDeviceId = "1",
                    onSelectDevice = { showAudioRouteSheet = false },
                    onDismiss = { showAudioRouteSheet = false }
                )
            }

            // ─── QUEUE OVERLAY (Now Playing shows its own queue in place) ───────
            if (isQueueOpen) {
                com.aashik.music.ui.QueueDrawer(
                    viewModel = viewModel,
                    onClose = { viewModel.closeQueue() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ─── FULL-SCREEN EQUALIZER (from v0.3.2) ────────────────────────────
            if (isEqualizerOpen) {
                Box(Modifier.fillMaxSize().background(MobileTheme.background).systemBarsPadding()) {
                    com.aashik.music.ui.EqualizerScreen(
                        viewModel = viewModel,
                        onClose = { viewModel.closeEqualizer() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}

/** Clip that grows from the mini-player capsule to the full screen as [progress] goes 0 → 1. */
private class HeroRevealShape(
    private val from: androidx.compose.ui.geometry.Rect,
    private val progress: Float
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val p = progress
        fun l(a: Float, b: Float) = a + (b - a) * p
        val rad = l(from.height / 2f, 0f)
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                l(from.left, 0f), l(from.top, 0f), l(from.right, size.width), l(from.bottom, size.height),
                androidx.compose.ui.geometry.CornerRadius(rad, rad)
            )
        )
    }
}

private val HeroEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
