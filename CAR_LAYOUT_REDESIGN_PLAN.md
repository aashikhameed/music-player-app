# Car / Landscape Liquid Glass Redesign: Implementation Plan

Status: **APPROVED (2026-11-12) and implemented, Phases 0–5.** Decisions D1–D6 went with the recommendations:
- D1: fixed 240 dp sidebar
- D2: tapping the capsule opens Now Playing, with a separate "locate" button
- D3: 320 px current-song artwork
- D4: solid art-tinted Artwork theme
- D5: latency readout moved to Settings → About → System
- D6: no Artists/Albums views

New code lives in `ui/car/glass/`. `layout.md` §2.0 is the new frozen car spec.

Deviations from the plan:
- Now Playing pieces were rebuilt in `CarNowPlaying.kt` rather than extracted from `MobileGlassNowPlaying`, so the mobile file is untouched (no risk to mobile pixels).
- Back now closes the top-most layer first: Settings or Equalizer opened from Now Playing draw above it.
- Car Settings reuse the mobile settings cards with flat surfaces and text scaled 1.2×. Their toggles and buttons keep the small built-in lens.
- Phase 6 done: the old car layout and old portrait (Material-style) screens were deleted:
  - `MusicListScreen`, `AndroidAutoBottomBar`, `LibraryModeHeader`, `TopRightStatusClock`, `BottomBar*`
  - `MediaLibraryContent` and its search, radio, empty-state and card helpers
  - the old `SettingsScreen` and its 10 section cards, `EqualizerDialog`, `SongActionDialog`, `DeleteSongDialog`
  - old portrait pieces: `MobileHeader`, `MobileMiniPlayer`, `MobileBottomNavBar`, `MobileMoreContent` and their helpers
- Later, at the user's request, the portrait HUD was removed too: `DriveModeScreen` and its pieces (`MobileHeroStage`, `MobileDriveHudStage`, `MobilePlayerActionBar`, `DriveHud*`, `HudPlaybackControlButtons`, `HudExpandedLyricsView`, `HudLyricLineDisplay`, `CustomHorizontalSeekBar`) are gone, and portrait Drive Mode requests open the Now Playing modal.
- Kept because the phone UI still calls them: `EqualizerScreen`, `QueueDrawer`, `GenreDetailScreen`.
- Now Playing was reimagined afterwards (user request): stage ↔ split morph with one shared artwork, action chips, an Up Next teaser row, a single Lyrics | Up Next switch (Queue and Up Next merged), icon-only top-bar toggles. The artwork decode went from 320 px to 640 px (~0.8 MB, RGB_565) because the stage artwork is ~620 px on screen.
- Driving side setting added (user request): `DriveSide` LEFT/RIGHT (default RIGHT), stored as `drive_side`, edited in Settings → Headunit (car card with layout previews) and in the phone Headunit card. It mirrors the car sidebar, Settings list, Equalizer header, queue sheet and Now Playing (stage + split + top bar) using a per-container RTL layout direction with each child reset to LTR, so text and icons are unaffected.
- Emulator note: on the android-37.1 preview image, MediaStore can't read metadata from any local file (`setDataSource failed`). The local-library list was therefore verified with online and radio content, which uses the same rows.

Target: 1280 × 720 @ 160 dpi (1 dp = 1 px), Cortex-A53, Mali GPU, 1–2 GB RAM.
Visual references: `designScreen/tablet/{Home,Album,Now Playing}.png` (landscape layouts) plus the mobile
liquid glass system already built in `ui/mobile/glass/`.

---

## 1. Goal

Replace the frozen landscape UI (`MusicListScreen` landscape branch → `LibraryModeHeader`,
`MediaLibraryContent`, `AndroidAutoBottomBar`, `DriveModeScreen`, `QueueDrawer`, `SettingsScreen`,
`EqualizerScreen`) with a landscape version of the new liquid glass design:

- the same themes as mobile (Light / Dark / Artwork, via `MobileTheme`)
- the same type scale, spacing, list style and glass controls as mobile
- tablet-style structure: **sidebar + content pane + floating playback capsule**, and a **split Now Playing**
  (artwork and controls on the left, queue or lyrics on the right)
- glanceable car sizing: touch targets ≥ 56 dp (`layout.md` §1), so the design is not a scaled-down phone UI

