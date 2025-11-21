# Phone Tricks - KMM Sensor App

A Kotlin Multiplatform Mobile app that collects and visualizes sensor data from accelerometers and
gyroscopes to detect phone movements, spins, flips, and tricks.

## Features

- **Real-time Sensor Data Collection**: Collects accelerometer and gyroscope data from both Android
  and iOS devices
- **Live Visualization**: Displays sensor data as live-updating numbers and visual axis
  representations
- **Shared Engine**: Common Kotlin code processes sensor data across all platforms
- **Cross-Platform UI**: Compose Multiplatform UI works on Android and iOS

## Architecture

### Shared Module (`composeApp/src/commonMain`)

- **`data/`**: Data models for sensor readings (SensorData, Accelerometer, Gyroscope)
- **`sensor/`**: Sensor manager interface with expect/actual implementations
- **`engine/`**: TrickEngine that collects and manages sensor data
- **`App.kt`**: Main UI with sensor visualization

### Platform-Specific Implementations

#### Android (`composeApp/src/androidMain`)

- Uses Android's `SensorManager` API for accelerometer and gyroscope access
- Implements sensor data collection with `SensorEventListener`

#### iOS (`composeApp/src/iosMain`)

- Uses CoreMotion's `CMMotionManager` for sensor access
- Collects accelerometer and gyroscope data through CoreMotion callbacks

## Sensor Data

The app collects:

- **Accelerometer**: Linear acceleration in m/s² along X, Y, Z axes
- **Gyroscope**: Angular velocity in rad/s around X, Y, Z axes

## UI Components

1. **Start/Stop Button**: Toggle sensor data recording
2. **Sensor Data Cards**: Display real-time numeric values for both sensors
3. **3D Axis Visualization**: Visual representation of acceleration with:
    - Red line: X-axis movement
    - Green line: Y-axis movement
    - Blue circle: Z-axis magnitude (size indicates depth)

## Building and Running

### Android

1. Open the project in Android Studio
2. Select the `composeApp` configuration
3. Run on an Android device or emulator (sensors work best on real hardware)

### iOS

1. In Android Studio, run the `iosApp` configuration for the simulator
2. For device deployment, open `iosApp/iosApp.xcodeproj` in Xcode
3. Run on a physical iOS device (simulator has limited sensor support)

## Future Enhancements

- Trick detection algorithms (spins, flips, combos)
- 10-second playback visualization with animated phone representation
- Recording and replay of sensor data sessions
- Gesture recognition and scoring system

## Technical Stack

- **Kotlin Multiplatform Mobile (KMM)**
- **Compose Multiplatform** for shared UI
- **Kotlin Coroutines & Flow** for reactive data streams
- **Platform Sensors**: Android SensorManager, iOS CoreMotion

## License

This is a sample project for learning KMM development.