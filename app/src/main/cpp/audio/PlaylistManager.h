#ifndef MUSIC_PLAYER_PLAYLIST_MANAGER_H
#define MUSIC_PLAYER_PLAYLIST_MANAGER_H

#include "Types.h"
#include <string>
#include <vector>
#include <mutex>
#include <memory>
#include <functional>

namespace audio {

/**
 * Native Playlist and Library Manager.
 * Scans directories, extracts audio metadata (MP3, M4A, FLAC, WAV, OGG),
 * and handles playback order (shuffle, repeat).
 */
class PlaylistManager {
public:
    PlaylistManager();
    ~PlaylistManager() = default;

    // Library Scanning
    std::vector<TrackMetadata> scanDirectory(
        const std::string& directoryPath,
        const std::function<void(int loaded, int total)>& progressCallback = nullptr
    );

    static TrackMetadata extractMetadata(const std::string& filePath);

    // Playlist Control
    void setPlaylist(const std::vector<TrackMetadata>& tracks, int startIndex = 0);
    void addTrack(const TrackMetadata& track);
    void clear();

    int getCurrentIndex() const;
    TrackMetadata getCurrentTrack() const;
    size_t getTrackCount() const;

    bool hasNext() const;
    bool hasPrevious() const;
    TrackMetadata getNextTrack();
    TrackMetadata getPreviousTrack();

    void setShuffleMode(bool enabled);
    bool isShuffleMode() const { return mShuffleEnabled; }

    void setRepeatMode(RepeatMode mode);
    RepeatMode getRepeatMode() const { return mRepeatMode; }

private:
    void rebuildPlaybackOrder();
    static bool isAudioFile(const std::string& filePath);

    mutable std::mutex mMutex;
    std::vector<TrackMetadata> mTracks;
    std::vector<size_t> mPlaybackOrder;
    size_t mCurrentOrderIndex = 0;

    bool mShuffleEnabled = false;
    RepeatMode mRepeatMode = RepeatMode::OFF;
};

} // namespace audio

#endif // MUSIC_PLAYER_PLAYLIST_MANAGER_H