Nothing the current car UI can do may be lost. §6 lists every feature and where it moves.

## 2. Hard constraints (from AGENTS.md / skill rules)

| Rule | How the redesign meets it |
|---|---|
| Mali fill rate: no gradients on repeated items/docks | All surfaces use flat `MobileTheme` colours. The legacy `AppGradients.card/capsule/border` usage goes away. |
| Liquid glass lens **only** on the seek slider (§7) | `LiquidSlider` + backdrop is used for the Now Playing seek bar only. The sidebar, capsule, sheets and buttons are solid or semi-translucent, with no `drawBackdrop` blur or lens. |
| No borders, no shadows | `border = null`, `shadow = { null }`. Separation comes from tone (`panel` vs `background`), not strokes. This also removes the many 0.8 dp borders in the current car UI. |
| Shapes/borders as top-level `private val` | New `ui/car/glass/CarShapes.kt`. |
| No `.clip()` before `.background(shape)` | Enforced; `audit_performance.sh` checks it. |
| High-frequency ticks only in leaf composables | Position/progress are read only inside `CarSeekSlider`, `CarTimeLabels`, `CarCapsuleProgress`, and the lyrics view. The shell, sidebar and content never collect them. |
| Thumbnails 64×64 RGB_565, 3 MB cache | Unchanged for list rows. Large Now Playing artwork: see decision D3. |
| APK arm64-only < 4 MB | The new code reuses mobile components. Legacy car files are deleted in Phase 6 to stay within budget. |
| Theme content adapts to backdrop | `rememberAdaptiveLuminance()` is used only where content sits over the slider backdrop. Everywhere else uses palette roles. |

## 3. Target layout (1280 × 720 dp)

### 3.1 Library shell (replaces landscape `MusicListScreen` + `LibraryModeHeader` + `AndroidAutoBottomBar`)

```
┌──────────────┬──────────────────────────────────────────────────────────────┐
│ 9:41 AM  🎛 ⚙ │  Songs                                   [🔍 Search      ]  │ 72dp title row
│──────────────│  1,204 songs                                                │
│ 🔍 Search     │ ┌──────────────────────────────────────────────────────────┐ │
│ ♪  Songs   ● │ │ ▢ Title                         Artist • Album  3:41 ♡ ⋯ │ │ 64dp rows
│ ♡  Favorites │ │ ▢ Title                         …                        │ │
│ 📁 Folders    │ │ …                                                        │ │
│ 📻 Radio      │ │                                                          │▌│ scrollbar
│──────────────│ │                                                          │ │
│ 🚗 Drive HUD  │ │   ╭─────────────────────────────────────────────────╮    │ │
│ 🎚 Equalizer  │ │   │ ⤮ ⏮ ⏯ ⏭  ▢ Title / Artist  1:02  FLAC   ☰  ⌄ │    │ │ 68dp floating
│ ⚙  Settings  │ │   ╰───────────────▔▔▔▔▔▔▔ progress ▔▔▔▔▔▔▔──────────╯    │ │ capsule
└──────────────┴──────────────────────────────────────────────────────────────┘
   240dp panel                     1040dp content (16dp gutters)
```

- **Sidebar (240 dp, `MobileTheme.panel`, solid).**
  - Top row: the clock (from `TopRightStatusClock`, AM/PM) with the Equalizer and Settings shortcuts.
  - Primary destinations, as 56 dp rows with r = 14 selected pills (`accent` @ 16% fill, `accent` glyph and text):
    Search, Songs (`ALL_SONGS`), Favorites (with count), Folders (with count), Radio & Genres (`RADIO`, with count).
  - Bottom group: Drive HUD, Equalizer, Settings.
  - Optional collapse to a 80 dp icon rail (tablet `⊟` toggle): decision D1.
- **Content pane.**
  - Large title row: 34 sp Bold title, a count subtitle in `textSecondary`, and an inline search field (44 dp capsule, `surface`) at the right.
  - Folders drill-down shows a back chevron and the folder name instead.
- **Song rows (1 column) or cards (`gridColumns` 2/3).**
  - 64 dp row, 48 dp r = 8 artwork, title 16 sp SemiBold over `Artist • Album` 13 sp.
  - Trailing items: duration, favourite heart (48 dp target), "⋯" (opens the actions sheet; long-press still works).
  - The now-playing row shows `accent` title text and the `LiveAnimatedEqualizer` glyph.
  - 0.5 dp hairline divider (10% text) starting under the text.
