# Mobile New & Charts Screen Specification (`New.png`)

> **File Reference**: `designScreen/mobile/New.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Dynamic Editorial Banners, Zero Red.

---

## 1. Screen Architecture & Overview
The "New" screen functions as the global discovery and charts hub. It features large featured exclusive editorial banners (e.g. "Best Songs of 2025"), daily top 100 chart cards ("Top 100: Global", "Top 100: USA"), a dense 4-row vertical carousel of "Latest Songs" with inline play triggers, City Charts carousels ("Top 25: New York City", "Top 25: Los Angeles"), and a grid of "New Releases".

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Navigation Top Bar
- **Height**: `52pt` (below status bar `44pt`).
- **Large Title**: `New` (`34sp Bold`, Titanium White `#FFFFFF`).
- **Right Action**: Profile pill / account avatar.

### 2.2 Featured Editorial Banner ("EXCLUSIVE PLAYLIST")
- **Container**: Width `361pt` (horizontal margin `16pt`), Height `240pt`.
- **Corner Radius**: `r=18pt`.
- **Artwork & Gradient Scrim**: Full card image with dark gradient ramp from bottom (`0.9`) to top (`0.0`).
- **Typography & Details**:
  - Eyebrow Badge: `EXCLUSIVE PLAYLIST` (`11sp Bold`, Tracking `1sp`, Ice-Cyan `#00E5FF`).
  - Hero Header: `Best Songs of 2025` (`28sp Bold`, White).
  - Editorial Blurb: `"The songs that brought us from reflection to release in 2025."` (`13.5sp Regular`, Slate `#94A3B8`, max 2 lines).
  - Curator Tag: `Apple Music Pop` (`12sp Medium`, Slate `#64748B`).

### 2.3 Section: "Daily Top 100" Charts
- **Header Row (`Height: 40pt, Padding: 16pt horizontal`)**:
  - Title: `Daily Top 100` (`20sp Bold`, White).
  - Chevron: `›` (`18sp SemiBold`, Slate `#64748B`).
- **Horizontal Scroll Carousel**:
  - Item Dimensions: Width `155pt`, Height `215pt`, Spacing `14pt`.
  - Chart Artwork: `155pt × 155pt` square card (`r=12pt`).
  - Graphic: Large typography overlay `Top 100` in vibrant gradient.
  - Title: `Top 100: Global` / `Top 100: USA` (`14sp SemiBold`, White).
  - Curator: `Apple Music` (`12sp Regular`, Slate `#94A3B8`).

### 2.4 Section: "Latest Songs" (Dense 4-Row Column)
- **Header Row (`Height: 40pt, Padding: 16pt horizontal`)**:
  - Title: `Latest Songs` (`20sp Bold`, White).
  - Chevron: `›`.
- **4-Row Vertical Chunk Layout**:
  - Row Height: `56pt` each (4 stacked items, total height `224pt`).
  - Thumbnail: `44pt × 44pt` square artwork (`r=6pt`).
  - Track Title: `For Good` / `The Fate Of Ophelia` (`14sp SemiBold`, White).
  - Explicit Badge: `[E]` container if explicit.
  - Artist Name: `Cynthia Erivo & Ariana Grande` (`12sp Regular`, Slate `#94A3B8`).
  - Trailing Action: Play circle or More `•••` options.

### 2.5 Section: "City Charts"
- **Header Row (`Height: 40pt, Padding: 16pt horizontal`)**:
  - Title: `City Charts` (`20sp Bold`, White).
  - Chevron: `›`.
- **Horizontal Carousel**:
  - Item Width: `150pt`, Height `210pt`.
  - Banner Artwork: `Top 25` graphic featuring skyline styling.
  - Title: `Top 25: New York City` / `Top 25: Los Angeles`.

### 2.6 Sub-View: "Latest Songs" Dedicated Feed (`< Latest Songs`)
- Full vertical scrolling list of newly released singles and tracks.
- Infinite scroll pagination.
- Filter chips: `All Genres`, `Pop`, `Hip-Hop`, `R&B`, `Electronic`.
