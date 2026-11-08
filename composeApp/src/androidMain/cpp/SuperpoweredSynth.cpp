#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <atomic>
#include <algorithm>
#include <mutex>
#include <cstring>
#include "superpowered/SuperpoweredSimple.h"
#include "superpowered/SuperpoweredAndroidAudioIO.h"

#define TAG "SuperpoweredSynth"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

enum WaveformType { SINE = 0, SQUARE = 1, TRIANGLE = 2, SAWTOOTH = 3 };
enum EffectId { FX_ECHO = 0, FX_DELAY = 1, FX_BITCRUSHER = 2, FX_REVERB = 3, FX_WHOOSH = 4, FX_DISTORTION = 5 };
enum FilterPresetId { F_LOWPASS = 0, F_HIGHPASS = 1, F_LOWSHELF = 2, F_HIGHSHELF = 3, F_BANDPASS = 4, F_NOTCH = 5, F_PARAMETRIC = 6 };

static const int MAX_DELAY_SAMPLES = 88200;

static const int FFT_SIZE = 2048;
static const int SPECTRUM_BANDS = 512;
static const int MAX_EQ_BANDS = 32;

static_assert(std::atomic<float>::is_always_lock_free,
              "Real-time controls require lock-free float atomics");
static_assert(std::atomic<int>::is_always_lock_free,
              "Real-time controls require lock-free int atomics");

static void fft_inplace(float* re, float* im, int n) {
    for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) { std::swap(re[i], re[j]); std::swap(im[i], im[j]); }
    }
    for (int len = 2; len <= n; len <<= 1) {
        float ang = -2.0f * (float)M_PI / len;
        float wRe = cosf(ang), wIm = sinf(ang);
        for (int i = 0; i < n; i += len) {
            float curRe = 1.0f, curIm = 0.0f;
            for (int k = 0; k < len / 2; k++) {
                float uRe = re[i+k], uIm = im[i+k];
                float vRe = re[i+k+len/2]*curRe - im[i+k+len/2]*curIm;
                float vIm = re[i+k+len/2]*curIm + im[i+k+len/2]*curRe;
                re[i+k] = uRe+vRe; im[i+k] = uIm+vIm;
                re[i+k+len/2] = uRe-vRe; im[i+k+len/2] = uIm-vIm;
                float nr = curRe*wRe - curIm*wIm;
                curIm = curRe*wIm + curIm*wRe;
                curRe = nr;
            }
        }
    }
}

struct EchoDelay {
    float buffer[MAX_DELAY_SAMPLES] = {};
    int writePos = 0;
    int delaySamples;
    float feedback;
    EchoDelay(int d, float f) : delaySamples(d), feedback(f) {}
    float process(float in) {
        int readPos = (writePos - delaySamples + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
        float out = in + buffer[readPos] * feedback;
        buffer[writePos] = out;
        writePos = (writePos + 1) % MAX_DELAY_SAMPLES;
        return out;
    }
};

struct ReverbAllpass {
    float buffer[4096] = {};
    int pos = 0;
    int size;
    float gain;
    ReverbAllpass(int s, float g) : size(s), gain(g) {}
    float process(float in) {
        float delayed = buffer[pos];
        float out = -in + delayed;
        buffer[pos] = in + delayed * gain;
        pos = (pos + 1) % size;
        return out;
    }
};

struct SimpleComb {
    float buffer[8192] = {};
    int pos = 0;
    int size;
    float feedback;
    float filterStore = 0;
    float damp;
    SimpleComb(int s, float fb, float d) : size(s), feedback(fb), damp(d) {}
    float process(float in) {
        float out = buffer[pos];
        filterStore = out * (1.0f - damp) + filterStore * damp;
        buffer[pos] = in + filterStore * feedback;
        pos = (pos + 1) % size;
        return out;
    }
};

struct BiquadFilter {
    float b0 = 1, b1 = 0, b2 = 0, a1 = 0, a2 = 0;
    float x1 = 0, x2 = 0, y1 = 0, y2 = 0;

