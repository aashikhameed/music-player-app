#include "AudioDecoder.h"
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <android/log.h>
#include <cstring>
#include <algorithm>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

#define LOG_TAG "NativeAudioDecoder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace audio {

std::unique_ptr<AudioDecoder> AudioDecoder::create(const std::string& filePath) {
    return std::make_unique<MediaCodecDecoder>();
}

MediaCodecDecoder::MediaCodecDecoder() {
    mPendingPcm.reserve(16 * 1024);
}

MediaCodecDecoder::~MediaCodecDecoder() {
    close();
}

bool MediaCodecDecoder::open(const std::string& filePath) {
    close();
    mFilePath = filePath;

    mExtractor = AMediaExtractor_new();
    if (!mExtractor) {
        LOGE("Failed to create AMediaExtractor");
        return false;
    }

    media_status_t err = AMEDIA_ERROR_UNKNOWN;
    int fd = ::open(filePath.c_str(), O_RDONLY);
    if (fd >= 0) {
        struct stat st{};
        if (::fstat(fd, &st) == 0 && st.st_size > 0) {
            err = AMediaExtractor_setDataSourceFd(mExtractor, fd, 0, st.st_size);
        }
        ::close(fd);
    }

    if (err != AMEDIA_OK) {
        err = AMediaExtractor_setDataSource(mExtractor, filePath.c_str());
    }

    if (err != AMEDIA_OK) {
        LOGE("AMediaExtractor_setDataSource failed for %s: %d", filePath.c_str(), err);
        releaseResources();
        return false;
    }

    const size_t numTracks = AMediaExtractor_getTrackCount(mExtractor);
    int audioTrackIndex = -1;

    for (size_t i = 0; i < numTracks; ++i) {
        AMediaFormat* format = AMediaExtractor_getTrackFormat(mExtractor, i);
        if (!format) continue;

        const char* mime = nullptr;
        if (AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime) && mime) {
            if (strncmp(mime, "audio/", 6) == 0) {
                audioTrackIndex = static_cast<int>(i);
                mTrackFormat = format;
                mFormat.mimeType = mime;
                break;
            }
        }
        AMediaFormat_delete(format);
    }

    if (audioTrackIndex < 0 || !mTrackFormat) {
        LOGE("No audio track found in file %s", filePath.c_str());
        releaseResources();
        return false;
    }

    AMediaExtractor_selectTrack(mExtractor, static_cast<size_t>(audioTrackIndex));

    int32_t sampleRate = 44100;
    int32_t channels = 2;
    int64_t durationUs = 0;
    int32_t bitrate = 0;

    AMediaFormat_getInt32(mTrackFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE, &sampleRate);
    AMediaFormat_getInt32(mTrackFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &channels);
    AMediaFormat_getInt64(mTrackFormat, AMEDIAFORMAT_KEY_DURATION, &durationUs);
    AMediaFormat_getInt32(mTrackFormat, AMEDIAFORMAT_KEY_BIT_RATE, &bitrate);

    mFormat.sampleRate = sampleRate > 0 ? sampleRate : 44100;
    mFormat.channelCount = channels > 0 ? channels : 2;
    mFormat.bitDepth = 16;
    mFormat.durationMs = durationUs > 0 ? (durationUs / 1000) : 0;
    mFormat.bitrate = bitrate;

    LOGI("Audio track detected: %s, %d Hz, %d ch, %lld ms, mime: %s",
         filePath.c_str(), mFormat.sampleRate, mFormat.channelCount,
         (long long)mFormat.durationMs, mFormat.mimeType.c_str());

    mCodec = AMediaCodec_createDecoderByType(mFormat.mimeType.c_str());
    if (!mCodec) {
        LOGE("Failed to create AMediaCodec for MIME %s", mFormat.mimeType.c_str());
        releaseResources();
        return false;
    }

    err = AMediaCodec_configure(mCodec, mTrackFormat, nullptr, nullptr, 0);
    if (err != AMEDIA_OK) {
        LOGE("AMediaCodec_configure failed: %d", err);
        releaseResources();
        return false;
    }

    err = AMediaCodec_start(mCodec);
    if (err != AMEDIA_OK) {
        LOGE("AMediaCodec_start failed: %d", err);
        releaseResources();
        return false;
    }

    mEof = false;
    mInputEof = false;
    mCurrentPositionMs = 0;
    mPendingPcm.clear();
    mPendingOffset = 0;

    return true;
}

