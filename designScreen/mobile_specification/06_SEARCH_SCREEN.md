# Mobile Search & Browse Screen Specification (`Search.png`)

> **File Reference**: `designScreen/mobile/Search.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Glass Search Bar, Zero Red.

---

## 1. Screen Architecture & Overview
The Search screen handles universal media discovery and catalog navigation. It operates in two core modes:
1. **Browse Categories Mode (Default)**: Top search bar, large featured editorial banner ("Spatial Audio"), horizontal pill carousel ("Perfect Playlists for AutoMix"), and a 2-column grid of colored genre & activity cards.
2. **Active Search & Autocomplete Mode (Search Focused)**: Live search bar with clear button, segmented tab filter (`Apple Music` / `Library`), "Recently Searched" history tokens with clear actions, real-time query suggestions (`col...`), and categorized instant results (Songs, Artists, Albums, Playlists).

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Navigation Bar & Search Field
- **Top Bar (`Height: 52pt`)**:
  - Title: `Search` (`34sp Bold`, White, hidden on active search focus).
- **Glass Search Field (`Height: 40pt, Margin: 16pt horizontal`)**:
  - Shape: Rounded capsule `r=12pt` (or `20pt`).
  - Background: `rgba(255, 255, 255, 0.08)`, Border: `1dp rgba(255, 255, 255, 0.10)`.
  - Leading Icon: Magnifying Glass `🔍` (`16pt`, Slate `#64748B`).
  - Text Field: `Search Apple Music and Library` (`15sp Regular`, Slate `#94A3B8`).
  - Trailing Clear Action: `✕` circle button when text is entered.
  - Cancel Button: `Cancel` (`16sp Regular`, Ice-Cyan `#00E5FF`, animated in from right).

### 2.2 Active Search Mode: Segmented Filter Pill
- **Dimensions**: Height `32pt`, Corner Radius `r=16pt`, Padding `2pt`.
- **Segments**: `Apple Music` (All catalog) and `Library` (Local tracks).
- **Active Selection**: Solid Ice-Cyan glass `rgba(0, 229, 255, 0.20)` with Cyan text.

### 2.3 Active Search Mode: "Recently Searched" List
- **Header Row (`Height: 36pt, Padding: 16pt horizontal`)**:
  - Title: `Recently Searched` (`14sp SemiBold`, Slate `#94A3B8`).
  - Clear Action: `Clear` button (`13sp Regular`, Ice-Cyan `#00E5FF`).
- **History Row Item (`Height: 44pt, Padding: 16pt horizontal`)**:
  - History Icon: Clock / Search icon (`16pt`, Slate `#64748B`).
  - Query String: `Billie Eilish`, `6 DE FEBRERO`, `Sabrina Carpenter` (`15sp Medium`, White).
  - Trailing Action: Remove `✕` button (`14pt`, Slate `#64748B`).

### 2.4 Active Search Mode: Instant Results List
- **Autocomplete Suggestions**: Rows prefixed with search icon `🔍` (`coldplay`, `cold heart`, `collaborations project`).
- **Top Result Card**: Hero card featuring the best match (e.g. Artist `Coldplay` or Song `Cold Heart`).
- **Sectioned Results**:
  - Songs: Track rows with thumbnail (`40pt × 40pt`), title, artist, explicit badge, more button.
  - Albums: Square artwork cards with release year.
  - Artists: Circular avatars (`48pt × 48pt`).

### 2.5 Default Mode: Browse Categories & Genre Grid
- **Hero Category Banner**:
  - Card: `361pt × 180pt`, `r=16pt`, "NOW IN SPATIAL AUDIO".
  - Artwork: `1989 (Taylor's Version)` banner.
- **AutoMix Playlists Carousel**:
  - Header: `Perfect Playlists for AutoMix` (`20sp Bold`, White).
  - Cards: `140pt × 140pt` cards ("Pop in Spatial Audio", "Dance in Spatial Audio").
- **2-Column Genre Card Grid**:
  - Columns: 2, Gap: `12pt horizontal, 12pt vertical`.
  - Dimensions: `174pt × 96pt` per card.
  - Corner Radius: `r=12pt`.
  - Visuals: Vibrant background gradients (e.g. Deep Blue, Purple, Amber, Emerald, Obsidian).
  - Title: Bottom-left aligned (`16sp Bold`, White).
  - Sample Categories: `Spatial Audio`, `Hip-Hop`, `Country`, `Pop`, `Set Lists`, `Charts`, `Holiday`, `Apple Music Live`.
