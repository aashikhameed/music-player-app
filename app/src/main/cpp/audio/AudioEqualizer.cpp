#include "AudioEqualizer.h"
#include <cmath>
#include <algorithm>

namespace audio {

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

AudioEqualizer::AudioEqualizer() {
    // Default to Acoustic Clarity preset
    applyPresetGains(PRESET_ACOUSTIC_CLARITY);
    updateCoefficients(44100);
}

void AudioEqualizer::setEnabled(bool enabled) {
    mEnabled.store(enabled, std::memory_order_relaxed);
}

void AudioEqualizer::setPreset(int32_t preset) {
    mCurrentPreset.store(preset, std::memory_order_relaxed);
    if (preset != PRESET_CUSTOM) {
        applyPresetGains(preset);
    }
}

void AudioEqualizer::applyPresetGains(int32_t preset) {
    switch (preset) {
        case PRESET_FLAT:
            mHpfMode.store(HPF_OFF, std::memory_order_relaxed);
            mBandGainsDb[0].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[1].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[2].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[3].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[4].store(0.0f, std::memory_order_relaxed);
            break;

        case PRESET_ACOUSTIC_CLARITY:
            // Tuned for vehicle cabin + external processor + door speakers:
            // - HPF at 80 Hz eliminates woofer excursion distortion
            // - Mud cut (-4.0 dB) at 250 Hz eliminates hollow door resonance
            // - +1.0 dB at 1 kHz brings vocal fundamentals forward
            // - +2.0 dB at 3.5 kHz adds crisp vocal/instrument separation
            // - +3.0 dB at 10 kHz brings out tweeter sparkle
            mHpfMode.store(HPF_80HZ, std::memory_order_relaxed);
            mBandGainsDb[0].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[1].store(-4.0f, std::memory_order_relaxed);
            mBandGainsDb[2].store(1.0f, std::memory_order_relaxed);
            mBandGainsDb[3].store(2.0f, std::memory_order_relaxed);
            mBandGainsDb[4].store(3.0f, std::memory_order_relaxed);
            break;

        case PRESET_VOCAL_SEPARATION:
            // Heavy vocal isolation with maximum acoustic mud suppression
            mHpfMode.store(HPF_80HZ, std::memory_order_relaxed);
            mBandGainsDb[0].store(-2.0f, std::memory_order_relaxed);
            mBandGainsDb[1].store(-5.0f, std::memory_order_relaxed);
            mBandGainsDb[2].store(2.0f, std::memory_order_relaxed);
            mBandGainsDb[3].store(3.0f, std::memory_order_relaxed);
            mBandGainsDb[4].store(1.0f, std::memory_order_relaxed);
            break;

        case PRESET_BASS_PUNCH:
            // Tight, fast mid-bass punch without door boominess
            mHpfMode.store(HPF_60HZ, std::memory_order_relaxed);
            mBandGainsDb[0].store(3.0f, std::memory_order_relaxed);
            mBandGainsDb[1].store(-3.0f, std::memory_order_relaxed);
            mBandGainsDb[2].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[3].store(1.0f, std::memory_order_relaxed);
            mBandGainsDb[4].store(2.0f, std::memory_order_relaxed);
            break;

        case PRESET_ROCK_DYNAMIC:
            mHpfMode.store(HPF_60HZ, std::memory_order_relaxed);
            mBandGainsDb[0].store(2.0f, std::memory_order_relaxed);
            mBandGainsDb[1].store(-2.0f, std::memory_order_relaxed);
            mBandGainsDb[2].store(0.0f, std::memory_order_relaxed);
            mBandGainsDb[3].store(2.0f, std::memory_order_relaxed);
            mBandGainsDb[4].store(3.0f, std::memory_order_relaxed);
            break;

        default:
            break;
    }
    mCoeffsDirty.store(true, std::memory_order_release);
}

void AudioEqualizer::setBandGain(size_t bandIndex, float gainDb) {
    if (bandIndex < NUM_BANDS) {
        // Clamp to [-12 dB, +12 dB]
        float clamped = std::max(-12.0f, std::min(12.0f, gainDb));
        mBandGainsDb[bandIndex].store(clamped, std::memory_order_relaxed);
        mCurrentPreset.store(PRESET_CUSTOM, std::memory_order_relaxed);
        mCoeffsDirty.store(true, std::memory_order_release);
    }
}

float AudioEqualizer::getBandGain(size_t bandIndex) const {
    if (bandIndex < NUM_BANDS) {
        return mBandGainsDb[bandIndex].load(std::memory_order_relaxed);
    }
    return 0.0f;
}

void AudioEqualizer::setHpfMode(int32_t mode) {
    mHpfMode.store(mode, std::memory_order_relaxed);
    mCoeffsDirty.store(true, std::memory_order_release);
}

void AudioEqualizer::setTimeAlignmentMode(int32_t mode) {
    mTimeAlignmentMode.store(mode, std::memory_order_relaxed);
}

void AudioEqualizer::setBassNormalizerMode(int32_t mode) {
    mBassNormalizerMode.store(mode, std::memory_order_relaxed);
}

void AudioEqualizer::setVirtualSurroundMode(int32_t mode) {
    mVirtualSurroundMode.store(mode, std::memory_order_relaxed);
}

void AudioEqualizer::setSpeedCompensation(float speedKmh, int32_t mode) {
    mCurrentSpeedKmh.store(speedKmh, std::memory_order_relaxed);
    mScvcMode.store(mode, std::memory_order_relaxed);
    if (mode == SCVC_OFF || speedKmh <= 15.0f) {
        mSpeedGainDb.store(0.0f, std::memory_order_relaxed);
        return;
    }

    float excessSpeed = speedKmh - 15.0f; // Compensation starts above 15 km/h
    float db = 0.0f;
    switch (mode) {
        case SCVC_MILD:
            db = std::min(3.0f, excessSpeed * (1.0f / 30.0f));
            break;
        case SCVC_MODERATE:
            db = std::min(5.0f, excessSpeed * (1.5f / 20.0f));
            break;
        case SCVC_DYNAMIC:
            db = std::min(7.0f, excessSpeed * (2.0f / 15.0f));
            break;
        default:
            db = 0.0f;
            break;
    }
    mSpeedGainDb.store(db, std::memory_order_relaxed);
}

void AudioEqualizer::setBalance(float balance) {
    // Clamp to [-1.0f, 1.0f]
    mBalance.store(std::max(-1.0f, std::min(1.0f, balance)), std::memory_order_relaxed);
}

void AudioEqualizer::setFader(float fader) {
    // Clamp to [-1.0f, 1.0f]
    mFader.store(std::max(-1.0f, std::min(1.0f, fader)), std::memory_order_relaxed);
}

void AudioEqualizer::reset() {
    for (size_t ch = 0; ch < MAX_CHANNELS; ++ch) {
        for (size_t b = 0; b < NUM_BANDS; ++b) {
            mBandStates[ch][b].reset();
        }
        mHpfStates[ch].reset();
    }
    mDelayBufferL.fill(0.0f);
    mDelayBufferR.fill(0.0f);
    mDelayIndexL = 0;
    mDelayIndexR = 0;
}

static BiquadCoeffs computeLowShelf(float f0, float fs, float gainDb, float Q) {
    BiquadCoeffs c;
    if (std::abs(gainDb) < 0.05f) {
        c.b0 = 1.0f; c.b1 = 0.0f; c.b2 = 0.0f;
        c.a1 = 0.0f; c.a2 = 0.0f;
        return c;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float w0 = static_cast<float>(2.0 * M_PI * f0 / fs);
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float alpha = sin_w0 / (2.0f * Q);
    float two_sqrt_A_alpha = 2.0f * std::sqrt(A) * alpha;

    float b0 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 + two_sqrt_A_alpha);
    float b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cos_w0);
    float b2 = A * ((A + 1.0f) - (A - 1.0f) * cos_w0 - two_sqrt_A_alpha);
    float a0 = (A + 1.0f) + (A - 1.0f) * cos_w0 + two_sqrt_A_alpha;
    float a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cos_w0);
    float a2 = (A + 1.0f) + (A - 1.0f) * cos_w0 - two_sqrt_A_alpha;

    float invA0 = 1.0f / a0;
    c.b0 = b0 * invA0;
    c.b1 = b1 * invA0;
    c.b2 = b2 * invA0;
    c.a1 = a1 * invA0;
    c.a2 = a2 * invA0;
    return c;
}

