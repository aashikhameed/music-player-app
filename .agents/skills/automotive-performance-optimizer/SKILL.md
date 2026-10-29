---
name: automotive-performance-optimizer
description: >-
  Audits, profiles, optimizes, and verifies performance for the Android Automotive Music Player on low-spec hardware (Blaupunkt Santa Rosa 985, Cortex-A53, Mali GPU, eMMC storage). Use whenever the user asks to look for optimizations, audit performance bottlenecks, check UI frame rate drops, or build the release APK.
---

# Automotive Performance Optimizer

This skill provides an automated audit, profiling checklist, and optimization workflow tailored for the **Blaupunkt Santa Rosa 985 Android Automotive Headunit** (1280×720 @ 160 dpi, ARM Cortex-A53 SoC, Mali GPU, 1–2 GB RAM, slow eMMC flash storage).

---

## Workflow Steps

### Step 1: Run the Comprehensive Multi-File Performance Audit
Run the automated source file inspection script to audit every file across Compose UI, C++ OpenSL ES, Room SQLite, JNI, and Gradle packaging:
```bash
bash .agents/skills/automotive-performance-optimizer/scripts/audit_performance.sh
```

The script audits all files for:
1. **Compose UI Layer (`app/src/main/java/**/ui/*.kt`)**:
   - Zero inline `BorderStroke(...)` allocations across all UI files (must use direct `border()` overload).
   - Zero dynamic `RoundedCornerShape(...)` instantiations inside `@Composable` functions (must use top-level constants).
   - Zero multi-stop `Brush.linearGradient` brushes on cards, docks, and capsules (Mali GPU fragment fill rate).
2. **Native C++ & JNI Layer (`app/src/main/cpp/**/*.{cpp,h}`)**:
   - Zero blocking `usleep` or spin-polling in audio decode threads (`mDecoderCv.wait(...)` verified).
   - Hardware OpenSL ES PCM buffer consumed callback queue replenishment.
   - SPSC `RingBuffer` lock-free acquire/release atomic memory barriers.
   - Compiler flags in `CMakeLists.txt` (`-O3`, `-flto`, `-ftree-vectorize`, `-ffast-math`).
3. **Database & Storage Layer (`app/src/main/java/**/{data,repository}/*.kt`)**:
   - SQLite Write-Ahead Logging (`WRITE_AHEAD_LOGGING`) enabled in `RoomDatabase.kt`.
   - B-tree indexing on queried sorting fields (`Index(value = ["title"])` on `Song.kt`).
   - Exact `ArrayList<Song>` pre-sizing from MediaStore cursor count.
4. **Memory, Image & Disk Cache (`app/src/main/java/**/{cache,utils}/*.kt`)**:
   - 64×64 RGB_565 thumbnail memory budget (8 KB per bitmap, 75% memory drop).
   - Hardware NEON JPEG compression instead of software WebP.
   - Strict in-memory cache ceiling (capped at 2–4 MB).
   - 0-byte `.noart` disk tombstone markers bypassing `MediaMetadataRetriever`.
5. **Service & Lifecycle Layer (`app/src/main/java/**/{service,nativeaudio}/*.kt`)**:
   - Zero-allocation JNI position callback (no `mainHandler.post` for high-frequency position updates).
   - Complete root window overdraw elimination via `window.setBackgroundDrawable(null)` in `MainActivity.kt`.
   - Epoch-based time formatting eliminating `Calendar.getInstance()` allocations.
6. **Packaging & R8 Minification (`app/build.gradle.kts`, `AndroidManifest.xml`)**:
   - Single-ABI ARM64 filter (`arm64-v8a` only, saving ~2 MB unused .so binaries).
   - R8 full-mode code shrinking and resource stripping enabled.


---

### Step 2: Audio Engine Core Verification
Inspect native C++ and JNI components:
- **`RingBuffer.h`**: Ensure SPSC read/write memory barriers use `std::memory_order_acquire` and `std::memory_order_release`.
- **`AudioPlayer.cpp`**: Verify the decode loop uses `mDecoderCv.wait(...)` driven by OpenSL ES buffer completion rather than `usleep(...)` spin-polling.
- **`NativeAudioBridge.kt`**: Verify `onPositionChanged` delivers updates directly to Kotlin `StateFlow` without posting `Runnable` lambdas to `mainHandler`.

---

### Step 3: Compose Recomposition Scoping
Verify high-frequency state isolation:
- High-frequency state flows (`positionFlow`, `durationFlow`, `progressFlow`) emitting at 250ms intervals must be read **only** in leaf composables (`BottomBarTimeCounter`, `BottomBarSeekBar`).
- Root lists and cards (`MusicListScreen`, `MediaLibraryContent`, `SongCard`) must not subscribe to progress flows.
- Artwork loading must be guarded with `AlbumArtCache.hasNoArt(path)` to prevent spawning coroutines for tracks without embedded art.

---

### Step 4: Verification & Build
After making any performance fixes, execute the verification suite:
```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```
Ensure:
1. All unit tests pass cleanly.
2. The release APK is generated at `app/build/outputs/apk/release/app-release.apk`.
3. The APK file size is under 4.0 MB.
