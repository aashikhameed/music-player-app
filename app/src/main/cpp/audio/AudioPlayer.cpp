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

AudioPlayer& AudioPlayer::getInstance() {
    static AudioPlayer instance;
    return instance;
}

AudioPlayer::AudioPlayer() = default;

AudioPlayer::~AudioPlayer() {
    shutdown();
}

ResultCode AudioPlayer::init() {
    setState(PlaybackState::INITIALIZED);
    LOGI("AudioPlayer initialized successfully");
    return ResultCode::OK;
}

void AudioPlayer::shutdown() {
    stop();
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

void AudioPlayer::setCrossfadeDuration(float seconds) {
    mCrossfadeDurationSec.store(std::max(0.0f, seconds), std::memory_order_release);
    LOGI("Crossfade duration set to %.1f seconds", seconds);
}

float AudioPlayer::getCrossfadeDuration() const {
    return mCrossfadeDurationSec.load(std::memory_order_relaxed);
}

void AudioPlayer::syncEqualizer() {
    std::lock_guard<std::mutex> lock(mTrackMutex);
    if (mCurrentTrack && mCurrentTrack->outputPlayer) {
        mCurrentTrack->outputPlayer->getEqualizer().copySettingsFrom(mEqualizer);
    }
}

void AudioPlayer::stopTrackSession(std::unique_ptr<TrackSession>& session) {
    if (!session) return;

    if (session->outputPlayer) {
        session->outputPlayer->stop();
    }

    session->decoderRunning.store(false, std::memory_order_release);
    session->decoderCv.notify_all();

    if (session->decoderThread.joinable()) {
        session->decoderThread.join();
    }

    if (session->decoder) {
        session->decoder->close();
        session->decoder.reset();
    }

    if (session->outputPlayer) {
        session->outputPlayer->shutdown();
        session->outputPlayer.reset();
    }

    session.reset();
}

void AudioPlayer::stopFaderThread() {
    mFaderRunning.store(false, std::memory_order_release);
    mFaderCv.notify_all();
    if (mFaderThread.joinable()) {
        mFaderThread.join();
    }
}

void AudioPlayer::startFaderThread(float durationSec) {
    stopFaderThread();
    mFaderRunning.store(true, std::memory_order_release);
    mFaderThread = std::thread([this, durationSec]() {
        pthread_setname_np(pthread_self(), "AudioFader");
        auto startTime = std::chrono::steady_clock::now();
        int64_t totalMs = static_cast<int64_t>(durationSec * 1000.0f);
        if (totalMs <= 0) totalMs = 1;

        while (mFaderRunning.load(std::memory_order_acquire)) {
            auto now = std::chrono::steady_clock::now();
            auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(now - startTime).count();
            float p = static_cast<float>(elapsedMs) / static_cast<float>(totalMs);
            if (p >= 1.0f) {
                p = 1.0f;
            }

            // Equal-power crossfade curve
            float angle = p * 1.57079632679f; // pi / 2
            float master = mMasterVolume.load(std::memory_order_relaxed);
            float fadeVol = std::cos(angle) * master;
            float currVol = std::sin(angle) * master;

            {
                std::lock_guard<std::mutex> lock(mTrackMutex);
                if (mFadingTrack && mFadingTrack->outputPlayer) {
                    mFadingTrack->outputPlayer->setVolume(fadeVol);
                }
                if (mCurrentTrack && mCurrentTrack->outputPlayer) {
                    mCurrentTrack->outputPlayer->setVolume(currVol);
                }
            }

            if (p >= 1.0f) {
                break;
            }

            std::unique_lock<std::mutex> faderLock(mFaderMutex);
            mFaderCv.wait_for(faderLock, std::chrono::milliseconds(40), [this] {
                return !mFaderRunning.load(std::memory_order_acquire);
            });
        }

        // Crossfade complete: restore current track volume and clean up fading track
        if (mFaderRunning.load(std::memory_order_acquire)) {
            {
                std::lock_guard<std::mutex> lock(mTrackMutex);
                if (mCurrentTrack && mCurrentTrack->outputPlayer) {
                    mCurrentTrack->outputPlayer->setVolume(mMasterVolume.load(std::memory_order_relaxed));
                }
            }
            std::unique_ptr<TrackSession> toDelete;
            {
                std::lock_guard<std::mutex> lock(mTrackMutex);
                toDelete = std::move(mFadingTrack);
            }
            if (toDelete) {
                stopTrackSession(toDelete);
            }
        }
    });
}

ResultCode AudioPlayer::playTrack(const std::string& filePath, int64_t startPositionMs) {
    LOGI("playTrack requested: %s (start: %lld ms)", filePath.c_str(), (long long)startPositionMs);

    float crossfadeSec = mCrossfadeDurationSec.load(std::memory_order_acquire);
    bool shouldCrossfade = false;
    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        shouldCrossfade = (crossfadeSec > 0.0f) &&
                          (startPositionMs == 0) &&
                          (mCurrentTrack != nullptr) &&
                          (mState.load(std::memory_order_relaxed) == PlaybackState::PLAYING);
    }

    // 1. Stop and join any active fader thread
    stopFaderThread();

    // 2. Prepare tracks: if crossfade active, transition current -> fading
    std::unique_ptr<TrackSession> oldFading;
    std::unique_ptr<TrackSession> oldCurrent;
    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        if (mFadingTrack) {
            oldFading = std::move(mFadingTrack);
        }
        if (shouldCrossfade) {
            mFadingTrack = std::move(mCurrentTrack);
        } else {
            oldCurrent = std::move(mCurrentTrack);
        }
    }
    if (oldFading) {
        stopTrackSession(oldFading);
    }
    if (oldCurrent) {
        stopTrackSession(oldCurrent);
    }

    // 3. Create new TrackSession
    auto newTrack = std::make_unique<TrackSession>();
    newTrack->filePath = filePath;
    newTrack->ringBuffer = std::make_shared<RingBuffer>(96 * 1024);
    newTrack->outputPlayer = std::make_unique<OpenSLESPlayer>(newTrack->ringBuffer);

    // Copy acoustic DSP equalizer settings
    newTrack->outputPlayer->getEqualizer().copySettingsFrom(mEqualizer);

    if (!newTrack->outputPlayer->init()) {
        LOGE("Failed to init OpenSLESPlayer for new track");
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Failed to init audio output");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    TrackSession* rawTrackPtr = newTrack.get();
    newTrack->outputPlayer->setOnBufferConsumedCallback([rawTrackPtr](size_t bytesConsumed) {
        rawTrackPtr->renderedBytes.fetch_add(bytesConsumed, std::memory_order_relaxed);
        rawTrackPtr->decoderCv.notify_one();
    });

    newTrack->decoder = AudioDecoder::create(filePath);
    if (!newTrack->decoder || !newTrack->decoder->open(filePath)) {
        LOGE("Could not open file: %s", filePath.c_str());
        notifyError(ResultCode::ERROR_UNSUPPORTED_FORMAT, "Cannot decode file format");
        return ResultCode::ERROR_UNSUPPORTED_FORMAT;
    }

    const AudioFormat& format = newTrack->decoder->getFormat();
    newTrack->durationMs.store(format.durationMs, std::memory_order_relaxed);
    newTrack->startPositionMs.store(startPositionMs, std::memory_order_relaxed);
    newTrack->renderedBytes.store(0, std::memory_order_relaxed);
    newTrack->sampleRate.store(format.sampleRate, std::memory_order_relaxed);
    newTrack->channels.store(format.channelCount, std::memory_order_relaxed);
    newTrack->currentPositionMs.store(startPositionMs, std::memory_order_relaxed);

    if (!newTrack->outputPlayer->configureAudio(format.sampleRate, format.channelCount)) {
        LOGE("Failed to configure OpenSL ES output for %d Hz, %d ch", format.sampleRate, format.channelCount);
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Audio output configuration failed");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    if (startPositionMs > 0) {
        newTrack->decoder->seekTo(startPositionMs);
    }

    newTrack->ringBuffer->reset();

    // Start decoder background thread
    newTrack->decoderRunning.store(true, std::memory_order_release);
    newTrack->decoderThread = std::thread(&AudioPlayer::decoderThreadLoop, this, rawTrackPtr);
    pthread_setname_np(newTrack->decoderThread.native_handle(), "AudioDecoder");

    // Wait until at least 8KB of PCM is pre-buffered or EOF reached
    int waitCount = 0;
    while (newTrack->ringBuffer->getAvailableRead() < 8 * 1024 && !newTrack->decoder->isEof() && waitCount < 30) {
        usleep(1000); // 1ms sleep for ultra-low startup latency
        waitCount++;
    }

    float initialVol = shouldCrossfade ? 0.0f : mMasterVolume.load(std::memory_order_relaxed);
    newTrack->outputPlayer->setVolume(initialVol);

    if (!newTrack->outputPlayer->play()) {
        LOGE("OpenSLESPlayer::play failed for new track");
        notifyError(ResultCode::ERROR_AUDIO_ENGINE_FAILED, "Cannot start OpenSL ES playback");
        return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
    }

    newTrack->state.store(PlaybackState::PLAYING, std::memory_order_relaxed);

    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        mCurrentTrack = std::move(newTrack);
    }

    setState(PlaybackState::PLAYING);

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onDurationUpdated) {
            mListener.onDurationUpdated(format.durationMs);
        }
        if (mListener.onPositionChanged) {
            mListener.onPositionChanged(startPositionMs);
        }
    }

    // 4. Start smooth crossfader if blending between tracks
    if (shouldCrossfade) {
        startFaderThread(crossfadeSec);
    }

    return ResultCode::OK;
}

