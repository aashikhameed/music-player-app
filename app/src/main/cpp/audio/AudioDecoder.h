#ifndef MUSIC_PLAYER_AUDIO_DECODER_H
#define MUSIC_PLAYER_AUDIO_DECODER_H

#include "Types.h"
#include <string>
#include <memory>
#include <cstdint>

// Forward declarations for Android Media NDK
struct AMediaExtractor;
struct AMediaCodec;
struct AMediaFormat;

namespace audio {

/**
 * Abstract Audio Decoder Interface.
 * Defines standard decoding contract for audio files (MP3, M4A/AAC, FLAC, WAV, OGG).
 */
class AudioDecoder {
public:
    virtual ~AudioDecoder() = default;

    virtual bool open(const std::string& filePath) = 0;
    virtual int32_t decode(uint8_t* outBuffer, size_t maxBytes, int64_t& outPositionMs) = 0;
    virtual bool seekTo(int64_t positionMs) = 0;
    virtual void close() = 0;
    virtual const AudioFormat& getFormat() const = 0;
    virtual bool isEof() const = 0;

    static std::unique_ptr<AudioDecoder> create(const std::string& filePath);
};

/**
 * Hardware-Accelerated Native MediaCodec / MediaExtractor Decoder.
 * Leverages Android 9 (API 28) MediaNDK to decode MP3, M4A, AAC, FLAC, OGG, and WAV directly
 * using the hardware audio DSP without requiring external binary dependencies.
 */
class MediaCodecDecoder : public AudioDecoder {
public:
    MediaCodecDecoder();
    ~MediaCodecDecoder() override;

    bool open(const std::string& filePath) override;
    int32_t decode(uint8_t* outBuffer, size_t maxBytes, int64_t& outPositionMs) override;
    virtual bool seekTo(int64_t positionMs) override;
    void close() override;
    const AudioFormat& getFormat() const override { return mFormat; }
    bool isEof() const override { return mEof; }

private:
    void releaseResources();
    bool feedInputBuffer();

    std::string mFilePath;
    AMediaExtractor* mExtractor = nullptr;
    AMediaCodec* mCodec = nullptr;
    AMediaFormat* mTrackFormat = nullptr;

    AudioFormat mFormat;
    bool mEof = false;
    bool mInputEof = false;
    int64_t mCurrentPositionMs = 0;

    // Buffer to hold residual PCM between decode chunks
    std::vector<uint8_t> mPendingPcm;
    size_t mPendingOffset = 0;
};

} // namespace audio

#endif // MUSIC_PLAYER_AUDIO_DECODER_H
