#ifndef MUSIC_PLAYER_AUDIO_PLAYER_H
#define MUSIC_PLAYER_AUDIO_PLAYER_H

#include "Types.h"
#include "RingBuffer.h"
#include "AudioDecoder.h"
#include "OpenSLESPlayer.h"
#include "PlaylistManager.h"
#include <memory>
#include <string>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <vector>

namespace audio {

// Callback listener structure
struct AudioPlayerListener {
    std::function<void(PlaybackState state)> onPlaybackStateChanged;
    std::function<void(int64_t positionMs)> onPositionChanged;
    std::function<void(int64_t durationMs)> onDurationUpdated;
    std::function<void()> onTrackEnded;
    std::function<void(int32_t errorCode, const std::string& message)> onError;
};

/**
 * Master Native Audio Engine.
 * Coordinates decoding thread, lock-free ring buffer, and OpenSL ES audio output.
 */
class AudioPlayer {
public:
    static AudioPlayer& getInstance();

    AudioPlayer();
    ~AudioPlayer();

    ResultCode init();
    void shutdown();

    ResultCode playTrack(const std::string& filePath, int64_t startPositionMs = 0);
    ResultCode pause();
    ResultCode resume();
    ResultCode stop();
    ResultCode seek(int64_t positionMs);

    void setVolume(float volume);
    float getVolume() const;

    int64_t getCurrentPosition() const;
    int64_t getDuration() const;
    PlaybackState getState() const;

    void setListener(AudioPlayerListener listener);
    PlaylistManager& getPlaylistManager() { return mPlaylistManager; }

private:
    void decoderThreadLoop();
    void setState(PlaybackState state);
    void notifyError(ResultCode code, const std::string& message);

    std::shared_ptr<RingBuffer> mRingBuffer;
    std::unique_ptr<AudioDecoder> mDecoder;
    std::unique_ptr<OpenSLESPlayer> mOutputPlayer;
    PlaylistManager mPlaylistManager;

    std::atomic<PlaybackState> mState{PlaybackState::IDLE};
    std::atomic<int64_t> mCurrentPositionMs{0};
    std::atomic<int64_t> mDurationMs{0};
    std::atomic<bool> mIsSeeking{false};
    std::atomic<int64_t> mPendingSeekMs{-1};

    std::thread mDecoderThread;
    std::atomic<bool> mDecoderRunning{false};
    std::mutex mDecoderMutex;
    std::condition_variable mDecoderCv;

    AudioPlayerListener mListener;
    std::mutex mListenerMutex;
};

} // namespace audio

#endif // MUSIC_PLAYER_AUDIO_PLAYER_H
