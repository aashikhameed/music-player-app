# Mobile UI Design Enhancements

> [!IMPORTANT]
> All proposals below are **suggestions only**. Per the Layout Freeze Mandate (`layout.md` §4 / `AGENTS.md` §6), every change requires **explicit user permission** before implementation.

---

## Table of Contents

1. [Mobile Header & Search](#1-mobile-header--search)
2. [Bottom Navigation Bar](#2-bottom-navigation-bar)
3. [Song Card (Song List Item)](#3-song-card)
4. [Folder Card (Folder Browser)](#4-folder-card)
5. [Mini Player (BottomControlStrip)](#5-mini-player-bottomcontrolstrip)
6. [Drive HUD / Now Playing (Mobile Portrait)](#6-drive-hud--now-playing-mobile-portrait)
7. [Queue Drawer (Bottom Sheet)](#7-queue-drawer-bottom-sheet)
8. [Equalizer Screen](#8-equalizer-screen)
9. [Radio Screen](#9-radio-screen)
10. [Settings Screen](#10-settings-screen)
11. [AlphabetScrubber (Fast Scroller)](#11-alphabetscrubber)
12. [Theme & Visual System](#12-theme--visual-system)
13. [Global / Cross-Cutting Improvements](#13-global--cross-cutting-improvements)
14. [New Feature Proposals](#14-new-feature-proposals)
15. [Discrepancies: Code vs layout.md](#15-discrepancies-code-vs-layoutmd)

---

## 1. Mobile Header & Search

**Current state:** 44dp header with title text ("Songs" / tab name) and a search icon button (40dp). When searching: full-width TextField with clear button and results count pill.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 1.1 | Header title switches between tab names but provides no other branding or context | Add a **compact app logo or icon** (24dp) at the left edge of the header for brand identity, with the title text adjacent | Low |
| 1.2 | Search activation is a binary toggle — no transition animation | Add a **smooth expand animation** where the search field grows from the icon position to full width, pushing the title out with a shared-axis horizontal transition | Low |
| 1.3 | No search history or suggestion dropdown | Show **recent searches** (last 5 queries) in a dropdown below the search field when focused and empty — stored locally in Room | Low |
| 1.4 | Results count pill is text-only — no way to quickly clear and return | Add a **"Cancel" text button** (accent color) at the right end of the search bar that closes search and restores the header, in addition to the clear icon | Medium |
| 1.5 | Search field has no voice input option | Add a **microphone icon** at the right edge of the search field for voice-to-text search (useful in car/hands-busy scenarios when phone is docked) | Low |

---

## 2. Bottom Navigation Bar

**Current state:** 56dp height, 2 tabs (All Songs with QueueMusic icon, More with MoreHoriz icon). Top corners rounded with `RoundedCornerShape(26.dp)`. Surface variant background with primary tint for active tab.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 2.1 | Only 2 tabs — all secondary features (Folders, Favorites, Explore, Drive Mode, Equalizer, Settings) are buried behind "More" | Consider adding a **third tab** — "Library" — that surfaces Favorites, Folders, and Playlists directly, keeping Settings and Equalizer in "More" | High |
| 2.2 | No badge/indicator on the "All Songs" tab when music is currently playing | Add an **animated equalizer mini-icon** (12dp, `#00E676` green) or a pulsing dot on the active tab when playback is in progress | Medium |
| 2.3 | The 26dp top corner radius is very rounded — while visually distinctive, it consumes some horizontal space at the edges | Reduce to **18dp** top corners for a slightly more modern, less "pill-like" appearance that maximizes usable width | Low |
| 2.4 | No haptic feedback on tab switch | Add **light haptic** (`HapticFeedbackType.LightClick`) on tab selection for tactile confirmation | Low |
| 2.5 | Tab labels ("All Songs", "More") are generic | Consider more descriptive labels: **"Songs"** and **"Menu"** or **"Songs"** and **"Browse"** — shorter and more intuitive | Low |

---

## 3. Song Card

**Current state:** 64dp row height. 52×52dp album art with 8dp rounded corners. Title (15sp SemiBold), Artist (13sp Normal). Duration (11sp) on right. 36dp three-dot menu. Green dot (6dp `#00E676`) for now-playing. Red heart for favorites. Precomputed shapes: `SongCardShape = RoundedCornerShape(12.dp)`, `ArtShape = RoundedCornerShape(8.dp)`. Background uses `AppGradients.card()` + `AppGradients.border()` 0.8dp border.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 3.1 | Three-dot menu at 36dp is below the Material 48dp minimum interactive component size | Increase **touch target to 44dp** (keeping 20dp icon size) using `Modifier.minimumInteractiveComponentSize()` — the icon stays visually the same but the tap area grows | **Critical** |
| 3.2 | Now-playing indicator is a small 6dp green dot — easily missed on a list of many songs | Replace with a **left-edge accent bar** (3dp wide × full card height, `#00E5FF` cyan primary) combined with the green dot, or add a mini **animated equalizer** icon (3-bar animation, 16dp, `#00E676`) replacing the album art overlay | High |
| 3.3 | No swipe gesture actions on song items | Add **swipe actions**: swipe right → add to queue (with queue icon reveal + cyan tint), swipe left → toggle favorite (with heart icon reveal + red tint). Snap back after action with confirmation vibration | Medium |
| 3.4 | Long-press behavior for multi-select has no visible indicator | On long-press, show a **circular checkbox** (24dp, accent color) overlaying the album art with a smooth crossfade, and display a **contextual action bar** at the top with batch actions (Add to Queue, Add to Playlist, Delete, Share) | Medium |
| 3.5 | No audio format indicator visible on song cards | Add a **codec badge pill** (like the mini player already has) next to the duration — e.g., "FLAC" / "MP3" / "AAC" in 9sp Bold, `RoundedCornerShape(4.dp)`, subtle surface background. This already exists in `BottomControlStrip` — reuse the same component | Medium |
| 3.6 | No download status visible on individual song cards | Add a **small icon** (14dp) next to the duration: cloud-download icon for not-downloaded, green checkmark for cached/downloaded | Low |
| 3.7 | Song card background uses `AppGradients.card()` which is a flat solid — fine per AGENTS.md, but the active card (`isActive=true`) styling with `#0E283C` could be more prominent | Add a **subtle left-accent glow** effect using a 2dp-wide solid accent bar at the left edge for the active/selected card, making it easier to identify at a glance | Low |

---

## 4. Folder Card

**Current state:** 76dp height. 54dp folder icon box with amber tones (`#FFB300`). Title (14.5sp SemiBold). Track count pill ("N TRACKS", 10sp Bold). Chevron right icon (24dp). Card shape `RoundedCornerShape(14.dp)`.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 4.1 | No visual preview of folder contents — user must navigate into each folder | Add a **small album art stack** (3 overlapping 24dp thumbnails with 4dp offset) to the right of the folder icon, showing artwork from the first 3 songs in the folder | Low |
| 4.2 | Track count pill only shows count — no duration estimate | Extend the pill to show **"N TRACKS • Xh Ym"** total duration for quick scanning | Low |
| 4.3 | Navigating deep folder hierarchies has no breadcrumb trail | Add a **breadcrumb bar** (horizontally scrollable, 32dp height) above the folder list showing the path: `Music > Artist > Album` — each segment tappable to jump back | Medium |
| 4.4 | No quick-play action — user must navigate into the folder then tap a song | Add a **Play All button** (accent-tinted, 36dp) on the right side of the folder card that plays all songs in that folder | Medium |

---

## 5. Mini Player (BottomControlStrip)

**Current state:** `RoundedCornerShape(20.dp)` top corners, ~72dp total height. 3dp waveform seekbar across top. Row: 40×40dp album art (10dp corners) → Title (13sp) + Artist (11sp) + Codec badge (9sp) → Play/Pause (40dp circle, primary fill) + Next (36dp circle). Background: `AppGradients.dock()` + 0.8dp border.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 5.1 | The 3dp waveform seekbar at the top is not scrub-able — too thin for touch interaction on mobile | Either expand the **touch target** to 16dp height (keeping visual at 3dp) with an invisible hit zone, or add a **larger interactive seekbar** that appears when the user long-presses the mini player | **Critical** |
| 5.2 | Next button at 36dp is below the 48dp Material minimum | Increase **touch target to 44dp** | **Critical** |
| 5.3 | No Previous track button in the mini player — only Play/Pause and Next | Add a **Previous button** (36dp circle, matching Next styling) to the left of Play/Pause. Transport layout: `[Prev] [Play/Pause] [Next]` | High |
| 5.4 | Tapping the album art / title area to open Drive HUD has no visual affordance | Add a subtle **upward chevron** (`Icons.Rounded.KeyboardArrowUp`, 14dp, 40% alpha) above the waveform seekbar, centered, hinting "swipe up" or "tap to expand" | High |
| 5.5 | No elapsed time visible — user can't gauge progress without opening the full player | Add a tiny **time display** (10sp, monospace Inter, secondary text color) below or next to the waveform bar: `1:23 / 4:56` | Medium |
| 5.6 | No gesture to dismiss the mini player when user wants to stop and hide it | Add **swipe-down to dismiss** gesture that stops playback and slides the mini player down with a spring animation; include a confirmation if a playlist is active | Low |
| 5.7 | No shared-element transition between mini player album art and Drive HUD album art | Implement a **shared-element transition** where the 40dp thumbnail morphs into the 130dp Drive HUD art on tap — creates smooth visual continuity | Medium |
| 5.8 | When no song is loaded, the mini player doesn't render — but there's no graceful first-launch state | Show a **"Tap a song to start playing"** prompt (13sp, secondary color) in the mini player area on first launch before any song is selected, then hide/replace once a song is tapped | Low |

---

## 6. Drive HUD / Now Playing (Mobile Portrait)

**Current state:** Vertical stack: Action bar (38dp circular buttons) → 130×130dp album art (16dp corners) → Song info (Title 16sp Bold + Artist 13sp beside album art) → Seekbar + time counters → Transport: Repeat (38dp), Shuffle (38dp), Previous (48dp), Play/Pause (56dp), Next (48dp). Horizontal swipe for prev/next, vertical swipe for queue.

### 6.1 Action Bar

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 6.1.1 | All action bar buttons at 38dp are below the 48dp Material touch target minimum | Increase **touch targets to 44dp** (keeping 20dp icon rendering size) across all action buttons | **Critical** |
| 6.1.2 | Many buttons (Back, Video, Favorite, Download, Radio, Up Next, Settings) create a crowded toolbar | Group into **left cluster** (Back only) and **right cluster** (Favorite + Download visible, others in overflow "⋯" menu) — reduces visual noise on 360dp-wide screens | Medium |
| 6.1.3 | Up Next preview card in the action bar competes with the Up Next card in the content area below | Remove the action bar Up Next preview on mobile portrait — it's redundant since the full Up Next card is visible in the scrollable content. Keep it only for automotive landscape | Medium |

### 6.2 Album Art & Media Stage

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 6.2.1 | Album art at 130dp is relatively small on modern tall phones (6.5"+ with 400dp+ width) | Make album art **responsive**: `min(screenWidth * 0.65, 260.dp)` — gives ~234dp on 360dp phones and ~260dp on 400dp phones, using available space much better | High |
| 6.2.2 | Song info (title + artist) placed beside album art reduces the art's visual impact | Move title + artist to **below the album art**, centered, with title at **18sp Bold** and artist at **14sp** — gives the artwork center stage prominence | Medium |
| 6.2.3 | When no album art is available, the fallback is a generic placeholder | Design a **styled placeholder**: large music note icon (64dp, 20% alpha white) centered on a subtly different surface (`#141A26`), matching the card shape | Medium |
| 6.2.4 | Horizontal swipe for prev/next exists but has no visual hint | Add **peek indicators** — when the user starts swiping, show a subtle shadow/preview of the next or previous song's album art sliding in from the edge | Low |
| 6.2.5 | No album art dominant color extraction | Extract the **dominant color** from album art and apply it as a subtle tinted background (10-15% alpha) behind the art container — creates a mood-matching ambiance per song. Use flat solid tint, NOT a gradient | Low |

### 6.3 Seekbar & Time

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 6.3.1 | Seekbar thumb at 12dp (from `CustomHorizontalSeekBar`) is functional but small for one-handed use | Add a **grow-on-touch** effect: thumb scales from 12dp → 20dp when the user's finger is near/on the seekbar, snapping back on release | Low |
| 6.3.2 | Time counters use monospace Inter but narrow time values (e.g., `0:05`) might cause subtle layout shifts compared to wider values (`59:59`) | Verify all time containers use **fixed width** (e.g., 48dp min) to prevent any reflow. This likely works already but should be confirmed | Low |

### 6.4 Transport Controls

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 6.4.1 | Repeat (38dp) and Shuffle (38dp) are below the 48dp minimum | Increase **touch targets to 44dp** | **Critical** |
| 6.4.2 | Play/Pause (56dp) is visually prominent with primary fill — good. But Prev/Next (48dp) are the same visual weight | Keep Prev/Next as **ghost/outline style** (transparent bg, white icons) while Play/Pause stays **filled** (cyan primary bg, dark icon) — creates clearer visual hierarchy | Medium |
| 6.4.3 | Shuffle/Repeat active state relies only on color change (cyan when active, white when inactive) | Add a **dot indicator** (4dp, `#00E5FF`) below Shuffle/Repeat when active — provides a secondary visual cue beyond color alone (accessibility benefit for colorblind users) | Medium |
| 6.4.4 | No haptic feedback on transport controls | Add **haptic vibration** (`HapticFeedbackType.LightClick`) on Play/Pause, Prev, Next, Shuffle, Repeat taps | Medium |

### 6.5 Lyrics View

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 6.5.1 | When lyrics view is expanded, song title/artist context is hidden | Keep a **compact now-playing bar** (32dp, title 12sp + artist 10sp, single row) pinned at the top of the lyrics view | Medium |
| 6.5.2 | Lyrics toggle is a tap on a pill — behavior not obvious | Replace with a **segmented control**: `[Now Playing | Lyrics]` tabs (36dp height) so the user can clearly switch between views | Low |

---

## 7. Queue Drawer (Bottom Sheet)

**Current state:** Bottom sheet, 85% screen height max, `RoundedCornerShape(topStart=18.dp, topEnd=18.dp)`. Drag handle pill (36×4dp). Header: "Playing Queue" title + song count + close button. Queue items: 58dp height, 42dp album art, title/artist, drag handle, remove button. Current song highlighted with primary accent bar.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 7.1 | Drag handle for reordering — touch area may be too narrow for precise reorder on a list of 50+ songs | Increase drag handle **touch area to 40dp width** and use a more visible icon (`Icons.Rounded.DragIndicator`, `#999999`) | High |
| 7.2 | Remove button — if it's below 44dp touch target, it's hard to hit | Ensure remove button has at least **44dp touch target** | **Critical** |
| 7.3 | No total queue duration visible anywhere | Add a **summary line** below the "Playing Queue" header: `"12 songs • 45 min"` in 11sp secondary text | Medium |
| 7.4 | "Clear queue" in the header has no confirmation safeguard | Add an **undo snackbar** (5-second window with "Undo" action) or a confirmation dialog before clearing the entire queue | High |
| 7.5 | No way to jump to the currently playing song if it's scrolled off screen | Add a **"Scroll to current"** FAB or button (accent color) that appears when the currently playing song is not visible in the viewport | Medium |
| 7.6 | No ability to save the current queue as a playlist | Add a **"Save as Playlist"** text button in the header that prompts for a playlist name and saves the current queue | Medium |
| 7.7 | The bottom sheet has no intermediate "peek" state | Add a **half-expanded peek** state (~40% height) showing the currently playing song and 2-3 upcoming tracks, with full expand on drag-up | Low |

---

## 8. Equalizer Screen

**Current state:** Full-screen with back navigation. **5-band parametric EQ** (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) — NOT 10-band as `layout.md` states. Preset pills (Flat, Bass Boost, Rock, Pop, Classical, Vocal, Heavy Metal). DspCard sections for Bass Boost / Virtualizer. Master enable/disable toggle. All shapes precomputed.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 8.1 | 5-band sliders may be difficult to precisely manipulate on narrow phones (360dp) | Ensure each slider column is at least **56dp wide** with **12dp spacing** to give enough room for thumb dragging | High |
| 8.2 | No visual EQ curve representation — user sees 5 independent sliders but can't visualize the overall frequency response | Add a **mini EQ curve display** (full width × 60dp) above the sliders showing a smooth polyline connecting the 5 band values — gives instant visual feedback of the overall EQ shape | Medium |
| 8.3 | No real-time preview — unclear if changes apply immediately or on "Apply" | Enable **live preview** where slider changes immediately affect the OpenSL ES engine. Rename "Apply" to **"Save"** and add **"Reset"** to revert to last saved state | High |
| 8.4 | No user-defined custom preset save/load | Add a **"Save Custom"** button that lets users name and store their current EQ curve as a personal preset, displayed alongside the built-in pills | Medium |
| 8.5 | Frequency labels may be technical for casual users | Add **plain-language hints** on hover/long-press: 60Hz → "Sub Bass", 230Hz → "Bass", 910Hz → "Mids", 3.6kHz → "Presence", 14kHz → "Brilliance" | Low |
| 8.6 | Bass Boost / Virtualizer sliders are in separate DspCard sections — user may not discover them | Add a subtle **"scroll down for more"** indicator or move Bass Boost and Virtualizer into a **collapsible "Advanced" section** clearly marked | Low |

---

## 9. Radio Screen

**Current state:** Internet radio station browser categorized by Genre, Country, Language. Station items (64dp height, 48×48dp circular logo, name + subtitle, play button). Search bar. Loading/error states.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 9.1 | No favorite/bookmark mechanism for radio stations | Add a **favorite star** (44dp touch target) on each station row and a **"Favorites" filter tab** at the top for quick access to saved stations | High |
| 9.2 | Station items all look identical — no section grouping when browsing by genre | Add **sticky section headers** (e.g., "Rock", "Jazz", "Electronic") that persist at the top during scroll, styled as 28dp rows with category name in 11sp Bold uppercase, accent color | Medium |
| 9.3 | No "Now Playing" indicator on the currently streaming station | Show an **animated equalizer icon** (3-bar animation, 16dp, `#00E676`) replacing the play button on the actively streaming station row | Medium |
| 9.4 | No recently played radio stations | Add a **"Recent" section** at the top showing the last 5 played stations as horizontal chips (compact, 36dp height, circular station logo + name) | Medium |
| 9.5 | Error state shows generic retry — no specific error info | Provide **contextual error messages**: "No internet connection" (with WiFi-off icon), "Station offline" (with broadcast icon), "Stream format unsupported" | Medium |
| 9.6 | No stream quality information visible | Show a **bitrate badge** (e.g., "128K", "320K") in 9sp on each station item to help users choose quality | Low |
| 9.7 | No buffer health indicator during playback | When a station is buffering, show a **buffer progress bar** below the station item with "Buffering 2.1s" text instead of just a spinner | Low |

---

## 10. Settings Screen

**Current state:** Full-screen with 44dp header (Settings + back). Sections: Theme & Appearance (Dark/Light/Auto chips), Layout Config (grid columns, card style), Playback Preferences (auto-resume, auto HUD, hide bottom bar, play videos, max quality, steering mute, sleep timer, crossfade), Download & Storage (download location, auto-download), Cache Management (size display, clear button), Equalizer Navigation Card (active preset, configure button), System Info (hardware diagnostics). All shapes precomputed.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 10.1 | Many settings sections in a single long scroll — discovering features like Sleep Timer or Crossfade requires scrolling past unrelated settings | Group into **collapsible category sections** with expandable headers, or add a **settings search bar** at the top | Medium |
| 10.2 | Sleep Timer options (Off/15m/30m/45m/60m) are chip-based — no custom duration | Add a **custom option** that opens a time picker for arbitrary durations (e.g., 90 min, 2 hours) | Low |
| 10.3 | Crossfade options (Off/1s/2s/3s/5s/8s) — no audio preview of what crossfade sounds like | Add a **"Preview" button** that plays a short demo crossfade between two sample audio snippets so users understand the effect | Low |
| 10.4 | Cache "Clear Cache" button has no success feedback | Show an **undo-capable snackbar**: "Cache cleared: freed 12.4 MB" with the cache size display updating in real time | Medium |
| 10.5 | Download location dialog with custom path input + "Test & Save" — the path input is freeform text which is error-prone | Replace with a **system folder picker** (`Intent.ACTION_OPEN_DOCUMENT_TREE`) that lets users browse and select folders visually | High |
| 10.6 | System Info section shows raw hardware diagnostics — useful for developers but confusing for regular users | Move System Info behind an **"Advanced" toggle** that is collapsed by default, or into a separate "Developer Info" sub-screen | Low |
| 10.7 | Theme toggle (Dark/Light/Auto) — switching themes has no preview | Add **thumbnail previews** (48×32dp screenshots showing the theme) next to each option so users see what they're choosing | Low |
| 10.8 | Equalizer Navigation Card shows active preset — but doesn't show what it sounds like | Add a **mini EQ curve icon** (32×20dp) next to the preset name showing the approximate shape | Low |

---

## 11. AlphabetScrubber

**Current state:** Vertical A-Z strip on right edge, 20dp column width. Each letter 11sp, primary color when active. Touch/drag to fast-scroll.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 11.1 | No bubble/overlay indicator showing the current letter being scrubbed | Add a **floating letter bubble** (40×40dp, primary-colored circle with white letter at 18sp Bold) that appears to the left of the scrubber when the user drags, showing the active letter | High |
| 11.2 | Letters at 11sp may be very small on shorter phones where the strip is compressed | Implement **adaptive density**: hide less-common letters (Q, X, Z) when the strip height can't fit all 26 comfortably, or group them as "Q-Z" | Low |
| 11.3 | No haptic feedback when crossing letter boundaries during drag | Add a **subtle tick vibration** (`HapticFeedbackType.TextHandleMove`) when the scrubber crosses into a new letter | Medium |
| 11.4 | The scrubber is always visible even when the list is short (< 26 items) | **Auto-hide** the scrubber when the list has fewer than ~30 items (no meaningful scroll distance to save) | Low |

---

## 12. Theme & Visual System

**Current state:** Dual themes — Cyber Obsidian (dark: `#090C12` bg, `#00E5FF` cyan primary, `#FFB300` amber secondary) and Ceramic Azure (light: `#EEF2F6` bg, `#0066FF` azure primary). Inter font family (Regular, Medium, SemiBold, Bold). `AppGradients` uses `SolidColor` only — zero GPU gradient overhead. All shapes pre-allocated as constants.

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 12.1 | Secondary text color (`#94A3B8`) on dark background (`#090C12`) — verify WCAG AA contrast compliance for small text (< 14sp) | **Test contrast ratio** — `#94A3B8` on `#090C12` = ~6.4:1 (passes AA at 4.5:1 but close for 10-11sp). Consider lightening to `#A8B8CC` (~7.5:1) for text below 12sp | Medium |
| 12.2 | Disabled text color (`#475569`) on dark background may fail contrast for important disabled labels | Lighten disabled text to **`#5A6A7E`** (~3.5:1 → ~4.0:1) or add strikethrough/opacity cue | Low |
| 12.3 | The cyan primary (`#00E5FF`) and green playing (`#00E676`) indicators could be confused by deuteranopia (green-blindness) users | Differentiate with **shape cues**: playing indicator uses an animated equalizer icon (not just color), active state uses underline/dot indicator alongside color | Medium |
| 12.4 | No dynamic theming — all songs use the same static color scheme | Consider **per-song ambient tint**: extract a dominant color from album art and apply as a very subtle (8-12% alpha) flat tint to the Drive HUD background — creates mood variation without gradients | Low |
| 12.5 | Light theme (Ceramic Azure) — verify all components render correctly in light mode | Conduct a **full light-theme audit**: every screen, dialog, and component should be tested in light mode to ensure no hardcoded dark colors leak through | Medium |

---

## 13. Global / Cross-Cutting Improvements

### 13.1 Touch Target Audit

| # | Component | Current Size | Required Min | Fix Priority |
|---|-----------|-------------|-------------|--------------|
| 13.1.1 | SongCard 3-dot menu | 36dp | 44dp | **Critical** |
| 13.1.2 | BottomControlStrip Next button | 36dp | 44dp | **Critical** |
| 13.1.3 | Drive HUD action bar buttons | 38dp | 44dp | **Critical** |
| 13.1.4 | Drive HUD Shuffle/Repeat | 38dp | 44dp | **Critical** |
| 13.1.5 | Queue item remove button | verify | 44dp | **Critical** |
| 13.1.6 | BottomControlStrip waveform seekbar | 3dp visual, unknown touch | 16dp touch | **Critical** |

> [!CAUTION]
> **6 touch target violations** identified. These are accessibility issues that can affect users with motor impairments and degrade the experience on any phone. Fix before any public release.

### 13.2 Navigation & Transitions

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 13.2.1 | Screen transitions appear instant with no animation | Add **shared-axis transitions** (Material Motion): vertical axis for drill-down (Library → Drive HUD), horizontal for tab switches | Medium |
| 13.2.2 | No Android 14+ predictive back support | Implement **`PredictiveBackHandler`** for Drive HUD, Queue Drawer, and Equalizer screens to show preview animation on back gesture | Medium |
| 13.2.3 | Edge-to-edge content — verify WindowInsets handling | Ensure proper **`Modifier.windowInsetsPadding()`** for status bar, nav bar, and display cutouts on phones with gesture navigation and camera punch-holes | High |

### 13.3 Loading & Empty States

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 13.3.1 | `SongLoadingBar` shows "Loading X / Y songs (Z%)" — functional but not visually engaging | Replace with **shimmer skeleton rows** matching `SongCard` dimensions (64dp rows with placeholder shapes) for perceived speed improvement | Medium |
| 13.3.2 | No empty state illustrations when categories have zero items | Add **illustrated empty states** with contextual CTAs: "No favorites yet — ♥ a song to add it", "No downloads — listen to songs to auto-download", "No folders found" | High |

### 13.4 Feedback & Notifications

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 13.4.1 | No snackbar system for user actions | Implement a global **Snackbar host** for: "Added to queue", "Added to favorites", "Download started", "Playlist created" — with undo support for destructive actions | High |
| 13.4.2 | No visual feedback for network state changes (WiFi lost during radio streaming) | Add a **network status banner** (compact, 28dp, amber warning color) that slides in at the top when connectivity is lost | Medium |

### 13.5 Accessibility

| # | Issue | Suggestion | Priority |
|---|-------|------------|----------|
| 13.5.1 | Verify all `Icon` and `IconButton` composables have meaningful `contentDescription` | Audit and add **screen-reader labels**: "Play song", "Next track", "Toggle favorite", "Open menu", "Open equalizer" etc. for TalkBack support | High |
| 13.5.2 | Color-only state indicators (cyan for active, green for playing) | Add **secondary visual cues** (icon change, dot indicator, underline) alongside color to support colorblind users | Medium |
| 13.5.3 | No `Modifier.semantics` for complex custom components (seekbar, equalizer sliders) | Add proper **semantic roles** (`Role.Slider`, progress values) to custom-drawn components for accessibility services | Medium |

---

## 14. New Feature Proposals

| # | Feature | Description | Priority |
|---|---------|-------------|----------|
| 14.1 | **Playlist Creation Flow** | Bottom sheet with: name input, optional cover art from songs, drag-to-reorder song selection, save/cancel. The Playlists tab exists but creation UX needs to be surfaced | High |
| 14.2 | **Song Detail / Info Sheet** | Half-screen bottom sheet showing: file path, format (MP3/FLAC/AAC), bitrate, sample rate, file size, date modified, embedded lyrics preview — triggered from SongCard 3-dot menu → "Info" | Medium |
| 14.3 | **Home Screen Widget** | 4×1 Glance widget: album art + title + play/pause + next button for quick control without opening the app | Medium |
| 14.4 | **Listening Stats Dashboard** | Local SQLite-tracked stats: most played songs (top 10), total listening time (daily/weekly graph), genre distribution pie chart — accessible from "More" tab | Low |
| 14.5 | **Onboarding Tour** | 3-step overlay on first launch: (1) "Your music library" → songs tab, (2) "Tap to play" → song item, (3) "Swipe up for full player" → mini player | Low |
| 14.6 | **Batch Download Manager** | Screen showing all active/queued/completed downloads with progress bars, pause/resume/cancel per item, and total storage used | Medium |
| 14.7 | **Gesture Customization** | Settings section to configure swipe actions on song cards (e.g., swipe left = delete vs. add to playlist), long-press behavior, and mini player gestures | Low |

---

## 15. Discrepancies: Code vs layout.md

The following inconsistencies were found between `layout.md` and the actual implementation:

| # | layout.md Says | Code Actually Has | Recommendation |
|---|---------------|-------------------|----------------|
| 15.1 | "10-Band Graphic Equalizer" (§2.5) with bands 31Hz–16kHz | **5-band parametric EQ** (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) in both `EqualizerScreen.kt` and `EqualizerDialog.kt` | Update `layout.md` to reflect the actual 5-band implementation, OR expand the EQ to 10 bands if that was the intended design |
| 15.2 | Song row height "68dp" (§2.1) | `SongCard.kt` uses **64dp** row height | Update `layout.md` to 64dp or adjust code to 68dp |
| 15.3 | Bottom bar component "ModernBottomBar" (§2.2) | Mobile uses **`BottomControlStrip`** (mini player); automotive uses `AndroidAutoBottomBar` — no file called `ModernBottomBar` exists | Update `layout.md` to reference actual component names |
| 15.4 | "64 × 64 dp" album art in song rows (§2.1) | SongCard uses **52 × 52dp** with 8dp corners | Update `layout.md` or resize art |
| 15.5 | Color scheme `#18191C` / `#121316` (§2.2) | Actual palette: `#090C12` (bg), `#10141D` (surface), `#171D29` (card) — "Cyber Obsidian" | Update `layout.md` colors to match actual Color.kt |
| 15.6 | "Category Navigation Tabs" with 7 tabs (§2.1) | Mobile uses **2-tab bottom nav** (All Songs / More), with sub-features in More list | Update `layout.md` to describe the actual mobile navigation model |

---

## Priority Legend

| Priority | Meaning |
|----------|---------|
| **Critical** | Accessibility/usability violation — must fix before release |
| **High** | Significant UX improvement with clear user benefit |
| **Medium** | Polish enhancement that improves overall experience |
| **Low** | Future consideration or minor refinement |

---

> [!WARNING]
> All changes are subject to the **Layout Freeze Protocol** (`layout.md` §4, `AGENTS.md` §6). No code modifications to any composable layout, modifier, dimension, padding, color, or structural hierarchy shall be made without explicit user authorization.

> [!NOTE]
> All implementations must respect **AGENTS.md** performance constraints: flat solid colors only (no multi-stop gradients), pre-allocated shapes, no redundant clipping, leaf-scoped state reads, RGB_565 thumbnails, and NEON JPEG compression.
