# Mobile Album View Specification (`Album.png`)

> **File Reference**: `designScreen/mobile/Album.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Liquid Glass Surface Elements, Zero Red.

---

## 1. Screen Architecture & Overview
The Album View provides deep listening immersion for a selected record. It features a collapsible parallax hero artwork header, metadata badge row, editorial album review expandable section, action button grid (Play, Shuffle, Download, Favorite, Share), full indexed tracklist with explicit badges and duration, and album copyright footer details.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Navigation Top Bar
- **Height**: `44pt` (below status bar `44pt`).
- **Left**: Back Chevron `‹` (`24pt`, White).
- **Center**: Title appears on scroll collapse (`16sp Bold`, White, alpha animated from `0.0` to `1.0`).
- **Right**: More Options `•••` pill button + Favorite Star `★`.

### 2.2 Hero Album Header
- **Container**: Centered vertical stack (`Padding: 16pt horizontal, 12pt bottom`).
- **Album Artwork**:
  - Dimensions: `240pt × 240pt` square card.
  - Corner radius: `r=14pt`.
  - Ambient Shadow: `elevation = 20pt`, blur `28pt`, `rgba(0,0,0,0.45)`.
  - Border: `1dp rgba(255, 255, 255, 0.12)`.
- **Album Title**: `The Life Of A Showgirl` (`24sp Bold`, Titanium White, center aligned).
- **Artist Name**: `Taylor Swift` (`16sp SemiBold`, Ice-Cyan `#00E5FF`, center aligned).
- **Metadata Badges Row**:
  - `Pop • 2019 • Dolby Atmos • Lossless`.
  - Badges: Pill containers `r=4pt`, background `rgba(255, 255, 255, 0.08)`, font `10sp Bold`, color `Slate #94A3B8`.

### 2.3 Editorial Review Expandable Box
- **Container**: Margin horizontal `16pt`, margin vertical `8pt`.
- **Text**: `"Let's start with that speech. In September 2022, as Taylor Swift accepted Songwriter-Artist of the Decade..."` (`13.5sp Regular`, Slate `#94A3B8`, max 3 lines collapsed).
- **Trailing Action**: `MORE` button (`13.5sp Bold`, Ice-Cyan `#00E5FF`). Tapping expands full text.

### 2.4 Action Button Bar
- **Flex Row (`Height: 46pt, Padding: 16pt horizontal`)**:
  - **Play Button (Primary)**:
    - Width: `160pt`, Height: `44pt`, `r=22pt`.
    - Fill: Pure Titanium White `#FFFFFF`.
    - Content: Solid black play icon `▶` + `Play` (`15sp Bold`, Obsidian `#0D0F12`).
  - **Shuffle Button**:
    - Width: `160pt`, Height: `44pt`, `r=22pt`.
    - Fill: Translucent Glass `rgba(255, 255, 255, 0.10)`.
    - Content: White shuffle icon `🔀` + `Shuffle` (`15sp SemiBold`, White).
  - **Secondary Actions (Icon Pills)**:
    - Download `⬇`, Favorite `♡`, Share `↗`, More `•••` (`36pt × 36pt` circular glass buttons).

### 2.5 Indexed Tracklist Section
- **Track Row (`Height: 56pt, Padding: 16pt horizontal`)**:
  - Hover / Selected Background: `rgba(255, 255, 255, 0.06)` or Cyan glass `rgba(0, 229, 255, 0.10)`.
  - Column 1: Track Index number (`1`, `2`, `3`... `14sp SemiBold`, Slate `#64748B`, width `28pt`).
  - Column 2: Track Details (Column):
    - Title: `The Fate Of Ophelia` (`15sp Medium`, White).
    - Explicit Tag `[E]`: `16pt × 13pt` pill (`r=2pt`, background `rgba(255, 255, 255, 0.20)`, font `9sp Bold`).
    - Subtitle / Featured Artist: `feat. Sabrina Carpenter` (`12sp Regular`, Slate `#94A3B8`).
  - Column 3: Duration (`3:42`, `12sp Regular`, Slate `#64748B`).
  - Column 4: Context Menu `•••` (`20pt`, Slate `#64748B`).
  - Divider: `0.8dp rgba(255, 255, 255, 0.06)` inset `44pt` from left.

### 2.6 Album Footer Information
- **Release Details Box (`Padding: 24pt horizontal, 32pt top/bottom`)**:
  - Date: `August 23, 2019` (`13sp Medium`, Slate `#94A3B8`).
  - Stats: `18 tracks, 1 hour 1 minute` (`13sp Regular`, Slate `#64748B`).
  - Copyright: `© 2019 Taylor Swift` (`11sp Regular`, Slate `#64748B`).
  - Format Badge: `Apple Digital Master` badge logo.