- **Scrollbar.** `ModernVerticalScrollbar` restyled to the right edge as a thin `textTertiary` thumb, kept for > 5 songs.
- **Floating playback capsule** (replaces `AndroidAutoBottomBar`, tablet reference style).
  - Size: 68 dp tall, r = 34, `panel` @ 92% alpha, no blur. It floats 12 dp above the bottom edge, centred in the content pane, and the list gets matching bottom content padding.
  - Left: shuffle, previous, play/pause (56 dp), next.
  - Centre: 44 dp artwork with the slide transition, title / artist, codec badge, and `BottomBarTimeCounter`.
    Tapping it opens Now Playing (decision D2). The legacy action is "scroll list to current song".
  - Right: queue, then "scroll to current" (a locate glyph, so the legacy action is kept).
  - A 3 dp progress track along the capsule's lower inner edge is scrubbable (it keeps `BottomBarSeekBar` drag-to-seek).
  - Hidden when `hideBottomBar` is on, and when no song is loaded.

### 3.2 Now Playing / Drive HUD (replaces landscape `DriveModeScreen` + `DriveHudActionBar`)

```
┌───────────────────────────────────────────────────────────────────────────────┐
│ ‹  9:41 AM  🌙  ⏱ 23:10  SCVC +3dB  🚫video@speed        [Music Video ◉]  ⚙ │ 56dp top bar
│                                                                               │
│   ┌───────────────┐            │  Playing Next            [Clear]           │
│   │               │            │  ▢ Song – Artist                    ↑ ↓ ✕ │
│   │   ARTWORK     │            │  ▢ Song – Artist                    ↑ ↓ ✕ │
│   │   320×320     │            │  …               (or synced LYRICS pane)    │
│   └───────────────┘            │                                             │
│   Title                ♡ ⬇ ⋯   │                                             │
│   Artist • Album               │                                             │
│   ━━━━━━━━━◉──────────  LiquidSlider (lens)                                  │
│   1:02        FLAC        -2:39│                                             │
│   ⤮/🔁  ⏮  ↺10  ⏯(72)  ↻10  ⏭   │                                             │
│                                │     [📻 Radio] [💬 Lyrics] [☰ Queue]        │ 56dp pane toggles
└───────────────────────────────────────────────────────────────────────────────┘
        left column 520dp                    right pane 720dp
```

- **Background:** the `MobileTheme.background` colour. The Artwork theme gives a solid colour tinted from the art (decision D4).
  The night dimmer swaps in the OLED black palette.
- **Left column:**
  - Artwork with the existing horizontal slide transition on track change.
  - Title and artist. Long text uses ellipsis, or the marquee the current HUD uses.
  - Favourite, download (with radial progress ring and the existing tap-to-delete-when-downloaded behaviour), and "⋯" (song actions sheet).
  - Seek: `LiquidSlider` with backdrop: the only lens element.
  - Elapsed / codec badge / remaining.
  - Transport row:
    - mode button: Shuffle → Straight → Repeat One; for radio queues, Straight ↔ Repeat One only
    - previous
    - −10 s
    - play/pause, 72 dp `accent` circle
    - +10 s
    - next
- **Right pane (720 dp)** switches between three states, like the tablet reference:
  1. **Queue.** The `QueuePane` logic (from `MobileGlassNowPlaying`) plus the `QueueDrawer` features: playlist "next up",
     user queue with tap-to-play, move up/down, remove, and Clear.
  2. **Lyrics.** `HudExpandedLyricsView` behaviour (synced, tap a line to seek, loading/empty state), restyled to tablet
     typography: 28 sp active line, dimmed lines in `textTertiary`.
  3. **Up Next card + hero.** The default when neither pane is open. It shows the next-song card (tap plays it) and room for the
     `LiveAnimatedEqualizer`, which keeps the current "Up Next preview" feature.
  - Pane toggles sit in the right pane's bottom strip: Radio (start radio for the current song, with a loading state), Lyrics, Queue.
