#include "OpenSLESPlayer.h"
#include <android/log.h>
#include <cmath>
#include <cstring>

#define LOG_TAG "OpenSLESPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace audio {

SLObjectItf OpenSLESPlayer::sEngineObject = nullptr;
SLEngineItf OpenSLESPlayer::sEngine = nullptr;
SLObjectItf OpenSLESPlayer::sOutputMixObject = nullptr;
int OpenSLESPlayer::sEngineRefCount = 0;
std::mutex OpenSLESPlayer::sEngineMutex;

OpenSLESPlayer::OpenSLESPlayer(std::shared_ptr<RingBuffer> ringBuffer)
    : mRingBuffer(std::move(ringBuffer)) {
}

OpenSLESPlayer::~OpenSLESPlayer() {
    shutdown();
}

bool OpenSLESPlayer::init() {
    std::lock_guard<std::mutex> lock(sEngineMutex);
    if (sEngineObject != nullptr) {
        sEngineRefCount++;
        return true;
    }

    SLresult result = slCreateEngine(&sEngineObject, 0, nullptr, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("slCreateEngine failed: %d", result);
        return false;
    }

    result = (*sEngineObject)->Realize(sEngineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Realize engine failed: %d", result);
        (*sEngineObject)->Destroy(sEngineObject);
        sEngineObject = nullptr;
        return false;
    }

    result = (*sEngineObject)->GetInterface(sEngineObject, SL_IID_ENGINE, &sEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("GetInterface SLEngine failed: %d", result);
        (*sEngineObject)->Destroy(sEngineObject);
        sEngineObject = nullptr;
        return false;
    }

    result = (*sEngine)->CreateOutputMix(sEngine, &sOutputMixObject, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("CreateOutputMix failed: %d", result);
        (*sEngineObject)->Destroy(sEngineObject);
        sEngineObject = nullptr;
        return false;
    }

    result = (*sOutputMixObject)->Realize(sOutputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Realize OutputMix failed: %d", result);
        (*sOutputMixObject)->Destroy(sOutputMixObject);
        sOutputMixObject = nullptr;
        (*sEngineObject)->Destroy(sEngineObject);
        sEngineObject = nullptr;
        return false;
    }

    sEngineRefCount = 1;
    LOGI("OpenSL ES engine and output mix initialized successfully");
    return true;
}

void OpenSLESPlayer::shutdown() {
    stop();
    destroyPlayer();

    std::lock_guard<std::mutex> lock(sEngineMutex);
    if (sEngineRefCount > 0) {
        sEngineRefCount--;
    }
    if (sEngineRefCount == 0) {
        if (sOutputMixObject) {
            (*sOutputMixObject)->Destroy(sOutputMixObject);
            sOutputMixObject = nullptr;
        }

        if (sEngineObject) {
            (*sEngineObject)->Destroy(sEngineObject);
            sEngineObject = nullptr;
            sEngine = nullptr;
        }
        LOGI("OpenSL ES shared engine and output mix destroyed");
    }
}

void OpenSLESPlayer::destroyPlayer() {
    if (mPlayerObject) {
        (*mPlayerObject)->Destroy(mPlayerObject);
        mPlayerObject = nullptr;
        mPlayerPlay = nullptr;
        mBufferQueue = nullptr;
        mPlayerVolume = nullptr;
    }
}

static SLuint32 getOpenSLSamplingRate(int32_t sampleRate) {
    switch (sampleRate) {
        case 8000:  return SL_SAMPLINGRATE_8;
        case 11025: return SL_SAMPLINGRATE_11_025;
        case 16000: return SL_SAMPLINGRATE_16;
        case 22050: return SL_SAMPLINGRATE_22_05;
        case 24000: return SL_SAMPLINGRATE_24;
        case 32000: return SL_SAMPLINGRATE_32;
        case 44100: return SL_SAMPLINGRATE_44_1;
        case 48000: return SL_SAMPLINGRATE_48;
        case 64000: return SL_SAMPLINGRATE_64;
        case 88200: return SL_SAMPLINGRATE_88_2;
        case 96000: return SL_SAMPLINGRATE_96;
        default:
            return sampleRate * 1000; // OpenSL ES uses milliHertz
    }
}

