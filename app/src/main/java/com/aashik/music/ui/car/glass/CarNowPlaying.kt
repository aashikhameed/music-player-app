package com.aashik.music.ui.car.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.aashik.music.model.DriveSide
import com.aashik.music.model.PlaybackMode
import com.aashik.music.model.Song
import com.aashik.music.service.DownloadStatus
import com.aashik.music.ui.VideoPlayerView
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.mobile.glass.LiquidSlider
import com.aashik.music.ui.mobile.glass.MobileTheme
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FavoriteRed = Color(0xFFFF375F)
private val DownloadedGreen = Color(0xFF30D158)
private val VideoScrim = Color(0x8C000000)

/** Side of the compact artwork when a panel is open ("split" state). */
private val SplitArtSide = 368.dp
/** Gap between the stage artwork and the control column. */
private val StageGap = 40.dp

/** NONE = stage (hero artwork + controls); LYRICS / UP_NEXT = split (compact left column + panel). */
private enum class CarPane { NONE, LYRICS, UP_NEXT }

private val PaneOrder = listOf(CarPane.LYRICS, CarPane.UP_NEXT)
private val PaneTabs = listOf(Icons.Rounded.Lyrics to "Lyrics", Icons.AutoMirrored.Rounded.QueueMusic to "Up Next")

/**
 * Car Now Playing / Drive HUD, two states:
 *  - **Stage**: artwork fills the stage height; the right column holds title, action chips, the liquid
 *    glass seek slider (the only lens on the headunit), big transport with ±10 s and the Lyrics / Up Next switch.
 *  - **Split**: choosing Lyrics or Up Next springs the artwork into a compact left column (title, slider,
 *    transport) and opens the panel on the right. Tap the artwork or press Back to return to the stage.
 * No top bar: swipe down or Back closes.
 * Gestures: swipe ← / → for next / previous, double-tap to play / pause.
 */
