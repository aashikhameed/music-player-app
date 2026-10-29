---
name: automotive-performance-verifier
description: >-
  Verifies, tests, and validates that all performance optimizations, memory budgets, and automotive hardware constraints are fully met for the Android Automotive Music Player on low-spec hardware (Blaupunkt Santa Rosa 985). Runs the automated verification runbook, static architecture audits, unit test suite, and release binary validation.
---

# Automotive Performance Verifier

This skill acts as an automated verification gatekeeper for the **Blaupunkt Santa Rosa 985 Android Automotive Headunit** (1280×720 @ 160 dpi, ARM Cortex-A53 SoC, Mali GPU, 1–2 GB RAM, slow eMMC flash storage).

It verifies that all source files comply with automotive performance constraints, that no regressions exist, that unit tests pass, and that the final release APK adheres to the strict size budget (< 4.0 MB) and architecture constraints (single `arm64-v8a` ABI).

---

## Verification Runbook

Execute the all-in-one verification agent script:
```bash
bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh
```

### Verification Gates Enforced:

1. **Gate 1: Static Multi-File Architecture Audit (`audit_performance.sh`)**
   - **UI Layer**:
     - No dynamic `BorderStroke(...)` allocations across any composables.
     - All `RoundedCornerShape(...)` instances pre-allocated as top-level `private val` constants.
     - No multi-stop `Brush.linearGradient` on repeated list items, cards, or docks (flat `SolidColor` enforced).
     - Direct Canvas drawing for high-frequency dynamic elements (`LiveAnimatedEqualizer` in `SongCard.kt`).
   - **Native C++ & JNI Layer**:
     - Zero `usleep` spin-polling in decoder loops (`mDecoderCv.wait(...)` event wait verified).
     - Hardware OpenSL ES PCM buffer consumed replenishment callbacks.
     - Single-Producer Single-Consumer (SPSC) lock-free `RingBuffer` with acquire/release memory semantics.
     - Compiler flags (`-O3`, `-flto`, `-ftree-vectorize`, `-ffast-math`).
   - **Database & Storage Layer**:
     - Room SQLite Write-Ahead Logging (`WRITE_AHEAD_LOGGING`) enabled for concurrent flash reads/writes.
     - Title column B-tree indexing in `Song.kt` for NOCASE sorting.
     - Pre-sized `ArrayList` allocations for scan results and thumbnail preloads.
   - **Memory & Bitmap Cache**:
     - 64×64 RGB_565 downscaling (8 KB memory footprint per thumbnail).
     - Hardware NEON-accelerated JPEG compression.
     - In-memory cache capped at 2–4 MB.
     - Fast 0-byte `.noart` disk tombstones.
     - Direct $O(1)$ cache hit lookup without snapshot copying.
   - **Service & System**:
     - Zero-allocation JNI position callbacks directly to atomic StateFlows.
     - Root window background stripped (`window.setBackgroundDrawable(null)`) for 0px display overdraw.

2. **Gate 2: Unit Testing (`./gradlew testDebugUnitTest`)**
   - 100% test pass rate with zero failures or skipped assertions.

3. **Gate 3: Release Build Compilation (`./gradlew assembleRelease`)**
   - R8 full-mode shrinking, dead code elimination, and resource stripping succeed.

4. **Gate 4: Binary Inspection (`app-release.apk`)**
   - APK size strictly under 4.0 MB.
   - Single ABI architecture: `lib/arm64-v8a/` only (no x86, x86_64, or armeabi-v7a bloat).
   - Native library `libaudio-player.so` packaged and verified.

---

## Result Interpretation
- **100% PASS (0 Regressions)**: Codebase is verified production-ready for Blaupunkt Santa Rosa 985.
- **Any FAIL**: Must resolve before deployment or release.
