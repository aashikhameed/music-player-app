#include "AudioPlayer.h"
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>
#include <chrono>
#include <vector>
#include <cmath>

#define LOG_TAG "NativeAudioPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace audio {

static inline int16_t clamp16(int32_t sample) {
    if (sample > 32767) return 32767;
    if (sample < -32768) return -32768;
    return static_cast<int16_t>(sample);
}

AudioPlayer& AudioPlayer::getInstance() {
    static AudioPlayer instance;
    return instance;
}

AudioPlayer::AudioPlayer()
    : mRingBuffer(std::make_shared<RingBuffer>(96 * 1024)),
      mOutputPlayer(std::make_unique<OpenSLESPlayer>(mRingBuffer)) {
    mOutputPlayer->setOnBufferConsumedCallback([this](size_t bytesConsumed) {
        mRenderedBytes.fetch_add(bytesConsumed, std::memory_order_relaxed);
        mDecoderCv.notify_one();
    });
}

AudioPlayer::~AudioPlayer() {
    shutdown();
}

ResultCode AudioPlayer::init() {
    if (!mOutputPlayer->init()) {
        LOGE("Failed to initialize OpenSL ES output");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }
    setState(PlaybackState::INITIALIZED);
    LOGI("AudioPlayer initialized successfully");
    return ResultCode::OK;
}

void AudioPlayer::shutdown() {
    stop();
    mOutputPlayer->shutdown();
    setState(PlaybackState::IDLE);
}

void AudioPlayer::setState(PlaybackState state) {
    mState.store(state, std::memory_order_relaxed);
    mDecoderCv.notify_all();
    std::lock_guard<std::mutex> lock(mListenerMutex);
    if (mListener.onPlaybackStateChanged) {
        mListener.onPlaybackStateChanged(state);
    }
}

void AudioPlayer::notifyError(ResultCode code, const std::string& message) {
    setState(PlaybackState::ERROR);
    std::lock_guard<std::mutex> lock(mListenerMutex);
    if (mListener.onError) {
        mListener.onError(static_cast<int32_t>(code), message);
    }
}

void AudioPlayer::setCrossfadeDuration(float seconds) {
    mCrossfadeDurationSec.store(std::max(0.0f, seconds), std::memory_order_release);
    LOGI("Crossfade duration set to %.1f seconds", seconds);
}

float AudioPlayer::getCrossfadeDuration() const {
    return mCrossfadeDurationSec.load(std::memory_order_relaxed);
}