- **Top bar (56 dp, transparent):**
  - back
  - `DriveHudClock`
  - Night dimmer toggle
  - Sleep-timer badge (when active)
  - SCVC/speed gain badge
  - speed-lock video pill (when locked)
  - Music Video toggle (`setPlayVideoForStreams`)
  - Settings gear
  - The audio latency readout badge moves into Settings → System Info (decision D5).
- **Video mode:**
  - full-bleed `VideoPlayerView` on OLED black
  - the controls overlay (top bar, a compact transport capsule, slider) auto-hides after 4.5 s while playing
  - tapping toggles the overlay
  - speed interlock (`currentSpeedKmh > 1`) forces audio mode
- **Gestures (kept unchanged):** horizontal swipe ≥ 56 dp → next/previous with the feedback badge; double-tap → play/pause.

### 3.3 Secondary screens

- **Queue from library:** the capsule's queue button opens a right-side sheet (480 dp, `panel`, solid, spring slide-in)
  with the same queue component as Now Playing. The left-aligned overlay drawer is retired.
- **Settings:** a master-detail layout.
  - Left (300 dp): section list — Equalizer, Driving & Volume (SCVC), Library & Refetch,
    Appearance (theme mode + grid columns + liquid glass tuning), Playback (auto-resume, Auto HUD, hide bottom
    bar, music video, max video quality, sleep timer, crossfade), Downloads & Storage, Cache, System Info,
    App Update.
  - Right: that section's controls, reusing `MobileSettingsSections` rows, `LiquidToggle`, and
    `LiquidSlider` without backdrop (a plain track, so the "lens only on seekbar" rule holds).
- **Equalizer** (full-screen `EqualizerScreen` + `EqualizerDialog`):
  - Same controls: enable switch, preset picker, 10-band sliders (−12…+12 dB), HPF, virtual surround, time alignment, bass normaliser.
  - Visual changes: flat `panel` cards, pill chips, `accent` state, no borders. The curve canvas stays.
- **Dialogs:**
  - `SongActionDialog` → `LiquidModalSheet`-style centred sheet (car width 560 dp) with Play Next, Add to Queue, Favourite, Start Radio, Download / Redownload / Delete Download, Delete.
  - `DeleteSongDialog` → `LiquidAlertDialog` (already used on mobile).
  - `AppUpdateDialog` and `DownloadLocationDialog` get the same restyle.
- **States:**
  - loading: `SongLoadingBar` centred in the content pane
  - empty songs: rescan / permission button
  - empty favourites and empty folders views
  - search: no local results, online searching spinner, offline
  - radio: category loading and empty states
  - every state is designed in all 3 themes

## 4. Architecture

New package `ui/car/glass/` (legacy files stay in place until Phase 6 for rollback):

| New file | Responsibility | Reuses |
|---|---|---|
| `CarTokens.kt` | dp/sp tokens (sidebar 240, row 64, capsule 68, targets 56) + `CarShapes` | — |
| `CarGlassApp.kt` | Theme provider (same palette animation as `MobileGlassMusicApp`), route state, `BackHandler`, inactivity watcher, dialog host | `MobileTheme`, `AutoHudInactivityWatcher` |
| `CarSidebar.kt` | Destinations, counts, clock, shortcuts, optional rail collapse | `TopRightStatusClock` logic |
| `CarLibraryPane.kt` | Title row, inline search, folder back, list/grid, scrollbar, scroll-to-current | `MediaLibraryContent` data logic, `UnifiedGlobalSearchResults`, `RadioExploreContent`, `MobileSongRow` |
| `CarSongRow.kt` / `CarSongCard.kt` | 64 dp row / grid card | `AlbumArtImage`, `LiveAnimatedEqualizer` |
| `CarPlaybackCapsule.kt` | Floating capsule + leaf progress/time | `BottomBarSeekBar` / `BottomBarTimeCounter` (leaf) |
| `CarNowPlaying.kt` | Split Now Playing, top bar, video mode, gestures | `MobileGlassNowPlaying` (`Scrubber`, `QueuePane`, `LyricsPane`, transport), `VideoPlayerView`, `DriveHudClock` |
| `CarQueueSheet.kt` | Right-side queue sheet | shared queue component |
| `CarSettingsScreen.kt` | Master-detail settings | existing `*Section.kt` view-model wiring |
| `CarEqualizerScreen.kt` | Restyled EQ | `EqualizerCurveCanvas`, view-model calls |

