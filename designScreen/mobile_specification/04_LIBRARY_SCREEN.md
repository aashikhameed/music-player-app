# Mobile Library Screen Specification (`Library.png`)

> **File Reference**: `designScreen/mobile/Library.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Liquid Glass Lists, Zero Red.

---

## 1. Screen Architecture & Overview
The Library Screen is the personal media organizer. It provides quick top-level category navigation (Playlists, Artists, Tracks, Albums, Music Videos, Genres), an expandable action to "Add Genres" or edit order, followed by the "Recently Added" media grid with album art, artist, and release year. It also houses dedicated sub-views for Playlists, Artists, and Track tables.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Navigation Top Bar
- **Height**: `52pt` (below status bar `44pt`).
- **Left**: Large Title `Library` (`34sp Bold`, White).
- **Right**: `Edit` action button (`16sp Medium`, Ice-Cyan `#00E5FF`).

### 2.2 Category Navigation List
- **Container**: Column with full width, horizontal row dividers.
- **Row Item (`Height: 50pt, Padding: 16pt horizontal`)**:
  - Left Icon: Vector icon (`22pt × 22pt`, Tint: Ice-Cyan `#00E5FF`).
  - Label: `17sp Regular`, Titanium White `#FFFFFF`.
  - Right Accessory: Disclosure chevron `›` (`14pt`, Slate `#64748B`).
  - Divider: Inset hairline `0.8dp rgba(255, 255, 255, 0.08)` starting at `52pt`.
- **Category Rows**:
  1. `Playlists` (Icon: Playlist lines `📑`)
  2. `Artists` (Icon: Microphone / Artist silhouette `👤`)
  3. `Tracks` (Icon: Musical note `🎵`)
  4. `Albums` (Icon: Vinyl disc `💿`)
  5. `Music Videos` (Icon: Video camera `📹`)
  6. `Genres` (Icon: Sound waves `🎹`)

### 2.3 Section: "Recently Added" Grid
- **Header Row (`Height: 44pt, Padding: 16pt horizontal`)**:
  - Title: `Recently Added` (`22sp Bold`, White).
- **2-Column Adaptive Grid**:
  - Columns: 2, Spacing: `14pt horizontal, 16pt vertical`.
  - Card Dimensions: `168pt × 220pt`.
  - Artwork Box: `168pt × 168pt` square, `r=10pt`, drop shadow `elevation = 8pt`.
  - Title: `My 21st Century Blues` (`14sp SemiBold`, White, max 1 line, ellipsis).
  - Subtitle: `RAYE` (`12sp Regular`, Slate `#94A3B8`).

### 2.4 Sub-Views within Library

#### A. Playlists Sub-View (`< Library`)
- **Top Actions**:
  - `New Playlist` button (`Height: 44pt`, Glass pill, `+` icon, White text).
  - Sort Bar: Sort by `Title`, `Inclusion date`, `Artist`, `Year`.
- **Special Row**: `Favorite Songs` (Heart tile `48pt × 48pt`, gradient dark purple/indigo).
- **Playlist Rows**: Thumbnail `48pt × 48pt` (`r=6pt`), title (`15sp SemiBold`), track count and update date (`12sp Regular`).

#### B. Artists Sub-View (`< Library`)
- **Alphabetical Index List**:
  - Row Height: `60pt`.
  - Circular Avatar: `48pt × 48pt` circle (`r=24pt`), border `1dp rgba(255, 255, 255, 0.10)`.
  - Artist Name: `16sp Medium`, Titanium White.
  - Right Chevron: `›`.

#### C. Tracks Sub-View (`< Library`)
- **Top Action Bar**:
  - `Play` button (`fillMaxWidth() / 2 - 20pt`, Solid White, black text).
  - `Shuffle` button (`fillMaxWidth() / 2 - 20pt`, Translucent glass).
- **Track Row (`Height: 52pt`)**:
  - Title: `15sp Medium`, White.
  - Artist / Album: `12sp Regular`, Slate `#94A3B8`.
  - More button: `•••` options.