ResultCode AudioPlayer::playTrack(const std::string& filePath, int64_t startPositionMs, bool isAutoCrossfade) {
    LOGI("playTrack requested: %s (start: %lld ms, autoCrossfade: %d)",
         filePath.c_str(), (long long)startPositionMs, isAutoCrossfade ? 1 : 0);

    std::lock_guard<std::mutex> playLock(mPlayMutex);

    float crossfadeSec = 0.0f;
    if (isAutoCrossfade) {
        // Natural end-of-track transition: use user-configured duration (default 3.0s)
        crossfadeSec = mCrossfadeDurationSec.load(std::memory_order_acquire);
    } else if (mState.load(std::memory_order_relaxed) == PlaybackState::PLAYING && startPositionMs == 0) {
        // Manual track switch (Next/Prev / user tap): fast 120ms anti-pop micro-fade (Apple CarPlay behavior)
        crossfadeSec = 0.12f;
    }

    bool shouldCrossfade = (crossfadeSec > 0.0f) &&
                           (startPositionMs == 0) &&
                           (mDecoder != nullptr) &&
                           (mState.load(std::memory_order_relaxed) == PlaybackState::PLAYING);

    mActiveCrossfadeDurationSec.store(shouldCrossfade ? crossfadeSec : 0.0f, std::memory_order_release);

    // Stop and join the decoder thread before swapping decoders
    mDecoderRunning.store(false, std::memory_order_release);
    mDecoderCv.notify_all();
    if (mDecoderThread.joinable()) {
        mDecoderThread.join();
    }

    std::unique_ptr<AudioDecoder> oldDecoder;
    if (shouldCrossfade) {
        oldDecoder = std::move(mDecoder);
    } else {
        if (mFadingDecoder) {
            mFadingDecoder->close();
            mFadingDecoder.reset();
        }
        if (mDecoder) {
            mDecoder->close();
            mDecoder.reset();
        }
        mRingBuffer->reset();
        mOutputPlayer->flush();
    }

    // Open new track decoder
    auto newDecoder = AudioDecoder::create(filePath);
    if (!newDecoder || !newDecoder->open(filePath)) {
        LOGE("Could not open file: %s", filePath.c_str());
        if (oldDecoder) {
            oldDecoder->close();
        }
        notifyError(ResultCode::ERROR_UNSUPPORTED_FORMAT, "Cannot decode file format");
        return ResultCode::ERROR_UNSUPPORTED_FORMAT;
    }

    const AudioFormat& format = newDecoder->getFormat();

    // Verify sample rate and channel compatibility for crossfading
    if (oldDecoder) {
        const AudioFormat& oldFormat = oldDecoder->getFormat();
        if (oldFormat.sampleRate != format.sampleRate || oldFormat.channelCount != format.channelCount) {
            oldDecoder->close();
            oldDecoder.reset();
            mRingBuffer->reset();
            mOutputPlayer->flush();
        }
    }

    if (mFadingDecoder) {
        mFadingDecoder->close();
        mFadingDecoder.reset();
    }
    mFadingDecoder = std::move(oldDecoder);
    mDecoder = std::move(newDecoder);

    mDurationMs.store(format.durationMs, std::memory_order_relaxed);
    mStartPositionMs.store(startPositionMs, std::memory_order_relaxed);
    mRenderedBytes.store(0, std::memory_order_relaxed);
    mSampleRate.store(format.sampleRate, std::memory_order_relaxed);
    mChannels.store(format.channelCount, std::memory_order_relaxed);
    mCurrentPositionMs.store(startPositionMs, std::memory_order_relaxed);

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onDurationUpdated) {
            mListener.onDurationUpdated(format.durationMs);
        }
        if (mListener.onPositionChanged) {
            mListener.onPositionChanged(startPositionMs);
        }
    }

    if (!mOutputPlayer->configureAudio(format.sampleRate, format.channelCount)) {
        LOGE("Failed to configure OpenSL ES output for %d Hz, %d ch", format.sampleRate, format.channelCount);
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Audio output configuration failed");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    if (startPositionMs > 0) {
        mDecoder->seekTo(startPositionMs);
    }

    setState(PlaybackState::PLAYING);

    // Start decoder background thread
    mDecoderRunning.store(true, std::memory_order_release);
    mDecoderThread = std::thread(&AudioPlayer::decoderThreadLoop, this);
    pthread_setname_np(mDecoderThread.native_handle(), "AudioDecoder");

    // Wait until at least 8KB of PCM is pre-buffered or EOF reached via event-driven CV
    {
        std::unique_lock<std::mutex> lock(mDecoderMutex);
        mDecoderCv.wait_for(lock, std::chrono::milliseconds(150), [this] {
            return mRingBuffer->getAvailableRead() >= 8 * 1024 || (mDecoder && mDecoder->isEof());
        });
    }

    if (!mOutputPlayer->play()) {
        LOGE("OpenSLESPlayer::play failed");
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Cannot start OpenSL ES playback");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    return ResultCode::OK;
}

ResultCode AudioPlayer::pause() {
    std::lock_guard<std::mutex> playLock(mPlayMutex);
    if (mState.load(std::memory_order_relaxed) != PlaybackState::PLAYING) {
        return ResultCode::OK;
    }

    mOutputPlayer->pause();
    setState(PlaybackState::PAUSED);
    return ResultCode::OK;
}

ResultCode AudioPlayer::resume() {
    std::lock_guard<std::mutex> playLock(mPlayMutex);
    if (mState.load(std::memory_order_relaxed) != PlaybackState::PAUSED) {
        return ResultCode::OK;
    }

    if (mOutputPlayer->play()) {
        setState(PlaybackState::PLAYING);
        mDecoderCv.notify_one();
        return ResultCode::OK;
    }
    return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
}

ResultCode AudioPlayer::stop() {
    std::lock_guard<std::mutex> playLock(mPlayMutex);

    mOutputPlayer->stop();

    mDecoderRunning.store(false, std::memory_order_release);
    mDecoderCv.notify_all();
    if (mDecoderThread.joinable()) {
        mDecoderThread.join();
    }

    if (mFadingDecoder) {
        mFadingDecoder->close();
        mFadingDecoder.reset();
    }
    if (mDecoder) {
        mDecoder->close();
        mDecoder.reset();
    }
    mRingBuffer->reset();

    mCurrentPositionMs.store(0, std::memory_order_relaxed);
    setState(PlaybackState::STOPPED);
    return ResultCode::OK;
}