    void setLowPass(float freq, float sr, float q = 0.707f) {
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0 = (1.0f - cosW) / 2.0f / a0;
        b1 = (1.0f - cosW) / a0;
        b2 = b0;
        a1 = -2.0f * cosW / a0;
        a2 = (1.0f - alpha) / a0;
    }

    void setHighPass(float freq, float sr, float q = 0.707f) {
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0 = (1.0f + cosW) / 2.0f / a0;
        b1 = -(1.0f + cosW) / a0;
        b2 = b0;
        a1 = -2.0f * cosW / a0;
        a2 = (1.0f - alpha) / a0;
    }

    void setLowShelf(float freq, float sr, float gainDb = 6.0f) {
        float A = powf(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / 2.0f * sqrtf((A + 1.0f / A) * (1.0f / 0.9f - 1.0f) + 2.0f);
        float a0 = (A + 1.0f) + (A - 1.0f) * cosW + 2.0f * sqrtf(A) * alpha;
        b0 = A * ((A + 1.0f) - (A - 1.0f) * cosW + 2.0f * sqrtf(A) * alpha) / a0;
        b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW) / a0;
        b2 = A * ((A + 1.0f) - (A - 1.0f) * cosW - 2.0f * sqrtf(A) * alpha) / a0;
        a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosW) / a0;
        a2 = ((A + 1.0f) + (A - 1.0f) * cosW - 2.0f * sqrtf(A) * alpha) / a0;
    }

    void setHighShelf(float freq, float sr, float gainDb = 6.0f) {
        float A = powf(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / 2.0f * sqrtf((A + 1.0f / A) * (1.0f / 0.9f - 1.0f) + 2.0f);
        float a0 = (A + 1.0f) - (A - 1.0f) * cosW + 2.0f * sqrtf(A) * alpha;
        b0 = A * ((A + 1.0f) + (A - 1.0f) * cosW + 2.0f * sqrtf(A) * alpha) / a0;
        b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW) / a0;
        b2 = A * ((A + 1.0f) + (A - 1.0f) * cosW - 2.0f * sqrtf(A) * alpha) / a0;
        a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosW) / a0;
        a2 = ((A + 1.0f) - (A - 1.0f) * cosW - 2.0f * sqrtf(A) * alpha) / a0;
    }

    void setBandPass(float freq, float sr, float q = 1.0f) {
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0 = alpha / a0;
        b1 = 0.0f;
        b2 = -alpha / a0;
        a1 = -2.0f * cosW / a0;
        a2 = (1.0f - alpha) / a0;
    }

    void setNotch(float freq, float sr, float q = 1.0f) {
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0 = 1.0f / a0;
        b1 = -2.0f * cosW / a0;
        b2 = b0;
        a1 = b1;
        a2 = (1.0f - alpha) / a0;
    }

    void setParametric(float freq, float sr, float gainDb = 0.0f, float q = 1.0f) {
        float A = powf(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * M_PI * freq / sr;
        float cosW = cosf(w0), sinW = sinf(w0);
        float alpha = sinW / (2.0f * q);
        float a0 = 1.0f + alpha / A;
        b0 = (1.0f + alpha * A) / a0;
        b1 = -2.0f * cosW / a0;
        b2 = (1.0f - alpha * A) / a0;
        a1 = b1;
        a2 = (1.0f - alpha / A) / a0;
    }

    float process(float in) {
        float out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1; x1 = in;
        y2 = y1; y1 = out;
        return out;
    }
};

struct EqBandState {
    float frequency;
    float gainDb;
    float q;
    BiquadFilter filter;
};

