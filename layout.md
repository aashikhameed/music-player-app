# Layout Specification & UI Freeze Document

> **STRICT LAYOUT FREEZE ACTIVE**
> **MANDATE:** For any prompt or task, the AI agent **MUST REQUEST EXPLICIT USER PERMISSION** before making any changes, refactoring, or adjustments to any layout, styling, dimensions, or UI structure documented herein.

---

## 1. Overview & Target Display Profiles

This document specifies the exact, frozen user interface architecture and screen layouts of the Music Player application across all supported target form factors:

| Form Factor | Orientation | Resolution / Density | Touch Target & Navigation Standard |
| :--- | :--- | :--- | :--- |
| **Car (Automotive Headunit)** | Landscape (`Configuration.ORIENTATION_LANDSCAPE`) | 1280 × 720 px @ 160 dpi (mdpi) | Minimum 56dp for glanceable automotive touch targets |
| **Mobile Device** | Portrait (`Configuration.ORIENTATION_PORTRAIT`) | Standard Android Phones (360–412dp width) | Minimum 48dp touch targets |
| **Android TV / Google TV** | Landscape (Leanback Launcher) | 1080p / 4K 16:9 Display | Full 10-foot D-Pad remote navigation & high-contrast focus rings |

---

## 1.5 Material 3 Component & Design Token Specifications (m3.material.io/components)

All UI elements strictly adhere to official **Material Design 3 (M3)** component design tokens and structural specifications while preserving the zero-GC-churn and flat solid fill-rate constraints for low-spec automotive headunits:

### A. Material 3 Shape System
- **Extra Small (4dp)**: Badges (`LiveBadgeShape`, `MiniCodecBadgeShape`, `PresetBadgeShape`).
- **Small (8dp)**: Chips & FilterChips (`GenreChipShape`, `EqChipShape`, `ChipPillShape`), Action buttons (`ActionButtonShape`, `ActionBtnShape`), Tooltips, Album art thumbnails (`SongArtShape`, `ActionArtThumbnailShape`, `ArtPlaceholderShape`).
- **Medium (12dp)**: Cards (`SongCardShape`, `FolderCardShape`, `EqCardShape`, `CardSectionShape`, `MoreCardShape`), Header capsules, Minimized artwork.
- **Large (16dp)**: Large Cards & Docks (`RadioCardShape`, `MoodCardShape`, `PortraitSongCardShape`, `LandscapeFullSongShape`, `PortraitDockShape`, `BottomMusicCardShape`, `EqContainerShape`, `SettingsContainerShape`, `DrawerShapeLandscape`).
- **Extra Large (28dp)**: Dialogs (`DialogContainerShape`), Navigation Bars (`MobileNavBarShape`), Modal Bottom Sheets (`DrawerShapePortrait`).
- **Full (CircleShape)**: Navigation bar active indicator pills (64×32dp), Circular transport buttons, FABs, Avatars.

### B. Material 3 Color Role Tokens (m3.material.io/styles/color)
- **Primary / OnPrimary**: Luminous Cyber Cyan `#00E5FF` (Dark) / Cobalt Azure `#0066FF` (Light). Used for primary CTAs, active equalizers, progress sliders.
- **PrimaryContainer / OnPrimaryContainer**: `#004E5B` / `#B5F4FF` (Dark), `#D6E4FF` / `#001B3F` (Light). Used for active cards and focused pills.
- **Secondary / OnSecondary**: Vibrant Amber `#FFFFB300` (Dark) / Daylight Amber `#D97706` (Light). Used for folder icons, secondary tags.
- **Tertiary / OnTertiary**: Status Emerald `#00E676` (Dark) / `#059669` (Light). Used for download statuses, live indicators.
- **Surface / SurfaceContainer (Lowest to Highest)**: `#090C12` base up to `#1B2230` container highest (Dark) / `#FFFFFF` to `#CBD5E1` (Light).
- **Outline / OutlineVariant**: Structural separation borders `#252F42` / `#1C2433` (Dark) and `#CBD5E1` / `#E2E8F0` (Light).

### C. Material 3 Typography Scale (m3.material.io/styles/typography)
Full 15-tier type hierarchy powered by `Inter` (`AppFontFamily`):
- `displayLarge` (57sp), `displayMedium` (45sp), `displaySmall` (36sp)
- `headlineLarge` (32sp), `headlineMedium` (28sp), `headlineSmall` (24sp) — Screen titles, Dialog titles
- `titleLarge` (22sp), `titleMedium` (16sp), `titleSmall` (14sp) — Track titles, Card titles, Folder names
- `bodyLarge` (16sp), `bodyMedium` (14sp), `bodySmall` (12sp) — Artist names, Settings descriptions
- `labelLarge` (14sp), `labelMedium` (12sp), `labelSmall` (11sp) — Button text, Chip labels, Durations, Badges

