# iOS Superpowered Integration Guide

## Overview

This guide explains how to integrate the Superpowered Audio SDK into the iOS part of your Kotlin
Multiplatform app to match the Android implementation.

## Prerequisites

1. Download the Superpowered SDK from https://superpowered.com/
2. Xcode installed

## Setup Steps

1. **Create Objective-C++ Wrapper**

Create a file named `SuperpoweredSynthesizer.mm` with the following implementation:

```objc
#import "SuperpoweredSynthesizer.h"
#import <Superpowered/Superpowered.h>

@implementation SuperpoweredSynthesizer {
    SuperpoweredOSXAudioIO* audioIO;
    float frequency;
    float amplitude;
    int waveform;
    bool playing;
    float phase;
    unsigned int samplerate;
}

- (id)init {
    self = [super init];
    if (self) {
        // Initialize Superpowered
        [Superpowered initialize];
        
        // Set initial values
        frequency = 440.0f;
        amplitude = 0.5f;
        waveform = 0; // SINE
        playing = false;
        phase = 0.0f;
        samplerate = 44100;
        
        // Initialize audio system
        audioIO = [[SuperpoweredOSXAudioIO alloc] initWithDelegate:(id<SuperpoweredOSXAudioIODelegate>)self
                                                        preferredBufferSizeMs:12
                                                        numberOfChannels:2
                                                        enableInput:NO];
        [audioIO start];
    }
    return self;
}

- (void)dealloc {
    [audioIO stop];
    audioIO = nil;
}

- (void)playSound:(float)freq amplitude:(float)amp waveformType:(int)waveformType {
    frequency = freq;
    amplitude = amp;
    waveform = waveformType;
    playing = true;
}

- (void)stopSound {
    playing = false;
}

// Generate a sample based on waveform and phase
- (float)generateSampleWithPhase:(float)phaseValue {
    switch (waveform) {
        case 0: // SINE
            return sinf(phaseValue);
            
        case 1: // SQUARE
            return (phaseValue < M_PI) ? 1.0f : -1.0f;
            
        case 2: { // TRIANGLE
            float normalizedPhase = phaseValue / (2.0f * M_PI);
            if (normalizedPhase < 0.25f)
                return normalizedPhase * 4.0f;
            else if (normalizedPhase < 0.75f)
                return 2.0f - (normalizedPhase * 4.0f);
            else
                return (normalizedPhase * 4.0f) - 4.0f;
        }
            
        case 3: { // SAWTOOTH
            float normalizedPhase = phaseValue / (2.0f * M_PI);
            return 2.0f * (normalizedPhase - floorf(0.5f + normalizedPhase));
        }
            
        default:
            return 0.0f;
    }
}

#pragma mark - SuperpoweredOSXAudioIODelegate

- (BOOL)audioProcessingCallback:(float **)buffers outputChannels:(unsigned int)outputChannels
                      inputChannels:(unsigned int)inputChannels numberOfFrames:(unsigned int)numberOfFrames
                        samplerate:(unsigned int)samplerate hostTime:(unsigned long long)hostTime {
    
    if (!playing) {
        // Output silence if not playing
        for (unsigned int i = 0; i < numberOfFrames * outputChannels; i++) {
            buffers[0][i] = 0;
        }
        return YES;
    }
    
    // Generate audio
    float phaseIncrement = 2.0f * M_PI * frequency / samplerate;
    
    for (unsigned int i = 0; i < numberOfFrames; i++) {
        // Generate sample
        float sample = [self generateSampleWithPhase:phase] * amplitude;
        
        // Write to all output channels
        for (unsigned int ch = 0; ch < outputChannels; ch++) {
            buffers[0][i * outputChannels + ch] = sample;
        }
        
        // Increment phase
        phase += phaseIncrement;
        if (phase >= 2.0f * M_PI) {
            phase -= 2.0f * M_PI;
        }
    }
    
    return YES; // Continue processing
}

@end
```

2. **Create Header File**

Create a file named `SuperpoweredSynthesizer.h`:

```objc
#import <Foundation/Foundation.h>

@interface SuperpoweredSynthesizer : NSObject

- (void)playSound:(float)frequency amplitude:(float)amplitude waveformType:(int)waveformType;
- (void)stopSound;

@end
```

3. **Create JNI Bridge**

Create a file named `KotlinBridge.mm` to interface with Kotlin:

```objc
#import <Foundation/Foundation.h>
#import "SuperpoweredSynthesizer.h"

extern "C" {

void* SuperpoweredSynth_create() {
    SuperpoweredSynthesizer *synth = [[SuperpoweredSynthesizer alloc] init];
    return (__bridge_retained void*)synth;
}

void SuperpoweredSynth_playSound(void *ptr, float frequency, float amplitude, int waveformType) {
    SuperpoweredSynthesizer *synth = (__bridge SuperpoweredSynthesizer*)ptr;
    [synth playSound:frequency amplitude:amplitude waveformType:waveformType];
}

void SuperpoweredSynth_stopSound(void *ptr) {
    SuperpoweredSynthesizer *synth = (__bridge SuperpoweredSynthesizer*)ptr;
    [synth stopSound];
}

void SuperpoweredSynth_release(void *ptr) {
    if (ptr) {
        SuperpoweredSynthesizer *synth = (__bridge_transfer SuperpoweredSynthesizer*)ptr;
        synth = nil;
    }
}

}
```

4. **Add to Xcode Project**

Make sure to add these files to your Xcode project and link against the Superpowered framework.

5. **Update build.gradle.kts**

Make sure your `build.gradle.kts` includes the necessary iOS configuration for native libraries:

```kotlin
kotlin {
    // ... existing config
    iosX64().compilations.getByName("main") {
        cinterops {
            create("synthesizer") {
                defFile(project.file("src/nativeInterop/cinterop/synthesizer.def"))
                includeDirs("src/iosMain/native")
            }
        }
    }
    iosArm64().compilations.getByName("main") {
        cinterops {
            create("synthesizer") {
                defFile(project.file("src/nativeInterop/cinterop/synthesizer.def"))
                includeDirs("src/iosMain/native")
            }
        }
    }
}
```

6. **Create C Interop Def File**

Create a file at `src/nativeInterop/cinterop/synthesizer.def`:

```
package = ocd.phonetricks.audio.native
language = Objective-C++
headers = SuperpoweredSynthesizer.h

# Functions from the bridge
static_library = SuperpoweredSynth_create SuperpoweredSynth_playSound SuperpoweredSynth_stopSound SuperpoweredSynth_release
```

7. **Connect in the Kotlin Code**

Finally, update your `AudioManager.ios.kt` to connect to the native functions defined above.