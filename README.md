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

2. **iOS sensors collide with each other.** `CMMotionManager` only supports a single device-motion handler at a time, but each of the six iOS flows calls `startDeviceMotionUpdatesToQueue` independently and each `awaitClose` calls `stopDeviceMotionUpdates`. Collecting more than one sensor flow simultaneously (which the app does) means the latest collector wins and any cancellation kills all the others. A single shared device-motion subscription needs to fan out to per-sensor flows.

3. **iOS Info.plist is missing required usage descriptions.** `iosApp/iosApp/Info.plist` does not declare `NSMicrophoneUsageDescription` (required by `AVAudioEngine.inputNode` / sample looper) or `NSMotionUsageDescription` (required by `CMMotionManager`). Both APIs will crash the app on first use.

4. **iOS audio session is never configured.** `IOSAudioManager.setupAudioEngine` and `IOSSamplePlayer.init` start `AVAudioEngine` without setting an `AVAudioSession` category. The synth will be silenced by the ringer switch, may not play if other audio is active, and recording will fail because the default category is playback-only.

7. **`SynthesizerViewModel.recompute` is invoked on every sensor sample.** Each accelerometer / gyroscope / rotation-vector emission calls `recompute(...)`, which in turn calls `audioManager.playSynthSound`, `setEffect`, and `setFilter` — each of those takes the C++ audio mutex on the audio thread. At `SENSOR_DELAY_GAME` (~50 Hz × 3 sensors) this is hundreds of mutex acquisitions per second on the real-time audio thread, causing priority inversion and glitches. Throttle, or push parameters into lock-free atomics.

8. **Real-time audio thread takes a `std::mutex` and allocates.** `SuperpoweredSynth.cpp::onAudioProcess` holds `std::mutex mutex` for the whole callback (line 384) and allocates a `std::vector<float>` for the FFT result every FFT_SIZE samples (line 465 / 473). Both are forbidden in real-time audio code. Use `std::atomic` for parameters and a fixed-size double-buffered spectrum array.

9. **`MainActivity` requests RECORD_AUDIO but never handles the result.** If the user denies the prompt, the SamplePlayer silently fails when it tries to construct `AudioRecord`. If the user grants the permission after the activity has already started, nothing re-tries. Use `ActivityResultContracts.RequestPermission` and gate the sampler UI on the granted state.

10. **`iOS IOSSamplePlayer.startRecording` does not check microphone permission.** Combined with the missing Info.plist key (#3), the first recording attempt will crash on a real device.

11. **Permission deny for microphone is unhandled on Android.** `SamplePlayer.android.kt` constructs `AudioRecord` even if RECORD_AUDIO was denied; with `@SuppressLint("MissingPermission")` the build proceeds but the recorder enters an error state at runtime and the UI never reflects it.

12. **`AndroidSensorManager` registers `null` listeners on devices without a sensor.** `magnetometerFlow`, `linearAccelerationFlow`, and `gravityFlow` use `magnetometer.let { ... registerListener(listener, it /* nullable */, ...) }` instead of `?.let`. If the device lacks one of those sensors, `getDefaultSensor` returns `null` and the listener is registered against `null` (no-op but logs a warning) and the flow never emits. The accelerometer/gyroscope/rotation flows above use `?.let` correctly — make all six consistent.

15. **iOS audio session is single-engine but used by two managers.** `IOSAudioManager` and `IOSSamplePlayer` each instantiate their own `AVAudioEngine`. Two engines on iOS contend for the same I/O hardware — at minimum the recorder's `inputNode` install will conflict with the synth's playback engine on some hardware paths.

16. **iOS phone visualization is just a placeholder.** `PhoneVisualization3D.ios.kt` shows the literal text "iOS 3D Visualization (Not yet implemented)". On iOS the orientation feedback loop the user is meant to play with is invisible.

17. **`createSensorManager()` (no-arg) on Android always throws.** `SensorManager.android.kt` defines `actual fun createSensorManager(): SensorManager = throw IllegalStateException(...)` — this satisfies the `expect`, but means any future common code that calls the no-arg `createSensorManager()` (the only signature on the `expect` declaration) crashes at runtime on Android. The platform-specific `createSensorManager(context)` overload is not part of the contract. Move the context dependency to a constructor parameter passed from `MainActivity`, or expose a separate Android-only entry point that `App.kt` doesn't reach.

### Smaller bugs / correctness issues

20. **`SamplePlayer.getPlayPosition()` (Android) breaks on int wrap.** `playbackHeadPosition` is an `Int` that wraps approximately every 13.5 hours at 44.1 kHz. The `head - loopStartFrame` subtraction is guarded with `coerceAtLeast(0L)`, which silently freezes the marker rather than handling the wrap.

21. **`AndroidSamplePlayer.startRecording` does not `join` the previous record job.** Calling `startRecording()` twice in quick succession leaves the old recorder draining in `finally` while a new one is allocated. Same pattern in `stopPlayback` for the play job.

23. **`SynthesizerViewModel.onCleared` releases the AudioManager but the manager is constructed in `App.kt` via `remember`.** When the ViewModel is destroyed (e.g., process death restoration), `release()` is called, but the `remember`-scoped manager will be recreated on next composition, which on Android tries to load the native library again and may double-init OpenSL ES. Move ownership: either the ViewModel owns the manager (and is the one constructing it), or `release()` doesn't happen in `onCleared`.

24. **`EqViewModel.startPolling` runs forever; no `stopPolling` is ever called.** The 50 ms spectrum poll keeps running for the lifetime of the ViewModel even when the EQ sheet is closed. Drive polling from a `LaunchedEffect` keyed on sheet visibility instead.

26. **`AndroidAudioManager.finalize()` is non-deterministic.** The fallback `protected fun finalize()` (line 83) won't run reliably; Kotlin's actor model assumes explicit `release()`. Document the contract or remove finalize and rely on owners.

27. **`AndroidManifest.xml` declares `Theme.Material.Light.NoActionBar` while the app uses `darkColorScheme`.** Splash and any non-Compose system UI will flash white before Compose draws.

28. **JNI `nativeSetEqBands` assumes all three arrays are the same length.** It reads `count = GetArrayLength(frequencies)` and then indexes `gainBuf[i]` and `qBuf[i]` up to `count`. If the Kotlin side is ever wrong, this is an out-of-bounds read. Add a guard.

29. **`FxScreen` filter-preset drag-to-cycle relies on a chord of x and y deltas** (`dragAmount.x - dragAmount.y`). This means a horizontal swipe right and a vertical swipe up both increment — confusing UX and easy to trigger by accident.

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