@Composable
internal fun CarNowPlaying(
    viewModel: MusicViewModel,
    albumArt: ImageBitmap?,
    onClose: () -> Unit,
    onSongActions: (Song) -> Unit
) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playVideoForStreams by viewModel.playVideoForStreams.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isSpeedLockActive = currentSpeedKmh > 1.0f
    val isVideoActive = playVideoForStreams && !isSpeedLockActive &&
        (!currentVideoId.isNullOrBlank() || song?.isVideo() == true)

    var pane by rememberSaveable { mutableStateOf(CarPane.NONE) }
    BackHandler(enabled = pane != CarPane.NONE && !isQueueOpen) { pane = CarPane.NONE }
    val togglePane: (CarPane) -> Unit = { pane = if (pane == it) CarPane.NONE else it }

    // ── Video: controls auto-hide after 4.5 s while playing; tap toggles them ──
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    LaunchedEffect(song?.id) { controlsVisible = true; lastInteraction = System.currentTimeMillis() }
    LaunchedEffect(isVideoActive, controlsVisible, isPlaying, lastInteraction) {
        if (isVideoActive && controlsVisible && isPlaying) {
            delay(4500L)
            controlsVisible = false
        }
    }
    val toggleControls = {
        controlsVisible = !controlsVisible
        if (controlsVisible) lastInteraction = System.currentTimeMillis()
    }

    // ── Swipe feedback badge ──
    val scope = rememberCoroutineScope()
    var swipeFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackToken by remember { mutableIntStateOf(0) }
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    fun showFeedback(text: String) {
        swipeFeedback = text
        val token = ++feedbackToken
        scope.launch { delay(900L); if (feedbackToken == token) swipeFeedback = null }
    }

    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(swipeThresholdPx) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = {
                        if (accumulated < -swipeThresholdPx) { viewModel.playNextSong(); showFeedback("Next track") }
                        else if (accumulated > swipeThresholdPx) { viewModel.playPreviousSong(); showFeedback("Previous track") }
                        accumulated = 0f
                    },
                    onDragCancel = { accumulated = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    accumulated += dragAmount
                }
            }
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy > 120f) onClose(); dy = 0f },
                    onDragCancel = { dy = 0f }
                ) { _, d -> dy += d }
            }
            .pointerInput(isVideoActive) {
                detectTapGestures(
                    onTap = { if (isVideoActive) toggleControls() },
                    onDoubleTap = { viewModel.togglePlayPause() }
                )
            }
    ) {
        // Background (recorded for the seek slider's lens)
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(if (isVideoActive) Color.Black else MobileTheme.background)
        )

        if (isVideoActive) {
            VideoPlayerView(
                videoId = currentVideoId ?: song?.getVideoStreamId(),
                videoUrl = if (song?.isLocalVideo() == true) song?.path else null,
                viewModel = viewModel,
                onTap = toggleControls,
                modifier = Modifier.fillMaxSize()
            )
            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().background(VideoScrim))
            }
        }

        AnimatedVisibility(
            visible = !isVideoActive || controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                if (isVideoActive) {
                    Spacer(Modifier.weight(1f))
                    VideoControlDeck(
                        viewModel = viewModel,
                        song = song,
                        backdrop = backdrop,
                        onSongActions = onSongActions
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    NowPlayingBody(
                        viewModel = viewModel,
                        song = song,
                        albumArt = albumArt,
                        isPlaying = isPlaying,
                        pane = pane,
                        onPane = { pane = it },
                        onTogglePane = togglePane,
                        backdrop = backdrop,
                        onSongActions = onSongActions,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }

        // Queue in video mode slides in as a sheet (audio mode shows it in the Up Next panel).
        if (isVideoActive) {
            CarQueueSheet(visible = isQueueOpen, viewModel = viewModel, onClose = { viewModel.closeQueue() })
        }

        // Swipe feedback badge
        AnimatedVisibility(
            visible = swipeFeedback != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(Modifier.background(MobileTheme.surface, CarPillShape).padding(horizontal = 28.dp, vertical = 16.dp)) {
                Text(swipeFeedback.orEmpty(), color = MobileTheme.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // A queue opened in video mode carries over to the Up Next panel if video turns off.
    LaunchedEffect(isQueueOpen, isVideoActive) {
        if (isQueueOpen && !isVideoActive) {
            pane = CarPane.UP_NEXT
            viewModel.closeQueue()
        }
    }
}

// ─── Body: stage ↔ split ─────────────────────────────────────────────────────

/**
 * One shared artwork springs between its stage size (full body height) and its compact split size;
 * the stage control column and the split layout cross-fade around it.
 */
@Composable
private fun NowPlayingBody(
    viewModel: MusicViewModel,
    song: Song?,
    albumArt: ImageBitmap?,
    isPlaying: Boolean,
    pane: CarPane,
    onPane: (CarPane) -> Unit,
    onTogglePane: (CarPane) -> Unit,
    backdrop: Backdrop,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val stageSide = minOf(maxHeight, maxWidth * 0.52f)
        val t by animateFloatAsState(
            targetValue = if (pane != CarPane.NONE) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
            label = "npMorph"
        )
        val artSide = lerp(stageSide, SplitArtSide, t)
        // Artwork stays on the driver's side in both Stage and Split, so opening lyrics only shrinks it.
        val driverRight = LocalDriveSide.current == DriveSide.RIGHT
        val stageArtX = if (driverRight) maxWidth - stageSide else 0.dp
        val splitArtX = if (driverRight) maxWidth - SplitArtSide else 0.dp
        val artX = lerp(stageArtX, splitArtX, t)
        val pauseScale by animateFloatAsState(if (isPlaying) 1f else 0.97f, spring(dampingRatio = 0.7f, stiffness = 300f), label = "heroScale")

        // Stage: control column to the right of the big artwork
        AnimatedVisibility(
            visible = pane == CarPane.NONE,
            enter = fadeIn(tween(260, delayMillis = 90)) + slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 320f)) { it / 12 },
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize().then(
                if (driverRight) Modifier.padding(end = stageSide + StageGap) else Modifier.padding(start = stageSide + StageGap)
            )
        ) {
            StageColumn(viewModel, song, isPlaying, backdrop, onSongActions, onPane, Modifier.fillMaxSize())
        }

        // Split: compact left column + panel
        AnimatedVisibility(
            visible = pane != CarPane.NONE,
            enter = fadeIn(tween(260, delayMillis = 100)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize()
        ) {
            val lastPane = remember { mutableStateOf(CarPane.LYRICS) }
            SideEffect { if (pane != CarPane.NONE) lastPane.value = pane }
            Row(Modifier.fillMaxSize()) {
                val column: @Composable () -> Unit = {
                    SplitLeft(viewModel, song, isPlaying, backdrop, onSongActions, Modifier.width(SplitArtSide).fillMaxHeight())
                }
                val panelSlot = Modifier.weight(1f).fillMaxHeight()
                val panel: @Composable (Modifier) -> Unit = { m ->
                    SplitPanel(viewModel, shown = if (pane != CarPane.NONE) pane else lastPane.value, selected = pane, onTogglePane = onTogglePane, modifier = m)
                }
                if (driverRight) {
                    panel(panelSlot); Spacer(Modifier.width(32.dp)); column()
                } else {
                    column(); Spacer(Modifier.width(32.dp)); panel(panelSlot)
                }
            }
        }

        // Artwork (drawn last, on top). Fixed square that never shifts; settles back slightly while paused.
        AnimatedContent(
            targetState = song?.id to albumArt,
            contentKey = { it.first },
            transitionSpec = {
                (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 300f)) { it / 3 } + fadeIn(tween(220))) togetherWith fadeOut(tween(160))
            },
            label = "heroArt",
            modifier = Modifier
                .offset(x = artX)
                .size(artSide)
                .clickable(enabled = pane != CarPane.NONE, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onPane(CarPane.NONE)
                }
        ) { (_, art) ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = pauseScale; scaleY = pauseScale }
                    .clip(CarHeroArtShape)
                    .background(carIdleFill),
                contentAlignment = Alignment.Center
            ) {
                if (art != null) Image(art, contentDescription = "Artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else CarGlyph(Icons.Rounded.MusicNote, null, MobileTheme.textTertiary, 96.dp)
            }
        }
    }
}