ResultCode AudioPlayer::seek(int64_t positionMs) {
    std::lock_guard<std::mutex> playLock(mPlayMutex);
    if (!mDecoder) {
        return ResultCode::ERROR_INVALID_STATE;
    }

    positionMs = std::max((int64_t)0, std::min(positionMs, mDurationMs.load(std::memory_order_relaxed)));
    mPendingSeekMs.store(positionMs, std::memory_order_release);
    mIsSeeking.store(true, std::memory_order_release);
    mDecoderCv.notify_one();
    return ResultCode::OK;
}

void AudioPlayer::setVolume(float volume) {
    mOutputPlayer->setVolume(volume);
}

float AudioPlayer::getVolume() const {
    return mOutputPlayer->getVolume();
}

int64_t AudioPlayer::getCurrentPosition() const {
    int64_t baseMs = mStartPositionMs.load(std::memory_order_relaxed);
    int64_t bytes = mRenderedBytes.load(std::memory_order_relaxed);
    int32_t sr = mSampleRate.load(std::memory_order_relaxed);
    int32_t ch = mChannels.load(std::memory_order_relaxed);
    if (sr <= 0) sr = 44100;
    if (ch <= 0) ch = 2;
    int64_t playedMs = baseMs + (bytes * 1000LL) / (static_cast<int64_t>(sr) * ch * 2);
    int64_t dur = mDurationMs.load(std::memory_order_relaxed);
    if (dur > 0 && playedMs > dur) playedMs = dur;
    return playedMs;
}

int64_t AudioPlayer::getDuration() const {
    return mDurationMs.load(std::memory_order_relaxed);
}

PlaybackState AudioPlayer::getState() const {
    return mState.load(std::memory_order_relaxed);
}

void AudioPlayer::setListener(AudioPlayerListener listener) {
    std::lock_guard<std::mutex> lock(mListenerMutex);
    mListener = std::move(listener);
}

