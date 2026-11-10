# Mobile Playlist Detail Specification (`Playlist.png`)

> **File Reference**: `designScreen/mobile/Playlist.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Curated Editorial Art, Zero Red.

---

## 1. Screen Architecture & Overview
The Playlist Screen presents curated and user-generated collections. It features a prominent square or 16:9 banner collage artwork, playlist creator attribution (`Apple Music Pop`), editorial description with Dolby Atmos / Spatial Audio highlights, Play & Shuffle buttons, a curated tracklist with custom ordering or release tags, and a "Featured Artists" circular avatar carousel at the bottom.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Navigation Top Bar
- **Height**: `44pt` (below status bar `44pt`).
- **Left**: Back Chevron `‹` (`24pt`, White).
- **Center**: Title appears on scroll collapse (`16sp Bold`, White).
- **Right**: More Options `•••` glass circle + Favorite Star `★`.

### 2.2 Hero Playlist Header
- **Container**: Centered vertical stack (`Padding: 16pt horizontal, 16pt bottom`).
- **Playlist Artwork**:
  - Dimensions: `240pt × 240pt` square card (`r=14pt`).
  - Graphic: Multi-artist mosaic or vibrant Apple Music curator graphic (e.g. "Taylor Swift Essentials" or "Hits in Spatial Audio").
  - Drop Shadow: `elevation = 20pt`, blur `28pt`.
- **Playlist Title**: `Taylor Swift Essentials` / `Hits in Spatial Audio` (`24sp Bold`, White, center aligned).
- **Curator & Stats**:
  - Creator: `Apple Music Pop` (`15sp SemiBold`, Ice-Cyan `#00E5FF`).
  - Last Updated: `Updated 2d ago` (`12sp Regular`, Slate `#94A3B8`).
  - Total Duration: `72 songs, 4 hours 32 minutes` (`12sp Regular`, Slate `#64748B`).

### 2.3 Editorial Description Box
- **Container**: Margin horizontal `16pt`, margin vertical `8pt`.
- **Text**: `"Ever wish you could get that feeling of hearing your favorite song for the first time all over again? No problem. Thanks to Spatial Audio with Dolby Atmos..."` (`13.5sp Regular`, line-height `18sp`, Slate `#94A3B8`, max 3 lines collapsed).
- **Trailing Action**: `MORE` button (`13.5sp Bold`, Ice-Cyan `#00E5FF`).

### 2.4 Action Button Bar
- **Flex Row (`Height: 46pt, Padding: 16pt horizontal`)**:
  - `Play` button: Solid white capsule (`160pt × 44pt`, `r=22pt`, black icon `▶ Play`).
  - `Shuffle` button: Translucent glass capsule (`160pt × 44pt`, `r=22pt`, white icon `🔀 Shuffle`).

### 2.5 Playlist Tracklist Section
- **Track Row (`Height: 58pt, Padding: 16pt horizontal`)**:
  - Hover / Selected Background: `rgba(255, 255, 255, 0.05)`.
  - Column 1: Album Artwork Thumbnail (`42pt × 42pt`, `r=6pt`, left aligned).
  - Column 2: Track Information (Column):
    - Track Title: `Shake It Off (Taylor's Version)` (`15sp Medium`, White).
    - Explicit Tag `[E]`: If explicit.
    - Artist: `Taylor Swift` (`12.5sp Regular`, Slate `#94A3B8`).
  - Column 3: Context Options `•••` (`20pt`, Slate `#64748B`).
  - Drag Reorder Handle (in edit mode): `☰` icon (`20pt`, Slate `#64748B`).

### 2.6 Section: "Featured Artists" Carousel
- **Header Row (`Height: 40pt, Padding: 16pt horizontal, Margin top: 24pt`)**:
  - Title: `Featured Artists` (`20sp Bold`, White).
- **Horizontal Circular Avatar Carousel**:
  - Item Dimensions: Width `100pt`, Height `130pt`.
  - Circular Avatar: `80pt × 80pt` circle (`r=40pt`, border `1dp rgba(255, 255, 255, 0.12)`).
  - Artist Name: `Taylor Swift`, `Billie Eilish`, `Sabrina Carpenter` (`13sp SemiBold`, White, center aligned).
