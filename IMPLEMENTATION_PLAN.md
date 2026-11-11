# Mobile UI vs v0.3.2: parity plan

v0.3.2 portrait = Material tabs + a "More" hub. Current portrait = Apple-Music liquid glass (Music / Library / Search). Landscape/headunit is unchanged.

## Done
- Single liquid capsule nav (Music, Library, Search), draggable lens indicator, canonical press-scale, stale-callback fix.
- Radio tab removed; radio genres/moods live in Search ("Radio & Genres") with 256px artwork and a favorite heart (persisted, favorites sort first).
- Genre drill-down inside Search; system Back closes it; empty/offline state added.

## Parity work: done and verified on emulator
- Library rows now: Favorites, Artists, Tracks, Albums, Folders, Genres (Genres opens Search's radio genres). Albums/Artists/Folders open real lists; Tracks/Favorites/folder open song lists.
- Equalizer and Drive Mode reachable from Settings (full-screen, Back closes).

## Also done
- Now Playing queue button opens the Playback Queue; lyrics button opens full-screen lyrics (both verified, Back closes).
- Genre screens now render Featured playlists and Albums shelves (they were parsed but never shown, so every genre looked empty).

## Still open
- Online stream download and sleep timer have no entry in the new UI.
- Now Playing shows hard-coded "iPhone > AirPods Max" and "LOSSLESS ALAC 24-bit/48kHz" text, and the AirPlay sheet lists fake devices.
- Genre shelves are not split by title, so all shelves land in one long "Featured playlists" row.

## Verification
`verify_release.sh`: all gates passed (APK 4.05 MB, arm64-v8a only).
