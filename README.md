# 🚗🎵 Music App for Android Car Infotainment

A modern, high-performance **Music Player App** built with **Kotlin & Jetpack Compose** specifically designed for **Android Automotive OS**, **Android Auto**, and **Car Infotainment Systems** (optimized for low-RAM / low-CPU 2GB head units).

---

## 📱 Features

- 🚗 **Android Auto Infotainment Interface**
  - Permanent landscape automotive layout with bottom taskbar and mini media player pill.
  - Split-screen navigation mode: Full-height **Google Maps** on the left with live real-time GPS tracking and 2-column music library on the right.
  - Seamless full-screen mode: 3-column music grid with sleek automotive vertical scrollbar.

- 🗺️ **Embedded Live Navigation Map**
  - Clean full-bleed map display with auto-centered GPS location updates.
  - Distraction-free: Stripped of web banners, "Open app" buttons, and watermarks.
  - Native theme integration: Instant zero-flicker dark & light mode styling.

- 🎚️ **Automotive Playback & Controls**
  - **Integrated Top-Border Seekbar**: Sleek 3dp progress bar seamlessly embedded directly into the top edge of the Now Playing media pill.
  - **Hardware Mute Auto-Pause**: Automatically pauses playback when steering wheel mute button is pressed or vehicle audio stream drops to 0 volume.
  - **Non-Interruptive Shuffle**: Dynamically shuffles the playlist without interrupting or restarting current track playback.
  - Full steering wheel and head unit media button support.

- 🎶 **Offline Music Playback & Low-RAM Optimization**
  - Supports `.mp3`, `.m4a`, `.opus`, `.flac`, `.wav`, and `.aac`.
  - Tuned ExoPlayer LoadControl buffering to minimize memory footprint on 2GB RAM automotive devices.
  - Fast local media scanning, folder browsing, and instant library cache.

- 📁 **Library & Folder Filtering**
  - Tabbed library filters (All Songs, Folders).
  - Quick top-right collapsible search.
  - In-place folder hierarchy browsing and long-press delete actions.

---

## 🧰 Tech Stack

| Layer            | Technology                  |
|------------------|-----------------------------|
| Language         | Kotlin                      |
| UI Framework     | Jetpack Compose + Material3 |
| Media Playback   | ExoPlayer (Media3)          |
| Car Integration  | MediaSessionCompat + Android Auto + BroadcastReceivers |
| GPS / Location   | Android LocationManager (GPS / Network Provider) |
| Data Storage     | Room DB + DataStore         |

---

## 📦 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/music-player-app.git
   cd music-player-app
   ```

2. **Build and Install**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```
