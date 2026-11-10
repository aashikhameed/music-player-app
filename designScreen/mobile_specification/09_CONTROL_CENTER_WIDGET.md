# Mobile Control Center & Lock Screen Widget Specification (`Control Center.png`)

> **File Reference**: `designScreen/mobile/Control Center.png`  
> **Target Viewport Baseline**: iOS Control Center & Lockscreen Music Modules  
> **Theme**: Translucent Frosted Glass `#14161A`, Dynamic Island, Zero Red.

---

## 1. Module Overview & Integration Scope
The Control Center and Dynamic Island widgets represent system-level quick controls for media playback. On Android Automotive / Mobile, this maps directly to the persistent media notification, quick settings media tile, and lockscreen playback card.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Large Control Center Media Card
- **Dimensions**: Width `fillMaxWidth() - 32pt` (e.g. `345pt`), Height `180pt`.
- **Corner Radius**: `r=24pt`.
- **Glass Shaders**:
  - Backdrop Blur: `30pt`.
  - Glass Fill: `rgba(20, 22, 26, 0.82)`.
  - Border: `1dp rgba(255, 255, 255, 0.15)`.
  - Shadow: `elevation = 16pt`, `rgba(0,0,0,0.40)`.
- **Content Layout**:
  - **Top Row (`Height: 64pt`)**:
    - Album Thumbnail: `60pt × 60pt` square (`r=10pt`, drop shadow).
    - Track Title: `The Fate Of Ophelia` (`16sp Bold`, White).
    - Artist: `Taylor Swift` (`13.5sp Regular`, Slate `#94A3B8`).
    - AirPlay / Route Icon: Connected device indicator (`20pt`, Cyan `#00E5FF`).
  - **Middle Row: Timeline Scrubber (`Height: 28pt`)**:
    - Progress Bar: Width `fillMaxWidth()`, Height `5pt`, `r=2.5pt`.
    - Played: Ice-Cyan `#00E5FF`.
    - Elapsed (`0:03`) & Remaining (`-6:10`): `11sp Medium`, Slate `#64748B`.
  - **Bottom Row: Transport Controls (`Height: 48pt`)**:
    - Previous (`⏮`): `22pt`, White.
    - Play / Pause (`⏸`): `32pt`, Solid White (or Cyan `#00E5FF`).
    - Next (`⏭`): `22pt`, White.
    - Volume Slider with Speaker Icons: Height `4pt`, `r=2pt`.

### 2.2 Compact Media Tile (2×2 Widget)
- **Dimensions**: `160pt × 160pt` square.
- **Corner Radius**: `r=24pt`.
- **Layout**:
  - Top Half: `48pt × 48pt` album art + title & artist (`13sp Bold`).
  - Bottom Half: Play/Pause circle (`36pt`) flanked by previous and next buttons.

### 2.3 Connected Audio Route Modal (AirPlay Quick Switcher)
- **Glass Flyout Menu (`Width: 300pt, r=20pt`)**:
  - Device 1: `AirPods Max` (Selected: Cyan checkmark, Battery indicator `100%`).
  - Device 2: `iPhone Speaker`.
  - Device 3: `John's MacBook Pro` (Remote AirPlay target).
