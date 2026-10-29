<div align="center">

# 🚗🎵 Android Automotive Music Player
### High-Performance Offline Car Infotainment Audio Player | Jetpack Compose + C++ OpenSL ES

[![GitHub Release](https://img.shields.io/github/v/release/aashikhameed/music-player-app?style=for-the-badge&color=00E5FF&logo=github)](https://github.com/aashikhameed/music-player-app/releases)
[![Build & Release APK](https://img.shields.io/github/actions/workflow/status/aashikhameed/music-player-app/build.yml?branch=main&style=for-the-badge&logo=githubactions&color=4CAF50)](https://github.com/aashikhameed/music-player-app/actions)
[![Platform](https://img.shields.io/badge/Platform-Android%20Automotive%20%7C%20API%2026%2B-0066FF?style=for-the-badge&logo=android)](https://developer.android.com/training/cars)
[![APK Size](https://img.shields.io/badge/APK%20Size-3.5%20MB%20(ARM64)-brightgreen?style=for-the-badge&logo=android)](https://github.com/aashikhameed/music-player-app/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)

<p align="center">
  <b>A lightweight, zero-latency, offline music player purpose-built for Android Automotive OS (AAOS) and aftermarket car head units.</b><br>
  Engineered with modern <b>Jetpack Compose</b> and a bare-metal <b>C++17 OpenSL ES native audio engine</b> for flawless 60 FPS playback on low-memory ARM Cortex / Mali GPU automotive hardware.
</p>

[**Download Latest APK (v0.0.23)**](https://github.com/aashikhameed/music-player-app/releases/latest) • [**Hardware Target**](#-hardware-targets--performance-benchmarks) • [**Key Features**](#-key-features) • [**Architecture**](#-architecture--tech-stack) • [**Build & Install**](#-build--installation)

---

</div>

## 📌 Overview

**Car Music Player** is an open-source, automotive-grade music player designed from the ground up for vehicle infotainment screens and Android Automotive head units. Unlike standard phone media apps ported to cars, this player specifically solves the hardware bottlenecks common to automotive infotainment systems: low RAM (1–2 GB), weak Mali/Adreno GPUs, slow eMMC flash storage, and strict driver-distraction safety constraints.

Featuring a **custom native C++ OpenSL ES pipeline**, it completely bypasses the Java garbage collection audio stutter typical of `MediaPlayer` and `ExoPlayer`, delivering instant touch-to-sound latency, lock-free ring buffering, real-time seamless crossfading, acoustic DSP room correction, and a silky-smooth 60 FPS driving UI.

---

## 🏎️ Hardware Targets & Performance Benchmarks

Engineered and verified against automotive reference head units (1280×720 @ 160 dpi, ARM64 Cortex-A53 SoC, Mali GPU, 1–2 GB RAM, slow eMMC storage):

| Performance Metric | Measured Value | Industry Standard | Automotive Benchmark Result |
| :--- | :--- | :--- | :--- |
| **UI Jank Rate** | **0.84%** (34 / 4,067 frames) | < 5.00% | 🟢 **>99.1% Fluid 60 FPS** |
| **Release APK Size** | **3.51 MB** (ARM64-v8a) | 25–60 MB | 🟢 **88% Smaller Footprint** |
| **Java Heap Footprint** | **13.1 MB** | 60–100 MB | 🟢 **Zero Out-Of-Memory Risk** |
| **Total Memory (PSS)** | **~111 MB** | 200–350 MB | 🟢 **Smooth on 1–2 GB RAM Units** |
| **Artwork Memory Cache** | **Capped at 2–4 MB** (64×64 RGB_565) | 20–50 MB | 🟢 **8 KB Per Cached Thumbnail** |
| **Track Switch Latency** | **< 5 ms** (SPSC Lock-Free) | 120–300 ms | 🟢 **Instant Hardware Audio Response** |
| **Flash I/O Journal Mode** | **SQLite WAL (Write-Ahead Logging)** | Rollback journal | 🟢 **Non-Blocking Concurrent Reads** |

---

## ✨ Key Features

### 🎛️ Driver-First Automotive UI & Floating Media Dock
- **Glanceable Touch Targets**: Oversized cards and spacious dock controls strictly complying with automotive distraction guidelines.
- **Unified Floating Media Dock**: Dual-segment floating capsule housing track metadata, codec badges, scoped real-time seekbar, and tactile playback controls.
- **Hardware-Accelerated Seekbar**: Direct Canvas-rendered scrubbing bar with live time bubble tooltip; zero heavy trigonometry animation overhead.
- **Live Canvas Equalizer**: In-place 4-bar dynamic equalizer drawn directly on Canvas inside `SongCard`, eliminating Compose `LayoutNode` hierarchy churn.
- **Adaptive Day & Night Theme**: Instant high-contrast toggle between Solar Amber/Light and Midnight Obsidian/Dark palettes designed for glare resistance and night driving.
- **Continuous Automotive Digital Clock**: Synchronized clock island with glanceable 12-hour AM/PM indicator.

### 🚗 Full-Screen Distraction-Free Drive Mode HUD
- **Blind Gesture Navigation**: Swipe left for Next Song, swipe right for Previous Song, and tap anywhere to Play/Pause.
- **Visual Swipe Feedback**: Instant directional badge overlay confirming queue navigation before automatically dismissing.
- **Tactile 10-Second Skip**: Dedicated 10s forward and rewind controls for rapid audio scrubbing while driving.
- **Live HUD Track Progress**: Full seekbar scrubber and folder switching directly accessible without leaving the drive interface.
- **High-Contrast Track Typography**: Massive title and artist display legible from driver seating distance.

### 🔀 Seamless Real-Time Crossfade Engine
- **Native C++ Linear Crossfading**: The upcoming song smoothly begins playing during the fade-out of the ending track with zero silence gap.
- **Configurable Durations**: Selectable crossfade lengths (0s Off, 1s, 2s, 3s, 5s, 8s — Default: 3 seconds) customizable in Settings.
- **Hardware State Synchronization**: Native OpenSL ES mixing handles track transition seamlessly without Java main looper stall.

### 🎚️ Automotive Soundstage & Acoustic DSP Suite
- **5-Band Biquad IIR Parametric EQ**: Frequency bands custom-tailored for vehicle cabin response:
  - `60 Hz`: Deep sub-bass punch.
  - `250 Hz`: **Mud-Cut Filter** to eliminate hollow vehicle door cavity resonance.
  - `1.0 kHz`: Midrange vocal fundamental clarity.
  - `3.5 kHz`: Vocal presence and instrument separation.
  - `10 kHz`: Treble air and tweeter shimmer.
- **Subsonic High-Pass Filter (HPF)**: 2nd-order Butterworth filter (Off, 60 Hz, 80 Hz) protecting vehicle door woofers from over-excursion distortion.
- **Driver Soundstage Time Alignment**: Sub-millisecond delay compensation aligning stereo arrival times for Center Stage, RHD Driver (India/UK), or LHD Driver (US/EU).
- **Bass Normalizer**: Dynamic low-end contour and peak control (+2 dB Mild, +4 dB Deep Dynamic).
- **Virtual Surround / Spatial Soundstage Widener**: Native Blumlein M-S matrix widener expanding the stereo soundstage (+25% Cabin Stage, +50% Concert Wide).
- **Digital DAC Headroom**: -2.5 dB digital preamp reduction preventing inter-sample clipping into external car DSP processors and amplifiers.

### ⚡ Bare-Metal C++ & OpenSL ES Native Audio Engine
- **Zero-GC Audio Streaming**: PCM playback streams through a custom C++ OpenSL ES queue buffer (`AudioPlayer.cpp`, `OpenSLESPlayer.cpp`), eliminating Java Garbage Collector audio pops and stutter.
- **SPSC Lock-Free FIFO RingBuffer**: Real-time Single-Producer Single-Consumer lockless audio buffer (`RingBuffer.h`) with explicit `std::memory_order_acquire` and `std::memory_order_release` atomic semantics.
- **Event-Driven Decoder Loop**: Hardware PCM buffer replenishment driven by `std::condition_variable` events rather than CPU spin-polling.
- **ARMv8 NEON Vectorization**: Compiled with `-O3`, `-flto`, `-march=armv8-a`, and `-ftree-vectorize` for fast SIMD processing.
- **Direct Atomic JNI Dispatch**: Throttled 250ms position updates dispatched straight to Kotlin `StateFlow` without posting allocations to the Android main looper.

### 📁 Smart Automotive Media Library
- **Dual Tab Navigation**: Effortless switching between **All Songs** (2-column landscape grid), **Folders** view, and **Favorites**.
- **Deep Multi-Field Search**: Instant real-time filtering across Song Title, Artist Name, Album Name, and Parent Folder Path.
- **USB Auto-Mount Scanning**: Automated `BroadcastReceiver` indexes newly connected USB flash drives (`ACTION_MEDIA_MOUNTED`) with zero manual interaction.
- **Initial Load Auto-Scroll**: Automatically focuses and centers the active or last-played song upon launching.
- **Hardware-Accelerated Artwork Caching**:
  - Memory: 64×64 thumbnail decoding in `Bitmap.Config.RGB_565` (8 KB/thumbnail, 75% RAM reduction).
  - Disk: Hardware NEON-accelerated JPEG compression (`quality = 75`) instead of CPU-heavy WebP.
  - Zero-Copy Lookup: Direct $O(1)$ LRU cache hits without snapshot map cloning.
  - Tombstones: 0-byte `.noart` disk markers bypassing `MediaMetadataRetriever` on missing artwork.

### 🚘 Android Automotive & MediaSession Integration
- **Steering Wheel Key Controls (SWC)**: Native hardware support for Previous, Next, Play/Pause, Fast-Forward, Rewind, and Mute buttons via direct `onKeyDown` and `MediaSessionCompat`.
- **System MediaBrowserService**: Integrates seamlessly with vehicle dashboard audio routing and automotive status bars.
- **Smart Hardware Volume Mute Handling**: Auto-pauses playback when the headunit is muted or system audio becomes noisy, auto-resuming upon unmute.
- **Parked Sleep Timer**: Automated power-saving playback shutoff (15, 30, 45, 60 minutes) preventing car battery drain when parked.

---

## 🎵 Supported Audio Formats & Codecs

| Codec / Container | File Extension | Decoding Method | Lossless Support |
| :--- | :--- | :--- | :--- |
| **FLAC** | `.flac` | Native MediaNDK / OpenSL ES | ✅ 24-bit / 96kHz Lossless |
| **MP3** | `.mp3` | Hardware Decoder / OpenSL ES | High-Bitrate VBR/CBR |
| **AAC / M4A** | `.aac`, `.m4a` | MediaNDK / OpenSL ES | LC-AAC / HE-AAC |
| **WAV** | `.wav` | Linear PCM Raw Stream | ✅ 16-bit / 24-bit Uncompressed |
| **OGG / Vorbis** | `.ogg` | Native MediaNDK | Vorbis Variable Bitrate |
| **OPUS** | `.opus` | Native OpenSL ES | Ultra-low latency voice/music |

---

## 🛠️ Architecture & Tech Stack

```text
┌──────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                     │
│    (Theme.kt • DriveModeScreen • AndroidAutoBottomBar)   │
└────────────────────────────┬─────────────────────────────┘
                             │ StateFlow & Events
┌────────────────────────────▼─────────────────────────────┐
│             Kotlin MVVM & Coroutines Engine              │
│ (MusicViewModel • Room SQLite WAL • AlbumArtCache L1/L2) │
└────────────────────────────┬─────────────────────────────┘
                             │ Direct JNI Bridge
┌────────────────────────────▼─────────────────────────────┐
│          Native C++17 Audio Engine (ARMv8 NEON)          │
│   ┌──────────────────────────────────────────────────┐   │
│   │ AudioPlayer.cpp • AudioDecoder.cpp (MediaNDK)    │   │
│   │ AudioEqualizer.cpp (5-Band IIR • Mud-Cut • HPF)  │   │
│   │ Crossfade Engine • Time Alignment • M-S Surround │   │
│   │ RingBuffer.h (Lock-Free SPSC FIFO)               │   │
│   │ OpenSLESPlayer.cpp (Hardware PCM Audio Queue)    │   │
│   └──────────────────────────────────────────────────┘   │
│   ┌──────────────────────────────────────────────────┐   │
│   │ Hardware Steering Wheel Key Event Interception   │   │
│   │ USB Auto-Mount BroadcastReceiver Integration     │   │
│   └──────────────────────────────────────────────────┘   │
└────────────────────────────┬─────────────────────────────┘
                             │ OpenSL ES Buffer Callbacks
┌────────────────────────────▼─────────────────────────────┐
│          Automotive Hardware Audio HAL / DSP             │
└──────────────────────────────────────────────────────────┘
```

- **UI Layer**: Jetpack Compose, Material 3, AndroidX Compose BOM, Kotlin Flow.
- **Audio Engine**: C++17, OpenSL ES, Android MediaNDK, JNI, NEON Auto-Vectorization.
- **Database & Storage**: Room SQLite (with `WRITE_AHEAD_LOGGING` enabled), Android MediaStore API.
- **Build System**: Android Gradle Plugin, CMake 3.22.1, R8 Full Mode Shrinking, Single-ABI `arm64-v8a` Packaging.
- **Automated Verification**: Integrated verification agent scripts (`audit_performance.sh`, `verify_release.sh`).

---

## 🚘 Compatible Automotive Platforms

This player is optimized for both factory-installed Android Automotive OS and aftermarket Android head units:

- **Automotive Infotainment Units**: Universal Android Automotive OS & aftermarket head units (ARM64 Cortex series).
- **Qualcomm Snapdragon Automotive Platforms**: SA6155P, SA8155P, SA8295P.
- **Rockchip Platforms**: RK3399, RK3588, PX6, PX5, PX30.
- **Allwinner / T3 / T5 / T8**: Quad-Core / Octa-Core Android 9 – Android 14 car stereos.
- **Universal Formats**: Double-DIN (10.1", 9", 7" touchscreens at 1280×720, 1024×600, and 1920×1080 resolutions).

---

## 📦 Build & Installation

### Option 1: Direct Download (Ready to Install)
Grab the pre-built, production-optimized APK from the releases page:
👉 **[Download Latest APK (`app-release.apk`)](https://github.com/aashikhameed/music-player-app/releases/latest)**

Install directly onto your vehicle's head unit via USB drive or ADB:
```bash
adb install -r app-release.apk
```

### Option 2: Build From Source

#### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- Android SDK 36 (minSdk 26, targetSdk 36)
- Android NDK (r25+ or cmake 3.22.1)
- JDK 17

#### Build Commands
```bash
# Clone the repository
git clone https://github.com/aashikhameed/music-player-app.git
cd music-player-app

# Run the automated performance audit & verification runbook
bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh

# Build debug APK
./gradlew assembleDebug

# Build production R8-optimized ARM64 APK (3.5 MB)
./gradlew assembleRelease
```

---

## 🧪 Automotive Performance Verification Suite

To maintain strict latency and memory compliance on low-spec hardware, the project includes automated verification agents:

```bash
# Run multi-file static architectural audit across all source files
bash .agents/skills/automotive-performance-optimizer/scripts/audit_performance.sh

# Run end-to-end release verification (Audit + Unit Tests + R8 Build + Binary Inspection)
bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh
```

Checks enforced automatically:
1. Zero inline `BorderStroke` or dynamic `RoundedCornerShape` instantiations in composables.
2. Flat solid colors replacing multi-stop linear gradients for Mali GPU fill-rate protection.
3. Event-driven C++ condition-variable decode loop (no `usleep` spin-polling).
4. Lock-free SPSC RingBuffer acquire/release memory order.
5. In-memory artwork cache strictly capped at 2–4 MB with 64×64 RGB_565 downscaling.
6. Target release APK strictly under 4.0 MB with single `arm64-v8a` ABI packaging.

---

## 🤝 Contributing

Contributions are welcome! Whether you are optimizing C++ audio routines, tuning Compose UI fill rates, or adding support for new car head units:

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/NewAutomotiveFeature`).
3. Verify compliance with `bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh`.
4. Commit your Changes (`git commit -m 'feat: Add support for automotive steering wheel controls'`).
5. Push to the Branch (`git push origin feature/NewAutomotiveFeature`).
6. Open a Pull Request.

---

## 📄 License

Distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for more information.

---

<div align="center">
  <b>Built with ❤️ for automotive audiophiles and car enthusiast developers.</b><br>
  <sub>If you find this project useful for your car headunit, please consider starring ⭐ the repository!</sub>
</div>
