#ifndef MUSIC_PLAYER_AUDIO_EQUALIZER_H
#define MUSIC_PLAYER_AUDIO_EQUALIZER_H

#include <cstdint>
#include <cstddef>
#include <atomic>
#include <array>

namespace audio {

enum EqualizerPreset : int32_t {
    PRESET_FLAT = 0,
    PRESET_ALPINE_CLARITY = 1,
    PRESET_VOCAL_SEPARATION = 2,
    PRESET_BASS_PUNCH = 3,
    PRESET_ROCK_DYNAMIC = 4,
    PRESET_CUSTOM = 5
};

enum HpfMode : int32_t {
    HPF_OFF = 0,
    HPF_60HZ = 1,
    HPF_80HZ = 2 // Recommended for Alpine door speakers
};

// Second-order Direct Form II Transposed Biquad Filter
struct BiquadCoeffs {
    float b0 = 1.0f;
    float b1 = 0.0f;
    float b2 = 0.0f;
    float a1 = 0.0f;
    float a2 = 0.0f;
};

struct BiquadState {
    float s1 = 0.0f;
    float s2 = 0.0f;

    inline void reset() {
        s1 = 0.0f;
        s2 = 0.0f;
    }

    inline float process(float in, const BiquadCoeffs& c) {
        float out = c.b0 * in + s1;
        s1 = c.b1 * in - c.a1 * out + s2;
        s2 = c.b2 * in - c.a2 * out;
        return out;
    }
};

/**
 * Automotive Audio Equalizer & DSP Engine.
 * Specifically tuned for Blaupunkt Santa Rosa 985 + external processor + Alpine speakers.
 * 
 * Features:
 * - 2nd-order Butterworth Subsonic High-Pass Filter (HPF) to prevent Alpine door woofer excursion distortion.
 * - 5-Band Biquad IIR Parametric EQ with dedicated 250 Hz mud-cut filter.
 * - Pre-cut headroom gain (-2.5 dB) to prevent DAC inter-sample clipping on Santa Rosa 985.
 * - Lock-free, zero-allocation real-time PCM processing.
 */
class AudioEqualizer {
public:
    static constexpr size_t NUM_BANDS = 5;
    static constexpr size_t MAX_CHANNELS = 2;

    AudioEqualizer();
    ~AudioEqualizer() = default;

    void setEnabled(bool enabled);
    bool isEnabled() const { return mEnabled.load(std::memory_order_relaxed); }

    void setPreset(int32_t preset);
    int32_t getPreset() const { return mCurrentPreset.load(std::memory_order_relaxed); }

    void setBandGain(size_t bandIndex, float gainDb);
    float getBandGain(size_t bandIndex) const;

    void setHpfMode(int32_t mode);
    int32_t getHpfMode() const { return mHpfMode.load(std::memory_order_relaxed); }

    // In-place 16-bit stereo PCM processing
    void process(int16_t* pcmData, size_t sampleCount, int32_t sampleRate, int32_t channels);

    void reset();

private:
    void updateCoefficients(int32_t sampleRate);
    void applyPresetGains(int32_t preset);

    std::atomic<bool> mEnabled{true};
    std::atomic<int32_t> mCurrentPreset{PRESET_ALPINE_CLARITY};
    std::atomic<int32_t> mHpfMode{HPF_80HZ};

    // 5 target frequencies:
    // Band 0: 60 Hz (Sub-bass / Low punch)
    // Band 1: 250 Hz (Mud Zone - car door acoustic cavity resonance)
    // Band 2: 1000 Hz (Vocal midrange body)
    // Band 3: 3500 Hz (Vocal presence / instrument clarity)
    // Band 4: 10000 Hz (Treble air & Alpine tweeter shimmer)
    static constexpr std::array<float, NUM_BANDS> BAND_FREQUENCIES = {
        60.0f, 250.0f, 1000.0f, 3500.0f, 10000.0f
    };

    static constexpr std::array<float, NUM_BANDS> BAND_Q = {
        0.9f, 1.2f, 1.0f, 1.2f, 0.8f
    };

    std::array<std::atomic<float>, NUM_BANDS> mBandGainsDb;

    // Internal DSP state
    int32_t mLastSampleRate = 44100;
    std::atomic<bool> mCoeffsDirty{true};

    // Coeffs computed for active sample rate
    std::array<BiquadCoeffs, NUM_BANDS> mBandCoeffs{};
    BiquadCoeffs mHpfCoeffs{};
    float mPreampGain = 0.75f; // -2.5 dB pre-cut for DAC headroom

    // Filter states per channel (L / R)
    std::array<std::array<BiquadState, NUM_BANDS>, MAX_CHANNELS> mBandStates{};
    std::array<BiquadState, MAX_CHANNELS> mHpfStates{};
};

} // namespace audio

#endif // MUSIC_PLAYER_AUDIO_EQUALIZER_H