/** Stage control column: title, action chips, seek, big transport, Lyrics / Up Next switch. */
@Composable
private fun StageColumn(
    viewModel: MusicViewModel,
    song: Song?,
    isPlaying: Boolean,
    backdrop: Backdrop,
    onSongActions: (Song) -> Unit,
    onPane: (CarPane) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        SongTitles(song, big = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        if (song != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FavoriteChip(viewModel, song)
                DownloadChip(viewModel, song)
                RadioChip(viewModel)
                Spacer(Modifier.weight(1f))
                CarIconButton(Icons.Rounded.MoreHoriz, "More actions", onClick = { onSongActions(song) }, filled = true, glyph = 24.dp)
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(20.dp))
        SeekSlider(viewModel, song, backdrop)
        Spacer(Modifier.height(4.dp))
        TransportRow(viewModel, isPlaying, TransportStyle.STAGE)
        Spacer(Modifier.height(12.dp))
        CarSegmented(
            items = PaneTabs,
            selected = null,
            onSelect = { onPane(PaneOrder[it]) },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        )
    }
}

/** Split left column: room for the artwork (drawn by the parent), compact title row, slider, transport. */
@Composable
private fun SplitLeft(
    viewModel: MusicViewModel,
    song: Song?,
    isPlaying: Boolean,
    backdrop: Backdrop,
    onSongActions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.height(SplitArtSide + 16.dp))
        CompactTitleRow(viewModel, song, onSongActions)
        Spacer(Modifier.weight(1f))
        SeekSlider(viewModel, song, backdrop)
        Spacer(Modifier.height(4.dp))
        TransportRow(viewModel, isPlaying, TransportStyle.COMPACT)
    }
}

/** Lyrics / Up Next panel with the segmented switch along its bottom edge. */
@Composable
private fun SplitPanel(
    viewModel: MusicViewModel,
    shown: CarPane,
    selected: CarPane,
    onTogglePane: (CarPane) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.clip(CarSheetShape).background(MobileTheme.panel)) {
        AnimatedContent(
            targetState = shown,
            transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 16 }) togetherWith fadeOut(tween(140)) },
            label = "npPane",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { p ->
            when (p) {
                CarPane.LYRICS -> LyricsPane(viewModel, Modifier.fillMaxSize())
                else -> CarQueueList(viewModel, Modifier.fillMaxSize().padding(top = 8.dp), horizontalPadding = 28.dp)
            }
        }
        CarSegmented(
            items = PaneTabs,
            selected = PaneOrder.indexOf(selected).takeIf { it >= 0 },
            onSelect = { onTogglePane(PaneOrder[it]) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
        )
    }
}

