#ifndef MUSIC_PLAYER_OPENSL_ES_PLAYER_H
#define MUSIC_PLAYER_OPENSL_ES_PLAYER_H

#include "RingBuffer.h"
#include "Types.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <memory>
#include <atomic>

namespace audio {

/**
 * OpenSL ES 1.1 Low-Latency Automotive Audio Output.
 * Feeds decoded PCM from RingBuffer into hardware audio output queue via non-blocking callbacks.
 */
class OpenSLESPlayer {
public:
    explicit OpenSLESPlayer(std::shared_ptr<RingBuffer> ringBuffer);
    ~OpenSLESPlayer();

    bool init();
    void shutdown();

    bool configureAudio(int32_t sampleRate, int32_t channelCount);
    bool play();
    bool pause();
    bool stop();
    void flush();

    void setVolume(float linearVolume); // 0.0f to 1.0f
    float getVolume() const { return mVolume; }

    bool isPlaying() const { return mIsPlaying.load(std::memory_order_relaxed); }

    // Internal buffer queue callback method
    void onBufferQueueCallback(SLAndroidSimpleBufferQueueItf bq);

private:
    void destroyPlayer();
    static void bufferQueueCallbackStatic(SLAndroidSimpleBufferQueueItf bq, void* context);

    std::shared_ptr<RingBuffer> mRingBuffer;

    // OpenSL ES Objects
    SLObjectItf mEngineObject = nullptr;
    SLEngineItf mEngine = nullptr;
    SLObjectItf mOutputMixObject = nullptr;

    SLObjectItf mPlayerObject = nullptr;
    SLPlayItf mPlayerPlay = nullptr;
    SLAndroidSimpleBufferQueueItf mBufferQueue = nullptr;
    SLVolumeItf mPlayerVolume = nullptr;

    int32_t mSampleRate = 44100;
    int32_t mChannelCount = 2;
    float mVolume = 1.0f;
    std::atomic<bool> mIsPlaying{false};

    // Double buffering for OpenSL ES queue chunks (4096 bytes per chunk)
    static constexpr size_t QUEUE_BUFFER_SIZE = 4096;
    static constexpr size_t NUM_QUEUE_BUFFERS = 2;
    uint8_t mQueueBuffers[NUM_QUEUE_BUFFERS][QUEUE_BUFFER_SIZE]{};
    size_t mCurrentBufferIndex = 0;
};

} // namespace audio

#endif // MUSIC_PLAYER_OPENSL_ES_PLAYER_H
