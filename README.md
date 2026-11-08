# Phone Tricks

Phone Tricks is a Kotlin Multiplatform motion-controlled synthesizer for Android and iOS. Touch and device sensors can be mapped to pitch, volume, waveform blending, effects, and filters. The app also includes scale snapping, a parametric EQ, spectrum display, and a sample looper.

## Platform audio

- **Android:** Superpowered provides the low-latency audio I/O layer. Synthesis, effects, filtering, EQ, and spectrum analysis are implemented in the app's C++ audio engine.
- **iOS:** A native `AVAudioEngine` graph hosts both the synthesizer and sample looper. The two clients share one engine and audio session so playback and microphone capture do not compete for the hardware route.

The iOS orientation display uses SceneKit. Android uses SceneView with the bundled `smartphone.glb` model.

## Stabilization status

The code-review remediation pass is complete as of 2026-11-08:

- Android's render callback no longer takes parameter or spectrum mutexes. Scalar controls and spectrum publication are lock-free atomics; EQ updates use a non-blocking sequence snapshot and audio-owned filter state.
- Android filters and EQ rebuild against the actual sample rate reported to the audio callback.
- iOS synth playback, sample playback, and microphone capture use one shared `AVAudioEngine`.
- The former iOS visualization placeholder is a live SceneKit phone model driven by the tared sensor quaternion.
- Sensor-driven parameter recomputation is throttled to approximately 60 Hz, and platform sensor streams are shared.
- Audio resources are released with the Compose lifecycle.
- Android and iOS permission paths recover cleanly when microphone access is denied.
- CI builds Android tests/native code and runs the iOS simulator test suite using Node 24-compatible action versions.

Local verification covers Android unit tests and APK/native builds for all four configured ABIs, iOS simulator tests, the complete Xcode simulator app build, and a successful simulator launch. Final playback, recording, permission, and motion checks still require physical Android and iOS devices.

## Build and test

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:externalNativeBuildDebug
./gradlew :composeApp:iosSimulatorArm64Test
```

Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS application. Use the `composeApp` Android application configuration from Android Studio for Android.

## Non-blocking engineering backlog

These are cleanup or future optimization candidates rather than known release blockers:

- Extract sensor-to-synth mapping from `SynthesizerViewModel` into a pure, directly testable mapper.
- Replace hand-written pointer gesture loops with Compose gesture detectors.
- Make EQ band identifiers durable across removal and restoration.
- Persist sample-loop speed and range.
- Gate the on-screen sensor/spectrum diagnostics behind a debug setting.
- Use Core Motion's monotonic device-motion timestamp on iOS.
- Replace the hand-written FFT with a platform-optimized implementation if profiling justifies it.

## Superpowered scope

Superpowered is currently used for Android audio I/O. The DSP algorithms in `SuperpoweredSynth.cpp` are project code, not Superpowered DSP primitives. See `SUPERPOWERED_INTEGRATION.md` for the exact integration boundary.
