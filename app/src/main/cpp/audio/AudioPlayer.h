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
    std::function<void()> onThreadStarted;
    std::function<void()> onThreadExiting;
};

struct TrackSession {
    std::string filePath;
    std::shared_ptr<RingBuffer> ringBuffer;
    std::unique_ptr<AudioDecoder> decoder;
    std::unique_ptr<OpenSLESPlayer> outputPlayer;

    std::thread decoderThread;
    std::atomic<bool> decoderRunning{false};
    std::mutex decoderMutex;
    std::condition_variable decoderCv;

    std::atomic<PlaybackState> state{PlaybackState::IDLE};
    std::atomic<int64_t> currentPositionMs{0};
    std::atomic<int64_t> durationMs{0};
    std::atomic<int64_t> renderedBytes{0};
    std::atomic<int64_t> startPositionMs{0};
    std::atomic<int32_t> sampleRate{44100};
    std::atomic<int32_t> channels{2};
    std::atomic<bool> isSeeking{false};
    std::atomic<int64_t> pendingSeekMs{-1};

    int64_t getCurrentPosition() const {
        int64_t baseMs = startPositionMs.load(std::memory_order_relaxed);
        int64_t bytes = renderedBytes.load(std::memory_order_relaxed);
        int32_t sr = sampleRate.load(std::memory_order_relaxed);
        int32_t ch = channels.load(std::memory_order_relaxed);
        if (sr <= 0) sr = 44100;
        if (ch <= 0) ch = 2;
        int64_t playedMs = baseMs + (bytes * 1000LL) / (static_cast<int64_t>(sr) * ch * 2);
        int64_t dur = durationMs.load(std::memory_order_relaxed);
        if (dur > 0 && playedMs > dur) playedMs = dur;
        return playedMs;
    }
};

/**
 * Master Native Audio Engine.
 * Supports seamless equal-power crossfade between tracks on low-spec automotive hardware.
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

    void setCrossfadeDuration(float seconds);
    float getCrossfadeDuration() const;

    int64_t getCurrentPosition() const;
    int64_t getDuration() const;
    PlaybackState getState() const;

    void setListener(AudioPlayerListener listener);
    PlaylistManager& getPlaylistManager() { return mPlaylistManager; }
    AudioEqualizer& getEqualizer() { return mEqualizer; }
    void syncEqualizer();

private:
    void decoderThreadLoop(TrackSession* session);
    void startFaderThread(float durationSec);
    void stopFaderThread();
    void stopTrackSession(std::unique_ptr<TrackSession>& session);
    void setState(PlaybackState state);
    void notifyError(ResultCode code, const std::string& message);

    PlaylistManager mPlaylistManager;
    AudioEqualizer mEqualizer;

    std::unique_ptr<TrackSession> mCurrentTrack;
    std::unique_ptr<TrackSession> mFadingTrack;
    mutable std::mutex mTrackMutex;

    std::thread mFaderThread;
    std::atomic<bool> mFaderRunning{false};
    std::mutex mFaderMutex;
    std::condition_variable mFaderCv;

    std::atomic<PlaybackState> mState{PlaybackState::IDLE};
    std::atomic<int64_t> mCurrentPositionMs{0};
    std::atomic<float> mMasterVolume{1.0f};
    std::atomic<float> mCrossfadeDurationSec{3.0f};

    AudioPlayerListener mListener;
    std::mutex mListenerMutex;
};

} // namespace audio

#endif // MUSIC_PLAYER_AUDIO_PLAYER_H
