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