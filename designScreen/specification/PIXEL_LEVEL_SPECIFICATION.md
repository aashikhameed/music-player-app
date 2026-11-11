# Fluid Glass macOS & Tablet Landscape: Pixel-Level Design Specification

> **Source Baseline**: Modern iPad/Tablet Landscape (`1194dp × 834dp` target canvas)  
> **Source Nodes Analyzed**:  
> - `694:4694` (`Album.sidebar.dark - iPad`)  
> - `671:2310` (`Home.dark.horizontal - iPad`)  
> - `677:4499` (`NowPlaying.horizontal - iPad`)  
> - `677:4654` (`NowPlaying.queue.horizontal - iPad`)  
> **Liquid Glass Engine**: [kyant.gitbook.io/backdrop](https://kyant.gitbook.io/backdrop) architecture  
> **Package Location**: `com.aashik.music.desktop.ui.glass.*` (`desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/`)

---

## 1. System Design Principles & Color Tokens

### 1.1 Strict Zero-Red Mandate
Across all screens, buttons, progress tracks, indicators, badges, and icons, red hues are **strictly prohibited**. The palette relies strictly on obsidian dark tones, translucent frosted glass layers, pure white typography, and an ice-cyan primary accent (`#00E5FF`).

### 1.2 Master Color & Glass Tokens
| Token | Hex / RGBA | Role / Application |
| :--- | :--- | :--- |
| `BgObsidian` | `#0D0F12` | Root window backing canvas |
| `SurfaceGlass` | `rgba(255, 255, 255, 0.06)` | Translucent card surface, unselected navigation item |
| `SurfaceGlassHover` | `rgba(255, 255, 255, 0.10)` | Button & row hover states |
| `SurfaceGlassActive` | `rgba(255, 255, 255, 0.16)` | Selected navigation pills, pressed buttons |
| `BorderHairline` | `rgba(255, 255, 255, 0.08)` | 1dp card borders, vertical sidebar border, divider rules |
| `BorderSpecular` | `rgba(255, 255, 255, 0.18)` | 1dp top rim specular highlight on floating glass |
| `TextPrimary` | `#FFFFFF` | 100% white for titles, headers, and active labels |
| `TextSecondary` | `#94A3B8` / `rgba(255, 255, 255, 0.64)` | Subtitles, artist names, inactive tab labels |
| `TextTertiary` | `#64748B` / `rgba(255, 255, 255, 0.40)` | Timestamps, durations, track indices |
| `AccentCyan` | `#00E5FF` | Primary active accent, scrubber progress, active pills |
| `AccentCyanGlow` | `rgba(0, 229, 255, 0.28)` | Ambient glow behind play button and hero artwork |
| `CapsuleDockBg` | `rgba(20, 22, 26, 0.85)` | Floating mini-player capsule background |
| `CapsuleDockBorder`| `rgba(255, 255, 255, 0.15)`| 1dp border enclosing the floating capsule dock |

---

## 2. Backdrop Liquid Glass Architecture (`kyant.gitbook.io/backdrop`)

All liquid glass views are modularized and exported under:  
📂 `com.aashik.music.desktop.ui.glass.*`

### 2.1 The Two-Stage Backdrop Hierarchy
Following the [Backdrop specification](https://kyant.gitbook.io/backdrop):
1. **Background Canvas Provider (`DesktopLiquidContainer`)**:
   - Renders an animated fluid lava-lamp canvas with 3 organic vector spheres (Ice-Cyan `#00E5FF`, Deep Indigo `#1E3A8A`, Midnight `#0F172A`) that smoothly morph and rotate behind the UI using `rememberInfiniteTransition`.
   - Exposes `LocalBackdrop` down the Compose tree via `CompositionLocalProvider`.
2. **Foreground Glass Consumer (`Modifier.liquidGlassEffect`)**:
   - Every button, floating bottom bar, top bar, sidebar, and card calls `Modifier.liquidGlassEffect(shape, tintColor, borderColor)` to render frosted glass surfaces, specular borders, and corner radii.

---

## 3. Pixel-Level Component Breakdown

### 3.1 Left Navigation Sidebar (`DesktopGlassSidebar`)
- **Width**: `280dp` (scaled for desktop; `340dp` tablet iPad baseline).
- **Height**: `fillMaxHeight()`.
- **Background**: Translucent frosted acrylic (`rgba(16, 18, 22, 0.85)`), with `1dp` right border `BorderHairline` (`rgba(255, 255, 255, 0.08)`).
- **Top Header (`Height: 54dp, Padding: 16dp horizontal`)**:
  - `Edit` button: Font `14sp Medium`, Color `TextSecondary`.
  - Window Sidebar toggle: Icon `◫`, Color `TextSecondary`.
- **Search Field (`Width: 248dp, Height: 36dp, r=10dp`)**:
  - Background `rgba(255, 255, 255, 0.06)`, border `1dp rgba(255, 255, 255, 0.08)`.
  - Magnifying icon `🔍` + Placeholder `Search` (Font `13.5sp Regular`, Color `TextTertiary`).
- **Navigation Row Items (`Height: 38dp, Margin vertical: 2dp, r=19dp`)**:
  - **Active State**: Background `rgba(0, 229, 255, 0.15)`, Border `1dp rgba(0, 229, 255, 0.30)`, Icon `AccentCyan`, Text `TextPrimary` (`14sp SemiBold`).
  - **Inactive State**: Background `Transparent`, Icon `TextSecondary`, Text `TextSecondary` (`14sp Regular`).
  - **Hover State**: Background `rgba(255, 255, 255, 0.08)`.
- **Section Headers (`Library` & `Playlists`)**:
  - Height: `44dp`, Font: `12sp Bold`, Tracking: `0.5sp`, Color: `TextTertiary` with expand chevron `⌄`.
- **Bottom User Profile Capsule (`Width: 248dp, Height: 48dp, r=24dp`)**:
  - Anchored at bottom with `14dp` padding.
  - Avatar: `32dp × 32dp` circular pill (`r=16dp`), initial letters `JS`.
  - Labels: `John Smith` (`13sp SemiBold`) and `Subscriber` (`10.5sp Regular`, `TextTertiary`).

---

### 3.2 Top Segmented App Bar (`DesktopGlassTopBar`)
- **Height**: `60dp`, Padding: `Start: 24dp, End: 24dp`.
- **Left Navigation Controls**:
  - Back (`‹`) and Forward (`›`): `32dp × 32dp` glass boxes (`r=8dp`), icons `20sp Bold`.
- **Center Segmented Pill Container**:
  - Height `40dp`, Corner Radius `r=20dp`.
  - Background: `rgba(255, 255, 255, 0.08)`, Border `1dp rgba(255, 255, 255, 0.10)`.
  - Items: `Home`, `New`, `Radio`, `Songs`.
  - Active Segment: Height `32dp`, `r=16dp`, Background `rgba(0, 229, 255, 0.22)`, Text `AccentCyan` (`13sp SemiBold`).
  - Inactive Segment: Text `TextSecondary` (`13sp Regular`).
- **Right Status Group**:
  - App title / status: `Music Player`, Font `12sp Medium`, Color `TextSecondary`.
  - Profile indicator: `32dp × 32dp` circular glass pill (`r=16dp`).

---

### 3.3 Floating Mini-Player Capsule Dock (`DesktopGlassBottomBar`)
*Anchored horizontally centered `24dp` above the bottom window edge.*

- **Geometry**: Width `700dp`, Height `66dp`, Corner Radius `r=33dp` (`r=1000dp` full capsule).
- **Glass Effects**:
  - Shadow: `elevation = 20dp`, shadow color `rgba(0, 0, 0, 0.50)`.
  - Specular Rim: Top border `1dp rgba(255, 255, 255, 0.15)`.
  - Surface Tint: `rgba(20, 22, 26, 0.85)`.
- **Left Transport Cluster (`202dp × 41dp`)**:
  - Shuffle (`🔀`): `15sp`, cyan when active.
  - Previous (`⏮`): `16sp`, pure white.
  - **Play/Pause Button**: `38dp × 38dp` circle (`r=19dp`), pure `AccentCyan` `#00E5FF` fill, solid black play/pause glyph, `12dp` cyan glow.
  - Next (`⏭`): `16sp`, pure white.
  - Repeat (`🔁`): `15sp`, cyan when active.
- **Center Track Metadata & Artwork (`288dp × 42dp`)**:
  - Album Artwork: `42dp × 42dp`, Corner Radius `r=6dp`.
  - Title: Font `13sp Bold`, `TextPrimary`.
  - Artist: Font `11.5sp Regular`, `TextSecondary`.
  - Click Action: Opens Fullscreen Now Playing Takeover.
- **Right Utility Cluster (`160dp × 36dp`)**:
  - Lyrics Toggle (`💬`): `16sp`, highlighted cyan when active.
  - Queue Toggle (`☰`): `17sp`, highlighted cyan when active.

---

### 3.4 Fullscreen Now Playing Takeover (`DesktopGlassNowPlaying`)
*Window-wide takeover (`1194dp × 834dp`) hiding sidebar, top bar, and mini-player dock.*

- **Top Dismiss Handle**: `Rectangle 73` (`80dp × 4dp`, `r=2dp`, `rgba(255, 255, 255, 0.30)`), centered at `top: 14dp`. Clicking dismisses takeover back to standard mode.
- **Two-Column Split Stage (`padding: 48dp horizontal, 20dp vertical`)**:
  - **Left Hero Player Stage (`412dp`)**:
    - Artwork Card: `380dp × 380dp`, `r=16dp`, `32dp` ambient cyan drop shadow.
    - Title: `26sp Bold`, `TextPrimary`.
    - Artist & Album: `15sp Medium`, `TextSecondary`.
    - Quality Badge: `LOSSLESS • ALAC 24-bit/48kHz` (`10sp Bold`, cyan text on dark pill).
    - Scrubber: `6dp` height track (`r=3dp`), elapsed / duration timestamps (`12sp`).
    - Transport Row: Shuffle, Previous, `56dp` cyan Play/Pause button, Next, Repeat.
  - **Right Split Stage (Lyrics & Queue)**:
    - Segmented Switcher (`36dp` height): `❝ Lyrics` and `☰ Playing Next (count)`.
    - **Lyrics Mode**: Time-synced borderless floating lyrics. Active line: `26sp Bold`, pure white; Inactive lines: `20sp Medium`, `rgba(255, 255, 255, 0.35)`.
    - **Queue Mode**: Indexed track rows (`52dp` height, `r=8dp`) with title, artist, and `☰` drag handles.

---

## 4. Exported Components Index (`ui/glass/`)

All liquid glass views are implemented and exported from:  
`desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/`

1. **[`DesktopLiquidContainer.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopLiquidContainer.kt)**: Root fluid lava-lamp canvas container.
2. **[`DesktopGlassButton.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassButton.kt)**: `Modifier.liquidGlassEffect`, `DesktopGlassButton`, `DesktopGlassPlayButton`.
3. **[`DesktopGlassPill.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassPill.kt)**: `DesktopGlassSegmentedPill` for segmented tab switchers.
4. **[`DesktopGlassTopBar.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassTopBar.kt)**: Glass top navigation bar with history navigation and status.
5. **[`DesktopGlassBottomBar.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassBottomBar.kt)**: The `700dp × 66dp` floating capsule mini-player dock.
6. **[`DesktopGlassSidebar.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassSidebar.kt)**: Glass left navigation sidebar with search, pills, headers, and user card.
7. **[`DesktopGlassCard.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassCard.kt)**: Glass container card for content and albums.
8. **[`DesktopGlassNowPlaying.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/glass/DesktopGlassNowPlaying.kt)**: Fullscreen Now Playing takeover overlay.

---

## 5. Desktop Application Integration
[`DesktopMainView.kt`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/desktop/src/main/kotlin/com/aashik/music/desktop/ui/DesktopMainView.kt) directly wires and consumes these components:
- `DesktopLiquidContainer` encapsulates the root window.
- `DesktopGlassSidebar` drives screen switching and category navigation.
- `DesktopGlassTopBar` coordinates history navigation and segmented screens.
- `DesktopGlassBottomBar` floats anchored `24dp` above the bottom edge.
- Clicking the mini-player's center info card opens `DesktopGlassNowPlaying` as an interactive full-window overlay with the top grab-bar dismiss handle.
