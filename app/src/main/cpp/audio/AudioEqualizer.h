#ifndef MUSIC_PLAYER_AUDIO_EQUALIZER_H
#define MUSIC_PLAYER_AUDIO_EQUALIZER_H

#include <cstdint>
#include <cstddef>
#include <atomic>
#include <array>

namespace audio {

enum EqualizerPreset : int32_t {
    PRESET_FLAT = 0,
    PRESET_ACOUSTIC_CLARITY = 1,
    PRESET_VOCAL_SEPARATION = 2,
    PRESET_BASS_PUNCH = 3,
    PRESET_ROCK_DYNAMIC = 4,
    PRESET_CUSTOM = 5
};

enum HpfMode : int32_t {
    HPF_OFF = 0,
    HPF_60HZ = 1,
    HPF_80HZ = 2 // Recommended for door speakers
};

enum TimeAlignmentMode : int32_t {
    ALIGN_OFF = 0,
    ALIGN_DRIVER_RHD = 1, // Delays Right speaker (~1.0ms) for RHD driver seat
    ALIGN_DRIVER_LHD = 2  // Delays Left speaker (~1.0ms) for LHD driver seat
};

enum BassNormalizerMode : int32_t {
    BASS_NORM_OFF = 0,
    BASS_NORM_MILD = 1,   // Dynamic +2 dB low-end contour & peak control
    BASS_NORM_DEEP = 2    // Full Deep Dynamic Loudness (+4 dB warmth)
};

enum VirtualSurroundMode : int32_t {
    SURROUND_OFF = 0,
    SURROUND_CABIN = 1,   // Mild spatial stage widening (+25% side channel)
    SURROUND_WIDE = 2     // Full automotive concert soundstage (+50% side channel)
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
 * Specifically tuned for vehicle acoustics + external processor + door speakers.
 * 
 * Features:
 * - 2nd-order Butterworth Subsonic High-Pass Filter (HPF) to prevent door woofer excursion distortion.
 * - 5-Band Biquad IIR Parametric EQ with dedicated 250 Hz mud-cut filter.
 * - Pre-cut headroom gain (-2.5 dB) to prevent DAC inter-sample clipping on automotive headunit.
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

    void setTimeAlignmentMode(int32_t mode);
    int32_t getTimeAlignmentMode() const { return mTimeAlignmentMode.load(std::memory_order_relaxed); }

    void setBassNormalizerMode(int32_t mode);
    int32_t getBassNormalizerMode() const { return mBassNormalizerMode.load(std::memory_order_relaxed); }

    void setVirtualSurroundMode(int32_t mode);
    int32_t getVirtualSurroundMode() const { return mVirtualSurroundMode.load(std::memory_order_relaxed); }

    // In-place 16-bit stereo PCM processing
    void process(int16_t* pcmData, size_t sampleCount, int32_t sampleRate, int32_t channels);

    void reset();

    void copySettingsFrom(const AudioEqualizer& other) {
        mEnabled.store(other.mEnabled.load(std::memory_order_relaxed), std::memory_order_relaxed);
        mCurrentPreset.store(other.mCurrentPreset.load(std::memory_order_relaxed), std::memory_order_relaxed);
        mHpfMode.store(other.mHpfMode.load(std::memory_order_relaxed), std::memory_order_relaxed);
        mTimeAlignmentMode.store(other.mTimeAlignmentMode.load(std::memory_order_relaxed), std::memory_order_relaxed);
        mBassNormalizerMode.store(other.mBassNormalizerMode.load(std::memory_order_relaxed), std::memory_order_relaxed);
        mVirtualSurroundMode.store(other.mVirtualSurroundMode.load(std::memory_order_relaxed), std::memory_order_relaxed);
        for (size_t i = 0; i < NUM_BANDS; ++i) {
            mBandGainsDb[i].store(other.mBandGainsDb[i].load(std::memory_order_relaxed), std::memory_order_relaxed);
        }
        mCoeffsDirty.store(true, std::memory_order_relaxed);
    }

private:
    void updateCoefficients(int32_t sampleRate);
    void applyPresetGains(int32_t preset);

    std::atomic<bool> mEnabled{true};
    std::atomic<int32_t> mCurrentPreset{PRESET_ACOUSTIC_CLARITY};
    std::atomic<int32_t> mHpfMode{HPF_80HZ};
    std::atomic<int32_t> mTimeAlignmentMode{ALIGN_OFF};
    std::atomic<int32_t> mBassNormalizerMode{BASS_NORM_OFF};
    std::atomic<int32_t> mVirtualSurroundMode{SURROUND_OFF};

    // 5 target frequencies:
    // Band 0: 60 Hz (Sub-bass / Low punch)
    // Band 1: 250 Hz (Mud Zone - car door acoustic cavity resonance)
    // Band 2: 1000 Hz (Vocal midrange body)
    // Band 3: 3500 Hz (Vocal presence / instrument clarity)
    // Band 4: 10000 Hz (Treble air & tweeter shimmer)
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

    // Time-alignment circular delay line (~1.0 ms delay buffer, 48 samples)
    static constexpr size_t TIME_ALIGN_DELAY_SAMPLES = 48;
    std::array<float, TIME_ALIGN_DELAY_SAMPLES> mDelayBufferL{};
    std::array<float, TIME_ALIGN_DELAY_SAMPLES> mDelayBufferR{};
    size_t mDelayIndexL = 0;
    size_t mDelayIndexR = 0;
};

} // namespace audio

#endif // MUSIC_PLAYER_AUDIO_EQUALIZER_H