bool OpenSLESPlayer::configureAudio(int32_t sampleRate, int32_t channelCount) {
    if (!sEngine) {
        if (!init()) return false;
    }

    if (mPlayerObject && mSampleRate == sampleRate && mChannelCount == channelCount) {
        return true;
    }

    destroyPlayer();
    mSampleRate = sampleRate;
    mChannelCount = channelCount;

    // 1. Data Source: Android Simple Buffer Queue
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
        SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
        NUM_QUEUE_BUFFERS
    };

    SLuint32 channelMask = (channelCount == 1) ? SL_SPEAKER_FRONT_CENTER :
                           (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT);

    SLDataFormat_PCM format_pcm = {
        SL_DATAFORMAT_PCM,
        static_cast<SLuint32>(channelCount),
        getOpenSLSamplingRate(sampleRate),
        SL_PCMSAMPLEFORMAT_FIXED_16,
        SL_PCMSAMPLEFORMAT_FIXED_16,
        channelMask,
        SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // 2. Data Sink: Output Mix
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, sOutputMixObject};
    SLDataSink audioSnk = {&loc_outmix, nullptr};

    // 3. Create Audio Player
    const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    SLresult result = (*sEngine)->CreateAudioPlayer(sEngine, &mPlayerObject, &audioSrc, &audioSnk, 2, ids, req);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("CreateAudioPlayer failed: %d", result);
        return false;
    }

    result = (*mPlayerObject)->Realize(mPlayerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Realize player failed: %d", result);
        destroyPlayer();
        return false;
    }

    result = (*mPlayerObject)->GetInterface(mPlayerObject, SL_IID_PLAY, &mPlayerPlay);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("GetInterface SLPlay failed: %d", result);
        destroyPlayer();
        return false;
    }

    result = (*mPlayerObject)->GetInterface(mPlayerObject, SL_IID_BUFFERQUEUE, &mBufferQueue);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("GetInterface SLBufferQueue failed: %d", result);
        destroyPlayer();
        return false;
    }

    result = (*mPlayerObject)->GetInterface(mPlayerObject, SL_IID_VOLUME, &mPlayerVolume);
    if (result != SL_RESULT_SUCCESS) {
        LOGW("GetInterface SLVolume not available: %d", result);
    }

    // Register callback
    result = (*mBufferQueue)->RegisterCallback(mBufferQueue, bufferQueueCallbackStatic, this);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("RegisterCallback failed: %d", result);
        destroyPlayer();
        return false;
    }

    setVolume(mVolume);
    LOGI("Audio player configured: %d Hz, %d channels", sampleRate, channelCount);
    return true;
}

void OpenSLESPlayer::bufferQueueCallbackStatic(SLAndroidSimpleBufferQueueItf bq, void* context) {
    auto* player = static_cast<OpenSLESPlayer*>(context);
    if (player) {
        player->onBufferQueueCallback(bq);
    }
}

void OpenSLESPlayer::onBufferQueueCallback(SLAndroidSimpleBufferQueueItf bq) {
    if (!mIsPlaying.load(std::memory_order_relaxed)) {
        return;
    }

    uint8_t* currentBuffer = mQueueBuffers[mCurrentBufferIndex];
    mCurrentBufferIndex = (mCurrentBufferIndex + 1) % NUM_QUEUE_BUFFERS;

    // Read PCM chunk from ring buffer
    size_t bytesRead = 0;
    if (mRingBuffer) {
        bytesRead = mRingBuffer->read(currentBuffer, QUEUE_BUFFER_SIZE);
    }

    if (bytesRead > 0 && mOnBufferConsumed) {
        mOnBufferConsumed(bytesRead);
    }

    if (bytesRead < QUEUE_BUFFER_SIZE) {
        // Fill remainder with silence to avoid click/pop artifacts on buffer starvation
        std::memset(currentBuffer + bytesRead, 0, QUEUE_BUFFER_SIZE - bytesRead);
    }

    // Process audio DSP equalizer & filter chain
    mEqualizer.process(reinterpret_cast<int16_t*>(currentBuffer),
                       QUEUE_BUFFER_SIZE / sizeof(int16_t),
                       mSampleRate,
                       mChannelCount);

    (*bq)->Enqueue(bq, currentBuffer, QUEUE_BUFFER_SIZE);
}

