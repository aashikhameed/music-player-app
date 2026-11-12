package com.aashik.music.ui.car.glass

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aashik.music.model.DriveSide
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.aashik.music.cache.AlbumArtCache
import com.aashik.music.model.Song
import com.aashik.music.ui.SongLoadingBar
import com.aashik.music.ui.car.AutoHudInactivityWatcher
import com.aashik.music.ui.mobile.glass.AppThemeMode
import com.aashik.music.ui.mobile.glass.LiquidAlertDialog
import com.aashik.music.ui.mobile.glass.LiquidGlassConfig
import com.aashik.music.ui.mobile.glass.LocalGlassConfig
import com.kyant.backdrop.backdrops.layerBackdrop
import com.aashik.music.ui.mobile.glass.MobilePalettes
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.utils.loadAlbumArt
import com.aashik.music.utils.loadAlbumArtHighRes
import com.aashik.music.viewmodel.LibraryTab
import com.aashik.music.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/** Size of the current song's Now Playing artwork: one 640 px RGB_565 decode (~0.8 MB), shown ~1:1 on the 1280×720 stage. */
private const val CarHeroArtPx = 640

/**
 * Car / landscape root (1280×720 @ 160 dpi) in the liquid glass design:
 * solid sidebar + content pane + floating playback capsule; Now Playing (Drive HUD),
 * Settings and Equalizer take over the full screen. Themes follow Light / Dark / Artwork.
 *
 * Only the Now Playing seek slider samples a backdrop (AGENTS.md §7); every other surface
 * here is a flat MobileTheme colour.
 */
