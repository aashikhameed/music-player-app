#ifndef MUSIC_PLAYER_TYPES_H
#define MUSIC_PLAYER_TYPES_H

#include <cstdint>
#include <string>

namespace audio {

// Playback state machine values
enum class PlaybackState : int32_t {
    IDLE = 0,
    INITIALIZED = 1,
    PREPARING = 2,
    PLAYING = 3,
    PAUSED = 4,
    STOPPED = 5,
    COMPLETED = 6,
    ERROR = -1
};

// Return result codes
enum class ResultCode : int32_t {
    OK = 0,
    ERROR_GENERIC = -1,
    ERROR_FILE_NOT_FOUND = -2,
    ERROR_UNSUPPORTED_FORMAT = -3,
    ERROR_CORRUPTED_FILE = -4,
    ERROR_INSUFFICIENT_MEMORY = -5,
    ERROR_AUDIO_ENGINE_FAILED = -6,
    ERROR_SEEK_FAILED = -7,
    ERROR_PERMISSION_DENIED = -8,
    ERROR_INVALID_STATE = -9
};

// Repeat mode
enum class RepeatMode : int32_t {
    OFF = 0,
    ONE = 1,
    ALL = 2
};

// Audio format metadata for decoding and playback
struct AudioFormat {
    int32_t sampleRate = 44100;
    int32_t channelCount = 2;
    int32_t bitDepth = 16;
    int64_t durationMs = 0;
    int32_t bitrate = 0;
    std::string mimeType;

    int32_t bytesPerFrame() const {
        return channelCount * (bitDepth / 8);
    }
};

// Track metadata extracted by native scanner/demuxer
struct TrackMetadata {
    std::string path;
    std::string title;
    std::string artist;
    std::string album;
    int64_t durationMs = 0;
    int32_t bitrate = 0;
    int32_t sampleRate = 0;
    int32_t channels = 0;
    std::string mimeType;
};

} // namespace audio

#endif // MUSIC_PLAYER_TYPES_H