void AudioPlayer::decoderThreadLoop() {
    pthread_setname_np(pthread_self(), "NativeAudioDec");

    struct sched_param param{};
    param.sched_priority = 2;
    pthread_setschedparam(pthread_self(), SCHED_RR, &param);

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onThreadStarted) {
            mListener.onThreadStarted();
        }
    }

    constexpr size_t CHUNK_SIZE = 8192;
    std::vector<uint8_t> decodeBuf(CHUNK_SIZE);
    std::vector<uint8_t> fadeBuf(CHUNK_SIZE);

    int64_t lastReportedPos = 0;
    auto lastPosReportTime = std::chrono::steady_clock::now();

    float crossfadeSec = mActiveCrossfadeDurationSec.load(std::memory_order_acquire);
    int32_t sr = mSampleRate.load(std::memory_order_relaxed);
    int32_t ch = mChannels.load(std::memory_order_relaxed);
    if (sr <= 0) sr = 44100;
    if (ch <= 0) ch = 2;

    int64_t totalCrossfadeBytes = static_cast<int64_t>(crossfadeSec * sr * ch * sizeof(int16_t));
    int64_t crossfadeBytesProcessed = 0;
    bool isCrossfading = (mFadingDecoder != nullptr && totalCrossfadeBytes > 0);

    while (mDecoderRunning.load(std::memory_order_acquire)) {
        // Handle pending seek request
        if (mIsSeeking.load(std::memory_order_acquire)) {
            int64_t targetPos = mPendingSeekMs.load(std::memory_order_acquire);
            mIsSeeking.store(false, std::memory_order_release);

            if (mFadingDecoder) {
                mFadingDecoder->close();
                mFadingDecoder.reset();
                isCrossfading = false;
            }

            mOutputPlayer->flush();
            mRingBuffer->reset();

            if (mDecoder && mDecoder->seekTo(targetPos)) {
                mStartPositionMs.store(targetPos, std::memory_order_release);
                mRenderedBytes.store(0, std::memory_order_release);
                mCurrentPositionMs.store(targetPos, std::memory_order_release);
                lastReportedPos = targetPos;
                lastPosReportTime = std::chrono::steady_clock::now();

                std::lock_guard<std::mutex> lock(mListenerMutex);
                if (mListener.onPositionChanged) {
                    mListener.onPositionChanged(targetPos);
                }
            }
        }

        // If paused, wait on condition variable
        if (mState.load(std::memory_order_relaxed) == PlaybackState::PAUSED) {
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            mDecoderCv.wait(lock, [this] {
                return !mDecoderRunning.load(std::memory_order_relaxed) ||
                       mState.load(std::memory_order_relaxed) == PlaybackState::PLAYING ||
                       mIsSeeking.load(std::memory_order_relaxed);
            });
            continue;
        }

        // If ring buffer is mostly full, wait on condition variable
        if (mRingBuffer->getAvailableWrite() < CHUNK_SIZE) {
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            mDecoderCv.wait_for(lock, std::chrono::milliseconds(50), [this] {
                return !mDecoderRunning.load(std::memory_order_relaxed) ||
                       mRingBuffer->getAvailableWrite() >= CHUNK_SIZE ||
                       mIsSeeking.load(std::memory_order_relaxed);
            });
            continue;
        }

        // Decode next PCM chunk
        int64_t framePosMs = 0;
        int32_t bytesDecoded = mDecoder ? mDecoder->decode(decodeBuf.data(), CHUNK_SIZE, framePosMs) : 0;

        if (bytesDecoded > 0) {
            if (isCrossfading && mFadingDecoder) {
                int64_t dummyPos = 0;
                int32_t fadeBytes = mFadingDecoder->decode(fadeBuf.data(), static_cast<size_t>(bytesDecoded), dummyPos);
                if (fadeBytes <= 0 || mFadingDecoder->isEof()) {
                    mFadingDecoder->close();
                    mFadingDecoder.reset();
                    isCrossfading = false;
                } else {
                    auto* pcmNew = reinterpret_cast<int16_t*>(decodeBuf.data());
                    auto* pcmOld = reinterpret_cast<const int16_t*>(fadeBuf.data());
                    size_t sampleCount = static_cast<size_t>(fadeBytes) / sizeof(int16_t);

                    for (size_t i = 0; i < sampleCount; ++i) {
                        float p = static_cast<float>(crossfadeBytesProcessed + i * sizeof(int16_t)) /
                                  static_cast<float>(totalCrossfadeBytes);
                        if (p > 1.0f) p = 1.0f;
                        float angle = p * 1.57079632679f; // pi / 2
                        float wOld = std::cos(angle);
                        float wNew = std::sin(angle);

                        int32_t mixed = static_cast<int32_t>(pcmNew[i] * wNew + pcmOld[i] * wOld);
                        pcmNew[i] = clamp16(mixed);
                    }

                    crossfadeBytesProcessed += fadeBytes;
                    if (crossfadeBytesProcessed >= totalCrossfadeBytes) {
                        mFadingDecoder->close();
                        mFadingDecoder.reset();
                        isCrossfading = false;
                        LOGI("Crossfade completed successfully");
                    }
                }
            }

            mRingBuffer->write(decodeBuf.data(), static_cast<size_t>(bytesDecoded));
            mDecoderCv.notify_one();

            if (mState.load(std::memory_order_relaxed) == PlaybackState::PLAYING) {
                mOutputPlayer->primeQueue();
            }

            int64_t actualPositionMs = getCurrentPosition();
            mCurrentPositionMs.store(actualPositionMs, std::memory_order_release);

            auto now = std::chrono::steady_clock::now();
            auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(now - lastPosReportTime).count();
            if (elapsedMs >= 250 && actualPositionMs != lastReportedPos) {
                lastReportedPos = actualPositionMs;
                lastPosReportTime = now;
                std::lock_guard<std::mutex> lock(mListenerMutex);
                if (mListener.onPositionChanged) {
                    mListener.onPositionChanged(actualPositionMs);
                }
            }
        } else if (mDecoder && mDecoder->isEof()) {
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            while (mRingBuffer->getAvailableRead() > 0 && mDecoderRunning.load(std::memory_order_relaxed)) {
                mDecoderCv.wait_for(lock, std::chrono::milliseconds(10), [this] {
                    return !mDecoderRunning.load(std::memory_order_relaxed) || mRingBuffer->getAvailableRead() == 0;
                });
            }

            if (mDecoderRunning.load(std::memory_order_relaxed)) {
                LOGI("Track reached natural completion");
                setState(PlaybackState::COMPLETED);
                std::lock_guard<std::mutex> lockListener(mListenerMutex);
                if (mListener.onTrackEnded) {
                    mListener.onTrackEnded();
                }
                break;
            }
        } else {
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            mDecoderCv.wait_for(lock, std::chrono::milliseconds(10), [this] {
                return !mDecoderRunning.load(std::memory_order_relaxed) || mIsSeeking.load(std::memory_order_relaxed);
            });
        }
    }

    if (mFadingDecoder) {
        mFadingDecoder->close();
        mFadingDecoder.reset();
    }

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onThreadExiting) {
            mListener.onThreadExiting();
        }
    }

    LOGI("Decoder thread loop terminated");
}

} // namespace audio
