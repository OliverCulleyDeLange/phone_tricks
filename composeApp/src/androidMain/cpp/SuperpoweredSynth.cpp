#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <atomic>
#include <mutex>
#include "superpowered/SuperpoweredSimple.h"
#include "superpowered/SuperpoweredAndroidAudioIO.h"

#define TAG "SuperpoweredSynth"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Waveform types
enum WaveformType {
    SINE = 0,
    SQUARE = 1,
    TRIANGLE = 2,
    SAWTOOTH = 3
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
            phase(0.0f),
            samplerate(44100) {
        // Create the audio I/O
        audioIO = new SuperpoweredAndroidAudioIO(
                samplerate,      // sample rate
                1024,            // buffer size
                false,           // enableInput
                true,            // enableOutput
                audioProcessing, // callback function
                this,            // clientdata (userData)
                -1,              // inputStreamType (default)
                -1               // outputStreamType (default)
        );

        // Start audio processing
        if (audioIO) {
            audioIO->start();
        }
    }

    ~SuperpoweredSynthesizer() {
        delete audioIO;
    }

    void playSound(float freq, float amp, int waveformTypeA, int waveformTypeB, float blendFactor) {
        std::lock_guard<std::mutex> lock(mutex);
        frequency = freq;
        amplitude = amp;
        waveformA = static_cast<WaveformType>(waveformTypeA);
        waveformB = static_cast<WaveformType>(waveformTypeB);
        blend = blendFactor;
        playing = true;
    }

    void stopSound() {
        std::lock_guard<std::mutex> lock(mutex);
        playing = false;
    }

private:
    SuperpoweredAndroidAudioIO *audioIO;
    float frequency;
    float amplitude;
    WaveformType waveformA;
    WaveformType waveformB;
    float blend;
    std::atomic<bool> playing;
    float phase;
    unsigned int samplerate;
    std::mutex mutex;

    float generateSample(WaveformType wf, float phaseValue) {
        switch (wf) {
            case SINE:
                return sinf(phaseValue);

            case SQUARE:
                return (phaseValue < M_PI) ? 1.0f : -1.0f;

            case TRIANGLE: {
                float normalizedPhase = phaseValue / (2.0f * M_PI);
                if (normalizedPhase < 0.25f)
                    return normalizedPhase * 4.0f;
                else if (normalizedPhase < 0.75f)
                    return 2.0f - (normalizedPhase * 4.0f);
                else
                    return (normalizedPhase * 4.0f) - 4.0f;
            }

            case SAWTOOTH: {
                float normalizedPhase = phaseValue / (2.0f * M_PI);
                return 2.0f * (normalizedPhase - floorf(0.5f + normalizedPhase));
            }

            default:
                return 0.0f;
        }
    }

    // Static callback function for audio processing
    static bool audioProcessing(
            void *clientdata,
            short int *audio,
            int numberOfFrames,
            int sr) {
        return static_cast<SuperpoweredSynthesizer *>(clientdata)->onAudioProcess(audio,
                                                                                  numberOfFrames,
                                                                                  sr);
    }

    // Non-static audio processing function
    bool onAudioProcess(short int *output, int numberOfFrames, int sr) {
        std::lock_guard<std::mutex> lock(mutex);

        if (!playing) {
            // Output silence if not playing
            memset(output, 0, numberOfFrames * sizeof(short int) * 2);
            return true;
        }

        // Generate audio
        float phaseIncrement = 2.0f * M_PI * frequency / sr;

        for (int i = 0; i < numberOfFrames; i++) {
            float sampleA = generateSample(waveformA, phase);
            float sampleB = generateSample(waveformB, phase);
            float sample = (sampleA * (1.0f - blend) + sampleB * blend) * amplitude;

            short int intSample = (short int) (sample * 32767.0f);
            output[i * 2] = intSample;
            output[i * 2 + 1] = intSample;

            phase += phaseIncrement;
            if (phase >= 2.0f * M_PI) {
                phase -= 2.0f * M_PI;
            }
        }

        return true; // Continue processing
    }
};

// JNI functions
extern "C" {

JNIEXPORT jlong JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeInit(JNIEnv *env, jobject thiz) {
    auto *synth = new SuperpoweredSynthesizer();
    return reinterpret_cast<jlong>(synth);
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativePlaySound(
        JNIEnv *env, jobject thiz, jlong synth_ptr, jfloat frequency, jfloat amplitude,
        jint waveform_a, jint waveform_b, jfloat blend) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) {
        synth->playSound(frequency, amplitude, waveform_a, waveform_b, blend);
    }
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeStopSound(
        JNIEnv *env, jobject thiz, jlong synth_ptr) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) {
        synth->stopSound();
    }
}

JNIEXPORT void JNICALL
Java_ocd_phonetricks_audio_AndroidAudioManager_nativeRelease(
        JNIEnv *env, jobject thiz, jlong synth_ptr) {
    auto *synth = reinterpret_cast<SuperpoweredSynthesizer *>(synth_ptr);
    if (synth) {
        delete synth;
    }
}

} // extern "C"