class SuperpoweredSynthesizer {
public:
    SuperpoweredSynthesizer() :
            audioIO(nullptr),
            frequency(440.0f),
            amplitude(0.5f),
            blend(0.0f),
            waveformA(SINE),
            waveformB(SINE),
            playing(false),
            tailFramesRemaining(0),
            phase(0.0f),
            samplerate(44100),
            echoWetDry(0.0f),
            delayWetDry(0.0f),
            bitcrusherWetDry(0.0f),
            reverbWetDry(0.0f),
            whooshWetDry(0.0f),
            distortionWetDry(0.0f),
            filterPreset(F_LOWPASS),
            filterFrequency(1000.0f),
            filterWetDry(0.0f),
            echoFx(22050, 0.5f),
            delayFx(14700, 0.3f),
            allpass1(389, 0.5f), allpass2(277, 0.5f),
            comb1(1557, 0.84f, 0.2f), comb2(1617, 0.84f, 0.2f),
            comb3(1491, 0.84f, 0.2f), comb4(1422, 0.84f, 0.2f),
            whooshPhase(0.0f) {

        for (int i = 0; i < MAX_EQ_BANDS; i++) {
            eqConfigFrequencies[i].store(1000.0f, std::memory_order_relaxed);
            eqConfigGainsDb[i].store(0.0f, std::memory_order_relaxed);
            eqConfigQs[i].store(1.4f, std::memory_order_relaxed);
        }
        for (int i = 0; i < SPECTRUM_BANDS; i++) {
            spectrumPublished[i].store(0.0f, std::memory_order_relaxed);
        }

        audioIO = new SuperpoweredAndroidAudioIO(
                samplerate, 1024, false, true,
                audioProcessing, this, -1, -1
        );
        if (audioIO) audioIO->start();
        biquad.setLowPass(filterFrequency.load(std::memory_order_relaxed), (float)samplerate);
    }

    ~SuperpoweredSynthesizer() { delete audioIO; }

    void playSound(float freq, float amp, int wfA, int wfB, float blendFactor) {
        frequency.store(freq, std::memory_order_relaxed);
        amplitude.store(amp, std::memory_order_relaxed);
        waveformA.store(wfA, std::memory_order_relaxed);
        waveformB.store(wfB, std::memory_order_relaxed);
        blend.store(blendFactor, std::memory_order_relaxed);
        tailFramesRemaining.store(0, std::memory_order_relaxed);
        playing.store(true, std::memory_order_release);
    }

    void stopSound() {
        tailFramesRemaining.store(
                currentSampleRate.load(std::memory_order_relaxed) * 2,
                std::memory_order_relaxed
        );
        playing.store(false, std::memory_order_release);
    }

    void setEffect(int effectId, float wetDry) {
        switch (effectId) {
            case FX_ECHO:       echoWetDry.store(wetDry, std::memory_order_relaxed); break;
            case FX_DELAY:      delayWetDry.store(wetDry, std::memory_order_relaxed); break;
            case FX_BITCRUSHER: bitcrusherWetDry.store(wetDry, std::memory_order_relaxed); break;
            case FX_REVERB:     reverbWetDry.store(wetDry, std::memory_order_relaxed); break;
            case FX_WHOOSH:     whooshWetDry.store(wetDry, std::memory_order_relaxed); break;
            case FX_DISTORTION: distortionWetDry.store(wetDry, std::memory_order_relaxed); break;
        }
    }

    void setFilter(int presetId, float freq, float wetDry) {
        filterPreset.store(presetId, std::memory_order_relaxed);
        filterFrequency.store(freq, std::memory_order_relaxed);
        filterWetDry.store(wetDry, std::memory_order_relaxed);
    }

private:
    SuperpoweredAndroidAudioIO *audioIO;
    std::atomic<float> frequency, amplitude, blend;
    std::atomic<int> waveformA, waveformB;
    std::atomic<bool> playing;
    std::atomic<int> tailFramesRemaining;
    float phase;
    unsigned int samplerate;
    std::atomic<int> currentSampleRate { 44100 };

    std::atomic<float> echoWetDry, delayWetDry, bitcrusherWetDry, reverbWetDry, whooshWetDry, distortionWetDry;
    std::atomic<int> filterPreset;
    std::atomic<float> filterFrequency, filterWetDry;
    int appliedFilterPreset = F_LOWPASS;
    float appliedFilterFrequency = 1000.0f;
    int appliedSampleRate = 44100;

