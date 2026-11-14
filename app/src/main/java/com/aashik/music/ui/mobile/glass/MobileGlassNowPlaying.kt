package com.aashik.music.ui.mobile.glass

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.aashik.music.model.PlaybackMode
import com.aashik.music.model.Song
import com.aashik.music.ui.AlbumArtImage
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.viewmodel.MusicViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as glassBlur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.aashik.music.service.DownloadStatus
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HandleShape = RoundedCornerShape(2.5.dp)
private val PillShape = RoundedCornerShape(50)
private val DraggedSheetShape = RoundedCornerShape(24.dp)
private val FlatSheetShape = RoundedCornerShape(0.dp)

/** Backdrop recorded from the sheet's own artwork background; every glass control here refracts it. */
private val LocalNpBackdrop = compositionLocalOf<Backdrop?> { null }

/** Which content fills the area above the controls (designScreen/mobile/Now Playing.png). */
private enum class NowPlayingPane { PLAYER, LYRICS, QUEUE }

/** Colours of the Now Playing sheet: Light / Dark follow the app palette; Album Art tints it from the cover. */
@Immutable
private data class NpColors(
    val top: Color,
    val bottom: Color,
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val fill: Color,
    val track: Color,
    /** Colour for glyphs drawn on top of a [text]-coloured fill (the opposite of [text]). */
    val onText: Color = Color.Black,
    val isLight: Boolean = false,
    /** Strength of the blurred cover behind the sheet. */
    val coverAlpha: Float = 0.45f
)

private fun npColorsFor(mode: AppThemeMode, art: Color?): NpColors {
    if (mode == AppThemeMode.LIGHT) {
        val p = MobilePalettes.Light
        return NpColors(
            top = p.background, bottom = p.surface, text = p.text,
            secondary = p.textSecondary, tertiary = p.textTertiary,
            fill = p.text.copy(alpha = 0.08f), track = p.text.copy(alpha = 0.16f), isLight = true, coverAlpha = 0.28f
        )
    }
    val text = MobilePalettes.Dark.text
    if (mode == AppThemeMode.DARK || art == null) {
        val base = MobilePalettes.Dark
        return NpColors(
            base.surface, base.background, text, text.copy(alpha = 0.62f), text.copy(alpha = 0.45f),
            text.copy(alpha = 0.14f), text.copy(alpha = 0.22f),
            coverAlpha = if (mode == AppThemeMode.DARK) 0.14f else 0.45f
        )
    }
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb((art.red * 255).toInt(), (art.green * 255).toInt(), (art.blue * 255).toInt()), hsv
    )
    fun hsvColor(s: Float, v: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
    val sat = hsv[1].coerceIn(0.2f, 0.85f)
    return NpColors(
        top = hsvColor(sat * 0.7f, 0.44f),
        bottom = hsvColor(sat * 0.75f, 0.24f),
        text = text,
        secondary = text.copy(alpha = 0.62f),
        tertiary = text.copy(alpha = 0.45f),
        fill = text.copy(alpha = 0.14f),
        track = text.copy(alpha = 0.22f)
    )
}

/**
 * Full-screen Now Playing sheet (designScreen/mobile/Now Playing.png):
 * grab handle, large artwork that settles back when paused, title/artist with favourite,
 * shuffle/repeat toggle, radio and (for streams) download, scrubber with format badge,
 * rewind / play-pause / fast-forward, and a bottom strip that morphs the upper area between
 * artwork, synced lyrics and the Playing Next queue. Drag the top area down to dismiss.
 */
