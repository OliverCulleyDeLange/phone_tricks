#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <atomic>
#include <mutex>
#include <cstring>
#include <vector>
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
            waveformA(SINE),
            waveformB(SINE),
            blend(0.0f),
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

        audioIO = new SuperpoweredAndroidAudioIO(
                samplerate, 1024, false, true,
                audioProcessing, this, -1, -1
        );
        if (audioIO) audioIO->start();
        biquad.setLowPass(filterFrequency, (float)samplerate);
    }

    ~SuperpoweredSynthesizer() { delete audioIO; }

    void playSound(float freq, float amp, int wfA, int wfB, float blendFactor) {
        std::lock_guard<std::mutex> lock(mutex);
        frequency = freq;
        amplitude = amp;
        waveformA = static_cast<WaveformType>(wfA);
        waveformB = static_cast<WaveformType>(wfB);
        blend = blendFactor;
        playing = true;
    }

    void stopSound() {
        std::lock_guard<std::mutex> lock(mutex);
        playing = false;
        tailFramesRemaining = samplerate * 2;
    }

    void setEffect(int effectId, float wetDry) {
        std::lock_guard<std::mutex> lock(mutex);
        switch (effectId) {
            case FX_ECHO:       echoWetDry = wetDry; break;
            case FX_DELAY:      delayWetDry = wetDry; break;
            case FX_BITCRUSHER: bitcrusherWetDry = wetDry; break;
            case FX_REVERB:     reverbWetDry = wetDry; break;
            case FX_WHOOSH:     whooshWetDry = wetDry; break;
            case FX_DISTORTION: distortionWetDry = wetDry; break;
        }
    }

    void setFilter(int presetId, float freq, float wetDry) {
        std::lock_guard<std::mutex> lock(mutex);
        filterPreset = static_cast<FilterPresetId>(presetId);
        filterFrequency = freq;
        filterWetDry = wetDry;
        rebuildFilter();
    }

private:
    SuperpoweredAndroidAudioIO *audioIO;
    float frequency, amplitude, blend;
    WaveformType waveformA, waveformB;
    std::atomic<bool> playing;
    int tailFramesRemaining;
    float phase;
    unsigned int samplerate;
    std::mutex mutex;

    float echoWetDry, delayWetDry, bitcrusherWetDry, reverbWetDry, whooshWetDry, distortionWetDry;
    FilterPresetId filterPreset;
    float filterFrequency, filterWetDry;

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
    float spectrumPublished[SPECTRUM_BANDS] = {};
    std::mutex spectrumMutex;

    std::vector<EqBandState> eqBands;

    void rebuildEqBand(EqBandState& band) {
        band.filter.setParametric(band.frequency, (float)samplerate, band.gainDb, band.q);
    }

public:
    void setEqBands(int count, float* frequencies, float* gainsDb, float* qs) {
        std::lock_guard<std::mutex> lock(mutex);
        eqBands.resize(count);
        for (int i = 0; i < count; i++) {
            eqBands[i].frequency = frequencies[i];
            eqBands[i].gainDb = gainsDb[i];
            eqBands[i].q = qs[i];
            rebuildEqBand(eqBands[i]);
        }
    }

    void getSpectrum(float* out, int size) {
        std::lock_guard<std::mutex> lock(spectrumMutex);
        int copy = std::min(size, SPECTRUM_BANDS);
        for (int i = 0; i < copy; i++) out[i] = spectrumPublished[i];
        for (int i = copy; i < size; i++) out[i] = 0.0f;
    }

private:

    void rebuildFilter() {
        float sr = (float)samplerate;
        switch (filterPreset) {
            case F_LOWPASS:    biquad.setLowPass(filterFrequency, sr); break;
            case F_HIGHPASS:   biquad.setHighPass(filterFrequency, sr); break;
            case F_LOWSHELF:   biquad.setLowShelf(filterFrequency, sr); break;
            case F_HIGHSHELF:  biquad.setHighShelf(filterFrequency, sr); break;
            case F_BANDPASS:   biquad.setBandPass(filterFrequency, sr); break;
            case F_NOTCH:      biquad.setNotch(filterFrequency, sr); break;
            case F_PARAMETRIC: biquad.setParametric(filterFrequency, sr); break;
        }
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
        std::lock_guard<std::mutex> lock(mutex);

        const bool isTail = !playing && tailFramesRemaining > 0;

        if (!playing && tailFramesRemaining <= 0) {
            memset(output, 0, numberOfFrames * sizeof(short int) * 2);
            return true;
        }

        if (isTail) {
            tailFramesRemaining -= numberOfFrames;
            if (tailFramesRemaining < 0) tailFramesRemaining = 0;
        }

        float phaseIncrement = 2.0f * M_PI * frequency / sr;
        float whooshInc = 2.0f * M_PI * 2.0f / sr;

        for (int i = 0; i < numberOfFrames; i++) {
            float dry = 0.0f;
            if (playing) {
                float sA = generateSample(waveformA, phase);
                float sB = generateSample(waveformB, phase);
                dry = (sA * (1.0f - blend) + sB * blend) * amplitude;
                phase += phaseIncrement;
                if (phase >= 2.0f * M_PI) phase -= 2.0f * M_PI;
            }

            float sample = dry;

            if (echoWetDry > 0.0f) {
                float wet = echoFx.process(dry);
                sample = dry * (1.0f - echoWetDry) + wet * echoWetDry;
            }

            if (delayWetDry > 0.0f) {
                float wet = delayFx.process(sample);
                sample = sample * (1.0f - delayWetDry) + wet * delayWetDry;
            }

            if (bitcrusherWetDry > 0.0f) {
                float crushed = applyBitcrusher(sample, bitcrusherWetDry);
                sample = sample * (1.0f - bitcrusherWetDry) + crushed * bitcrusherWetDry;
            }

            if (reverbWetDry > 0.0f) {
                float wet = applyReverb(sample);
                sample = sample * (1.0f - reverbWetDry) + wet * reverbWetDry;
            }

            if (whooshWetDry > 0.0f) {
                float mod = 0.5f + 0.5f * sinf(whooshPhase);
                float wet = sample * mod;
                sample = sample * (1.0f - whooshWetDry) + wet * whooshWetDry;
                whooshPhase += whooshInc;
                if (whooshPhase >= 2.0f * M_PI) whooshPhase -= 2.0f * M_PI;
            }

            if (distortionWetDry > 0.0f) {
                float drive = 1.0f + distortionWetDry * 20.0f;
                float distorted = tanhf(sample * drive) / tanhf(drive);
                sample = sample * (1.0f - distortionWetDry) + distorted * distortionWetDry;
            }

            if (filterWetDry > 0.0f) {
                float filtered = biquad.process(sample);
                sample = sample * (1.0f - filterWetDry) + filtered * filterWetDry;
            }

            for (auto& band : eqBands) {
                sample = band.filter.process(sample);
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
                std::lock_guard<std::mutex> slock(spectrumMutex);
                for (int j = 0; j < SPECTRUM_BANDS; j++) {
                    float m = sqrtf(fftReScratch[j]*fftReScratch[j] + fftImScratch[j]*fftImScratch[j]) / FFT_SIZE;
                    float v = (m > 0.0f) ? (20.0f * log10f(m) + 80.0f) / 80.0f : 0.0f;
                    if (v < 0.0f) v = 0.0f;
                    if (v > 1.0f) v = 1.0f;
                    spectrumPublished[j] = v;
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





