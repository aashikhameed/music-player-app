# Complete Liquid Glass Components Audit & Requirements Matrix

> **Library Engine Reference**: [kyant.gitbook.io/backdrop](https://kyant.gitbook.io/backdrop) (`io.github.kyant0:backdrop`)  
> **Visual Token Foundation**: Obsidian Canvas (`#0D0F12`), Frosted Acrylic Shaders, Specular Hairline Borders, Cyan Glows (`#00E5FF`), **Strictly ZERO Red**.

---

## 1. Master Liquid Glass Matrix by Component Category

This matrix specifies **every UI element across Mobile & Tablet/Desktop** that requires the Liquid Glass shader effect, detailing its target shape, corner radius, backdrop blur, surface tint, and specular border styling:

| Component | Target Screen(s) | Shape & Geometry | Blur Radius | Glass Surface Fill | Specular Border & Highlight | Drop Shadow & Glow |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Floating Bottom Navigation Bar** | Home, New, Library, Search | Rounded Capsule (`361×54pt`, `r=27pt` mobile; `44pt` desktop) | `30pt` / `30dp` | `rgba(18, 20, 24, 0.85)` | `1dp rgba(255, 255, 255, 0.16)` top rim | `elevation = 16pt`, `rgba(0,0,0,0.45)` |
| **Floating Mini-Player Capsule Dock** | Persistent across all standard views | Rounded Capsule (`361×56pt`, `r=28pt` mobile; `700×66dp`, `r=33dp` desktop) | `30pt` / `30dp` | `rgba(20, 22, 26, 0.82)` | `1dp rgba(255, 255, 255, 0.18)` | `elevation = 20pt`, `rgba(0,0,0,0.50)` |
| **Top Segmented Pill Bar** | Home, New, Search, Desktop Shell | Rounded Capsule (`Height: 40pt`, `r=20pt`) | `20pt` / `20dp` | `rgba(255, 255, 255, 0.08)` | `1dp rgba(255, 255, 255, 0.10)` | None |
| **Active Segment Pill Indicator** | Top Segmented Pill Bar | Rounded Capsule (`Height: 32pt`, `r=16pt`) | `12pt` / `12dp` | `rgba(0, 229, 255, 0.22)` (Cyan glass) | `1dp rgba(0, 229, 255, 0.40)` | Subtle cyan glow `rgba(0, 229, 255, 0.25)` |
| **Search Field Capsule** | Search screen, Desktop Sidebar | Rounded Capsule (`Height: 40pt`, `r=20pt` / `r=10pt`) | `20pt` / `20dp` | `rgba(255, 255, 255, 0.07)` | `1dp rgba(255, 255, 255, 0.08)` | None |
| **Audio Route Pill** | Now Playing top header | Rounded Capsule (`Height: 32pt`, `r=16pt`) | `16pt` / `16dp` | `rgba(255, 255, 255, 0.08)` | `1dp rgba(255, 255, 255, 0.12)` | None |
| **Secondary Action Shuffle Button** | Album, Playlist, Artist | Rounded Capsule (`160×44pt`, `r=22pt` / `r=19dp`) | `20pt` / `20dp` | `rgba(255, 255, 255, 0.10)` | `1dp rgba(255, 255, 255, 0.15)` | `elevation = 6pt` |
| **Circular Action Icon Buttons** | Favorite `★`, Download `⬇`, Share `↗`, Options `•••`, Back `‹` | Circle (`36×36pt`, `r=18pt` or `32×32dp`, `r=16dp`) | `16pt` / `16dp` | `rgba(255, 255, 255, 0.08)` | `1dp rgba(255, 255, 255, 0.14)` | `elevation = 4pt` |
| **User Profile Avatar Pill** | Top navigation bar, Sidebar bottom | Rounded Capsule (`36×36pt` / `48pt` height, `r=24pt`) | `16pt` / `16dp` | `rgba(255, 255, 255, 0.07)` | `1dp rgba(255, 255, 255, 0.12)` | None |
| **Left Navigation Sidebar** | Desktop Shell, Tablet Landscape | Full-height Column (`Width: 280dp`, `r=0dp`) | `35dp` | `rgba(16, 18, 22, 0.85)` | `1dp rgba(255, 255, 255, 0.08)` right border | None |
| **Selected Sidebar Navigation Pill** | Desktop Sidebar | Rounded Capsule (`Height: 38dp`, `r=19dp`) | `12dp` | `rgba(0, 229, 255, 0.15)` (Cyan glass) | `1dp rgba(0, 229, 255, 0.30)` | Subtle cyan glow |
| **Control Center Media Card** | Lockscreen, Quick Settings | Rounded Rectangle (`345×180pt`, `r=24pt`) | `30pt` / `30dp` | `rgba(20, 22, 26, 0.82)` | `1dp rgba(255, 255, 255, 0.15)` | `elevation = 16pt`, `rgba(0,0,0,0.40)` |
| **AirPlay Output Selector Sheet** | Now Playing route trigger | Modal Card (`fillMaxWidth() - 32pt`, `r=24pt`) | `35pt` / `35dp` | `rgba(22, 24, 28, 0.88)` | `1dp rgba(255, 255, 255, 0.14)` | `elevation = 24pt`, `rgba(0,0,0,0.60)` |
| **Lyrics & Playing Next Split Card** | Desktop Now Playing Stage, Mobile Queue | Large Card (`640×712dp`, `r=18dp`) | `30dp` | `rgba(255, 255, 255, 0.04)` | `1dp rgba(255, 255, 255, 0.08)` | None |
| **Audio Format Badges** | Album header, Now Playing metadata | Rounded Pill (`r=4pt` / `r=6dp`) | `8pt` / `8dp` | `rgba(255, 255, 255, 0.08)` | `0.8dp rgba(0, 229, 255, 0.30)` | None |

---

## 2. Detailed Screen-by-Screen Liquid Glass Breakdown

### 2.1 Home Screen (`01_HOME_SCREEN.md`)
1. **Floating Glass Bottom Navigation Bar**:
   - Capsule `361×54pt`, `r=27pt`.
   - Frosted acrylic `rgba(18, 20, 24, 0.85)`, blur `30pt`.
   - Must refract the scrolling content and animated lava-lamp mesh behind it.
2. **Floating Mini-Player Capsule Dock**:
   - Capsule `361×56pt`, `r=28pt`.
   - Surface fill `rgba(20, 22, 26, 0.82)`, blur `30pt`.
   - Specular top rim `1dp rgba(255, 255, 255, 0.18)`.
3. **User Profile Account Button**:
   - Circular glass pill `36×36pt`, `r=18pt`.
   - Surface fill `rgba(255, 255, 255, 0.08)`, border `1dp rgba(255, 255, 255, 0.12)`.

---

### 2.2 Now Playing Screen (`02_NOW_PLAYING_SCREEN.md`)
1. **Top Audio Route Pill**:
   - Capsule `Height: 32pt`, `r=16pt`.
   - Displays `iPhone > AirPods Max` in frosted glass `rgba(255, 255, 255, 0.08)`.
2. **Audio Quality Codec Pill**:
   - Pill container `r=6pt`, background `rgba(255, 255, 255, 0.08)`, border `1dp rgba(0, 229, 255, 0.25)`.
   - Displays `LOSSLESS • ALAC 24-bit/48kHz`.
3. **Circular Action Buttons**:
   - Favorite heart and More `•••` options: `32×32pt` circular glass buttons (`r=16pt`).
4. **Lower Action Strip**:
   - Floating pill housing lyrics trigger `💬`, connected audio route, and queue button `☰`.
5. **AirPlay Device Selector Modal**:
   - Full glass card sheet (`r=24pt`, blur `35pt`, fill `rgba(22, 24, 28, 0.88)`).

---

### 2.3 Album Screen (`03_ALBUM_SCREEN.md`)
1. **Top Navigation Controls**:
   - Back chevron `‹` and More options `•••`: `36×36pt` circular glass buttons (`r=18pt`).
2. **Format Badges Pill**:
   - Capsule container displaying `Pop • 2019 • Dolby Atmos • Lossless`.
3. **Editorial Review Container**:
   - Subtle frosted glass card with hairline border `rgba(255, 255, 255, 0.06)`.
4. **Shuffle Action Button**:
   - Full capsule `160×44pt`, `r=22pt`.
   - Translucent glass fill `rgba(255, 255, 255, 0.10)`, specular border `1dp rgba(255, 255, 255, 0.15)`.
5. **Secondary Circular Actions**:
   - Download `⬇`, Favorite `♡`, Share `↗` (`36×36pt` circular glass buttons).

---

### 2.4 Library Screen (`04_LIBRARY_SCREEN.md`)
1. **Top Action Pill**:
   - `Edit` button: Glass capsule `r=16pt` with cyan label.
2. **Category Rows (Interactive Hover / Pressed Glass)**:
   - Rows for `Playlists`, `Artists`, `Tracks`, `Albums`, `Music Videos`, `Genres`.
   - Active / pressed state renders translucent cyan glass `rgba(0, 229, 255, 0.12)`.
3. **New Playlist Button (Playlists Sub-View)**:
   - Capsule `Height: 44pt`, `r=22pt` in frosted glass `rgba(255, 255, 255, 0.08)`.
4. **Sort Filter Bar**:
   - Glass pill strip with dropdown selectors (`Title`, `Inclusion date`, `Artist`, `Year`).

---

### 2.5 New & Charts Screen (`05_NEW_SCREEN.md`)
1. **Featured Editorial Banner ("EXCLUSIVE PLAYLIST")**:
   - Card `361×240pt`, `r=18pt`.
   - Lower text region uses liquid glass gradient scrim with blur `20pt`.
2. **Daily Top 100 Chart Cards**:
   - `155×215pt` cards with frosted glass overlay on artwork (`r=12pt`).
3. **City Charts Cards**:
   - `150×210pt` cards with frosted glass caption footer.

---

### 2.6 Search Screen (`06_SEARCH_SCREEN.md`)
1. **Liquid Glass Capsule Search Field**:
   - Capsule `Height: 40pt`, `r=20pt`.
   - Glass fill `rgba(255, 255, 255, 0.08)`, border `1dp rgba(255, 255, 255, 0.10)`.
   - Refracts underlying genre cards during scrolling.
2. **Segmented Catalog Filter Pill**:
   - Capsule `Height: 32pt`, `r=16pt`.
   - Houses `Apple Music` and `Library`.
   - Active segment: Cyan glass `rgba(0, 229, 255, 0.22)` with cyan border.
3. **Recently Searched Dismiss Pills**:
   - Dismiss `✕` buttons: `24×24pt` circular glass pills (`r=12pt`).
4. **Autocomplete Suggestion Rows**:
   - Hover / selected state applies glass sheen `rgba(255, 255, 255, 0.06)`.

---

### 2.7 Artist Screen (`07_ARTIST_SCREEN.md`)
1. **Top Floating Navigation Buttons**:
   - Back chevron `‹`, Share button, Favorite Star `★`: `36×36pt` circular glass buttons (`r=18pt`, blur `16pt`).
2. **Shuffle Action Button**:
   - Capsule `140×40pt`, `r=20pt` in frosted glass `rgba(255, 255, 255, 0.10)`.
3. **"About" Artist Biography Card**:
   - Large card `Margin: 16pt`, `r=18pt`.
   - Frosted acrylic `rgba(255, 255, 255, 0.05)`, border `1dp rgba(255, 255, 255, 0.08)`.
   - Displays origin metadata (`From`, `Born`, `Genre`) and biography paragraph.

---

### 2.8 Playlist Detail Screen (`08_PLAYLIST_SCREEN.md`)
1. **Top Floating Navigation Buttons**:
   - Back `‹`, Favorite `★`, More `•••`: `36×36pt` circular glass buttons.
2. **Editorial Blurb Card**:
   - Subtle frosted glass box with `MORE` cyan trigger.
3. **Shuffle Action Button**:
   - Capsule `160×44pt`, `r=22pt` in frosted glass `rgba(255, 255, 255, 0.10)`.
4. **Featured Artists Circular Avatars**:
   - `80×80pt` circles with glass rim border `1dp rgba(255, 255, 255, 0.12)`.

---

### 2.9 Control Center & Lock Screen Widget (`09_CONTROL_CENTER_WIDGET.md`)
1. **Large Control Center Media Card**:
   - Card `345×180pt`, `r=24pt`.
   - Surface fill `rgba(20, 22, 26, 0.82)`, backdrop blur `30pt`.
   - Specular top rim `1dp rgba(255, 255, 255, 0.15)`.
2. **Compact 2×2 Media Tile**:
   - Card `160×160pt`, `r=24pt`, blur `25pt`.
3. **AirPlay Device Switcher Flyout**:
   - Floating glass menu `Width: 300pt`, `r=20pt`, blur `35pt`.
   - Connected device highlight in Ice-Cyan glass `rgba(0, 229, 255, 0.15)`.

---

### 2.10 Desktop Shell & Tablet Landscape (`DesktopMainView.kt`)
1. **Root Window Canvas**:
   - Hosted inside `DesktopLiquidContainer` with morphing cyan/indigo lava-lamp canvas.
2. **Left Navigation Sidebar**:
   - Full-height column `Width: 280dp`, blur `35dp`, surface `rgba(16, 18, 22, 0.85)`.
3. **Top Segmented Navigation Bar**:
   - Header `Height: 60dp`, central segmented pill `Height: 40dp`, `r=20dp`.
4. **Floating Mini-Player Capsule Dock**:
   - Capsule `700×66dp`, `r=33dp`, anchored `24dp` above bottom edge.
5. **Fullscreen Now Playing Takeover Overlay**:
   - Modal takeover `1194×834dp` with `Rectangle 73` dismiss handle and two-column stage.

