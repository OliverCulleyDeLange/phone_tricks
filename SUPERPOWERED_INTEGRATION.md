# Superpowered Integration

## Current scope

Phone Tricks uses `SuperpoweredAndroidAudioIO` as the low-latency Android input/output layer. The application's synthesis and DSP remain project code in `SuperpoweredSynth.cpp`:

- Oscillator and waveform blending
- Echo, delay, bitcrusher, reverb, whoosh, and distortion
- Biquad filters and parametric EQ
- FFT and spectrum publication

The repository does not currently use Superpowered's DSP primitives, and iOS uses `AVAudioEngine` rather than Superpowered.

## Bundled Android files

Headers and Android audio-I/O source live under:

```text
composeApp/src/androidMain/cpp/superpowered/
```

Architecture-specific static libraries are bundled for:

- `arm64-v8a`
- `armeabi-v7a`
- `x86`
- `x86_64`

`composeApp/src/androidMain/cpp/CMakeLists.txt` selects the matching library through `${ANDROID_ABI}` and links it with the app's native library.

## Verification

Build every configured Android ABI with:

```bash
./gradlew :composeApp:externalNativeBuildDebug
```

The build requires Android NDK `27.1.12297006`, as configured in `composeApp/build.gradle.kts`.

## Licensing

Before distributing the application, confirm that the bundled Superpowered SDK version and your intended use comply with Superpowered's current licensing terms. No license key or account credential should be committed to this repository.