    EchoDelay echoFx;
    EchoDelay delayFx;
    ReverbAllpass allpass1, allpass2;
    SimpleComb comb1, comb2, comb3, comb4;
    BiquadFilter biquad;
    float whooshPhase;

    float fftInputBuffer[FFT_SIZE] = {};
    int fftWritePos = 0;
    float fftReScratch[FFT_SIZE] = {};
    float fftImScratch[FFT_SIZE] = {};
    std::atomic<float> spectrumPublished[SPECTRUM_BANDS];

    // EQ configuration is published with a sequence lock. The producer may
    // briefly take eqWriterMutex, but the audio callback never does: it takes
    // one best-effort atomic snapshot and keeps its own filter state.
    std::mutex eqWriterMutex;
    std::atomic<unsigned int> eqConfigSequence { 0 };
    std::atomic<int> eqConfigCount { 0 };
    std::atomic<float> eqConfigFrequencies[MAX_EQ_BANDS];
    std::atomic<float> eqConfigGainsDb[MAX_EQ_BANDS];
    std::atomic<float> eqConfigQs[MAX_EQ_BANDS];
    float pendingEqFrequencies[MAX_EQ_BANDS] = {};
    float pendingEqGainsDb[MAX_EQ_BANDS] = {};
    float pendingEqQs[MAX_EQ_BANDS] = {};
    EqBandState activeEqBands[MAX_EQ_BANDS] = {};
    int activeEqBandCount = 0;
    unsigned int appliedEqSequence = 0;

    void rebuildEqBand(EqBandState& band, int sampleRate) {
        const float nyquistSafeFrequency = std::min(
                std::max(band.frequency, 10.0f),
                sampleRate * 0.45f
        );
        band.filter.setParametric(
                nyquistSafeFrequency,
                (float)sampleRate,
                band.gainDb,
                std::max(band.q, 0.01f)
        );
    }

public:
    void setEqBands(int count, float* frequencies, float* gainsDb, float* qs) {
        std::lock_guard<std::mutex> lock(eqWriterMutex);
        const int safeCount = std::min(std::max(count, 0), MAX_EQ_BANDS);
        if (count > MAX_EQ_BANDS) {
            LOGE("setEqBands: truncating %d bands to real-time limit %d", count, MAX_EQ_BANDS);
        }
        eqConfigSequence.fetch_add(1, std::memory_order_acq_rel); // odd: write in progress
        for (int i = 0; i < safeCount; i++) {
            eqConfigFrequencies[i].store(frequencies[i], std::memory_order_relaxed);
            eqConfigGainsDb[i].store(gainsDb[i], std::memory_order_relaxed);
            eqConfigQs[i].store(qs[i], std::memory_order_relaxed);
        }
        eqConfigCount.store(safeCount, std::memory_order_relaxed);
        eqConfigSequence.fetch_add(1, std::memory_order_release); // even: stable snapshot
    }

    void getSpectrum(float* out, int size) {
        int copy = std::min(size, SPECTRUM_BANDS);
        for (int i = 0; i < copy; i++) {
            out[i] = spectrumPublished[i].load(std::memory_order_relaxed);
        }
        for (int i = copy; i < size; i++) out[i] = 0.0f;
    }

private:

    void refreshEqBands(int sampleRate, bool forceRebuild) {
        const unsigned int before = eqConfigSequence.load(std::memory_order_acquire);
        if ((before & 1u) != 0u || (!forceRebuild && before == appliedEqSequence)) return;

        const int count = std::min(
                std::max(eqConfigCount.load(std::memory_order_relaxed), 0),
                MAX_EQ_BANDS
        );
        for (int i = 0; i < count; i++) {
            pendingEqFrequencies[i] = eqConfigFrequencies[i].load(std::memory_order_relaxed);
            pendingEqGainsDb[i] = eqConfigGainsDb[i].load(std::memory_order_relaxed);
            pendingEqQs[i] = eqConfigQs[i].load(std::memory_order_relaxed);
        }
        const unsigned int after = eqConfigSequence.load(std::memory_order_acquire);
        if (before != after || (after & 1u) != 0u) return;

        activeEqBandCount = count;
        for (int i = 0; i < count; i++) {
            activeEqBands[i].frequency = pendingEqFrequencies[i];
            activeEqBands[i].gainDb = pendingEqGainsDb[i];
            activeEqBands[i].q = pendingEqQs[i];
            rebuildEqBand(activeEqBands[i], sampleRate);
        }
        appliedEqSequence = after;
    }