@Composable
fun MobileGlassNowPlaying(
    viewModel: MusicViewModel,
    currentSong: Song?,
    isPlaying: Boolean,
    albumArt: ImageBitmap?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    /** Hero transition hooks: fade of the sheet content, whether the real artwork is shown yet, and its bounds. */
    contentAlpha: () -> Float = { 1f },
    /** Opening progress 0..1 (mini-player → full sheet); the artwork is placed between [miniArt] and its hero spot. */
    entry: () -> Float = { 1f },
    /** Mini-player thumbnail bounds in root coordinates; null = no shared-element start. */
    miniArt: androidx.compose.ui.geometry.Rect? = null,
    onDrag: (Float) -> Unit = {},
    onDragEnd: (Float) -> Unit = {}
) {
    // ── Artwork-derived colours ─────────────────────────────────────────────
    var artColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(albumArt) {
        artColor = albumArt?.let { img ->
            withContext(Dispatchers.Default) {
                runCatching {
                    val bmp = img.asAndroidBitmap().let {
                        if (Build.VERSION.SDK_INT >= 26 && it.config == android.graphics.Bitmap.Config.HARDWARE) {
                            it.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                        } else it
                    }
                    MobilePalettes.dominantColor(bmp)
                }.getOrNull()
            }
        }
    }
    val themeMode by viewModel.themeMode.collectAsState()
    val target = remember(themeMode, artColor) { npColorsFor(themeMode, artColor) }
    val top by animateColorAsState(target.top, tween(450), label = "npTop")
    val bottom by animateColorAsState(target.bottom, tween(450), label = "npBottom")
    // Text and icons adapt to what is actually behind them: the sheet background (tinted gradient
    // plus blurred cover) is sampled and the content flips between dark and white.
    val adaptive = rememberAdaptiveLuminance(blendGlassSurface = false, startDark = themeMode == AppThemeMode.LIGHT)
    val content = adaptive.contentColor
    val contentIsLight = content.luminance() > 0.5f
    val c = target.copy(
        top = top,
        bottom = bottom,
        text = content,
        secondary = content.copy(alpha = 0.62f),
        tertiary = content.copy(alpha = 0.45f),
        fill = content.copy(alpha = if (contentIsLight) 0.14f else 0.08f),
        track = content.copy(alpha = if (contentIsLight) 0.25f else 0.16f),
        onText = if (contentIsLight) Color(0xFF0F172A) else Color.White
    )

    var pane by remember { mutableStateOf(NowPlayingPane.PLAYER) }
    BackHandler(enabled = pane != NowPlayingPane.PLAYER) { pane = NowPlayingPane.PLAYER }

    // ── Drag-down-to-dismiss: the drag drives the hero transition itself (see MobileGlassMusicApp) ──
    val dragModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta -> onDrag(delta) },
        onDragStopped = { velocity -> onDragEnd(velocity) }
    )

    val sheetBackdrop = rememberLayerBackdrop()
    val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current)

    CompositionLocalProvider(LocalNpBackdrop provides sheetBackdrop) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Background: artwork tint with a softly blurred copy of the cover (API 31+), recorded
        // so the liquid slider thumbs refract it.
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(sheetBackdrop)
                .drawWithContent {
                    // Record the background for luminance sampling, then draw it once.
                    adaptive.layer.record { this@drawWithContent.drawContent() }
                    drawLayer(adaptive.layer)
                }
                .background(Brush.verticalGradient(listOf(c.top, c.bottom)))
        ) {
            if (albumArt != null && Build.VERSION.SDK_INT >= 31) {
                Image(
                    bitmap = albumArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = c.coverAlpha }
                        .blur(90.dp)
                )
                Box(Modifier.fillMaxSize().background(c.bottom.copy(alpha = if (c.isLight) 0.6f else 0.35f)))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Grab handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .graphicsLayer { alpha = contentAlpha() }
                    .then(dragModifier)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(width = 36.dp, height = 5.dp).clip(HandleShape).background(c.text.copy(alpha = 0.4f)))
            }

            UpperArea(
                pane = pane,
                song = currentSong,
                albumArt = albumArt,
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                viewModel = viewModel,
                c = c,
                dragModifier = dragModifier,
                entry = entry,
                miniArt = miniArt,
                contentAlpha = contentAlpha,
                originY = statusTop + with(LocalDensity.current) { 28.dp.roundToPx() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            Column(Modifier.graphicsLayer { alpha = contentAlpha() }) {
            Spacer(Modifier.height(14.dp))
            Scrubber(viewModel = viewModel, song = currentSong, c = c, backdrop = sheetBackdrop, artColor = artColor)
            TransportRow(isPlaying, onPrevious, onPlayPause, onNext, c)
            Spacer(Modifier.height(10.dp))
            BottomStrip(
                pane = pane,
                onPane = { pane = if (pane == it) NowPlayingPane.PLAYER else it },
                c = c
            )
            // Lift the whole control stack a little off the bottom edge.
            Spacer(Modifier.height(34.dp))
            }
        }
    }
    }
}