ResultCode AudioPlayer::pause() {
    stopFaderThread();

    std::unique_ptr<TrackSession> oldFading;
    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        if (mFadingTrack) {
            oldFading = std::move(mFadingTrack);
        }
        if (mCurrentTrack && mCurrentTrack->outputPlayer) {
            mCurrentTrack->outputPlayer->pause();
            mCurrentTrack->outputPlayer->setVolume(mMasterVolume.load(std::memory_order_relaxed));
            mCurrentTrack->state.store(PlaybackState::PAUSED, std::memory_order_relaxed);
        }
    }
    if (oldFading) {
        stopTrackSession(oldFading);
    }

    setState(PlaybackState::PAUSED);
    return ResultCode::OK;
}

ResultCode AudioPlayer::resume() {
    std::lock_guard<std::mutex> lock(mTrackMutex);
    if (mCurrentTrack && mCurrentTrack->outputPlayer) {
        mCurrentTrack->outputPlayer->setVolume(mMasterVolume.load(std::memory_order_relaxed));
        if (mCurrentTrack->outputPlayer->play()) {
            mCurrentTrack->state.store(PlaybackState::PLAYING, std::memory_order_relaxed);
            mCurrentTrack->decoderCv.notify_one();
            setState(PlaybackState::PLAYING);
            return ResultCode::OK;
        }
    }
    return ResultCode::ERROR_AUDIO_ENGINE_FAILED;
}

