# Apple Music Mobile Redesign: Complete Master Specification & Screen Index

> **Source Location**: `designScreen/mobile/*.png`  
> **Target Framework**: Jetpack Compose & Compose Multiplatform (CMP)  
> **Theme Foundation**: Obsidian Dark Canvas (`#0D0F12`), Liquid Glass Overlays (`kyant.gitbook.io/backdrop`), Titanium White `#FFFFFF`, Ice-Cyan `#00E5FF`, **Strictly ZERO Red**.

---

## 1. Master Screen Document Index

Every screen screenshot from `designScreen/mobile/` has been thoroughly audited, measured, and documented into its own dedicated specification file inside `designScreen/mobile_specification/`:

| No. | Specification Document | Source Screenshot | Primary Components & Scope |
| :---: | :--- | :--- | :--- |
| **01** | [`01_HOME_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/01_HOME_SCREEN.md) | `Home.png` | Personalized discovery hub, Mood Hero card, Top Picks carousel, Recently Played 2-row grid, floating mini-player capsule dock, and 5-tab floating glass bottom bar. |
| **02** | [`02_NOW_PLAYING_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/02_NOW_PLAYING_SCREEN.md) | `Now Playing.png` | Fullscreen modal takeover, dismiss handle `Rectangle 73`, `335pt × 335pt` hero artwork, timeline scrubber, `64pt` cyan play button, AirPlay device selector drawer, and lyrics/queue sheets. |
| **03** | [`03_ALBUM_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/03_ALBUM_SCREEN.md) | `Album.png` | Parallax hero artwork (`240pt × 240pt`), Dolby Atmos / Lossless badges, editorial review box with `MORE` trigger, Play & Shuffle capsules, and indexed tracklist. |
| **04** | [`04_LIBRARY_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/04_LIBRARY_SCREEN.md) | `Library.png` | Category rows (Playlists, Artists, Tracks, Albums, Videos, Genres), Recently Added 2-column grid, and sub-views for Playlists, Artists, and Track tables. |
| **05** | [`05_NEW_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/05_NEW_SCREEN.md) | `New.png` | Exclusive editorial banner (`361pt × 240pt`), Daily Top 100 carousels (Global, USA), dense 4-row Latest Songs column, City Charts, and New Releases grid. |
| **06** | [`06_SEARCH_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/06_SEARCH_SCREEN.md) | `Search.png` | Glass search field with cancel trigger, Apple Music / Library segmented pill, Recently Searched history tokens, real-time autocomplete suggestions, and 2-column genre grid. |
| **07** | [`07_ARTIST_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/07_ARTIST_SCREEN.md) | `Artist.png` | Full-bleed parallax portrait photo header (`360pt`), Share & Favorite actions, 4-row Top Songs list, Essential Albums carousel, and "About" artist origin/biography card. |
| **08** | [`08_PLAYLIST_SCREEN.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/08_PLAYLIST_SCREEN.md) | `Playlist.png` | Curated mosaic artwork (`240pt × 240pt`), curator attribution, editorial description, Play & Shuffle buttons, custom tracklist, and Featured Artists circular avatar carousel. |
| **09** | [`09_CONTROL_CENTER_WIDGET.md`](file:///Users/aashikhameed/Documents/aashik/projects/music-player-app/designScreen/mobile_specification/09_CONTROL_CENTER_WIDGET.md) | `Control Center.png` | System-level lock screen and notification media controls: Large `345pt × 180pt` glass media card, 2×2 compact widget, timeline scrubber, and AirPlay quick-switcher flyout. |

---

## 2. Global Shared UI Components & Patterns

### 2.1 The Floating Glass Bottom Bar
- **Dimensions**: Width `fillMaxWidth() - 32pt` (e.g. `361pt`), Height `54pt`, Bottom margin `16pt`.
- **Corner Radius**: `r=27pt` (Pill shape).
- **Glass Spec**: Frosted acrylic `rgba(18, 20, 24, 0.88)` with top specular hairline border `1dp rgba(255, 255, 255, 0.15)`.
- **5 Nav Tabs**:
  1. `Home` (`🏠`)
  2. `New` (`✨`)
  3. `Radio` (`📻`)
  4. `Library` (`📑`)
  5. `Search` (`🔍`)

### 2.2 The Floating Mini-Player Dock
- **Dimensions**: Width `fillMaxWidth() - 32pt`, Height `56pt`, Bottom margin `10pt` directly above the tab bar.
- **Corner Radius**: `r=28pt`.
- **Glass Spec**: Backdrop blur `30pt`, surface `rgba(20, 22, 26, 0.85)`, border `1dp rgba(255, 255, 255, 0.15)`.
- **Elements**: `40pt × 40pt` artwork thumbnail (`r=6pt`), song title, artist, play/pause circle (`32pt`, Cyan `#00E5FF`), and next track (`⏭`).

### 2.3 Strict Zero-Red Design Language
- Under no circumstances may red be used for buttons, scrubbers, hearts, or badges.
- All active states, selected highlights, and primary actions are rendered in **Ice-Cyan (`#00E5FF`)** or **Pure Titanium White (`#FFFFFF`)** over **Obsidian Dark Canvas (`#0D0F12`)**.
