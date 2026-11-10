# Mobile Apple Music Migration Readiness & Missing Prerequisites Audit

> **Target Goal**: Complete 1:1 redesign of the mobile experience from scratch based on `designScreen/mobile/*.png` using Liquid Glass (`kyant.gitbook.io/backdrop`).  
> **Status**: **Missing Essential Implementation Prerequisites Identified & Resolved Below.**

---

## 1. Executive Summary & Readiness Assessment

### Is the current specification enough to start coding immediately?
**NO — Not yet without the 5 architectural gaps resolved in this document.** 

While the visual design specifications and screen-by-screen breakdown are comprehensive, several critical **architectural, technical, and data contract prerequisites** were missing before hands-on migration could start safely:

1. **MISSING: Top-Level Mobile Screen Navigation & Routing Engine** (The app currently only switches between `MusicListScreen` and `DriveModeScreen`).
2. **MISSING: Data Models & Repository Contracts** (The app lacks distinct `Album`, `Artist`, and `Playlist` entities; it only has flat `Song.kt`).
3. **MISSING: Liquid Glass Shader Port & Headunit Hardware Guardrails** (Need a clean fallback strategy for low-spec Mali GPU / Headunits per `AGENTS.md`).
4. **MISSING: Modal Presentation Engine for Fullscreen Now Playing** (Need seamless transition from the floating capsule dock into the fullscreen takeover).
5. **MISSING: Form-Factor Decoupling Strategy** (Prevent breaking the frozen Automotive Headunit 1280×720 landscape layout while replacing mobile portrait).

All 5 missing requirements have been designed, structured, and specified below.

---

## 2. The 5 Missing Implementation Specifications

### 2.1 Missing Item 1: Top-Level Mobile Navigation Graph & State Router

#### Current Problem:
`MusicApp.kt` directly embeds `MusicListScreen(viewModel)` with an internal `enum class LibraryTab { ALL_SONGS, FOLDERS, FAVORITES, RADIO, MORE }`. There is no router for:
- `MobileScreen.HOME`
- `MobileScreen.NEW`
- `MobileScreen.RADIO`
- `MobileScreen.LIBRARY`
- `MobileScreen.SEARCH`
- Nested sub-screens: `ALBUM_DETAIL(albumId)`, `ARTIST_PAGE(artistId)`, `PLAYLIST_DETAIL(playlistId)`.

#### Resolution Specification:
Define a sealed navigation hierarchy in `com.aashik.music.ui.mobile.navigation.MobileNavRoute`:

```kotlin
package com.aashik.music.ui.mobile.navigation

sealed class MobileNavRoute {
    // 5 Primary Floating Bottom Bar Tabs
    object Home : MobileNavRoute()
    object New : MobileNavRoute()
    object Radio : MobileNavRoute()
    object Library : MobileNavRoute()
    object Search : MobileNavRoute()

    // Nested Detail Destinations
    data class AlbumDetail(val albumName: String, val artistName: String) : MobileNavRoute()
    data class ArtistPage(val artistName: String) : MobileNavRoute()
    data class PlaylistDetail(val playlistId: String, val title: String) : MobileNavRoute()
}

class MobileNavigationController {
    var currentRoute by mutableStateOf<MobileNavRoute>(MobileNavRoute.Home)
        private set
    private val backStack = mutableStateListOf<MobileNavRoute>(MobileNavRoute.Home)

    fun navigateTo(route: MobileNavRoute) {
        if (currentRoute != route) {
            backStack.add(route)
            currentRoute = route
        }
    }

    fun popBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            currentRoute = backStack.last()
            return true
        }
        return false
    }
}
```

---

### 2.2 Missing Item 2: Domain Data Entities & Repository Layer

#### Current Problem:
Currently, the SQLite / Room database and `MusicViewModel` only have `Song.kt` and `MusicFolder.kt`. The screenshots heavily rely on:
- **`Album` entity**: `title`, `artist`, `year`, `genre`, `tracksCount`, `editorialReview`, `isDolbyAtmos`, `isLossless`.
- **`Artist` entity**: `name`, `photoUrl`, `from`, `born`, `genre`, `biography`, `topSongs`, `essentialAlbums`.
- **`Playlist` entity**: `id`, `name`, `curator`, `description`, `lastUpdated`, `songsCount`, `totalDuration`, `featuredArtists`.

#### Resolution Specification:
Add high-performance domain models under `com.aashik.music.model.domain`:

```kotlin
package com.aashik.music.model.domain

import com.aashik.music.model.Song

data class Album(
    val title: String,
    val artist: String,
    val year: String = "2025",
    val genre: String = "Pop",
    val artworkUri: String? = null,
    val editorialReview: String = "",
    val isDolbyAtmos: Boolean = true,
    val isLossless: Boolean = true,
    val tracks: List<Song> = emptyList()
)

data class Artist(
    val name: String,
    val photoUrl: String? = null,
    val origin: String = "United States",
    val born: String = "",
    val genre: String = "Pop",
    val biography: String = "",
    val topSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList()
)

data class Playlist(
    val id: String,
    val title: String,
    val curator: String = "Apple Music Pop",
    val description: String = "",
    val lastUpdated: String = "Updated recently",
    val songs: List<Song> = emptyList(),
    val featuredArtists: List<Artist> = emptyList()
)
```