// ─── Upper area ──────────────────────────────────────────────────────────────

/**
 * Artwork and title that morph between the hero layout (big cover, title underneath) and the
 * compact header (56dp cover beside the title) while lyrics / queue fade in below — the
 * shared-element move from the reference, driven by one spring.
 */
@Composable
private fun UpperArea(
    pane: NowPlayingPane,
    song: Song?,
    albumArt: ImageBitmap?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    viewModel: MusicViewModel,
    c: NpColors,
    dragModifier: Modifier,
    entry: () -> Float,
    miniArt: androidx.compose.ui.geometry.Rect?,
    contentAlpha: () -> Float,
    originY: Int,
    modifier: Modifier
) {
    val compact = pane != NowPlayingPane.PLAYER
    // 0 = hero, 1 = compact header
    val t by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "npMorph"
    )
    val heroActions by remember { derivedStateOf { t < 0.98f } }
    val radiusDp = (12f - 6f * t).roundToInt()
    val artShape = remember(radiusDp) { RoundedCornerShape(radiusDp.dp) }

    Layout(
        modifier = modifier,
        content = {
            // [0] artwork
            Box(
                modifier = Modifier
                    .then(dragModifier)
                    .clip(artShape)
                    .background(c.fill),
                contentAlignment = Alignment.Center
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt, contentDescription = "Artwork", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LargeGlyph(Icons.Rounded.MusicNote, null, c.tertiary, 40.dp)
                }
                // Hero layout: the action buttons sit inside the artwork's bottom-right corner.
                if (heroActions) {
                    NowPlayingActions(
                        song, isFavorite, onToggleFavorite, viewModel, c,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .graphicsLayer { alpha = (1f - t) * entry() * entry() }
                    )
                }
            }
            // [1] title
            TitleRow(song = song, c = c, shrink = t, modifier = dragModifier.graphicsLayer { alpha = contentAlpha() })
            // [2] lyrics / queue, faded in with the morph. The action buttons ride along in the
            // compact header (they are inside the artwork in the hero layout).
            Box(Modifier.graphicsLayer { alpha = t }) {
                AnimatedContent(
                    targetState = pane,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 14 }) togetherWith fadeOut(tween(160))
                    },
                    label = "npPane"
                ) { p ->
                    when (p) {
                        NowPlayingPane.LYRICS -> LyricsPane(viewModel, c, Modifier.fillMaxSize())
                        NowPlayingPane.QUEUE -> QueuePane(viewModel, song, c, Modifier.fillMaxSize())
                        NowPlayingPane.PLAYER -> Box(Modifier.fillMaxSize())
                    }
                }
            }
            // [3] compact-header actions
            NowPlayingActions(
                song, isFavorite, onToggleFavorite, viewModel, c,
                modifier = Modifier.graphicsLayer { alpha = t }
            )
        }
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val compactArt = 56.dp.roundToPx()
        val compactX = 22.dp.roundToPx()
        val compactY = 6.dp.roundToPx()
        val headerH = 76.dp.roundToPx()
        val titleShift = 64.dp.roundToPx()
        fun lerpI(a: Int, b: Int) = (a + (b - a) * t).roundToInt()

        val actions = measurables[3].measure(Constraints())
        val actionsGap = 8.dp.roundToPx()
        val actionsEnd = 22.dp.roundToPx()
        val titleW = lerpI(w, w - titleShift - actions.width - actionsGap - actionsEnd)
        val title = measurables[1].measure(Constraints(minWidth = titleW, maxWidth = titleW))
        val heroArea = h - title.height - 8.dp.roundToPx()
        // Cover is the largest square that fits; cover + title are centred as one group.
        val heroSide = minOf(w - 56.dp.roundToPx(), heroArea - 24.dp.roundToPx()).coerceAtLeast(compactArt)
        val heroGap = 18.dp.roundToPx()
        val groupTop = ((h - (heroSide + heroGap + title.height)) / 2).coerceAtLeast(0)
        val side = lerpI(heroSide, compactArt)
        // Shared element: while opening/closing the cover travels from the mini-player thumbnail.
        val e = entry()
        val mini = miniArt
        val artSide = if (mini != null) (mini.width + (side - mini.width) * e).roundToInt().coerceAtLeast(1) else side
        val art = measurables[0].measure(Constraints.fixed(artSide, artSide))
        val content = measurables[2].measure(Constraints(maxWidth = w, maxHeight = (h - headerH).coerceAtLeast(0)))

        val artX = lerpI((w - heroSide) / 2, compactX)
        val artY = lerpI(groupTop, compactY)
        val titleX = lerpI(0, titleShift)
        val titleY = lerpI(groupTop + heroSide + heroGap, compactY + (compactArt - title.height) / 2)
        layout(w, h) {
            content.place(0, headerH)
            if (mini != null) {
                art.place(
                    (mini.left + (artX - mini.left) * e).roundToInt(),
                    (mini.top - originY + (artY - (mini.top - originY)) * e).roundToInt()
                )
            } else art.place(artX, artY)
            title.place(titleX, titleY)
            // Only the header copy is placed; the hero copy lives inside the artwork.
            if (t > 0.02f) {
                actions.place(w - actionsEnd - actions.width, compactY + (compactArt - actions.height) / 2)
            }
        }
    }
}