    void refreshFilter(int sampleRate, bool forceRebuild) {
        const int preset = filterPreset.load(std::memory_order_relaxed);
        const float requestedFrequency = filterFrequency.load(std::memory_order_relaxed);
        if (!forceRebuild && preset == appliedFilterPreset && requestedFrequency == appliedFilterFrequency) return;

        const float safeFrequency = std::min(
                std::max(requestedFrequency, 10.0f),
                sampleRate * 0.45f
        );
        const float sr = (float)sampleRate;
        switch (static_cast<FilterPresetId>(preset)) {
            case F_LOWPASS:    biquad.setLowPass(safeFrequency, sr); break;
            case F_HIGHPASS:   biquad.setHighPass(safeFrequency, sr); break;
            case F_LOWSHELF:   biquad.setLowShelf(safeFrequency, sr); break;
            case F_HIGHSHELF:  biquad.setHighShelf(safeFrequency, sr); break;
            case F_BANDPASS:   biquad.setBandPass(safeFrequency, sr); break;
            case F_NOTCH:      biquad.setNotch(safeFrequency, sr); break;
            case F_PARAMETRIC: biquad.setParametric(safeFrequency, sr); break;
        }
        appliedFilterPreset = preset;
        appliedFilterFrequency = requestedFrequency;
    }

    float generateSample(WaveformType wf, float p) {
        switch (wf) {
            case SINE:     return sinf(p);
            case SQUARE:   return (p < M_PI) ? 1.0f : -1.0f;
            case TRIANGLE: {
                float n = p / (2.0f * M_PI);
                if (n < 0.25f) return n * 4.0f;
                else if (n < 0.75f) return 2.0f - n * 4.0f;
                else return n * 4.0f - 4.0f;
            }
            case SAWTOOTH: {
                float n = p / (2.0f * M_PI);
                return 2.0f * (n - floorf(0.5f + n));
            }
            default: return 0.0f;
        }
    }

    float applyBitcrusher(float sample, float amount) {
        int bits = (int)(1.0f + (1.0f - amount) * 14.0f);
        float levels = powf(2.0f, (float)bits);
        return roundf(sample * levels) / levels;
    }

    float applyReverb(float in) {
        float c1 = comb1.process(in);
        float c2 = comb2.process(in);
        float c3 = comb3.process(in);
        float c4 = comb4.process(in);
        float mixed = (c1 + c2 + c3 + c4) * 0.25f;
        mixed = allpass1.process(mixed);
        mixed = allpass2.process(mixed);
        return mixed;
    }

    static bool audioProcessing(void *clientdata, short int *audio, int numberOfFrames, int sr) {
        return static_cast<SuperpoweredSynthesizer *>(clientdata)->onAudioProcess(audio, numberOfFrames, sr);
    }