ResultCode AudioPlayer::stop() {
    stopFaderThread();

    std::unique_ptr<TrackSession> fading;
    std::unique_ptr<TrackSession> current;
    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        fading = std::move(mFadingTrack);
        current = std::move(mCurrentTrack);
    }

    if (fading) stopTrackSession(fading);
    if (current) stopTrackSession(current);

    mCurrentPositionMs.store(0, std::memory_order_relaxed);
    setState(PlaybackState::STOPPED);
    return ResultCode::OK;
}

ResultCode AudioPlayer::seek(int64_t positionMs) {
    stopFaderThread();

    std::unique_ptr<TrackSession> oldFading;
    {
        std::lock_guard<std::mutex> lock(mTrackMutex);
        if (mFadingTrack) {
            oldFading = std::move(mFadingTrack);
        }
        if (!mCurrentTrack || !mCurrentTrack->decoder) {
            return ResultCode::ERROR_INVALID_STATE;
        }
        mCurrentTrack->outputPlayer->setVolume(mMasterVolume.load(std::memory_order_relaxed));
        int64_t dur = mCurrentTrack->durationMs.load(std::memory_order_relaxed);
        positionMs = std::max((int64_t)0, std::min(positionMs, dur));
        mCurrentTrack->pendingSeekMs.store(positionMs, std::memory_order_release);
        mCurrentTrack->isSeeking.store(true, std::memory_order_release);
        mCurrentTrack->decoderCv.notify_one();
    }
    if (oldFading) {
        stopTrackSession(oldFading);
    }

    return ResultCode::OK;
}

void AudioPlayer::setVolume(float volume) {
    mMasterVolume.store(std::max(0.0f, std::min(1.0f, volume)), std::memory_order_relaxed);
    std::lock_guard<std::mutex> lock(mTrackMutex);
    if (mCurrentTrack && mCurrentTrack->outputPlayer && !mFaderRunning.load(std::memory_order_relaxed)) {
        mCurrentTrack->outputPlayer->setVolume(volume);
    }
}

float AudioPlayer::getVolume() const {
    return mMasterVolume.load(std::memory_order_relaxed);
}

int64_t AudioPlayer::getCurrentPosition() const {
    std::lock_guard<std::mutex> lock(mTrackMutex);
    if (mCurrentTrack) {
        return mCurrentTrack->getCurrentPosition();
    }
    return 0;
}

int64_t AudioPlayer::getDuration() const {
    std::lock_guard<std::mutex> lock(mTrackMutex);
    if (mCurrentTrack) {
        return mCurrentTrack->durationMs.load(std::memory_order_relaxed);
    }
    return 0;
}

PlaybackState AudioPlayer::getState() const {
    return mState.load(std::memory_order_relaxed);
}

void AudioPlayer::setListener(AudioPlayerListener listener) {
    std::lock_guard<std::mutex> lock(mListenerMutex);
    mListener = std::move(listener);
}