@Composable
private fun TitleRow(song: Song?, c: NpColors, shrink: Float, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 28.dp, end = 22.dp)) {
        Text(
            text = song?.title?.ifBlank { null } ?: "Not Playing",
            color = c.text,
            fontSize = (20f - 4f * shrink).sp,
            lineHeight = (25f - 5f * shrink).sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song?.artist?.ifBlank { null } ?: "Unknown Artist",
            color = c.secondary,
            fontSize = (18f - 4f * shrink).sp,
            lineHeight = (23f - 5f * shrink).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Favourite, shuffle/repeat, radio and (streams) download: the glass actions that used to sit beside the title. */
@Composable
private fun NowPlayingActions(
    song: Song?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    viewModel: MusicViewModel,
    c: NpColors,
    modifier: Modifier = Modifier
) {
    // Favourite star pops when toggled.
    val starScale = remember { Animatable(1f) }
    var seenFavorite by remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite != seenFavorite) {
            seenFavorite = isFavorite
            starScale.snapTo(0.6f)
            starScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 700f))
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NpGlassButton(
            icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            c = c,
            size = 34.dp,
            glyph = 18.dp,
            modifier = Modifier.graphicsLayer { scaleX = starScale.value; scaleY = starScale.value },
            onClick = onToggleFavorite
        )
        ModeToggleButton(viewModel, c)
        RadioButton(viewModel, c)
        if (song != null && song.isStream() && !song.isLiveStream()) {
            DownloadButton(song, viewModel, c)
        }
    }
}

/** One button that cycles Shuffle → in order → Repeat One, mirroring the headunit's mode control. */
@Composable
private fun ModeToggleButton(viewModel: MusicViewModel, c: NpColors) {
    val mode by viewModel.playbackMode.collectAsState()
    val (icon, label, active) = when (mode) {
        PlaybackMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Shuffle on", true)
        PlaybackMode.REPEAT_ONE -> Triple(Icons.Rounded.RepeatOne, "Repeat one on", true)
        PlaybackMode.QUEUE -> Triple(Icons.Rounded.Repeat, "Playing in order", false)
    }
    NpGlassButton(icon, label, c, size = 34.dp, glyph = 18.dp, active = active) { viewModel.cyclePlaybackMode() }
}

@Composable
private fun RadioButton(viewModel: MusicViewModel, c: NpColors) {
    val radioLoading by viewModel.isRadioLoading.collectAsState()
    val radioName by viewModel.activeRadioStationName.collectAsState()
    NpGlassButton(
        Icons.Rounded.Radio, "Start radio from this song", c,
        size = 34.dp, glyph = 18.dp, active = radioName != null && !radioLoading, loading = radioLoading
    ) { viewModel.startRadioForCurrentSong() }
}