    bool onAudioProcess(short int *output, int numberOfFrames, int sr) {
        const int sampleRate = sr > 0 ? sr : 44100;
        currentSampleRate.store(sampleRate, std::memory_order_relaxed);
        const bool sampleRateChanged = sampleRate != appliedSampleRate;
        refreshFilter(sampleRate, sampleRateChanged);
        refreshEqBands(sampleRate, sampleRateChanged);
        appliedSampleRate = sampleRate;

        // Take one coherent-enough scalar snapshot per render buffer. These
        // values are independent controls, so observing adjacent UI updates
        // in different buffers is harmless and never blocks audio.
        const bool playingNow = playing.load(std::memory_order_acquire);
        const float frequencyNow = frequency.load(std::memory_order_relaxed);
        const float amplitudeNow = amplitude.load(std::memory_order_relaxed);
        const auto waveformANow = static_cast<WaveformType>(waveformA.load(std::memory_order_relaxed));
        const auto waveformBNow = static_cast<WaveformType>(waveformB.load(std::memory_order_relaxed));
        const float blendNow = blend.load(std::memory_order_relaxed);
        const float echoWetDryNow = echoWetDry.load(std::memory_order_relaxed);
        const float delayWetDryNow = delayWetDry.load(std::memory_order_relaxed);
        const float bitcrusherWetDryNow = bitcrusherWetDry.load(std::memory_order_relaxed);
        const float reverbWetDryNow = reverbWetDry.load(std::memory_order_relaxed);
        const float whooshWetDryNow = whooshWetDry.load(std::memory_order_relaxed);
        const float distortionWetDryNow = distortionWetDry.load(std::memory_order_relaxed);
        const float filterWetDryNow = filterWetDry.load(std::memory_order_relaxed);
        const int tailFrames = tailFramesRemaining.load(std::memory_order_relaxed);

        const bool isTail = !playingNow && tailFrames > 0;

        if (!playingNow && tailFrames <= 0) {
            memset(output, 0, numberOfFrames * sizeof(short int) * 2);
            return true;
        }

        if (isTail) {
            tailFramesRemaining.fetch_sub(numberOfFrames, std::memory_order_relaxed);
        }

        float phaseIncrement = 2.0f * M_PI * frequencyNow / sampleRate;
        float whooshInc = 2.0f * M_PI * 2.0f / sampleRate;

        for (int i = 0; i < numberOfFrames; i++) {
            float dry = 0.0f;
            if (playingNow) {
                float sA = generateSample(waveformANow, phase);
                float sB = generateSample(waveformBNow, phase);
                dry = (sA * (1.0f - blendNow) + sB * blendNow) * amplitudeNow;
                phase += phaseIncrement;
                if (phase >= 2.0f * M_PI) phase -= 2.0f * M_PI;
            }

            float sample = dry;

            if (echoWetDryNow > 0.0f) {
                float wet = echoFx.process(dry);
                sample = dry * (1.0f - echoWetDryNow) + wet * echoWetDryNow;
            }

            if (delayWetDryNow > 0.0f) {
                float wet = delayFx.process(sample);
                sample = sample * (1.0f - delayWetDryNow) + wet * delayWetDryNow;
            }

            if (bitcrusherWetDryNow > 0.0f) {
                float crushed = applyBitcrusher(sample, bitcrusherWetDryNow);
                sample = sample * (1.0f - bitcrusherWetDryNow) + crushed * bitcrusherWetDryNow;
            }

            if (reverbWetDryNow > 0.0f) {
                float wet = applyReverb(sample);
                sample = sample * (1.0f - reverbWetDryNow) + wet * reverbWetDryNow;
            }

            if (whooshWetDryNow > 0.0f) {
                float mod = 0.5f + 0.5f * sinf(whooshPhase);
                float wet = sample * mod;
                sample = sample * (1.0f - whooshWetDryNow) + wet * whooshWetDryNow;
                whooshPhase += whooshInc;
                if (whooshPhase >= 2.0f * M_PI) whooshPhase -= 2.0f * M_PI;
            }

            if (distortionWetDryNow > 0.0f) {
                float drive = 1.0f + distortionWetDryNow * 20.0f;
                float distorted = tanhf(sample * drive) / tanhf(drive);
                sample = sample * (1.0f - distortionWetDryNow) + distorted * distortionWetDryNow;
            }

            if (filterWetDryNow > 0.0f) {
                float filtered = biquad.process(sample);
                sample = sample * (1.0f - filterWetDryNow) + filtered * filterWetDryNow;
            }

            for (int bandIndex = 0; bandIndex < activeEqBandCount; bandIndex++) {
                sample = activeEqBands[bandIndex].filter.process(sample);
            }

            fftInputBuffer[fftWritePos++] = sample;
            if (fftWritePos >= FFT_SIZE) {
                fftWritePos = 0;
                // Reuse member-scoped scratch buffers — std::vector<float>(SPECTRUM_BANDS)
                // and large stack arrays (16KB) on the audio thread were the
                // worst real-time offenders.
                for (int j = 0; j < FFT_SIZE; j++) {
                    float hann = 0.5f * (1.0f - cosf(2.0f * M_PI * j / (FFT_SIZE - 1)));
                    fftReScratch[j] = fftInputBuffer[j] * hann;
                    fftImScratch[j] = 0.0f;
                }
                fft_inplace(fftReScratch, fftImScratch, FFT_SIZE);
                for (int j = 0; j < SPECTRUM_BANDS; j++) {
                    float m = sqrtf(fftReScratch[j]*fftReScratch[j] + fftImScratch[j]*fftImScratch[j]) / FFT_SIZE;
                    float v = (m > 0.0f) ? (20.0f * log10f(m) + 80.0f) / 80.0f : 0.0f;
                    if (v < 0.0f) v = 0.0f;
                    if (v > 1.0f) v = 1.0f;
                    spectrumPublished[j].store(v, std::memory_order_relaxed);
                }
            }

            short int out = (short int)(fmaxf(-1.0f, fminf(1.0f, sample)) * 32767.0f);
            output[i * 2] = out;
            output[i * 2 + 1] = out;
        }

        return true;
    }
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeInit(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new SuperpoweredSynthesizer());
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativePlaySound(
        JNIEnv *env, jobject thiz, jlong synth_ptr, jfloat frequency, jfloat amplitude,
        jint waveform_a, jint waveform_b, jfloat blend) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) synth->playSound(frequency, amplitude, waveform_a, waveform_b, blend);
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeStopSound(JNIEnv *env, jobject thiz, jlong synth_ptr) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) synth->stopSound();
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeRelease(JNIEnv *env, jobject thiz, jlong synth_ptr) {
    delete reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeSetEffect(
        JNIEnv *env, jobject thiz, jlong synth_ptr, jint effect_id, jfloat wet_dry) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) synth->setEffect(effect_id, wet_dry);
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeSetFilter(
        JNIEnv *env, jobject thiz, jlong synth_ptr, jint preset_id, jfloat frequency, jfloat wet_dry) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) synth->setFilter(preset_id, frequency, wet_dry);
}

