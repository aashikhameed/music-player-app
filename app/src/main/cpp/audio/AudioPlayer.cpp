#include "AudioPlayer.h"
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>
#include <chrono>

#define LOG_TAG "NativeAudioPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace audio {

AudioPlayer& AudioPlayer::getInstance() {
    static AudioPlayer instance;
    return instance;
}

AudioPlayer::AudioPlayer()
    : mRingBuffer(std::make_shared<RingBuffer>(96 * 1024)),
      mOutputPlayer(std::make_unique<OpenSLESPlayer>(mRingBuffer)) {
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

ResultCode AudioPlayer::playTrack(const std::string& filePath, int64_t startPositionMs) {
    LOGI("playTrack requested: %s (start: %lld ms)", filePath.c_str(), (long long)startPositionMs);

    // Stop existing playback and drain decoder thread
    stop();

    mDecoder = AudioDecoder::create(filePath);
    if (!mDecoder || !mDecoder->open(filePath)) {
        LOGE("Could not open file: %s", filePath.c_str());
        notifyError(ResultCode::ERROR_UNSUPPORTED_FORMAT, "Cannot decode file format");
        return ResultCode::ERROR_UNSUPPORTED_FORMAT;
    }

    const AudioFormat& format = mDecoder->getFormat();
    mDurationMs.store(format.durationMs, std::memory_order_relaxed);
    mCurrentPositionMs.store(startPositionMs, std::memory_order_relaxed);

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onDurationUpdated) {
            mListener.onDurationUpdated(format.durationMs);
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

    mRingBuffer->reset();

    // Start decoder background thread
    mDecoderRunning.store(true, std::memory_order_release);
    mDecoderThread = std::thread(&AudioPlayer::decoderThreadLoop, this);

    // Wait until at least 8KB (2 OpenSL ES queue buffers) of PCM is pre-buffered or EOF reached
    int waitCount = 0;
    while (mRingBuffer->getAvailableRead() < 8 * 1024 && !mDecoder->isEof() && waitCount < 30) {
        usleep(1000); // 1ms sleep for ultra-low startup latency
        waitCount++;
    }

    if (!mOutputPlayer->play()) {
        LOGE("OpenSLESPlayer::play failed");
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Cannot start OpenSL ES playback");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    setState(PlaybackState::PLAYING);
    return ResultCode::OK;
}

ResultCode AudioPlayer::pause() {
    if (mState.load(std::memory_order_relaxed) != PlaybackState::PLAYING) {
        return ResultCode::OK;
    }

    mOutputPlayer->pause();
    setState(PlaybackState::PAUSED);
    return ResultCode::OK;
}

ResultCode AudioPlayer::resume() {
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
    // 1. Stop audio output
    mOutputPlayer->stop();

    // 2. Stop and join decoder thread
    mDecoderRunning.store(false, std::memory_order_release);
    mDecoderCv.notify_all();
    if (mDecoderThread.joinable()) {
        mDecoderThread.join();
    }

    // 3. Close decoder and clear buffer
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
    return mCurrentPositionMs.load(std::memory_order_relaxed);
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

    // ── Decoder thread: real-time round-robin scheduling ─────────────────────
    // SCHED_RR prevents the kernel from preempting this thread when Compose is
    // doing a recomposition pass, eliminating audio glitches under CPU load.
    // Priority 2 = below SCHED_FIFO audio HAL (prio 8) but above normal tasks.
    struct sched_param param{};
    param.sched_priority = 2;
    pthread_setschedparam(pthread_self(), SCHED_RR, &param);

    constexpr size_t CHUNK_SIZE = 8192;
    std::vector<uint8_t> decodeBuf(CHUNK_SIZE);

    int64_t lastReportedPos = 0;
    auto lastPosReportTime = std::chrono::steady_clock::now();

    while (mDecoderRunning.load(std::memory_order_acquire)) {
        // Handle pending seek request
        if (mIsSeeking.load(std::memory_order_acquire)) {
            int64_t targetPos = mPendingSeekMs.load(std::memory_order_acquire);
            mIsSeeking.store(false, std::memory_order_release);

            mOutputPlayer->flush();
            mRingBuffer->reset();

            if (mDecoder && mDecoder->seekTo(targetPos)) {
                mCurrentPositionMs.store(targetPos, std::memory_order_release);
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

        // If ring buffer is mostly full, wait briefly with CV
        if (mRingBuffer->getAvailableWrite() < CHUNK_SIZE) {
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            mDecoderCv.wait_for(lock, std::chrono::milliseconds(5), [this] {
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
            mRingBuffer->write(decodeBuf.data(), static_cast<size_t>(bytesDecoded));
            mCurrentPositionMs.store(framePosMs, std::memory_order_release);

            // Throttle position reports to ~250ms
            auto now = std::chrono::steady_clock::now();
            auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(now - lastPosReportTime).count();
            if (elapsedMs >= 250 && framePosMs != lastReportedPos) {
                lastReportedPos = framePosMs;
                lastPosReportTime = now;
                std::lock_guard<std::mutex> lock(mListenerMutex);
                if (mListener.onPositionChanged) {
                    mListener.onPositionChanged(framePosMs);
                }
            }
        } else if (mDecoder && mDecoder->isEof()) {
            // Wait until output player drains the remaining buffered PCM
            std::unique_lock<std::mutex> lock(mDecoderMutex);
            while (mRingBuffer->getAvailableRead() > 0 && mDecoderRunning.load(std::memory_order_relaxed)) {
                mDecoderCv.wait_for(lock, std::chrono::milliseconds(5), [this] {
                    return !mDecoderRunning.load(std::memory_order_relaxed) || mRingBuffer->getAvailableRead() == 0;
                });
            }

            if (mDecoderRunning.load(std::memory_order_relaxed)) {
                LOGI("Track reached natural completion");
                setState(PlaybackState::COMPLETED);
                {
                    std::lock_guard<std::mutex> lock(mListenerMutex);
                    if (mListener.onTrackEnded) {
                        mListener.onTrackEnded();
                    }
                }
                break;
            }
        } else {
            // Temporary starvation from extractor, wait briefly
            usleep(5000);
        }
    }

    LOGI("Decoder thread loop terminated");
}

} // namespace audio