/** Online tracks only; local files are already on the device. */
@Composable
private fun DownloadButton(song: Song, viewModel: MusicViewModel, c: NpColors) {
    val context = LocalContext.current
    val downloads by viewModel.downloadStates.collectAsState()
    when (val status = downloads[song.id] ?: DownloadStatus.Idle) {
        is DownloadStatus.Downloading -> NpGlassButton(
            Icons.Rounded.Download, "Downloading", c, size = 34.dp, glyph = 18.dp, progress = status.progress
        ) {}
        is DownloadStatus.Downloaded -> NpGlassButton(
            Icons.Rounded.CheckCircle, "Downloaded", c, size = 34.dp, glyph = 18.dp, active = true
        ) { android.widget.Toast.makeText(context, "Saved for offline listening", android.widget.Toast.LENGTH_SHORT).show() }
        is DownloadStatus.Failed -> NpGlassButton(
            Icons.Rounded.Refresh, "Retry download", c, size = 34.dp, glyph = 18.dp
        ) { viewModel.downloadSong(song, force = true) }
        else -> NpGlassButton(
            Icons.Rounded.Download, "Download", c, size = 34.dp, glyph = 18.dp
        ) { viewModel.downloadSong(song) }
    }
}

private fun Song.isLiveStream(): Boolean = duration <= 0L

@Composable
private fun LyricsPane(viewModel: MusicViewModel, c: NpColors, modifier: Modifier) {
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isLoading by viewModel.isLoadingLyrics.collectAsState()
    val positionMs by viewModel.positionFlow.collectAsState()

    if (lyrics.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (isLoading) "Loading lyrics…" else "Lyrics aren't available for this song.",
                color = c.secondary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val activeIndex = remember(lyrics, positionMs) {
        lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 28.dp, end = 40.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(lyrics, key = { i, line -> "${line.timeMs}-$i" }) { i, line ->
            val active = i == activeIndex
            val alpha by animateFloatAsState(if (active) 1f else 0.35f, tween(300), label = "lyricAlpha")
            Text(
                text = line.text.ifBlank { "♪" },
                color = c.text,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.alpha = alpha }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        viewModel.seekTo(line.timeMs)
                    }
            )
        }
    }
}