JNIEXPORT jfloatArray JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeGetSpectrum(JNIEnv *env, jobject thiz, jlong synth_ptr) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    jfloatArray result = env->NewFloatArray(SPECTRUM_BANDS);
    if (!synth || !result) return result;
    float buf[SPECTRUM_BANDS];
    synth->getSpectrum(buf, SPECTRUM_BANDS);
    env->SetFloatArrayRegion(result, 0, SPECTRUM_BANDS, buf);
    return result;
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeSetEqBands(
        JNIEnv *env, jobject thiz, jlong synth_ptr,
        jfloatArray frequencies, jfloatArray gains, jfloatArray qs) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (!synth) return;
    const int freqLen = env->GetArrayLength(frequencies);
    const int gainLen = env->GetArrayLength(gains);
    const int qLen    = env->GetArrayLength(qs);
    if (freqLen != gainLen || freqLen != qLen) {
        // Mismatched arrays would cause out-of-bounds reads on the
        // shorter of gains/qs. Refuse the update rather than crash
        // the audio thread.
        LOGE("nativeSetEqBands: array length mismatch (freq=%d gain=%d q=%d)",
             freqLen, gainLen, qLen);
        return;
    }
    float* freqBuf = env->GetFloatArrayElements(frequencies, nullptr);
    float* gainBuf = env->GetFloatArrayElements(gains, nullptr);
    float* qBuf    = env->GetFloatArrayElements(qs, nullptr);
    synth->setEqBands(freqLen, freqBuf, gainBuf, qBuf);
    env->ReleaseFloatArrayElements(frequencies, freqBuf, JNI_ABORT);
    env->ReleaseFloatArrayElements(gains, gainBuf, JNI_ABORT);
    env->ReleaseFloatArrayElements(qs, qBuf, JNI_ABORT);
}

} // extern "C"

