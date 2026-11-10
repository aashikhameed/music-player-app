# Mobile Home Screen Specification (`Home.png`)

> **File Reference**: `designScreen/mobile/Home.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt (3x retina: 1179 × 2556px)  
> **Theme**: Obsidian Dark Canvas (`#0D0F12`), Liquid Glass Overlays, Zero Red.

---

## 1. Screen Architecture & Overview
The Home Screen serves as the primary personalized discovery hub for mobile users. It features dynamic featured hero cards, personalized mood playlists, horizontal carousels for "Top Picks for You", "Recently Played", and "Hits in Spatial Audio", floating above an ambient liquid glass background with the sticky mini-player dock and tab bar at the bottom.

---

## 2. Layout Structure & Geometry (Top to Bottom)

### 2.1 Top System Status & Navigation Bar
- **Status Bar**:
  - Height: `44pt` (Dynamic Island / Notch clearance).
  - Time: `9:41` (Left aligned, `15sp SemiBold`, White).
  - Cellular, Wi-Fi, Battery indicators: Right aligned (`16pt` margin).
- **Navigation Header (`Height: 52pt`)**:
  - Left Title: `Home` (`34sp Bold`, Titanium White `#FFFFFF`).
  - Right Action: User Profile Pill / Account Avatar (`36pt × 36pt` circular glass button with border `1dp rgba(255, 255, 255, 0.15)`).

### 2.2 Featured Hero Carousel ("Mood for You" / Top Pick)
- **Container**: Horizontal snap carousel (`fillMaxWidth()`, item width `345pt`, height `220pt`).
- **Margins**: `16pt` horizontal, `12pt` top spacing below large title.
- **Card Styling**:
  - Corner radius: `r=18pt`.
  - Background: Full-bleed artwork with bottom-to-top scrim gradient (`rgba(0,0,0,0.85)` at bottom to `transparent` at 45% height).
  - Top Badge: `FEATURED PLAYLIST` (`11sp Bold`, Tracking `1sp`, Ice-Cyan `#00E5FF`).
  - Hero Title: `Mood for You: Relax` (`24sp Bold`, White).
  - Subtitle: `Featuring Taylor Swift, Billie Eilish, Sabrina Carpenter` (`13sp Regular`, Slate `#94A3B8`).

### 2.3 Section 1: "Top Picks for you"
- **Header Row (`Height: 40pt`)**:
  - Section Title: `Top Picks for you` (`20sp Bold`, White).
  - Accessory Chevron: `›` (`18sp SemiBold`, Slate `#64748B`).
- **Horizontal Scroll Carousel**:
  - Item Dimensions: Width `150pt`, Height `210pt`.
  - Item Gap: `14pt`, Content Padding: `16pt` start/end.
  - Artwork Box: `150pt × 150pt` square, corner radius `r=10pt`, subtle drop shadow.
  - Album Title: `Speak Now (Taylor's Version)` (`14sp SemiBold`, White, max 1 line, ellipsis).
  - Artist / Type: `Taylor Swift • 2023` (`12sp Regular`, Slate `#94A3B8`).

### 2.4 Section 2: "Recently Played"
- **Header Row (`Height: 40pt`)**:
  - Section Title: `Recently Played` (`20sp Bold`, White).
  - Trailing Action: `›` chevron.
- **2-Row Horizontal Scroll Grid**:
  - Two rows of tracks / albums scrolling horizontally.
  - Card Dimensions: `150pt × 150pt`.
  - Sample Items: `The Fate Of Ophelia`, `Hits in Spatial Audio`, `1989 (Taylor's Version)`.

### 2.5 Sticky Floating Mini-Player Dock
- **Dimensions**: Width `fillMaxWidth() - 32pt` (e.g. `361pt`), Height `56pt`, Margin bottom `10pt` above tab bar.
- **Corner Radius**: `r=28pt` (Complete floating pill).
- **Glass Effect**: Liquid glass backdrop blur (`30pt`), fill `rgba(20, 22, 26, 0.85)`, border `1dp rgba(255, 255, 255, 0.15)`.
- **Content Row**:
  - Thumbnail: `40pt × 40pt`, `r=6pt`, left margin `8pt`.
  - Title: `The Fate Of Ophelia` (`13.5sp Bold`, White).
  - Artist: `Taylor Swift` (`11.5sp Regular`, Slate `#94A3B8`).
  - Right Controls: Play/Pause button (`32pt` circle, Cyan `#00E5FF`) + Next Track (`⏭`, White).

### 2.6 Bottom Floating Glass Navigation Bar
- **Dimensions**: Width `fillMaxWidth() - 32pt`, Height `54pt`, Bottom margin `16pt` (above home indicator).
- **Shape**: Rounded capsule `r=27pt`.
- **Glass Fill**: Translucent frosted glass `rgba(18, 20, 24, 0.88)` with specular top hairline rim.
- **Tabs (5 items)**:
  1. `Home` (Active: Ice-Cyan `#00E5FF`, filled icon, `10sp SemiBold`).
  2. `New` (Inactive: Slate `#94A3B8`, outlined icon, `10sp Regular`).
  3. `Radio` (Inactive: Slate `#94A3B8`).
  4. `Library` (Inactive: Slate `#94A3B8`).
  5. `Search` (Inactive: Slate `#94A3B8`).