static BiquadCoeffs computePeakingEQ(float f0, float fs, float gainDb, float Q) {
    BiquadCoeffs c;
    if (std::abs(gainDb) < 0.05f) {
        c.b0 = 1.0f; c.b1 = 0.0f; c.b2 = 0.0f;
        c.a1 = 0.0f; c.a2 = 0.0f;
        return c;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float w0 = static_cast<float>(2.0 * M_PI * f0 / fs);
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float alpha = sin_w0 / (2.0f * Q);

    float b0 = 1.0f + alpha * A;
    float b1 = -2.0f * cos_w0;
    float b2 = 1.0f - alpha * A;
    float a0 = 1.0f + alpha / A;
    float a1 = -2.0f * cos_w0;
    float a2 = 1.0f - alpha / A;

    float invA0 = 1.0f / a0;
    c.b0 = b0 * invA0;
    c.b1 = b1 * invA0;
    c.b2 = b2 * invA0;
    c.a1 = a1 * invA0;
    c.a2 = a2 * invA0;
    return c;
}

static BiquadCoeffs computeHighShelf(float f0, float fs, float gainDb, float Q) {
    BiquadCoeffs c;
    if (std::abs(gainDb) < 0.05f) {
        c.b0 = 1.0f; c.b1 = 0.0f; c.b2 = 0.0f;
        c.a1 = 0.0f; c.a2 = 0.0f;
        return c;
    }

    float A = std::pow(10.0f, gainDb / 40.0f);
    float w0 = static_cast<float>(2.0 * M_PI * f0 / fs);
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float alpha = sin_w0 / (2.0f * Q);
    float two_sqrt_A_alpha = 2.0f * std::sqrt(A) * alpha;

    float b0 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 + two_sqrt_A_alpha);
    float b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cos_w0);
    float b2 = A * ((A + 1.0f) + (A - 1.0f) * cos_w0 - two_sqrt_A_alpha);
    float a0 = (A + 1.0f) - (A - 1.0f) * cos_w0 + two_sqrt_A_alpha;
    float a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cos_w0);
    float a2 = (A + 1.0f) - (A - 1.0f) * cos_w0 - two_sqrt_A_alpha;

    float invA0 = 1.0f / a0;
    c.b0 = b0 * invA0;
    c.b1 = b1 * invA0;
    c.b2 = b2 * invA0;
    c.a1 = a1 * invA0;
    c.a2 = a2 * invA0;
    return c;
}