void AudioPlayer::decoderThreadLoop(TrackSession* session) {
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

    int64_t lastReportedPos = 0;
    auto lastPosReportTime = std::chrono::steady_clock::now();
    auto& mDecoderCv = session->decoderCv;

    while (session->decoderRunning.load(std::memory_order_acquire)) {
        // Handle pending seek request
        if (session->isSeeking.load(std::memory_order_acquire)) {
            int64_t targetPos = session->pendingSeekMs.load(std::memory_order_acquire);
            session->isSeeking.store(false, std::memory_order_release);

            session->outputPlayer->flush();
            session->ringBuffer->reset();

            if (session->decoder && session->decoder->seekTo(targetPos)) {
                session->startPositionMs.store(targetPos, std::memory_order_release);
                session->renderedBytes.store(0, std::memory_order_release);
                session->currentPositionMs.store(targetPos, std::memory_order_release);
                lastReportedPos = targetPos;
                lastPosReportTime = std::chrono::steady_clock::now();

                bool isCurrent = false;
                {
                    std::lock_guard<std::mutex> trackLock(mTrackMutex);
                    isCurrent = (session == mCurrentTrack.get());
                }
                if (isCurrent) {
                    std::lock_guard<std::mutex> lock(mListenerMutex);
                    if (mListener.onPositionChanged) {
                        mListener.onPositionChanged(targetPos);
                    }
                }
            }
        }

        // If paused, wait on condition variable
        if (session->state.load(std::memory_order_relaxed) == PlaybackState::PAUSED) {
            std::unique_lock<std::mutex> lock(session->decoderMutex);
            mDecoderCv.wait(lock, [session] {
                return !session->decoderRunning.load(std::memory_order_relaxed) ||
                       session->state.load(std::memory_order_relaxed) == PlaybackState::PLAYING ||
                       session->isSeeking.load(std::memory_order_relaxed);
            });
            continue;
        }

        // If ring buffer is mostly full, wait on condition variable
        if (session->ringBuffer->getAvailableWrite() < CHUNK_SIZE) {
            std::unique_lock<std::mutex> lock(session->decoderMutex);
            mDecoderCv.wait_for(lock, std::chrono::milliseconds(50), [session] {
                return !session->decoderRunning.load(std::memory_order_relaxed) ||
                       session->ringBuffer->getAvailableWrite() >= CHUNK_SIZE ||
                       session->isSeeking.load(std::memory_order_relaxed);
            });
            continue;
        }

        // Decode next PCM chunk
        int64_t framePosMs = 0;
        int32_t bytesDecoded = session->decoder ? session->decoder->decode(decodeBuf.data(), CHUNK_SIZE, framePosMs) : 0;

        if (bytesDecoded > 0) {
            session->ringBuffer->write(decodeBuf.data(), static_cast<size_t>(bytesDecoded));

            if (session->state.load(std::memory_order_relaxed) == PlaybackState::PLAYING) {
                session->outputPlayer->primeQueue();
            }

            bool isCurrent = false;
            {
                std::lock_guard<std::mutex> trackLock(mTrackMutex);
                isCurrent = (session == mCurrentTrack.get());
            }

            if (isCurrent) {
                int64_t actualPositionMs = session->getCurrentPosition();
                session->currentPositionMs.store(actualPositionMs, std::memory_order_release);
                mCurrentPositionMs.store(actualPositionMs, std::memory_order_relaxed);

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
            }
        } else if (session->decoder && session->decoder->isEof()) {
            std::unique_lock<std::mutex> lock(session->decoderMutex);
            while (session->ringBuffer->getAvailableRead() > 0 && session->decoderRunning.load(std::memory_order_relaxed)) {
                mDecoderCv.wait_for(lock, std::chrono::milliseconds(10), [session] {
                    return !session->decoderRunning.load(std::memory_order_relaxed) || session->ringBuffer->getAvailableRead() == 0;
                });
            }

            if (session->decoderRunning.load(std::memory_order_relaxed)) {
                bool isCurrent = false;
                {
                    std::lock_guard<std::mutex> trackLock(mTrackMutex);
                    isCurrent = (session == mCurrentTrack.get());
                }

                if (isCurrent) {
                    LOGI("Track reached natural completion");
                    setState(PlaybackState::COMPLETED);
                    std::lock_guard<std::mutex> lockListener(mListenerMutex);
                    if (mListener.onTrackEnded) {
                        mListener.onTrackEnded();
                    }
                }
                break;
            }
        } else {
            std::unique_lock<std::mutex> lock(session->decoderMutex);
            mDecoderCv.wait_for(lock, std::chrono::milliseconds(10), [session] {
                return !session->decoderRunning.load(std::memory_order_relaxed) || session->isSeeking.load(std::memory_order_relaxed);
            });
        }
    }

    {
        std::lock_guard<std::mutex> lock(mListenerMutex);
        if (mListener.onThreadExiting) {
            mListener.onThreadExiting();
        }
    }

    LOGI("Decoder thread loop terminated for track: %s", session->filePath.c_str());
}

} // namespace audio