@Composable
fun CarGlassApp(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedRadioCategory by viewModel.selectedRadioCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val hideBottomBar by viewModel.hideBottomBar.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isDriveModeOpen by viewModel.isDriveModeOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val driveSide by viewModel.driveSide.collectAsState()

    // ─── THEME: Light / Dark / Artwork (same palettes as the mobile UI) ─────────
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current
    var artColor by remember { mutableStateOf<Color?>(null) }
    var thumbArt by remember { mutableStateOf<ImageBitmap?>(null) }
    var heroArt by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(themeMode, currentSong?.path, currentSong?.artworkUrl) {
        val song = currentSong
        if (song == null) {
            artColor = null; thumbArt = null; heroArt = null
            return@LaunchedEffect
        }
        val thumb = withContext(Dispatchers.Default) {
            AlbumArtCache.getOrLoad(context, song.path) { loadAlbumArt(it, artworkUrl = song.artworkUrl) }
        }
        thumbArt = thumb?.asImageBitmap()
        artColor = if (themeMode == AppThemeMode.ALBUM_ART && thumb != null) {
            withContext(Dispatchers.Default) { MobilePalettes.dominantColor(thumb) }
        } else null
        val hero = AlbumArtCache.getOrLoadHighRes(song.path, "_car$CarHeroArtPx") { path ->
            loadAlbumArtHighRes(path, artworkUrl = song.artworkUrl, targetSize = CarHeroArtPx)?.let(::fitHeroArt)
        }
        heroArt = (hero ?: thumb)?.asImageBitmap()
    }
    val targetPalette = remember(themeMode, artColor) {
        when {
            themeMode == AppThemeMode.LIGHT -> MobilePalettes.Light
            themeMode == AppThemeMode.DARK -> MobilePalettes.Dark
            else -> artColor?.let { MobilePalettes.fromArt(it) } ?: MobilePalettes.Dark
        }
    }
    val themeAnim = tween<Color>(500)
    val animBg by animateColorAsState(targetPalette.background, themeAnim, label = "carBg")
    val animSurface by animateColorAsState(targetPalette.surface, themeAnim, label = "carSurface")
    val animDeep by animateColorAsState(targetPalette.surfaceDeep, themeAnim, label = "carDeep")
    val animPanel by animateColorAsState(targetPalette.panel, themeAnim, label = "carPanel")
    val animAccent by animateColorAsState(targetPalette.accent, themeAnim, label = "carAccent")
    val animatedPalette = targetPalette.copy(
        background = animBg, surface = animSurface, surfaceDeep = animDeep, panel = animPanel, accent = animAccent
    )
    SideEffect { MobileTheme.palette = animatedPalette }

    // System bar icons follow the theme.
    val view = LocalView.current
    val lightIcons = !targetPalette.isDark
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightIcons
            isAppearanceLightNavigationBars = lightIcons
        }
    }

    // Glass used by the seek slider (the only lens element on the headunit).
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

    // ─── Inactivity → Auto HUD (any touch resets the timer) ─────────────────────
    val userInteractionChannel = remember { Channel<Unit>(Channel.CONFLATED) }
    AutoHudInactivityWatcher(viewModel = viewModel, userInteractionChannel = userInteractionChannel)

    var isSearchOpen by remember { mutableStateOf(false) }
    var songForActions by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    val closeSearch = {
        isSearchOpen = false
        viewModel.onSearchQueryChanged("")
    }

    // ─── System Back: closes the top-most layer first (dialogs → queue → equalizer →
    // settings → Now Playing), then the library drill-downs as on the legacy headunit ──
    BackHandler(
        enabled = songToDelete != null || songForActions != null || isQueueOpen || isEqualizerOpen ||
            isDriveModeOpen || isSettingsOpen || isSearchOpen ||
            (selectedTab == LibraryTab.FOLDERS && selectedFolder != null) || selectedTab == LibraryTab.RADIO
    ) {
        when {
            songToDelete != null -> songToDelete = null
            songForActions != null -> songForActions = null
            isQueueOpen -> viewModel.closeQueue()
            isEqualizerOpen -> viewModel.closeEqualizer()
            isSettingsOpen -> viewModel.closeSettings()
            isDriveModeOpen -> viewModel.closeDriveMode()
            isSearchOpen -> closeSearch()
            selectedTab == LibraryTab.FOLDERS && selectedFolder != null -> viewModel.closeFolder()
            selectedTab == LibraryTab.RADIO -> {
                if (selectedRadioCategory != null) viewModel.closeRadioCategory()
                else viewModel.selectTab(LibraryTab.ALL_SONGS)
            }
        }
    }

    val libraryBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    CompositionLocalProvider(LocalGlassConfig provides glassConfig, LocalDriveSide provides driveSide, LocalCarBackdrop provides libraryBackdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MobileTheme.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.isNotEmpty()) userInteractionChannel.trySend(Unit)
                        }
                    }
                }
        ) {
            // ─── LIBRARY: sidebar + content + floating capsule ──────────────────
            CompositionLocalProvider(
                LocalLayoutDirection provides if (driveSide == DriveSide.RIGHT) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
            Row(Modifier.fillMaxSize()) {
                val sidebarCollapsed by viewModel.sidebarCollapsed.collectAsState()
                val sidebarWidth by androidx.compose.animation.core.animateDpAsState(
                    if (sidebarCollapsed) CarDims.SidebarCollapsedWidth else CarDims.SidebarWidth,
                    androidx.compose.animation.core.tween(220), label = "sidebarWidth"
                )
                val contentSlot = Modifier.weight(1f).fillMaxHeight()
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                CarSidebar(
                    viewModel = viewModel,
                    selectedTab = selectedTab,
                    isSearchOpen = isSearchOpen,
                    onOpenSearch = { isSearchOpen = true },
                    onSelectTab = { tab ->
                        if (isSearchOpen) closeSearch()
                        if (tab == LibraryTab.FOLDERS && selectedTab == LibraryTab.FOLDERS) viewModel.closeFolder()
                        if (tab == LibraryTab.RADIO && selectedRadioCategory != null) viewModel.closeRadioCategory()
                        viewModel.selectTab(tab)
                    },
                    modifier = Modifier.width(sidebarWidth).fillMaxHeight()
                )
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(contentSlot) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { SongLoadingBar(viewModel) }
                    } else {
                        // Only the list is recorded for the glass surfaces (the capsule sits outside the layer).
                        Box(
                            Modifier.fillMaxSize().background(MobileTheme.background).then(
                                if (android.os.Build.VERSION.SDK_INT >= 31) Modifier.layerBackdrop(libraryBackdrop) else Modifier
                            )
                        ) {
                        CarLibraryPane(
                            viewModel = viewModel,
                            songs = songs,
                            selectedTab = selectedTab,
                            selectedFolder = selectedFolder,
                            searchQuery = searchQuery,
                            isSearchOpen = isSearchOpen,
                            onCloseSearch = closeSearch,
                            favoritePaths = favoritePaths,
                            currentSongId = currentSong?.id,
                            bottomInset = if (currentSong != null && !hideBottomBar) CarDims.CapsuleHeight + CarDims.CapsuleBottomGap * 2 else 16.dp,
                            onSongActions = { songForActions = it },
                            modifier = Modifier.fillMaxSize()
                        )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility( // plain overload (not RowScope)
                        visible = currentSong != null && !hideBottomBar,
                        enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it } + fadeIn(),
                        exit = slideOutVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = CarDims.ContentGutter, vertical = CarDims.CapsuleBottomGap)
                    ) {
                        CarPlaybackCapsule(
                            viewModel = viewModel,
                            albumArt = thumbArt,
                            onOpenNowPlaying = { viewModel.openDriveMode() }
                        )
                    }
                }
                }
            }
            }

            // ─── QUEUE SHEET (from the capsule) ─────────────────────────────────
            if (!isDriveModeOpen) {
                CarQueueSheet(visible = isQueueOpen, viewModel = viewModel, onClose = { viewModel.closeQueue() })
            }

            // ─── FULL-SCREEN DESTINATIONS (Now Playing below Settings / Equalizer,
            // which can be opened from its top bar) ────────────────────────────
            AnimatedVisibility(
                visible = isDriveModeOpen,
                enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 300f)) { it } + fadeIn(),
                exit = slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 300f)) { it } + fadeOut()
            ) {
                CarNowPlaying(
                    viewModel = viewModel,
                    albumArt = heroArt,
                    onClose = { viewModel.closeDriveMode() },
                    onSongActions = { songForActions = it }
                )
            }
            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 6 } + fadeIn(),
                exit = slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 6 } + fadeOut()
            ) {
                CarSettingsScreen(viewModel = viewModel, onClose = { viewModel.closeSettings() })
            }
            AnimatedVisibility(
                visible = isEqualizerOpen,
                enter = slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 6 } + fadeIn(),
                exit = slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 6 } + fadeOut()
            ) {
                CarEqualizerScreen(viewModel = viewModel, onClose = { viewModel.closeEqualizer() })
            }

            // ─── DIALOGS ────────────────────────────────────────────────────────
            CarSongActionSheet(
                song = songForActions,
                viewModel = viewModel,
                isFavorite = songForActions?.let { favoritePaths.contains(it.path) } == true,
                onDeleteRequest = { songToDelete = it },
                onDismiss = { songForActions = null }
            )
            LiquidAlertDialog(
                visible = songToDelete != null,
                onDismissRequest = { songToDelete = null },
                title = "Delete song?",
                message = songToDelete?.let { "\"${it.title.ifBlank { "Untitled" }}\" will be permanently removed from this device." } ?: "",
                confirmButtonText = "Delete",
                onConfirm = {
                    songToDelete?.let { viewModel.deleteSong(it) }
                    songToDelete = null
                },
                dismissButtonText = "Cancel",
                onDismissButton = { songToDelete = null },
                confirmTint = Color(0xFFFF453A),
                surfaceColor = MobileTheme.panel,
                modifier = Modifier.widthIn(max = 560.dp) // phone dialog would otherwise span the 1280 dp screen
            )
        }
    }
}

/** Centre-crops / downsizes the decoded cover to exactly [CarHeroArtPx] square (RGB_565 kept). */
private fun fitHeroArt(src: Bitmap): Bitmap {
    if (src.width == CarHeroArtPx && src.height == CarHeroArtPx) return src
    val side = minOf(src.width, src.height)
    val cropped = if (src.width != src.height) {
        Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    } else src
    if (side <= CarHeroArtPx) return cropped
    return Bitmap.createScaledBitmap(cropped, CarHeroArtPx, CarHeroArtPx, true)
}
