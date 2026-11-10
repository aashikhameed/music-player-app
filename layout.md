# Layout Specification & UI Freeze Document

> **STRICT LAYOUT FREEZE ACTIVE**
> **MANDATE:** For any prompt or task, the AI agent **MUST REQUEST EXPLICIT USER PERMISSION** before making any changes, refactoring, or adjustments to any layout, styling, dimensions, or UI structure documented herein.

---

## 1. Overview & Target Display Profiles

This document specifies the exact, frozen user interface architecture and screen layouts of the Music Player application across both supported target form factors:

| Form Factor | Orientation | Resolution / Density | Touch Target Standard |
| :--- | :--- | :--- | :--- |
| **Car (Automotive Headunit)** | Landscape (`Configuration.ORIENTATION_LANDSCAPE`) | 1280 × 720 px @ 160 dpi (mdpi) | Minimum 56dp for primary touch targets |
| **Mobile Device** | Portrait (`Configuration.ORIENTATION_PORTRAIT`) | Standard Android Phones (360–412dp width) | Minimum 48dp touch targets |

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

### 2.1 Main Media Library Screen (`MusicListScreen` / `MediaLibraryContent`)

The home screen presents the user's media collection with high-contrast automotive typography and zero-overdraw background hierarchy.

#### Screen Hierarchy:
1. **Top Header & Search Bar**:
   - Left: App branding / Drive HUD mode shortcut icon.
   - Center: Dynamic filter & search field with instantaneous SQLite indexed query.
   - Right: Media Rescan action, Filter chip toggle, and Overflow menu.
2. **Category Navigation Tabs**:
   - Horizontally scrollable or fixed tab strip: **Songs**, **Albums**, **Artists**, **Playlists**, **Folders**, **Radio**, **Downloads**.
   - Active tab indicator with precomputed rounded rectangular indicator (flat solid accent color, no gradients).
3. **Content Display Stage**:
   - **Car (Landscape)**: Dual-pane split view or expansive multi-column grid depending on category. Left pane: Fast-scroll song list. Right pane: Current album/artist details or quick queue preview.
   - **Mobile (Portrait)**: Single-column vertical lazy list with letter fast-scroller.
4. **Song Item Row (`SongItem`)**:
   - Height: 68dp (optimized for tactile automotive interaction).
   - Left: 64 × 64 dp pre-cached thumbnail (`Bitmap.Config.RGB_565`, hardware NEON JPEG compressed disk cache).
   - Center: Track title (single line, bold, ellipsize end) stacked over `Artist • Album` subtitle.
   - Right: Track duration, favorite toggle (38dp circular touch surface), and options trigger.

---

### 2.2 Persistent Bottom Playback Dock (`AndroidAutoBottomBar` / `ModernBottomBar`)

A high-performance, leaf-recomposition playback bar docked to the bottom of all non-HUD screens.

#### Component Breakdown:
- **Root Container**: Flat solid surface (`#18191C` / `#121316`), pre-allocated border stroke, no gradients. Window background set to null to eliminate GPU fill rate overdraw.
- **Top Seeker Progress Line (`BottomBarSeekBar`)**:
  - 3dp height micro-scrubber spanning full bar width.
  - Position ticks strictly read within leaf composable to isolate recompositions from parent tree.
- **Left Stage**: 48 × 48 dp square album art thumbnail with 8dp rounded corner.
- **Center Stage**:
  - Title & Artist rolling marquee / truncated text.
  - Tapping this stage launches the **Full-Screen Drive HUD Mode**.
- **Right Stage (Transport Controls)**:
  - Previous Track (44dp target).
  - Play / Pause button (48dp prominent automotive target).
  - Next Track (44dp target).
  - Favorite Heart button.

---

### 2.3 Full-Screen Drive HUD Mode (`DriveModeScreen`)

The primary in-cabin driver infotainment and mobile music stage designed for zero distraction and glanceable clarity.