bool OpenSLESPlayer::play() {
    if (!mPlayerPlay || !mBufferQueue) return false;

    mIsPlaying.store(true, std::memory_order_release);
    primeQueue();

    SLresult result = (*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_PLAYING);
    return result == SL_RESULT_SUCCESS;
}

bool OpenSLESPlayer::pause() {
    mIsPlaying.store(false, std::memory_order_release);
    if (mPlayerPlay) {
        SLresult result = (*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_PAUSED);
        return result == SL_RESULT_SUCCESS;
    }
    return false;
}

bool OpenSLESPlayer::stop() {
    mIsPlaying.store(false, std::memory_order_release);
    if (mPlayerPlay) {
        (*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_STOPPED);
    }
    flush();
    return true;
}

void OpenSLESPlayer::flush() {
    if (mBufferQueue) {
        (*mBufferQueue)->Clear(mBufferQueue);
    }
    mCurrentBufferIndex = 0;
    mEqualizer.reset();
}

void OpenSLESPlayer::primeQueue() {
    if (!mPlayerPlay || !mBufferQueue) return;
    if (!mIsPlaying.load(std::memory_order_relaxed)) return;

    SLAndroidSimpleBufferQueueState state{};
    SLresult res = (*mBufferQueue)->GetState(mBufferQueue, &state);
    if (res != SL_RESULT_SUCCESS) return;

    // Only enqueue if the hardware buffer queue has run dry or was flushed
    if (state.count == 0) {
        for (size_t i = 0; i < NUM_QUEUE_BUFFERS; ++i) {
            uint8_t* currentBuffer = mQueueBuffers[mCurrentBufferIndex];
            mCurrentBufferIndex = (mCurrentBufferIndex + 1) % NUM_QUEUE_BUFFERS;

            size_t bytesRead = 0;
            if (mRingBuffer) {
                bytesRead = mRingBuffer->read(currentBuffer, QUEUE_BUFFER_SIZE);
            }
            if (bytesRead > 0 && mOnBufferConsumed) {
                mOnBufferConsumed(bytesRead);
            }
            if (bytesRead < QUEUE_BUFFER_SIZE) {
                std::memset(currentBuffer + bytesRead, 0, QUEUE_BUFFER_SIZE - bytesRead);
            }

            // Process audio DSP equalizer & filter chain
            mEqualizer.process(reinterpret_cast<int16_t*>(currentBuffer),
                               QUEUE_BUFFER_SIZE / sizeof(int16_t),
                               mSampleRate,
                               mChannelCount);

            (*mBufferQueue)->Enqueue(mBufferQueue, currentBuffer, QUEUE_BUFFER_SIZE);
        }
        (*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_PLAYING);
    }
}

void OpenSLESPlayer::setVolume(float linearVolume) {
    mVolume = std::max(0.0f, std::min(1.0f, linearVolume));
    if (!mPlayerVolume) return;

    // Linear volume to OpenSL ES millibels: 0.0 -> SL_MILLIBEL_MIN (-9600), 1.0 -> 0 mB
    SLmillibel millibel;
    if (mVolume <= 0.0001f) {
        millibel = SL_MILLIBEL_MIN;
    } else {
        millibel = static_cast<SLmillibel>(2000.0f * std::log10(mVolume));
        if (millibel < SL_MILLIBEL_MIN) millibel = SL_MILLIBEL_MIN;
        if (millibel > 0) millibel = 0;
    }

    (*mPlayerVolume)->SetVolumeLevel(mPlayerVolume, millibel);
}

} // namespace audio