And synthesize these collections on-device directly from existing scanned `List<Song>`:
- Group by `song.album` -> `Album`
- Group by `song.artist` -> `Artist`
- Seed featured playlists ("Mood for You", "Daily Top 100", "Hits in Spatial Audio") dynamically.

---

### 2.3 Missing Item 3: Hardware-Adaptive Liquid Glass Strategy (Mali GPU Safety)

#### Current Problem:
`AGENTS.md` strictly enforces automotive performance rules for the low-spec ARM64 Cortex-A53 SoC / Mali GPU. Real-time multi-pass shader blurs on every frame can cause UI jank or thermal throttling on vehicle headunits.

#### Resolution Specification:
Implement **Conditional Liquid Glass Rendering**:
1. **On High-Performance Mobile Devices (Qualcomm / Tensor / Desktop CMP)**:
   - Run full Skia / AGSL shader backdrop sampling with `vibrancy()`, `blur(30.dp)`, and `lens()` refraction.
2. **On Automotive Headunits (Cortex-A53 / Mali GPU)**:
   - Automatically fall back to **Flat Specular Glass**: Solid precomputed translucent acrylic (`rgba(18, 20, 24, 0.88)`), specular hairline borders, and flat solid colors without continuous runtime sampling.

```kotlin
@Composable
fun isHighPerformanceGlassSupported(): Boolean {
    val config = LocalConfiguration.current
    val isAutomotiveLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Headunit landscape mode adheres to the flat solid GPU fill-rate rule
    return !isAutomotiveLandscape
}
```

---

### 2.4 Missing Item 4: Modal Presentation Engine for Fullscreen Now Playing

#### Current Problem:
How does tapping the floating mini-player capsule dock smoothly transition into the Fullscreen Now Playing screen without janky navigation recompositions?

#### Resolution Specification:
Use a root-level **Backdrop Scrim Overlay** inside `MobileShell`:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // 1. Root Screen Host (Home, New, Library, Search)
    MobileContentHost(currentRoute = nav.currentRoute)

    // 2. Floating Bottom Mini-Player Capsule (docked 10dp above bottom bar)
    AnimatedVisibility(
        visible = !isNowPlayingExpanded && currentSong != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        MobileGlassMiniPlayer(
            song = currentSong,
            onOpenNowPlaying = { isNowPlayingExpanded = true }
        )
    }

    // 3. Floating Bottom Glass Navigation Bar
    AnimatedVisibility(
        visible = !isNowPlayingExpanded,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        MobileGlassBottomNavBar(
            currentRoute = nav.currentRoute,
            onSelectRoute = { nav.navigateTo(it) }
        )
    }

    // 4. Fullscreen Modal Now Playing Takeover
    AnimatedVisibility(
        visible = isNowPlayingExpanded,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
    ) {
        MobileGlassNowPlaying(
            currentSong = currentSong,
            onClose = { isNowPlayingExpanded = false }
        )
    }
}
```

---

### 2.5 Missing Item 5: Form-Factor Decoupling Guardrail (`AGENTS.md`)

#### Current Constraint:
Rule 6 in `AGENTS.md` mandates a **STRICT LAYOUT FREEZE** on the Automotive Headunit layout (`Configuration.ORIENTATION_LANDSCAPE` 1280×720). Modifying automotive code without explicit permission is forbidden.

#### Resolution Specification:
Decouple the root entry point in `MusicListScreen.kt`:
```kotlin
val config = LocalConfiguration.current
val isCarLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

if (isCarLandscape) {
    // Automotive Headunit: 100% FROZEN LEGACY LAYOUT PRESERVED
    AutomotiveCarMusicScreen(viewModel)
} else {
    // Mobile Portrait: NEW 1:1 APPLE MUSIC LIQUID GLASS ARCHITECTURE
    MobileAppleMusicApp(viewModel)
}
```
This guarantees:
1. **Zero regressions** to the automotive headunit layout.
2. Complete freedom to implement the **1:1 Apple Music mobile redesign** in clean, decoupled files under `com.aashik.music.ui.mobile.*`.

---

## 3. Migration Readiness Checklist

With these 5 missing specifications established, the codebase has everything required to begin coding:

- [x] Pixel-level Figma node measurements documented.
- [x] All 9 mobile screens documented with Current vs. Target gap analysis.
- [x] Comprehensive Liquid Glass requirement matrix created.
- [x] Sealed Navigation Route Controller defined.
- [x] Domain entities (`Album`, `Artist`, `Playlist`) designed.
- [x] Headunit GPU fallback strategy designed.
- [x] Modal takeover animation architecture specified.
- [x] Automotive Layout Freeze guardrail enforced.

**Status**: **100% READY TO BEGIN IMPLEMENTATION.**