@Composable
private fun QueuePane(viewModel: MusicViewModel, currentSong: Song?, c: NpColors, modifier: Modifier) {
    val userQueue by viewModel.userQueue.collectAsState()
    val mode by viewModel.playbackMode.collectAsState()
    val upcoming = remember(userQueue, currentSong, mode) {
        if (userQueue.isNotEmpty()) userQueue else viewModel.getUpcomingSongs(40)
    }
    val fromUserQueue = userQueue.isNotEmpty()

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Playing Next", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (!fromUserQueue) {
                    Text(
                        text = if (mode == PlaybackMode.SHUFFLE) "Shuffling" else "Continuing in order",
                        color = c.secondary,
                        fontSize = 13.sp
                    )
                }
            }
            if (fromUserQueue) {
                Text(
                    text = "Clear",
                    color = c.secondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { viewModel.clearQueue() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (upcoming.isEmpty()) {
            Text(
                text = "Nothing up next.",
                color = c.secondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(upcoming, key = { i, s -> "${s.id}-$i" }) { i, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable {
                                if (fromUserQueue) viewModel.playFromQueue(i) else viewModel.play(song, direction = 1)
                            }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(path = song.path, artworkUrl = song.artworkUrl, size = 44.dp, borderRadius = 6.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title.ifBlank { "Untitled" }, color = c.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist.ifBlank { "Unknown Artist" }, color = c.secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (fromUserQueue) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).clickable { viewModel.removeFromQueue(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Remove", tint = c.secondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Bright, saturated version of the cover's dominant hue for the seekbar fill; null when there is no art. */
private fun artSeekFill(art: Color?): Color? {
    if (art == null) return null
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb((art.red * 255).toInt(), (art.green * 255).toInt(), (art.blue * 255).toInt()), hsv
    )
    hsv[1] = hsv[1].coerceIn(0.45f, 0.9f)
    hsv[2] = 1f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// ─── Controls ────────────────────────────────────────────────────────────────

@Composable
private fun Scrubber(viewModel: MusicViewModel, song: Song?, c: NpColors, backdrop: com.kyant.backdrop.Backdrop, artColor: Color?) {
    // Filled part follows the cover: its hue, lifted to a bright, vivid tint so it reads on the dark sheet.
    val fillColor by animateColorAsState(artSeekFill(artColor) ?: c.text.copy(alpha = 0.9f), tween(450), label = "seekFill")
    // Position is read here (leaf) so the rest of the sheet doesn't recompose on every tick.
    val positionMs by viewModel.positionFlow.collectAsState()
    val durationMs by viewModel.durationFlow.collectAsState()
    var scrub by remember { mutableStateOf<Float?>(null) }
    var scrubToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = scrub ?: progress
    val shownMs = (shown * durationMs).toLong()

    // After a seek keep the thumb where the user put it until the player reports the new
    // position (or 1.2 s passes), instead of snapping back to the old position.
    LaunchedEffect(progress) {
        val s = scrub
        if (s != null && kotlin.math.abs(progress - s) < 0.02f) scrub = null
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        LiquidSlider(
            // Read playback state inside the lambda so the slider tracks it as it changes.
            value = {
                scrub ?: if (viewModel.durationFlow.value > 0)
                    (viewModel.positionFlow.value.toFloat() / viewModel.durationFlow.value).coerceIn(0f, 1f) else 0f
            },
            onValueChange = { v ->
                if (durationMs > 0) { scrub = v; scrubToken++ }
            },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            accentColor = fillColor,
            trackColorOverride = c.text.copy(alpha = 0.12f),
            trackHeight = 8.dp,
            thumbWidth = 40.dp,
            thumbHeight = 24.dp,
            onValueChangeFinished = {
                val target = scrub
                if (target != null && durationMs > 0) {
                    viewModel.seekTo((target * durationMs).toLong())
                    val token = ++scrubToken
                    scope.launch { kotlinx.coroutines.delay(1200); if (scrubToken == token) scrub = null }
                } else {
                    scrub = null
                }
            }
        )
        Box(Modifier.fillMaxWidth().offset(y = (-6).dp)) {
            Text(formatTimeMs(shownMs), color = c.tertiary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterStart))
            formatBadge(song)?.let { badge ->
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(PillShape)
                        .background(c.fill)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = c.secondary, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(badge, color = c.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "-" + formatTimeMs((durationMs - shownMs).coerceAtLeast(0)),
                color = c.tertiary,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun TransportRow(isPlaying: Boolean, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, c: NpColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(Icons.Rounded.FastRewind, "Previous", 44.dp, c, onPrevious)
        TransportButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", 64.dp, c, onPlayPause)
        TransportButton(Icons.Rounded.FastForward, "Next", 44.dp, c, onNext)
    }
}

@Composable
private fun TransportButton(icon: ImageVector, contentDescription: String, glyph: Dp, c: NpColors, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "tpScale")
    val bgAlpha by animateFloatAsState(if (pressed) 0.14f else 0f, tween(120), label = "tpBg")
    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(c.text.copy(alpha = bgAlpha))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedGlyph(icon, contentDescription, c.text, glyph)
    }
}

/** Glyph that scales/fades between icons (play ⇄ pause, shuffle ⇄ repeat). */
@Composable
private fun AnimatedGlyph(icon: ImageVector, contentDescription: String?, tint: Color, size: Dp) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(tween(160)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f))) togetherWith
                (fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)))
        },
        label = "glyph"
    ) { ic -> LargeGlyph(ic, contentDescription, tint, size) }
}

/** The design-system Icon never scales a vector past its 24dp intrinsic size; big glyphs need Fit. */
@Composable
private fun LargeGlyph(icon: ImageVector, contentDescription: String?, tint: Color, size: Dp) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(size)
    )
}