static BiquadCoeffs computeButterworthHPF(float f0, float fs) {
    BiquadCoeffs c;
    float w0 = static_cast<float>(2.0 * M_PI * f0 / fs);
    float cos_w0 = std::cos(w0);
    float sin_w0 = std::sin(w0);
    float alpha = sin_w0 / (2.0f * 0.70710678f); // Q = sqrt(2)/2 = 0.7071

    float b0 = (1.0f + cos_w0) * 0.5f;
    float b1 = -(1.0f + cos_w0);
    float b2 = (1.0f + cos_w0) * 0.5f;
    float a0 = 1.0f + alpha;
    float a1 = -2.0f * cos_w0;
    float a2 = 1.0f - alpha;

    float invA0 = 1.0f / a0;
    c.b0 = b0 * invA0;
    c.b1 = b1 * invA0;
    c.b2 = b2 * invA0;
    c.a1 = a1 * invA0;
    c.a2 = a2 * invA0;
    return c;
}

void AudioEqualizer::updateCoefficients(int32_t sampleRate) {
    mLastSampleRate = sampleRate;
    float fs = static_cast<float>(sampleRate);

    // 1. HPF Filter
    int32_t hpf = mHpfMode.load(std::memory_order_relaxed);
    if (hpf == HPF_80HZ) {
        mHpfCoeffs = computeButterworthHPF(80.0f, fs);
    } else if (hpf == HPF_60HZ) {
        mHpfCoeffs = computeButterworthHPF(60.0f, fs);
    } else {
        mHpfCoeffs.b0 = 1.0f; mHpfCoeffs.b1 = 0.0f; mHpfCoeffs.b2 = 0.0f;
        mHpfCoeffs.a1 = 0.0f; mHpfCoeffs.a2 = 0.0f;
    }

    // 2. Band 0: Low-Shelf @ 60 Hz
    float g0 = mBandGainsDb[0].load(std::memory_order_relaxed);
    mBandCoeffs[0] = computeLowShelf(BAND_FREQUENCIES[0], fs, g0, BAND_Q[0]);

    // 3. Bands 1..3: Peaking EQ
    for (size_t i = 1; i < 4; ++i) {
        float g = mBandGainsDb[i].load(std::memory_order_relaxed);
        mBandCoeffs[i] = computePeakingEQ(BAND_FREQUENCIES[i], fs, g, BAND_Q[i]);
    }

    // 4. Band 4: High-Shelf @ 10 kHz
    float g4 = mBandGainsDb[4].load(std::memory_order_relaxed);
    mBandCoeffs[4] = computeHighShelf(BAND_FREQUENCIES[4], fs, g4, BAND_Q[4]);

    // 5. Digital Headroom / Preamp Calculation
    // Find maximum boost across all bands and ensure at least 2.5 dB headroom
    float maxBoost = 0.0f;
    for (size_t i = 0; i < NUM_BANDS; ++i) {
        float g = mBandGainsDb[i].load(std::memory_order_relaxed);
        if (g > maxBoost) maxBoost = g;
    }
    // Base pre-cut is -2.5 dB (0.75f) to prevent DAC inter-sample clipping on automotive headunit
    float totalPrecutDb = -2.5f - std::max(0.0f, maxBoost * 0.75f);
    mPreampGain = std::pow(10.0f, totalPrecutDb / 20.0f);

    mCoeffsDirty.store(false, std::memory_order_release);
}