// ─── Titles & chips ──────────────────────────────────────────────────────────

/** Title block that cross-fades on track change. [big] = stage (two-line title, artist, album), else compact. */
@Composable
private fun SongTitles(song: Song?, big: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = song,
        contentKey = { it?.id },
        transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith fadeOut(tween(140)) },
        label = "npTitle",
        modifier = modifier
    ) { s ->
        val artist = s?.artist?.ifBlank { null } ?: "Unknown Artist"
        val album = s?.album.orEmpty()
        Column {
            Text(
                s?.title?.ifBlank { null } ?: "Not Playing",
                color = MobileTheme.text,
                fontSize = if (big) 40.sp else 26.sp,
                lineHeight = if (big) 46.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (big) (-0.5).sp else 0.sp,
                maxLines = if (big) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (big) {
                Text(artist, color = MobileTheme.textSecondary, fontSize = 22.sp, lineHeight = 28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (album.isNotBlank()) {
                    Text(album, color = MobileTheme.textTertiary, fontSize = 16.sp, lineHeight = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    listOf(artist, album).filter { it.isNotBlank() }.joinToString(" • "),
                    color = MobileTheme.textSecondary, fontSize = 17.sp, lineHeight = 22.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Heart pops when the favourite state flips. */
@Composable
private fun rememberHeartScale(isFavorite: Boolean): Animatable<Float, AnimationVector1D> {
    val heartScale = remember { Animatable(1f) }
    var seenFavorite by remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite != seenFavorite) {
            seenFavorite = isFavorite
            heartScale.snapTo(0.6f)
            heartScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 700f))
        }
    }
    return heartScale
}

/** Split / deck title row: titles + favourite + ⋯. */
@Composable
private fun CompactTitleRow(viewModel: MusicViewModel, song: Song?, onSongActions: (Song) -> Unit) {
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val isFavorite = song?.path?.let { favoritePaths.contains(it) } == true
    val heartScale = rememberHeartScale(isFavorite)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SongTitles(song, big = false, modifier = Modifier.weight(1f))
        if (song != null) {
            CarIconButton(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = { viewModel.toggleFavorite(song.path) },
                tint = if (isFavorite) FavoriteRed else MobileTheme.text,
                size = 48.dp, glyph = 24.dp,
                modifier = Modifier.graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
            )
            CarIconButton(Icons.Rounded.MoreHoriz, "More actions", onClick = { onSongActions(song) }, size = 48.dp, glyph = 24.dp)
        }
    }
}

/** Video-mode title row: titles + favourite + download + ⋯. */
@Composable
private fun TitleRow(viewModel: MusicViewModel, song: Song?, onSongActions: (Song) -> Unit) {
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val isFavorite = song?.path?.let { favoritePaths.contains(it) } == true
    val heartScale = rememberHeartScale(isFavorite)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SongTitles(song, big = false, modifier = Modifier.weight(1f))
        if (song != null) {
            CarIconButton(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = { viewModel.toggleFavorite(song.path) },
                tint = if (isFavorite) FavoriteRed else MobileTheme.text,
                filled = true,
                glyph = 24.dp,
                modifier = Modifier.graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
            )
            DownloadButton(viewModel, song)
            CarIconButton(Icons.Rounded.MoreHoriz, "More actions", onClick = { onSongActions(song) }, filled = true, glyph = 24.dp)
        }
    }
}

/** Pill action with a leading glyph / progress ring and a short label (56 dp tall). */
@Composable
private fun CarActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MobileTheme.text,
    fill: Color = carIdleFill,
    progress: Float? = null,
    loading: Boolean = false
) {
    val animatedFill by animateColorAsState(fill, tween(220), label = "chipFill")
    Row(
        modifier = modifier
            .height(CarDims.Target)
            .clip(CarPillShape)
            .background(animatedFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            progress != null -> CircularProgressIndicator(
                progress = progress.coerceIn(0.04f, 1f), color = tint, trackColor = tint.copy(alpha = 0.25f),
                strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp)
            )
            loading -> CircularProgressIndicator(color = tint, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            else -> CarGlyph(icon, null, tint, 22.dp)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun FavoriteChip(viewModel: MusicViewModel, song: Song) {
    val favoritePaths by viewModel.favoritePaths.collectAsState()
    val isFavorite = favoritePaths.contains(song.path)
    val heartScale = rememberHeartScale(isFavorite)
    CarActionChip(
        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        label = "Favorite",
        onClick = { viewModel.toggleFavorite(song.path) },
        tint = if (isFavorite) FavoriteRed else MobileTheme.text,
        fill = if (isFavorite) FavoriteRed.copy(alpha = 0.16f) else carIdleFill,
        modifier = Modifier.graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value }
    )
}

/** Streams can be saved offline (progress ring); a saved track can be deleted again. */
@Composable
private fun DownloadChip(viewModel: MusicViewModel, song: Song) {
    val downloads by viewModel.downloadStates.collectAsState()
    val status = downloads[song.id] ?: DownloadStatus.Idle
    val isDownloaded = status is DownloadStatus.Downloaded || viewModel.isSongDownloaded(song.id) ||
        song.id.startsWith("download_") || song.path.contains("MusicPlayer")
    val downloadable = (song.isStream() && song.duration > 0) || isDownloaded
    if (!downloadable) return
    when {
        status is DownloadStatus.Downloading -> CarActionChip(Icons.Rounded.Download, "Saving", onClick = {}, progress = status.progress)
        isDownloaded -> CarActionChip(
            Icons.Rounded.CheckCircle, "Offline", onClick = { viewModel.deleteDownload(song) },
            tint = DownloadedGreen, fill = DownloadedGreen.copy(alpha = 0.16f)
        )
        status is DownloadStatus.Failed -> CarActionChip(Icons.Rounded.Refresh, "Retry", onClick = { viewModel.downloadSong(song, force = true) })
        else -> CarActionChip(Icons.Rounded.Download, "Download", onClick = { viewModel.downloadSong(song) })
    }
}

@Composable
private fun RadioChip(viewModel: MusicViewModel) {
    val radioLoading by viewModel.isRadioLoading.collectAsState()
    val radioName by viewModel.activeRadioStationName.collectAsState()
    val active = radioName != null && !radioLoading
    CarActionChip(
        icon = Icons.Rounded.Podcasts,
        label = if (radioLoading) "Starting" else if (active) "Radio on" else "Radio",
        onClick = { viewModel.startRadioForCurrentSong() },
        tint = if (active) MobileTheme.accent else MobileTheme.text,
        fill = if (active) carSelectedFill else carIdleFill,
        loading = radioLoading
    )
}

/** Video-mode download button. */
@Composable
private fun DownloadButton(viewModel: MusicViewModel, song: Song) {
    val downloads by viewModel.downloadStates.collectAsState()
    val status = downloads[song.id] ?: DownloadStatus.Idle
    val isDownloaded = status is DownloadStatus.Downloaded || viewModel.isSongDownloaded(song.id) ||
        song.id.startsWith("download_") || song.path.contains("MusicPlayer")
    val downloadable = (song.isStream() && song.duration > 0) || isDownloaded
    if (!downloadable) return
    when {
        status is DownloadStatus.Downloading -> CarIconButton(
            Icons.Rounded.Download, "Downloading", onClick = {}, filled = true, glyph = 24.dp, progress = status.progress
        )
        isDownloaded -> CarIconButton(
            Icons.Rounded.CheckCircle, "Saved offline — tap to delete download", onClick = { viewModel.deleteDownload(song) },
            filled = true, glyph = 24.dp, tint = DownloadedGreen
        )
        status is DownloadStatus.Failed -> CarIconButton(
            Icons.Rounded.Refresh, "Retry download", onClick = { viewModel.downloadSong(song, force = true) }, filled = true, glyph = 24.dp
        )
        else -> CarIconButton(Icons.Rounded.Download, "Download", onClick = { viewModel.downloadSong(song) }, filled = true, glyph = 24.dp)
    }
}

// ─── Segmented switch ────────────────────────────────────────────────────────

/** Flat segmented capsule with a spring-sliding accent indicator ([selected] = null shows none). */
@Composable
private fun CarSegmented(
    items: List<Pair<ImageVector, String>>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.background(carIdleFill, CarPillShape).padding(4.dp)) {
        val segWidth = maxWidth / items.size
        val indicatorX by animateDpAsState(segWidth * (selected ?: 0), spring(dampingRatio = 0.85f, stiffness = 400f), label = "segX")
        val indicatorAlpha by animateFloatAsState(if (selected == null) 0f else 1f, tween(200), label = "segAlpha")
        Box(
            Modifier
                .offset(x = indicatorX)
                .size(segWidth, maxHeight)
                .graphicsLayer { alpha = indicatorAlpha }
                .background(MobileTheme.accent, CarPillShape)
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, (icon, label) ->
                val tint by animateColorAsState(if (selected == i) MobileTheme.onText else MobileTheme.text, tween(200), label = "segTint")
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CarPillShape)
                        .clickable { onSelect(i) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarGlyph(icon, null, tint, 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// ─── Seek + transport ────────────────────────────────────────────────────────

/**
 * Leaf: liquid glass seek slider (backdrop lens, AGENTS.md §7) with elapsed / format / remaining.
 * Position is read here so the rest of the screen doesn't recompose on ticks.
 */
@Composable
private fun SeekSlider(viewModel: MusicViewModel, song: Song?, backdrop: Backdrop) {
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    var scrub by remember { mutableStateOf<Float?>(null) }
    var scrubToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownMs = ((scrub ?: progress) * durationMs).toLong()
    // Keep the thumb where the user dropped it until the player reports the new position.
    LaunchedEffect(progress) {
        val s = scrub
        if (s != null && kotlin.math.abs(progress - s) < 0.02f) scrub = null
    }
    Column(Modifier.fillMaxWidth()) {
        LiquidSlider(
            value = {
                scrub ?: if (viewModel.durationFlow.value > 0)
                    (viewModel.positionFlow.value.toFloat() / viewModel.durationFlow.value).coerceIn(0f, 1f) else 0f
            },
            onValueChange = { v -> if (durationMs > 0) { scrub = v; scrubToken++ } },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            accentColor = MobileTheme.accent,
            trackColorOverride = MobileTheme.text.copy(alpha = 0.14f),
            trackHeight = 8.dp,
            thumbWidth = 40.dp,
            thumbHeight = 24.dp,
            onValueChangeFinished = {
                val target = scrub
                if (target != null && durationMs > 0) {
                    viewModel.seekTo((target * durationMs).toLong())
                    val token = ++scrubToken
                    scope.launch { delay(1200); if (scrubToken == token) scrub = null }
                } else scrub = null
            }
        )
        Box(Modifier.fillMaxWidth().offset(y = (-4).dp)) {
            Text(carFormatTime(shownMs), color = MobileTheme.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterStart))
            carFormatBadge(song)?.let { badge ->
                Box(Modifier.align(Alignment.Center).background(carIdleFill, CarPillShape).padding(horizontal = 10.dp, vertical = 2.dp)) {
                    Text(badge, color = MobileTheme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                if (durationMs > 0) "-" + carFormatTime((durationMs - shownMs).coerceAtLeast(0)) else "Live",
                color = MobileTheme.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

private enum class TransportStyle { STAGE, COMPACT, DECK }

/** Button sizes per transport style. [skip] = show the ±10 s buttons. */
private class TransportSizes(
    val height: Dp, val mode: Dp, val modeGlyph: Dp, val prevNext: Dp, val prevNextGlyph: Dp,
    val skip: Dp, val skipGlyph: Dp, val play: Dp, val playGlyph: Dp, val showSkip: Boolean
)

private val StageTransport = TransportSizes(112.dp, 56.dp, 26.dp, 64.dp, 40.dp, 56.dp, 28.dp, 96.dp, 52.dp, true)
private val CompactTransport = TransportSizes(84.dp, 48.dp, 24.dp, 56.dp, 36.dp, 0.dp, 0.dp, 72.dp, 40.dp, false)
private val DeckTransport = TransportSizes(84.dp, 48.dp, 24.dp, 56.dp, 38.dp, 48.dp, 26.dp, 76.dp, 42.dp, true)

@Composable
private fun TransportRow(viewModel: MusicViewModel, isPlaying: Boolean, style: TransportStyle = TransportStyle.DECK) {
    val mode by viewModel.playbackMode.collectAsState()
    val (modeIcon, modeLabel, modeActive) = when (mode) {
        PlaybackMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Shuffle on", true)
        PlaybackMode.REPEAT_ONE -> Triple(Icons.Rounded.RepeatOne, "Repeat one on", true)
        PlaybackMode.QUEUE -> Triple(Icons.Rounded.Repeat, "Playing in order", false)
    }
    val s = when (style) {
        TransportStyle.STAGE -> StageTransport
        TransportStyle.COMPACT -> CompactTransport
        TransportStyle.DECK -> DeckTransport
    }
    Row(
        Modifier.fillMaxWidth().height(s.height),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarIconButton(modeIcon, modeLabel, onClick = { viewModel.cyclePlaybackMode() }, size = s.mode, glyph = s.modeGlyph,
            tint = if (modeActive) MobileTheme.accent else MobileTheme.textSecondary)
        CarIconButton(Icons.Rounded.SkipPrevious, "Previous", onClick = { viewModel.playPreviousSong() }, size = s.prevNext, glyph = s.prevNextGlyph)
        if (s.showSkip) {
            CarIconButton(Icons.Rounded.Replay10, "Back 10 seconds", onClick = { viewModel.skipBackward10s() }, size = s.skip, glyph = s.skipGlyph, tint = MobileTheme.textSecondary)
        }
        CarIconButton(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (isPlaying) "Pause" else "Play",
            onClick = { viewModel.togglePlayPause() },
            size = s.play,
            glyph = s.playGlyph,
            active = true
        )
        if (s.showSkip) {
            CarIconButton(Icons.Rounded.Forward10, "Forward 10 seconds", onClick = { viewModel.skipForward10s() }, size = s.skip, glyph = s.skipGlyph, tint = MobileTheme.textSecondary)
        }
        CarIconButton(Icons.Rounded.SkipNext, "Next", onClick = { viewModel.playNextSong() }, size = s.prevNext, glyph = s.prevNextGlyph)
        Spacer(Modifier.size(s.mode)) // balances the mode button so play/pause sits on the centre line
    }
}

// ─── Lyrics ──────────────────────────────────────────────────────────────────

/** Synced lyrics: active line full strength, others dimmed; tap a line to seek. Leaf (reads position). */
@Composable
private fun LyricsPane(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isLoading by viewModel.isLoadingLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()
    if (lyrics.isEmpty()) {
        Box(modifier.padding(28.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                if (isLoading) "Loading lyrics…" else "Lyrics aren't available for this song.",
                color = MobileTheme.textSecondary, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold
            )
        }
    } else {
        val activeIndex = remember(lyrics, positionMs) { lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0) }
        val listState = rememberLazyListState()
        LaunchedEffect(activeIndex) { listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0)) }
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemsIndexed(lyrics, key = { i, line -> "${line.timeMs}-$i" }) { i, line ->
                val active = i == activeIndex
                val alpha by animateFloatAsState(if (active) 1f else 0.35f, tween(300), label = "lyricAlpha")
                Text(
                    line.text.ifBlank { "♪" },
                    color = MobileTheme.text,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { this.alpha = alpha }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.seekTo(line.timeMs) }
                )
            }
        }
    }
}

// ─── Video mode controls ─────────────────────────────────────────────────────

@Composable
private fun VideoControlDeck(viewModel: MusicViewModel, song: Song?, backdrop: Backdrop, onSongActions: (Song) -> Unit) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MobileTheme.panel.copy(alpha = 0.88f), CarSheetShape)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        TitleRow(viewModel, song, onSongActions)
        Spacer(Modifier.height(4.dp))
        SeekSlider(viewModel, song, backdrop)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TransportRow(viewModel, isPlaying, TransportStyle.DECK) }
            Spacer(Modifier.width(16.dp))
            CarIconButton(Icons.Rounded.Podcasts, "Start radio", onClick = { viewModel.startRadioForCurrentSong() }, filled = true, glyph = 24.dp)
            Spacer(Modifier.width(8.dp))
            CarIconButton(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", onClick = { viewModel.toggleQueue() }, filled = true, glyph = 24.dp)
        }
    }
}
