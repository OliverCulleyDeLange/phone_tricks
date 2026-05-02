# Phone Tricks

A Kotlin Multiplatform app with synthesizer functionality.

## Audio Implementation

### Current Setup

- **Android**: Uses native C++ code with the Superpowered SDK for high-performance audio synthesis
- **iOS**: Uses a pure Kotlin implementation with AVAudioEngine for audio synthesis (temporary
  solution)

### Planned Enhancements

#### iOS Native Integration

To achieve optimal audio performance on iOS, there are plans to integrate the Superpowered SDK:

1. Download the Superpowered SDK from https://superpowered.com/
2. Create the native implementation files in the `composeApp/src/iosMain/native/` directory
3. Update the C interop definition in `composeApp/src/nativeInterop/cinterop/synthesizer.def`
4. Update the build.gradle.kts to enable the C interop
5. Update the IOSAudioManager to use the native implementation

Refer to `composeApp/src/iosMain/native/README.md` for detailed instructions on the iOS native
integration.

---

## Code Review (2026-05-02)

The findings below are grouped by severity. Line numbers refer to the file at the time of review.

### Bugs — likely blocks builds or breaks features

8. **Real-time audio thread still takes `std::mutex mutex` for parameter reads.** Heap allocation has been moved off the audio thread (commit below), but the per-callback parameter mutex is still in place. Convert the simple scalar fields (frequency, amplitude, blend, per-effect wet/dry, filterFrequency, filterWetDry) to `std::atomic<float>` so the audio callback never blocks. The eqBands vector is more involved — it needs an atomic pointer swap or a lock-free SPSC queue.

15. **iOS audio session is single-engine but used by two managers.** `IOSAudioManager` and `IOSSamplePlayer` each instantiate their own `AVAudioEngine`. Two engines on iOS contend for the same I/O hardware — at minimum the recorder's `inputNode` install will conflict with the synth's playback engine on some hardware paths.

16. **iOS phone visualization is just a placeholder.** `PhoneVisualization3D.ios.kt` shows the literal text "iOS 3D Visualization (Not yet implemented)". On iOS the orientation feedback loop the user is meant to play with is invisible.

### Smaller bugs / correctness issues

### Suggested improvements

- **Lock-free parameter updates in `SuperpoweredSynth.cpp`.** Replace the per-callback `std::mutex` with `std::atomic<float>` for frequency, amplitude, blend, and per-effect wet/dry mixes. The audio thread should never block.
- **Pre-allocate the FFT scratch and spectrum buffers.** `float re[FFT_SIZE]` and `std::vector<float> mag` are created on the audio thread on every FFT — promote them to fields and use a lock-free SPSC pattern (or atomic pointer swap into a double-buffered array) for the UI side.
- **Throttle parameter updates from `SynthesizerViewModel.recompute` to ~60 Hz.** Use `Flow.sample(16.milliseconds)` on the merged sensor flow rather than firing on every individual emission of three sensors.
- **Share sensor flows.** Use `shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)` so multiple collectors don't each register their own `SensorEventListener` (Android) or fight over `CMMotionManager` (iOS).
- **Move sensor mapping/normalization out of the ViewModel.** A pure `data class ControlMapper(val mappings: List<ControlMapping>)` with a `compute(surfaceValues: Map<...>): SynthParams` makes the logic testable; right now `SynthesizerViewModel` is too big to unit-test.
- **Replace `System.currentTimeMillis()` calls in `EqScreen.kt` gesture detection** with `kotlin.time.TimeSource.Monotonic` (already used in `Dial.kt`). Wall-clock time can jump backward and double-tap detection will misbehave.
- **Use `androidx.compose.foundation.gestures.detectTapGestures` and `detectDragGestures`** in `EqScreen.kt` and `SampleLooperScreen.kt` instead of hand-rolled `awaitPointerEvent` loops — less code, fewer edge cases (multitouch, cancellation).
- **EQ band id allocation.** `nextId = (_bands.value.maxOfOrNull { it.id } ?: 0) + 1` collides if the user removes and re-adds bands across app restarts because saved `id`s come back. Use a UUID-string id or persist `nextId` alongside the bands.
- **Persist FX `wetDry` map by enum name, not ordinal.** Reordering `AudioEffect` entries silently mis-maps stored settings.
- **Persist `loopSpeed`, `startPoint`, `endPoint`** in `SamplePlayer` — they reset every launch.
- **The on-screen debug panel** (`MainScreen.DebugInfo`) renders three sensor charts and the spectrum on top of the touch pad. It looks like development scaffolding; gate it behind a setting or remove it.
- **iOS sensor timestamps** should use `mach_absolute_time` or the `CMDeviceMotion.timestamp` field (the OS-provided high-resolution timestamp), not `time(null)` or wall-clock time.
- **`SuperpoweredSynth.cpp` ignores the actual sample rate** chosen by the audio device. The constructor passes 44100 to `SuperpoweredAndroidAudioIO` and uses the same value to build filters, but the callback receives an `int sr` parameter that may differ. Re-build filters when `sr` first arrives or assert equality.
- **iOS `IOSAudioManager.generateAndPlaySound` recursion** can build up arbitrarily deep call stacks because the completion callback re-enters the function. Schedule onto a coroutine instead of recursing.
- **`SUPERPOWERED_INTEGRATION.md` claims integration is complete**, but `SuperpoweredSynth.cpp` doesn't call any Superpowered API beyond `SuperpoweredAndroidAudioIO` for I/O — all DSP (filters, reverb, FFT, distortion) is hand-rolled. Either delete the doc or actually use the SDK's primitives.
- **Remove unused magnetometer / linear-acceleration / gravity flows** from `SensorViewModel` if the UI doesn't render them. They allocate per-event `List` copies and add load to the sensor subsystem.
- **Add a CI workflow** that builds both Android and iOS targets — the iOS sensor module bug (#1) and the missing test class (#5) would be caught immediately.
- **Add basic unit tests for `MusicalScale.snapFrequency`, `ControlMapping` normalize/range math, and `SettingsRepository` round-trips** — these are pure, testable, and likely to drift.
- **Consider replacing the hand-rolled FFT** with `kissfft` (Android) / `vDSP` (iOS, already imported via `platform.Accelerate.*`). The current radix-2 implementation works but is a hot loop on the audio thread.
