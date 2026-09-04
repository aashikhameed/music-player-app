# 🚗🎵 Car Music Player (Android Automotive)

[![Release](https://img.shields.io/github/v/release/aashikhameed/music-player-app?style=flat-square)](https://github.com/aashikhameed/music-player-app/releases)
[![Build & Release APK](https://img.shields.io/github/actions/workflow/status/aashikhameed/music-player-app/build.yml?branch=main&style=flat-square)](https://github.com/aashikhameed/music-player-app/actions)
[![Platform](https://img.shields.io/badge/Platform-Android%20Automotive%20%7C%20API%2026%2B-blue?style=flat-square)](https://developer.android.com/training/cars)
[![Optimized for](https://img.shields.io/badge/Spec-1280x720%20%40%20160dpi%20(9.18%22)-teal?style=flat-square)](#hardware-specification)

A high-performance, automotive-grade offline music player application built with **Jetpack Compose** and a **custom C++ OpenSL ES native audio engine**. Engineered for responsive touch control and fluid performance on automotive head units (such as the Blaupunkt Santa Rosa 985 and 2GB RAM Android Automotive infotainment displays).

---

## 🏎️ Hardware Specification & Optimization Target

- **Display**: 1280 × 720 px @ 160 dpi (9.18" Automotive Landscape Touchscreen)
- **Architecture**: ARM64 (`arm64-v8a`) with NEON SIMD vectorization
- **Memory Footprint**: Target < 120 MB PSS (Achieved: **~111 MB**, Java Heap: **13.1 MB**)
- **UI Performance**: **0.84% janky frames** across 4,000+ continuous playback frames (>99.1% fluid 60fps)
- **Binary Size**: **3.7 MB** optimized release APK (84% size reduction via R8 and resource shrinking)

---

## ✨ Key Features

### 🎛️ Unified Floating Media Dock
- **Combined Song Info & Waveform Seeker**:
  - Live album artwork with pulsating playing glow halo.
  - Marquee track title with smooth auto-scroll.
  - File codec badge (`.mp3`, `.m4a`, `.wav`, `.flac`, `.opus`, `.aac`).
  - Real-time time elapsed and total duration counter.
  - Interactive Material You Android 13+ squiggly wave seekbar with scrub tooltip.
- **Spacious Driver Controls**:
  - Tactile touch targets with zero edge-clipping padding.
  - Shuffle toggle with non-interruptive playlist reordering.
  - Previous / Next track navigation.
  - Hero Play/Pause button with ambient breathing halo animation.
- **Status Island**:
  - 12-hour AM/PM clock with superscript indicator (updates continuously every second).
  - Quick-switch Day/Night theme toggle (Sun/Moon icon).

### ⚡ C++ OpenSL ES Native Audio Core
- **Zero GC Audio Stutters**: Audio decoding, ring buffering (`RingBuffer.h`), and PCM playback stream directly in native C++, completely eliminating Java Garbage Collection pauses and audio pops.
- **Ultra-Low Latency**: Sub-5ms track switching and instant seek response.
- **NEON SIMD Vectorization**: Compiled with `-O3 -march=armv8-a -ftree-vectorize -ffast-math`.

### 📂 Library & Automotive Navigation
- **Initial Load Auto-Scroll**: Instantly focuses the grid on the currently playing or last-played track upon app startup.
- **Tap to Navigate**: Tapping the bottom bar track info instantly scrolls the library grid back to the playing song.
- **Dual Browsing Modes**: Quick-switch between **All Songs** (3-column landscape grid) and **Folders** hierarchy.
- **In-Place Search**: Real-time debounced search bar with instant title, artist, and album filtering.
- **Pre-Cached Album Art**: Asynchronous background decoding preventing UI thread frame drops.

---

## 📊 Measured Performance Profile

| Metric | Measured Value | Benchmark | Status |
| :--- | :--- | :--- | :--- |
| **Jank Rate** | **0.84%** (34 / 4,067 frames) | < 2.0% | 🟢 Exceptional |
| **Slow Bitmap Uploads** | **0** | < 10 | 🟢 Optimal (Zero image stall) |
| **Java Heap Memory** | **~13.1 MB** | < 45 MB | 🟢 Ultra-Lightweight |
| **Total Memory (PSS)** | **~111 MB** | < 180 MB | 🟢 Low Footprint |
| **Swap Memory Usage** | **0 KB** | 0 KB | 🟢 Zero thrashing |
| **Release APK Size** | **3.7 MB** | < 15 MB | 🟢 Highly Compact |

---

## 🛠️ Architecture & Tech Stack

| Component | Technology |
| :--- | :--- |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Audio Pipeline** | Native C++17 + OpenSL ES + Android MediaNDK |
| **Language** | Kotlin 1.9 + C++17 |
| **Architecture** | MVVM + Coroutines + Kotlin StateFlow |
| **Database** | Room SQLite (with ProGuard bytecode preservation) |
| **Optimization** | R8 Full Mode + Resource Shrinking + ART Profile AOT Compilation |

---

## 📦 Building and Running

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 36 (minSdk 26)
- Android NDK (r25+ or cmake 3.22.1)
- JDK 17

### Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Optimized Minified Production APK (3.7 MB)
./gradlew assembleRelease

# Install directly to connected automotive head unit or emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
