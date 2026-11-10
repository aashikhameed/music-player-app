# Mobile Now Playing & Audio Route Specification (`Now Playing.png`)

> **File Reference**: `designScreen/mobile/Now Playing.png`  
> **Target Viewport Baseline**: iPhone 393pt × 852pt  
> **Theme**: Obsidian Canvas `#0D0F12`, Fluid Mesh Gradient Canvas, Zero Red.

---

## 1. Screen Architecture & Overview
The Now Playing experience on mobile consists of a full-screen modal takeover with multiple contextual states:
1. **Hero Player View**: Dominant album art card, audio route indicator, track metadata, timeline scrubber, transport controls, and lower action cluster.
2. **Audio Route Picker (AirPlay / Device Selector)**: Floating glass bottom sheet showing available playback outputs (AirPods Max, iPhone Speaker, MacBook Pro).
3. **Song Credits & Lyrics View**: Time-synced floating lyrics or detailed production credits (Performing Artists, Vocals, Instruments, Composition).
4. **Playing Next Queue Sheet**: Draggable up-next queue drawer with song reordering handles and clear queue actions.

---

## 2. Layout Structure & Detailed Geometry

### 2.1 Top Bar & Modal Dismiss Handle
- **Dismiss Handle (Rectangle 73)**: Width `44pt`, Height `5pt`, `r=2.5pt`, Color `rgba(255, 255, 255, 0.35)`, centered `10pt` from top.
- **Top Audio Route Pill**:
  - Container: Centered capsule pill (`Height: 32pt, r=16pt`).
  - Background: `rgba(255, 255, 255, 0.08)`, Border `1dp rgba(255, 255, 255, 0.12)`.
  - Icon: Headphones / AirPlay icon (`14pt`).
  - Text: `iPhone > AirPods Max` (`12sp Medium`, White).
  - Interaction: Tapping opens the Audio Route Output Sheet.

### 2.2 Hero Artwork Card
- **Dimensions**: Square `335pt × 335pt` (or `fillMaxWidth() - 48pt`).
- **Corner Radius**: `r=20pt`.
- **Drop Shadow**: Soft ambient colored glow matching artwork (`elevation = 24pt`, blur `36pt`, opacity `0.40`).
- **Border**: `1dp rgba(255, 255, 255, 0.12)`.
- **Artwork State**: Smooth scale effect (`0.85x` when paused, `1.0x` when actively playing).

### 2.3 Track Metadata & Action Cluster
- **Horizontal Flex Row (`Height: 64pt, Padding: 24pt horizontal`)**:
  - Left Column (Track Info):
    - Track Title: `The Fate Of Ophelia` (`22sp Bold`, White, max 1 line, ellipsis).
    - Artist: `Taylor Swift` (`16sp Medium`, Slate `#94A3B8`).
    - Format Pill: `Lossless • Apple ALAC 24-bit/48kHz` (`10sp SemiBold`, Ice-Cyan `#00E5FF`).
  - Right Cluster:
    - Favorite Heart button (`22pt`, Ice-Cyan `#00E5FF` when active, else Slate `#94A3B8`).
    - More Options `•••` button (`22pt`, Glass circle `32pt × 32pt`).

### 2.4 Timeline Scrubber Track
- **Geometry**: Width `fillMaxWidth() - 48pt`, Height `6pt`, `r=3pt`.
- **Played Track**: Solid Ice-Cyan `#00E5FF`.
- **Unplayed Track**: `rgba(255, 255, 255, 0.18)`.
- **Scrubber Knob**: White circle `16pt × 16pt`, subtle drop shadow.
- **Timestamp Labels Row (`Height: 20pt, Margin top: 6pt`)**:
  - Elapsed: `0:03` (Left, `12sp Regular`, Slate `#64748B`).
  - Remaining: `-6:10` (Right, `12sp Regular`, Slate `#64748B`).

### 2.5 Transport Controls
- **Flex Row (`Height: 72pt, Padding horizontal: 24pt`)**:
  - Shuffle Button (`🔀`): `20pt`, Slate `#94A3B8` (Cyan when active).
  - Previous Track (`⏮`): `28pt`, Solid Titanium White `#FFFFFF`.
  - **Center Play/Pause Button**:
    - Circle: `64pt × 64pt`, `r=32pt`.
    - Fill: Solid Ice-Cyan `#00E5FF`.
    - Icon: Solid obsidian black `#0D0F12` play triangle / pause bars (`24pt`).
    - Glow: Ambient blur `16pt` cyan glow.
  - Next Track (`⏭`): `28pt`, Solid Titanium White `#FFFFFF`.
  - Repeat Button (`🔁`): `20pt`, Slate `#94A3B8` (Cyan when active).

### 2.6 Volume Slider & Lower Bottom Cluster
- **Volume Slider**: Width `fillMaxWidth() - 48pt`, height `4pt`, flanked by low/high speaker icons.
- **Lower Action Pill Bar (`Height: 48pt`)**:
  - Lyrics Trigger (`💬`): Highlights cyan when lyrics sheet is open.
  - Audio Route Output Pill: Centered button showing active connected device.
  - Up Next Queue Trigger (`☰`): Highlights cyan when queue drawer is open.

---

## 3. Modal Overlays & Drawers

### 3.1 Audio Route Selector (AirPlay Drawer)
- Glass Card: `fillMaxWidth() - 32pt`, corner radius `r=24pt`, backdrop blur `30pt`.
- Device List Items (`Height: 52pt` each):
  - Device 1: `AirPods Max` (Selected checkmark, Cyan `#00E5FF`).
  - Device 2: `iPhone Speaker` (`15sp Regular`, White).
  - Device 3: `John's MacBook Pro` (`15sp Regular`, White).

### 3.2 Playing Next Queue Drawer
- **Header**: `Playing Next` (`17sp Bold`), Clear button (`14sp Medium`, Slate `#94A3B8`).
- **Track Rows (`Height: 56pt` each)**:
  - Drag Handle: 3-bar icon `☰` on right edge.
  - Track Title (`14sp SemiBold`, White), Artist (`12sp Regular`, Slate `#94A3B8`).