Rules for sharing code with mobile:
- Extract the private `QueuePane`, `LyricsPane`, `Scrubber` and transport pieces of `MobileGlassNowPlaying` into
  internal shared composables that take size parameters. Mobile must look **pixel-identical** afterwards. This is
  verified with before/after screenshots.
- Do not change shared design-system components in a way that alters mobile.

Wiring:
- `MusicApp.kt`: `if (isCarLandscape) CarGlassApp(viewModel) else MobileGlassMusicApp(viewModel)`.
- Landscape now honours `themeMode` (Light / Dark / Artwork). It currently follows only `isDarkTheme`.
- No view-model API changes are expected. All actions listed in §6 already exist on `MusicViewModel`.

## 5. Phases

Each phase ends with the build passing, an emulator look at 1280×720/160 dpi in all 3 themes, and `verify_release.sh`.

0. **Foundations:**
   - tokens and shapes
   - landscape theme plumbing
   - a 1280×720 mdpi AVD profile for screenshots
   - capture "before" screenshots of every legacy car screen and mobile Now Playing (to check mobile stays unchanged later)
1. **Shell:**
   - `CarGlassApp` with the sidebar, empty content pane and capsule, behind the `MusicApp` switch
   - Back-handler order, inactivity watcher, interaction channel, service/permission flows untouched
2. **Library content:**
   - Songs, Favourites, Folders (plus drill-down), Radio & Genres (plus drill-down), Search (local, online, stations)
   - grid columns, scrollbar, initial/explicit scroll-to-current, empty/loading/permission states
   - long-press and "⋯" actions
3. **Now Playing:**
   - split layout, LiquidSlider seek, transport, mode button, ±10 s, fav / download / radio / lyrics / queue
   - top bar badges, night dimmer, sleep badge, SCVC, speed lock, video mode with auto-hide, gestures
   - Auto HUD entry
4. **Queue sheet, dialogs, Equalizer, Settings master-detail.**
5. **Polish:**
   - spring motion (capsule press scale, pane cross-fade 300 ms, sheet slide `dampingRatio 0.85`)
   - long-title passes, alignment measurements (concentric insets, shared centre lines)
   - frame-time check with `adb shell dumpsys gfxinfo` on list fling and Now Playing
6. **Cleanup:**
   - delete the legacy landscape files no longer referenced, and the dead portrait branch inside `MusicListScreen`
   - update `layout.md` with the new frozen spec, and `AGENTS.md` §1 component names (`BottomBarTimeCounter` etc.)
   - re-verify APK size

## 6. Feature parity checklist (nothing may be dropped)

