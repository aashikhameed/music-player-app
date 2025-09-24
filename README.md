# 🚗🎵 Music App for Android Car Infotainment

A modern, feature-rich **Music Player App** built with **Kotlin** specifically designed for **Android Automotive OS** and **Car Infotainment Systems**. It supports local audio playback, voice/media controls, and seamless Bluetooth integration for an optimal in-car entertainment experience.

---
## 📱 Screenshots
<img width="500" height="500" alt="Screenshot_1758726567" src="https://github.com/user-attachments/assets/b891df38-6944-429d-afb6-1892b94bc4a7" />
<img width="500" height="500" alt="Screenshot_1758726559" src="https://github.com/user-attachments/assets/4162dd2a-05fe-4bb0-a94d-f4db8e4ab2d3" />

## 📱 Features

- 🎶 **Offline Music Playback**
  - Supports `.mp3`, `.m4a`, `.opus`, and other common formats.
  - Fast local media scanning and caching.

- 🚗 **Android Auto & Bluetooth Support**
  - Fully integrated with `MediaSessionCompat` and `MediaBrowserServiceCompat`.
  - Supports steering wheel, dashboard, and Bluetooth headset controls.

- 🎚️ **Advanced Playback Controls**
  - Shuffle, Repeat, Loop, Seek
  - Smooth fade in/out transitions
  - Foreground playback service for uninterrupted listening

- 🌗 **UI & UX**
  - Tablet-optimized 3-column layout
  - Material 3 design with light/dark theme toggle
  - Persistent playback state and theme using DataStore

- 📂 **Library & Folder Filtering**
  - Search and filter by folder
  - Playlist creation and local storage
  - Long-press delete support

- 💾 **Persistence**
  - Remembers last played song on app restart
  - Room database for storing songs and playlists

---

## 🧰 Tech Stack

| Layer            | Technology                  |
|------------------|-----------------------------|
| Language         | Kotlin                      |
| UI Framework     | Jetpack Compose + Material3 |
| Media Playback   | ExoPlayer (Media3)          |
| Car Integration  | MediaSessionCompat + Android Auto |
| Background Tasks | Foreground Service + Coroutine |
| Data Storage     | Room DB + DataStore         |

---

## 📦 Installation

1. **Clone this repository**

```bash
git clone https://github.com/yourusername/music-car-app.git
cd music-car-app
