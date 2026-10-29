# Android Automotive Performance & Optimization Agent Rules
Target Hardware: Automotive Headunit (1280×720 @ 160 dpi, ARM64 Cortex-A53 SoC, Mali GPU, 1–2 GB RAM, slow eMMC flash storage).

Whenever developing, optimizing, or reviewing code in this repository, the agent MUST enforce the following constraints:

---

## 1. Jetpack Compose & GPU Fill Rate (Mali GPU)
- **Flat Solid Colors Over Gradients**: The Mali GPU has limited fragment shader fill rate at 1280×720. Never use multi-stop `Brush.linearGradient` on repeated list items, cards, or docks. Use `SolidColor` or flat hex values.
- **Precompute Shapes & Borders**: Never instantiate `RoundedCornerShape(...)` or `BorderStroke(...)` inside `@Composable` functions. Pre-allocate all shapes as top-level `private val` constants.
- **Eliminate Redundant Clipping**: Never stack `.clip(shape)` right before `.background(..., shape = shape)`. Only use `.clip()` when child content overflows the bounds.
- **Root Window Overdraw Elimination**: Always ensure `window.setBackgroundDrawable(null)` is called in `MainActivity.onCreate()` before `setContent()`.
- **Recomposition Scoping**: High-frequency playback ticks (e.g., `positionMs`, `durationMs`, `progress`) must be read strictly inside leaf composables (`BottomBarTimeCounter`, `BottomBarSeekBar`). Never read high-frequency flows at the root `MusicListScreen` or `MediaLibraryContent` level.
- **Direct Canvas Drawing**: Avoid recomposition loops in Canvas components. Measure geometry in `Modifier.onSizeChanged` rather than during the draw phase.

---

## 2. C++ & JNI Native Audio Engine
- **Event-Driven Decoder Loop**: The decode thread must never spin-poll with sleeps (e.g. `usleep(5000)`). Use `std::condition_variable` (`mDecoderCv.wait(...)`) driven by buffer-consumed callbacks from the OpenSL ES hardware queue.
- **Lock-Free RingBuffer**: Maintain Single-Producer Single-Consumer (SPSC) lockless FIFO with explicit `std::memory_order_acquire` and `std::memory_order_release` semantics.
- **JNI Attachment Lifecycle**: Attach native threads once on startup via `AttachCurrentThreadAsDaemon` and detach cleanly on exit. Never attach/detach per audio frame.
- **JNI Callback Throttling**: Throttle playback position callbacks to ~250ms. Never allocate `Runnable` objects on the Android Main Looper for state updates that can update atomic `StateFlow` values directly.

---

## 3. Memory, Bitmap & Disk Caching
- **Thumbnail Memory Footprint**: Never load full-resolution album artwork into RAM. Decode artwork to 64×64 pixels in `Bitmap.Config.RGB_565` (8 KB per bitmap, 75% memory drop compared to ARGB_8888).
- **Hardware-Accelerated Disk Cache**: Compress cached artwork with hardware NEON-accelerated JPEG (`quality = 75`), not software WebP.
- **Cache Size Ceiling**: Cap in-memory artwork cache at 3 MB (`AlbumArtCache`).
- **Disk Tombstone Markers**: Write a 0-byte `.noart` marker file on disk when a song has no embedded artwork to permanently bypass `MediaMetadataRetriever` on future launches.
- **Background Thread Priority**: Run artwork background preloading at `THREAD_PRIORITY_LOWEST` with a startup delay to avoid competing with UI initialization.

---

## 4. SQLite & Room Database
- **Write-Ahead Logging (WAL)**: Always enable `setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)` in `Room.databaseBuilder`. This allows concurrent non-blocking reads while writing on slow eMMC automotive flash.
- **Indexed Search Columns**: Ensure queried columns (e.g. `title` used in `ORDER BY title COLLATE NOCASE ASC`) are indexed in `@Entity(indices = [...])`.
- **Pre-sized Collections**: Pre-size `ArrayList<Song>` using `cursor.count` to prevent reallocation and copying during database scans.

---

## 5. Verification Runbook
After any performance modifications, always execute the automated verification suite:
```bash
bash .agents/skills/automotive-performance-verifier/scripts/verify_release.sh
```
This automated gatekeeper enforces:
1. Multi-file static performance audit across all source files (`audit_performance.sh`).
2. `./gradlew testDebugUnitTest` must pass with zero failures.
3. `./gradlew assembleRelease` must succeed with R8 full-mode shrinking enabled.
4. Target APK must remain an ARM64-only binary (`arm64-v8a`) under 4 MB.