bool MediaCodecDecoder::feedInputBuffer() {
    if (mInputEof || !mCodec || !mExtractor) return false;

    // Timeout: 2000 microseconds
    ssize_t inBufIndex = AMediaCodec_dequeueInputBuffer(mCodec, 2000);
    if (inBufIndex < 0) {
        return false;
    }

    size_t inBufSize = 0;
    uint8_t* inBuf = AMediaCodec_getInputBuffer(mCodec, inBufIndex, &inBufSize);
    if (!inBuf) {
        return false;
    }

    ssize_t sampleSize = AMediaExtractor_readSampleData(mExtractor, inBuf, inBufSize);
    if (sampleSize < 0) {
        // End of input stream reached
        mInputEof = true;
        AMediaCodec_queueInputBuffer(mCodec, inBufIndex, 0, 0, 0, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
        return true;
    }

    int64_t presentationTimeUs = AMediaExtractor_getSampleTime(mExtractor);
    AMediaCodec_queueInputBuffer(mCodec, inBufIndex, 0, static_cast<size_t>(sampleSize), presentationTimeUs, 0);
    AMediaExtractor_advance(mExtractor);
    return true;
}

int32_t MediaCodecDecoder::decode(uint8_t* outBuffer, size_t maxBytes, int64_t& outPositionMs) {
    if (!mCodec || mEof) {
        outPositionMs = mCurrentPositionMs;
        return 0;
    }

    size_t totalBytesWritten = 0;

    // 1. Drain pending residual PCM from previous decode pass
    if (mPendingOffset < mPendingPcm.size()) {
        size_t available = mPendingPcm.size() - mPendingOffset;
        size_t toCopy = std::min(available, maxBytes);
        std::memcpy(outBuffer, &mPendingPcm[mPendingOffset], toCopy);
        mPendingOffset += toCopy;
        totalBytesWritten += toCopy;

        if (mPendingOffset >= mPendingPcm.size()) {
            mPendingPcm.clear();
            mPendingOffset = 0;
        }

        if (totalBytesWritten >= maxBytes) {
            outPositionMs = mCurrentPositionMs;
            return static_cast<int32_t>(totalBytesWritten);
        }
    }

    // 2. Decode frames from MediaCodec until buffer target is reached or wait needed
    int attempts = 0;
    while (totalBytesWritten < maxBytes && attempts < 8 && !mEof) {
        attempts++;

        // Keep input pipeline full
        feedInputBuffer();

        AMediaCodecBufferInfo info;
        ssize_t outBufIndex = AMediaCodec_dequeueOutputBuffer(mCodec, &info, 2000);

        if (outBufIndex >= 0) {
            if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                mEof = true;
            }

            if (info.size > 0) {
                size_t outBufSize = 0;
                uint8_t* codecOutBuf = AMediaCodec_getOutputBuffer(mCodec, outBufIndex, &outBufSize);
                if (codecOutBuf) {
                    uint8_t* pcmData = codecOutBuf + info.offset;
                    size_t pcmBytes = static_cast<size_t>(info.size);

                    if (info.presentationTimeUs > 0) {
                        mCurrentPositionMs = info.presentationTimeUs / 1000;
                    }

                    size_t needed = maxBytes - totalBytesWritten;
                    if (pcmBytes <= needed) {
                        std::memcpy(outBuffer + totalBytesWritten, pcmData, pcmBytes);
                        totalBytesWritten += pcmBytes;
                    } else {
                        // Copy needed portion and save remainder in mPendingPcm
                        std::memcpy(outBuffer + totalBytesWritten, pcmData, needed);
                        totalBytesWritten += needed;

                        size_t remainder = pcmBytes - needed;
                        mPendingPcm.assign(pcmData + needed, pcmData + needed + remainder);
                        mPendingOffset = 0;
                    }
                }
            }
            AMediaCodec_releaseOutputBuffer(mCodec, outBufIndex, false);
        } else if (outBufIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            AMediaFormat* newFormat = AMediaCodec_getOutputFormat(mCodec);
            if (newFormat) {
                int32_t sr = mFormat.sampleRate;
                int32_t ch = mFormat.channelCount;
                AMediaFormat_getInt32(newFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE, &sr);
                AMediaFormat_getInt32(newFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &ch);
                mFormat.sampleRate = sr;
                mFormat.channelCount = ch;
                LOGI("MediaCodec output format changed: %d Hz, %d channels", sr, ch);
                AMediaFormat_delete(newFormat);
            }
        } else if (outBufIndex == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            // No output ready yet; try to feed more input
            if (!feedInputBuffer()) {
                break;
            }
        }
    }

    outPositionMs = mCurrentPositionMs;
    return static_cast<int32_t>(totalBytesWritten);
}

bool MediaCodecDecoder::seekTo(int64_t positionMs) {
    if (!mExtractor || !mCodec) return false;

    int64_t targetUs = positionMs * 1000;
    media_status_t err = AMediaExtractor_seekTo(mExtractor, targetUs, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    if (err != AMEDIA_OK) {
        LOGW("AMediaExtractor_seekTo failed: %d", err);
        return false;
    }

    AMediaCodec_flush(mCodec);
    mEof = false;
    mInputEof = false;
    mCurrentPositionMs = positionMs;
    mPendingPcm.clear();
    mPendingOffset = 0;
    return true;
}

void MediaCodecDecoder::close() {
    releaseResources();
    mFilePath.clear();
    mPendingPcm.clear();
    mPendingOffset = 0;
    mEof = true;
    mInputEof = true;
}

void MediaCodecDecoder::releaseResources() {
    if (mCodec) {
        AMediaCodec_stop(mCodec);
        AMediaCodec_delete(mCodec);
        mCodec = nullptr;
    }
    if (mTrackFormat) {
        AMediaFormat_delete(mTrackFormat);
        mTrackFormat = nullptr;
    }
    if (mExtractor) {
        AMediaExtractor_delete(mExtractor);
        mExtractor = nullptr;
    }
}

} // namespace audio