void AudioEqualizer::process(int16_t* pcmData, size_t sampleCount, int32_t sampleRate, int32_t channels) {
    if (!mEnabled.load(std::memory_order_relaxed) || pcmData == nullptr || sampleCount == 0) {
        return;
    }

    if (mCoeffsDirty.load(std::memory_order_acquire) || sampleRate != mLastSampleRate) {
        updateCoefficients(sampleRate);
    }

    int32_t hpf = mHpfMode.load(std::memory_order_relaxed);
    int32_t timeAlign = mTimeAlignmentMode.load(std::memory_order_relaxed);
    int32_t bassNorm = mBassNormalizerMode.load(std::memory_order_relaxed);
    int32_t surroundMode = mVirtualSurroundMode.load(std::memory_order_relaxed);
    bool applyHpf = (hpf != HPF_OFF);
    size_t numChannels = (channels == 1) ? 1 : 2;

    float preamp = mPreampGain;

    // Speed-Compensated Volume Control (SCVC) Gain Multiplier
    float speedDb = mSpeedGainDb.load(std::memory_order_relaxed);
    float speedMultiplier = (speedDb > 0.01f) ? std::pow(10.0f, speedDb / 20.0f) : 1.0f;

    // 4-Way Balance & Fader Gains
    float balance = mBalance.load(std::memory_order_relaxed);
    float fader = mFader.load(std::memory_order_relaxed);
    float balL = (balance <= 0.0f) ? 1.0f : (1.0f - balance);
    float balR = (balance >= 0.0f) ? 1.0f : (1.0f + balance);
    float faderFrontGain = (fader >= 0.0f) ? (1.0f + fader * 0.15f) : (1.0f + fader * 0.3f);

    float totalGainL = balL * faderFrontGain * speedMultiplier;
    float totalGainR = balR * faderFrontGain * speedMultiplier;

    if (numChannels == 2) {
        // Stereo interleaved PCM [L, R, L, R, ...]
        size_t numFrames = sampleCount / 2;
        for (size_t f = 0; f < numFrames; ++f) {
            size_t idxL = f * 2;
            size_t idxR = idxL + 1;

            float sampleL = static_cast<float>(pcmData[idxL]) * preamp;
            float sampleR = static_cast<float>(pcmData[idxR]) * preamp;

            // Subsonic HPF
            if (applyHpf) {
                sampleL = mHpfStates[0].process(sampleL, mHpfCoeffs);
                sampleR = mHpfStates[1].process(sampleR, mHpfCoeffs);
            }

            // 5 Biquad Bands
            for (size_t b = 0; b < NUM_BANDS; ++b) {
                sampleL = mBandStates[0][b].process(sampleL, mBandCoeffs[b]);
                sampleR = mBandStates[1][b].process(sampleR, mBandCoeffs[b]);
            }

            // Bass Normalizer & Dynamic Loudness Contour
            if (bassNorm != BASS_NORM_OFF) {
                float contour = (bassNorm == BASS_NORM_DEEP) ? 1.35f : 1.18f;
                sampleL *= contour;
                sampleR *= contour;
                // Warm acoustic soft knee
                if (sampleL > 28000.0f) sampleL = 28000.0f + (sampleL - 28000.0f) * 0.45f;
                else if (sampleL < -28000.0f) sampleL = -28000.0f + (sampleL + 28000.0f) * 0.45f;
                if (sampleR > 28000.0f) sampleR = 28000.0f + (sampleR - 28000.0f) * 0.45f;
                else if (sampleR < -28000.0f) sampleR = -28000.0f + (sampleR + 28000.0f) * 0.45f;
            }

            // Virtual Surround / Stereo Stage Widener
            if (surroundMode != SURROUND_OFF) {
                float width = (surroundMode == SURROUND_WIDE) ? 1.45f : 1.25f;
                float mid = (sampleL + sampleR) * 0.5f;
                float side = (sampleL - sampleR) * 0.5f * width;
                sampleL = mid + side;
                sampleR = mid - side;
            }

            // Driver Seat Soundstage Time Alignment (~1.0 ms micro-delay)
            if (timeAlign == ALIGN_DRIVER_RHD) {
                // Delay Right channel so sound reaches driver simultaneously with Left speaker
                float delayedR = mDelayBufferR[mDelayIndexR];
                mDelayBufferR[mDelayIndexR] = sampleR;
                mDelayIndexR = (mDelayIndexR + 1) % TIME_ALIGN_DELAY_SAMPLES;
                sampleR = delayedR;
            } else if (timeAlign == ALIGN_DRIVER_LHD) {
                // Delay Left channel for LHD driver seat
                float delayedL = mDelayBufferL[mDelayIndexL];
                mDelayBufferL[mDelayIndexL] = sampleL;
                mDelayIndexL = (mDelayIndexL + 1) % TIME_ALIGN_DELAY_SAMPLES;
                sampleL = delayedL;
            }

            // Apply SCVC & Balance/Fader Gains
            sampleL *= totalGainL;
            sampleR *= totalGainR;

            // Soft saturation / clamping back to 16-bit signed PCM
            pcmData[idxL] = static_cast<int16_t>(std::clamp(sampleL, -32767.0f, 32767.0f));
            pcmData[idxR] = static_cast<int16_t>(std::clamp(sampleR, -32767.0f, 32767.0f));
        }
    } else {
        // Mono PCM
        float monoGain = faderFrontGain * speedMultiplier;
        for (size_t s = 0; s < sampleCount; ++s) {
            float sample = static_cast<float>(pcmData[s]) * preamp;

            if (applyHpf) {
                sample = mHpfStates[0].process(sample, mHpfCoeffs);
            }

            for (size_t b = 0; b < NUM_BANDS; ++b) {
                sample = mBandStates[0][b].process(sample, mBandCoeffs[b]);
            }

            if (bassNorm != BASS_NORM_OFF) {
                float contour = (bassNorm == BASS_NORM_DEEP) ? 1.35f : 1.18f;
                sample *= contour;
                if (sample > 28000.0f) sample = 28000.0f + (sample - 28000.0f) * 0.45f;
                else if (sample < -28000.0f) sample = -28000.0f + (sample + 28000.0f) * 0.45f;
            }

            sample *= monoGain;
            pcmData[s] = static_cast<int16_t>(std::clamp(sample, -32767.0f, 32767.0f));
        }
    }
}

} // namespace audio
