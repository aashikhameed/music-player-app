# Mobile Artist Page Specification (`Artist.png`)

> **File Reference**: `designScreen/mobile/Artist.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Full-Bleed Parallax Header, Zero Red.

---

## 1. Screen Architecture & Overview
The Artist Screen provides a complete discography and editorial profile for a music artist. It features a full-bleed parallax hero photo header with dynamic scrim gradient, prominent artist name and action buttons (Share Artist, Favorite Star `★`), a 4-row "Top Songs" section, a horizontal carousel of "Essential Albums", followed by an in-depth editorial "About" biography card displaying origin, birth date, and biography text.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Full-Bleed Parallax Hero Header
- **Container**: Width `100%`, Height `360pt`.
- **Image**: High-resolution portrait photograph of the artist (e.g. Taylor Swift) with smooth parallax scrolling.
- **Gradient Scrim**: Bottom black gradient ramp from `rgba(13, 15, 18, 1.0)` at bottom to `transparent` at `180pt`.
- **Artist Name**: `Taylor Swift` (`36sp Bold`, Titanium White, bottom aligned, `20pt` margin).
- **Navigation Overlay**:
  - Left: Back Chevron `‹` (`24pt`, Glass circle `36pt × 36pt`).
  - Right: Favorite Star `★` button + Share Artist button (`36pt × 36pt` glass circles).

### 2.2 Top Action Button Row
- **Flex Row (`Height: 44pt, Padding: 16pt horizontal, Margin top: 12pt`)**:
  - `Play` button: Solid white capsule (`140pt × 40pt`, `r=20pt`, black play icon `▶ Play`).
  - `Shuffle` button: Translucent glass capsule (`140pt × 40pt`, `r=20pt`, white icon `🔀 Shuffle`).
  - `Favorite` pill: Icon `★` (`40pt × 40pt` circle).

### 2.3 Section: "Top Songs"
- **Header Row (`Height: 40pt, Padding: 16pt horizontal`)**:
  - Title: `Top Songs` (`20sp Bold`, White).
  - Accessory: `›` chevron.
- **Top Songs List (4 Rows)**:
  - Row Height: `56pt` each.
  - Column 1: Index / Popularity bar (`1`, `2`, `3`, `4`, `14sp SemiBold`, Slate `#64748B`, width `24pt`).
  - Column 2: Album Thumbnail (`44pt × 44pt`, `r=6pt`).
  - Column 3: Song Metadata (Column):
    - Track Title: `The Fate Of Ophelia` (`15sp SemiBold`, White).
    - Release Info: `The Life of a Showgirl • 2025` (`12sp Regular`, Slate `#94A3B8`).
  - Column 4: Context Menu `•••` (`20pt`, Slate `#64748B`).

### 2.4 Section: "Essential Albums"
- **Header Row (`Height: 40pt, Padding: 16pt horizontal`)**:
  - Title: `Essential Albums` (`20sp Bold`, White).
  - Accessory: `›` chevron.
- **Horizontal Carousel**:
  - Dimensions: Width `160pt`, Height `220pt`.
  - Artwork Box: `160pt × 160pt` square (`r=12pt`, drop shadow).
  - Album Title: `folklore (deluxe version)` / `1989 (Taylor's Version)` / `Red` (`14sp SemiBold`, White).
  - Year: `2020` / `2023` / `2012` (`12sp Regular`, Slate `#94A3B8`).

### 2.5 Section: "About" Artist Biography Card
- **Container**: Glass Card (`Margin: 16pt horizontal, Padding: 20pt`).
- **Corner Radius**: `r=18pt`.
- **Background**: Translucent liquid glass `rgba(255, 255, 255, 0.05)`, border `1dp rgba(255, 255, 255, 0.08)`.
- **Origin Metadata Grid (`Height: 48pt`)**:
  - Column 1: `From`: `West Reading, PA, United States` (`13sp Medium`, White).
  - Column 2: `Born`: `December 13, 1989` (`13sp Medium`, White).
  - Column 3: `Genre`: `Pop` (`13sp Medium`, Ice-Cyan `#00E5FF`).
- **Biography Body**:
  - Title: `About` (`18sp Bold`, White).
  - Text: `"The country world feigned surprise when, after three albums of Music Row-indebted songcraft, Taylor Swift formally embraced pop on 2012's Red..."` (`14sp Regular`, line-height `20sp`, Slate `#94A3B8`).
  - Read More: `MORE` button (`14sp Bold`, Ice-Cyan `#00E5FF`).