#### A. Automotive Landscape Stage (Car 1280 × 720)
- **Unified Action Bar (`DriveHudActionBar`)**:
  - Height: 54dp.
  - Left to Right arrangement:
    1. **Back Button**: 38dp circular surface returning to Media Library.
    2. **Video / Audio Toggle Button**: 38dp circular surface switching between video streaming and audio playback.
    3. **Favorite Heart Button**: 38dp circular surface displaying active/inactive favorite state.
    4. **Radial Download Button**: 38dp circular surface displaying download progress wheel or cached status matching favorite styling.
    5. **Radio Station Button**: 38dp circular surface for quick access to live broadcast streams.
    6. **Up Next Preview Card**: Compact horizontal card displaying next song thumbnail (32dp), title, and artist name.
    7. **Settings Gear Button**: 38dp circular surface opening quick configuration dialog.
- **Main Infotainment Stage (Split View)**:
  - **Left Half (Media Stage)**:
    - *Video Mode*: Embedded HTML5/ExoPlayer video canvas with smooth aspect-ratio preservation.
    - *Audio Mode*: 160 × 160 dp high-contrast Album Art stage with glowing vinyl groove disc, song title, and `$artist • $album` metadata pill.
    - *Synchronized Lyrics*: Tap on lyrics pill smoothly hides title/artist and expands full synchronized lyrics view.
  - **Right Half (Control & Scrubbing Deck)**:
    - Big automotive time counters (`BottomBarTimeCounter`) for elapsed and remaining time.
    - Automotive high-contrast linear seekbar with drag scrubbing.
    - Primary transport deck: Repeat, Shuffle, Prev (52dp), Play/Pause (64dp elevated button), Next (52dp).

#### B. Mobile Portrait Stage (Phone Display)
- **`MobileHeroStage` & `MobilePlayerActionBar`**:
  - Vertically stacked hierarchy:
    1. Top: Top App Bar with back navigation, track title, and options.
    2. Center Stage (`MobileHeroStage`): Responsive high-contrast Album Art viewport with integrated action surfaces:
       - Floating Favorite (Heart) button anchored in bottom-left corner with precomputed shape.
       - Floating Offline Download button anchored in bottom-right corner with live progress ring.
       - Both floating corner actions smoothly fade out when lyrics are expanded or video mode is active.
       - Expanding synchronized lyrics replaces the artwork stage without shifting surrounding layouts.
    3. Action Bar (`MobilePlayerActionBar`):
       - Four balanced, 44dp Material 3 action buttons:
         - **Playback Mode / Shuffle**: When queue is from radio, shuffle is excluded and toggles strictly between Straight (sequential) and Repeat One. For normal playlists, cycles Shuffle -> Straight -> Repeat One.
         - **Playback Queue**: Positioned directly to the right of Shuffle/Mode; opens the full playback queue drawer overlay.
         - **Radio Station**: Positioned to the right of Queue; initiates instant algorithmic radio for the active song.
         - **Synchronized Lyrics**: Positioned on the right; toggles the full lyrics display.
    4. Bottom Transport Deck: Scrubbing seekbar with time counters, and standard automotive transport controls.

---

### 2.4 Queue Drawer (`QueueDrawer`)

- Slides in from bottom (Mobile) or right-side sheet (Car).
- Displays active playing queue with drag-to-reorder handles and swipe-to-dismiss.
- Current active playing track highlighted with accent color bar and animated equalizer indicator.

---

### 2.5 10-Band Graphic Equalizer (`EqualizerScreen` / `EqualizerDialog`)

- Real-time OpenSL ES native audio engine parametric EQ.
- Automotive-sized vertical sliders for 10 frequency bands (31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz).
- Pre-allocated preset pills (Flat, Bass Boost, Rock, Pop, Classical, Vocal, Heavy Metal).
- Bass Boost & Virtualizer dial knobs.

---

### 2.6 Radio Explorer Screen (`RadioScreen`)

- Live internet radio station browser categorized by Genre, Country, and Language.
- Buffering status indicator and direct-stream OpenSL ES buffer monitoring.

---

### 2.7 Settings Screen (`SettingsScreen`)

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