### D. Component Alignments (m3.material.io/components)
- **Buttons (`Button`, `OutlinedButton`, `TextButton`)**: Standard 40dp height, 16/24dp padding, `labelLarge` typography.
- **Icon Buttons (`IconButton`)**: Standard 40×40dp container, 24dp icon glyph, minimum 48dp tactile interaction target.
- **Chips (`FilterChip`)**: Standard 32dp height, 8dp radius (`shapes.small`), 12dp horizontal padding, `labelLarge` text.
- **Cards (`Card`, `OutlinedCard`)**: Standard 12dp corner radius (`shapes.medium`), flat solid fill, 1dp `outlineVariant` border.
- **Navigation Bar (`NavigationBar`)**: Standard 80dp height, `surfaceContainer` container, active 64×32dp `CircleShape` pill indicator.
- **Dialogs (`AlertDialog`)**: Standard 28dp radius (`shapes.extraLarge`), `headlineSmall` title, 24dp structural padding.
- **Sliders (`Slider`)**: 4dp active/inactive track height, 20×20dp circular thumb with tactile hit target.

---

## 2. Core Screen Layouts

### 2.0 Car / Landscape Liquid Glass Layout (`ui/car/glass/`, approved redesign — see `CAR_LAYOUT_REDESIGN_PLAN.md`)

This is the only car (landscape) layout; the earlier Material-style car screens have been removed. Root: `CarGlassApp` (from `MusicApp` when landscape).
Themes: Light / Dark / Artwork via `MobileTheme`; Night Highway Dimmer swaps in `CarNightPalette` while Now Playing is open.
Only the Now Playing seek slider samples a backdrop (`LiquidSlider`); all other car surfaces are flat theme colours, no borders, no shadows.

- **Library shell**: 240 dp solid sidebar (collapsible to an 84 dp icon-only rail via the header toggle, remembered across launches; the destinations list scrolls, footer pinned; `CarSidebar`: clock; Search, Songs, Favorites, Folders, Radio & Genres; Drive HUD, Equalizer, Settings; 56 dp rows, r 14 selected pill) + content pane (`CarLibraryPane`: 96 dp title row, 34 sp title, inline 460×56 dp search field, back chevron for folder/genre drill-down).
- **Song rows** (`CarSongRow`): 64 dp, 48 dp r 8 artwork, 16 sp title over 13 sp `Artist • Album`, duration, heart and "⋯" (48 dp targets), hairline under the text. Grid columns follow the 2/3-column setting.
- **Playback capsule** (`CarPlaybackCapsule`): 68 dp, r 34, `panel` @ 94%, floats 12 dp above the bottom of the content pane. Mode, previous, play/pause (accent), next · artwork + title (tap → Now Playing) + format badge + time · locate current song, queue. 3 dp scrubbable progress line along the bottom. Hidden when "Hide Bottom Player Dock" is on.
- **Now Playing / Drive HUD** (`CarNowPlaying`), two states sharing one spring-animated artwork:
  - **Stage** (default): no top bar (swipe down or Back closes). Artwork fills the body height (≈624 dp square) on the driver's side (the same side it stays on in Split, so opening lyrics only shrinks it); the control column (≈568 dp) is on the far side (see Driving side below): 40 sp title (2 lines), artist, album; action chips Favorite · Download/Offline · Radio · ⋯ (56 dp pills); liquid glass seek slider with elapsed / format / remaining; transport (mode, previous, −10 s, 96 dp play/pause, +10 s, next); Lyrics | Up Next segmented switch (60 dp).
  - **Split** (a switch segment tapped): artwork springs to 368 dp, left column holds title row (favourite, ⋯), slider and compact transport (mode, previous, 72 dp play/pause, next); the right panel (`panel`, r 24) shows synced Lyrics or the Up Next / queue list (reorder, remove, clear) with the segmented switch along its bottom. Tap the artwork, tap the selected segment, or press Back to return to the stage.
  - Video mode: full-bleed video with auto-hiding overlay controls. Gestures: swipe ←/→ next/previous, double-tap play/pause.