| # | Current car feature (source) | New home |
|---|---|---|
| 1 | Tabs: All Songs / Folders / Favorites / Radio with counts (`LibraryModeHeader`) | Sidebar destinations with counts |
| 2 | Inline search toggle + clear; query cleared on close | Content title-row search field + sidebar Search item |
| 3 | Unified search: local songs, online songs (seed-only playlist → radio, v0.4.2), stations with fav/start-radio | `CarLibraryPane` search state |
| 4 | Folder list → open folder → back (header back + system Back) | Content pane drill-down with back chevron |
| 5 | Radio explore: genres, favourite genres, stations (play/fav/start radio), shuffle-radio button, category drill-down + back | Radio & Genres destination |
| 6 | Song tap plays with current list as playlist | Rows/cards |
| 7 | Favourite toggle on each song | Row heart |
| 8 | Long-press → actions (play next, add to queue, favourite, start radio, download, redownload, delete download, delete with confirm) | Long-press + "⋯" → glass sheet, `LiquidAlertDialog` delete |
| 9 | Grid columns 2/3 setting | Content grid; setting in Appearance |
| 10 | Vertical scrollbar (> 5 songs) | Restyled scrollbar |
| 11 | Initial scroll to playing song; `scrollToIndex` / `triggerScrollToCurrentSong` | Same effects + capsule "locate" button |
| 12 | Loading bar, empty songs (rescan / request permission), empty favourites, empty folders | Content states |
| 13 | Bottom dock: art slide transition, title/artist, codec badge, time counter, scrubbable seekbar, shuffle, prev, play/pause, next, queue, collapse | Floating capsule |
| 14 | `hideBottomBar` preference | Capsule hidden |
| 15 | Drive HUD entry (car icon) + Auto HUD after inactivity (`AutoHudInactivityWatcher`) | Sidebar Drive HUD item + same watcher |
| 16 | Equalizer entry (status clock) + `EqualizerDialog` | Sidebar/clock shortcut, restyled screen & dialog |
| 17 | Settings entry (status clock, HUD gear) | Sidebar + Now Playing gear |
| 18 | Clock with AM/PM | Sidebar header + HUD top bar |
| 19 | HUD: back, clock, night dimmer, sleep badge, SCVC badge, speed-lock pill, Music Video toggle, latency badge | Now Playing top bar (latency → D5) |
| 20 | HUD: artwork slide transition, title/artist/album, lyrics expand/collapse | Left column + Lyrics pane |
| 21 | Synced lyrics, tap line to seek, loading state | Lyrics pane |
| 22 | HUD action bar: queue, favourite, download with progress ring / delete when downloaded, start radio for current song (loading state), up-next card (tap plays next) | Left column actions + right-pane toggles + Up Next state |
| 23 | Seek bar with drag, times | LiquidSlider + labels |
| 24 | Transport: mode/shuffle, prev, −10 s, play/pause, +10 s, next; stream-aware | Transport row |
| 25 | Swipe next/prev with feedback badge; double-tap play/pause | Same gestures on Now Playing |
| 26 | Video mode: full-screen video, auto-hide controls 4.5 s, tap toggle, scrim, speed interlock | Video mode |
| 27 | Queue drawer: next-from-playlist, user queue, play from queue, move up/down, remove, clear | Queue pane + right sheet |
| 28 | Back priority: queue → EQ → HUD → settings → folder → radio category → radio tab → songs | `CarGlassApp` `BackHandler` (same order) |
| 29 | Settings sections: EQ nav, SCVC, Refetch, Theme/grid, Liquid glass tuning, Playback prefs (auto-resume, Auto HUD, hide bar, music video, max video quality, sleep timer, crossfade), Download storage + location dialog, Cache, System info, App update | Settings master-detail |
| 30 | EQ: enable, presets, 10 bands, HPF, surround, time alignment, bass normaliser, curve | Car EQ screen |
| 31 | App update dialog, notification permission, service binding, song sync (`MusicApp`) | Unchanged |
| 32 | Pointer-interaction channel resetting the inactivity timer | Root `pointerInput` in `CarGlassApp` |

## 7. Verification

- AVD: 1280×720, 160 dpi, landscape. Screenshots taken with `adb shell screencap -p` + `adb pull`.
- Each screen is checked in Light, Dark and Artwork; at rest, scrolled, pressed and dragging; with long titles; in video mode; with night dimmer on.
- Alignment is measured in 2× crops: capsule insets, sidebar row centres, transport centre line.
- The mobile Now Playing before/after diff must show no change.
- Walk through the parity checklist (§6) item by item on the emulator.
- Run `bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh`: audit, unit tests, R8 release build, arm64-only APK < 4 MB.

## 8. Open decisions (need user answers before Phase 1)

- **D1 Sidebar collapse.** Fixed 240 dp sidebar (recommended for glanceability), or collapsible to an 80 dp icon rail like the tablet reference?
- **D2 Capsule tap.** Open Now Playing on tap (recommended, as mobile and tablet do), with "scroll to current" moved to its own button? Or keep the legacy tap = scroll?
- **D3 Large artwork.** A 64 px cached thumbnail looks soft at 320 dp. Options:
  - (a) decode only the current song's art at 320 px RGB_565 (~200 KB, not in the 3 MB LRU), recommended
  - (b) keep the art at ~160 dp, as today
- **D4 Artwork theme background.** A solid art-tinted colour (recommended; Mali-safe and AGENTS-compliant), or a blurred-artwork backdrop like the tablet reference (expensive full-screen blur)?
- **D5 Latency readout.** The badge shows a hard-coded "48kHz • 96KB • 5.3ms". Options: move it to Settings → System Info (recommended), keep it in the HUD, or drop it.
- **D6 Extra library views.** Add Artists / Albums browsing (already built for mobile) to the car sidebar? This is new car functionality, not parity, so it is off by default.