@Composable
private fun BottomStrip(pane: NowPlayingPane, onPane: (NowPlayingPane) -> Unit, c: NpColors) {
    val context = LocalContext.current
    val route = rememberAudioRouteName()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaneToggle(Icons.Rounded.Lyrics, "Lyrics", pane == NowPlayingPane.LYRICS, c) { onPane(NowPlayingPane.LYRICS) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "routeScale")
            Row(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .height(40.dp)
                    .npGlassSurface(c, 40.dp)
                    .clickable(interactionSource = interaction, indication = null) { openSystemOutputSwitcher(context) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LargeGlyph(route.icon, "Audio output", c.text, 18.dp)
                Text(route.label, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        PaneToggle(Icons.AutoMirrored.Rounded.FormatListBulleted, "Playing Next", pane == NowPlayingPane.QUEUE, c) { onPane(NowPlayingPane.QUEUE) }
    }
}

@Composable
private fun PaneToggle(icon: ImageVector, contentDescription: String, active: Boolean, c: NpColors, onClick: () -> Unit) {
    NpGlassButton(icon, contentDescription, c, size = 40.dp, glyph = 20.dp, active = active, onClick = onClick)
}

// ─── Small pieces ────────────────────────────────────────────────────────────

/** Liquid-glass surface (blur + lens from the Glass Style setting, no shadow) over the sheet's backdrop. */
@Composable
private fun Modifier.npGlassSurface(c: NpColors, height: Dp, active: Boolean = false): Modifier {
    val backdrop = LocalNpBackdrop.current
    val glass = floatingGlass(elementHeight = height)
    val surface by animateColorAsState(
        if (active) c.text.copy(alpha = 0.92f) else c.text.copy(alpha = glass.surfaceAlpha * 0.45f),
        tween(120), label = "glassSurface"
    )
    return if (backdrop != null) {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
                vibrancy()
                if (glass.blur.value > 0f) glassBlur(glass.blur.toPx())
                if (glass.lensAmount.value > 0f) lens(glass.lensHeight.toPx(), glass.lensAmount.toPx())
            },
            shadow = { null },
            innerShadow = { null },
            onDrawSurface = { drawRect(surface) }
        )
    } else {
        this.clip(PillShape).background(surface)
    }
}

/**
 * Round liquid-glass control used across the sheet: the sheet's own backdrop is blurred and
 * refracted (parameters from Settings → Glass Style), with a light tint instead of a flat fill.
 * [active] turns it into a solid light disc; [progress] shows a spinner/ring in place of the icon.
 */
@Composable
private fun NpGlassButton(
    icon: ImageVector,
    contentDescription: String,
    c: NpColors,
    size: Dp,
    glyph: Dp,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    loading: Boolean = false,
    progress: Float? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "glassScale")
    val tint by animateColorAsState(if (active) c.onText else c.text, tween(120), label = "glassTint")
    val glassModifier = Modifier.npGlassSurface(c, size, active)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(glassModifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            progress != null -> CircularProgressIndicator(
                progress = progress.coerceIn(0.04f, 1f),
                color = tint,
                trackColor = tint.copy(alpha = 0.25f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(glyph + 4.dp)
            )
            loading -> CircularProgressIndicator(
                color = tint,
                strokeWidth = 2.dp,
                modifier = Modifier.size(glyph + 4.dp)
            )
            else -> AnimatedGlyph(icon, contentDescription, tint, glyph)
        }
    }
}

/** "Lossless" for FLAC/ALAC/WAV files, the container name otherwise, "Streaming" for online tracks. */
private fun formatBadge(song: Song?): String? {
    song ?: return null
    if (song.isStream()) return "Streaming"
    val ext = song.path.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "flac", "alac", "wav", "aiff", "aif" -> "Lossless"
        "m4a" -> "AAC"
        "" -> null
        else -> ext.uppercase()
    }
}

private data class AudioRoute(val label: String, val icon: ImageVector)

/** Name of the device audio is currently going to; updates when headphones/Bluetooth connect. */
@Composable
private fun rememberAudioRouteName(): AudioRoute {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    fun read(): AudioRoute {
        val outs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }?.let {
            return AudioRoute(it.productName?.toString()?.ifBlank { null } ?: "Bluetooth", Icons.Rounded.Headphones)
        }
        outs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }?.let { return AudioRoute("Headphones", Icons.Rounded.Headphones) }
        return AudioRoute("Phone Speaker", Icons.Rounded.Speaker)
    }
    var route by remember { mutableStateOf(read()) }
    DisposableEffect(audio) {
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) { route = read() }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) { route = read() }
        }
        audio.registerAudioDeviceCallback(cb, null)
        onDispose { audio.unregisterAudioDeviceCallback(cb) }
    }
    return route
}

/** Opens the system media output picker (Android 11+), falling back to Bluetooth settings. */
private fun openSystemOutputSwitcher(context: Context) {
    if (Build.VERSION.SDK_INT >= 30) {
        val ok = runCatching {
            context.sendBroadcast(
                Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                    .setPackage("com.android.systemui")
                    .putExtra("package_name", context.packageName)
            )
        }.isSuccess
        if (ok) return
    }
    runCatching {
        context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