- **Driving side** (`DriveSide`, Settings → Headunit → Driving side; default right-hand drive): navigation and primary controls sit on the driver's side, artwork and lists on the passenger side. Right-hand drive = sidebar, Settings list, Equalizer header, Now Playing controls and the queue sheet on the right; left-hand drive mirrors all of them. In Now Playing the top bar keeps close + clock on the driver's edge, and in split the compact artwork + controls column moves to the driver's side while the panel takes the far side. Only placement is mirrored: text, icons and arrows keep their normal direction.
- **Queue sheet** (`CarQueueSheet`): 480 dp sheet from the capsule, sliding in from the driver's side; reorder, remove, clear.
- **Settings** (`CarSettingsScreen`): master-detail — 300 dp section list (Appearance, Playback, Driving & Volume, Headunit, Library & Storage, Equalizer & Audio, About) + section content.
- **Equalizer** (`CarEqualizerScreen`): enable switch, preset chips, single-Canvas 10-band editor with curve, HPF / bass normaliser / soundstage / time-alignment chips.
- **Dialogs**: `CarSongActionSheet` (600 dp, 2-up 64 dp action tiles); delete confirmation `LiquidAlertDialog` capped at 560 dp.

### 2.1 Phone Now Playing (portrait, `MobileGlassNowPlaying`)

There is no separate Drive HUD on the phone. The mini-player opens the Now Playing modal, and anything that used to
request Drive Mode (restoring a playing session, old entry points) is mapped to that same modal in `MobileGlassMusicApp`.

- Grab handle, large artwork (settles back when paused), title/artist with favourite, playback-mode button, radio button and (streams) download.
- Liquid glass scrubber with format badge, previous / play-pause / next.
- Bottom strip morphs the upper area between artwork, synced lyrics and the Playing Next queue; the audio output button opens the system output picker.
- Drag the top area down or press Back to dismiss.

---

### 2.4 Queue (`QueueDrawer` on phone, `CarQueueSheet` on the car)

- Slides in from bottom (Mobile) or right-side sheet (Car).
- Displays active playing queue with drag-to-reorder handles and swipe-to-dismiss.
- Current active playing track highlighted with accent color bar and animated equalizer indicator.

---

### 2.5 10-Band Graphic Equalizer (`EqualizerScreen` on phone, `CarEqualizerScreen` on the car)

- Real-time OpenSL ES native audio engine parametric EQ.
- Automotive-sized vertical sliders for 10 frequency bands (31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz).
- Pre-allocated preset pills (Flat, Bass Boost, Rock, Pop, Classical, Vocal, Heavy Metal).
- Bass Boost & Virtualizer dial knobs.

---

### 2.6 Radio Explorer Screen (`RadioScreen`)

- Live internet radio station browser categorized by Genre, Country, and Language.
- Buffering status indicator and direct-stream OpenSL ES buffer monitoring.

---

### 2.7 Settings Screen (`MobileSettingsScreen` on phone, `CarSettingsScreen` on the car)

- **Audio Engine Diagnostics**: Native OpenSL ES status, buffer latency, sample rate.
- **Auto-Download Setting**: Switch to automatically download songs listened to for at least 1 minute (bypasses already downloaded songs).
- **Video Playback Settings**: Switch for preferring music videos over album art when available.
- **Cache Management**: Display artwork cache size and one-tap cache flush.
- **Theme & Appearance**: High-contrast dark automotive mode options.

---

## 3. Performance & Hardware Constraints (Automotive Headunit)

All layouts must strictly respect the hardware constraints defined in `AGENTS.md`:
1. **Flat Solid Colors**: Never use multi-stop gradients on repeated items.
2. **Precomputed Shapes**: All `RoundedCornerShape` and `BorderStroke` instances must remain top-level constants.
3. **No Redundant Clipping**: Never stack `.clip(shape)` before `.background(..., shape)`.
4. **Leaf-Scoped State**: All fast-updating state flows (`positionMs`, seek progress) must only trigger leaf composables.
5. **No Layout Shift**: Album art containers, Up Next cards, and transport controls must have fixed dimension constraints to prevent layout jump or shifting when metadata or lyrics update.

---

## 4. Layout Freeze Protocol

Any future agent, developer, or automated prompt MUST adhere to the following protocol:
1. **No Layout Edits Without Asking**: Do NOT alter composable layouts, modifier sizes, paddings, arrangements, colors, or structural hierarchies.
2. **Prompting the User**: If a feature or request necessitates a layout modification, the agent must explain the proposed change and wait for user authorization before touching layout files.


> Headunit glass (API 31+): the playback capsule, queue sheet and song-action sheet are frosted (vibrancy + blur over the recorded library list, no lens/border/shadow). Below API 31 (e.g. Android 9) they stay flat solid panels and no backdrop is captured.
