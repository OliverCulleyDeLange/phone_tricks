# Phone Tricks - KMM Sensor App

A Kotlin Multiplatform Mobile app that collects and visualizes comprehensive sensor data from
accelerometers, gyroscopes, magnetometers, and more to detect phone movements, spins, flips, and
tricks.

## Features

- **Comprehensive Sensor Collection**: Up to 6 different sensor types on Android, 5 on iOS
- **Real-time Visualization**: Live graphs with adaptive scaling showing historical data
- **Efficient Ring Buffer**: 10 seconds of data at 60Hz with zero garbage collection pressure
- **Auto-start Recording**: Begins collecting data immediately on app launch
- **Scrollable Dashboard**: View all sensors with organized sections
- **Cross-Platform UI**: Compose Multiplatform UI works on Android and iOS

## Sensor Support

### Core Sensors (Always Available)

- **Accelerometer**: Raw linear acceleration (m/s²) including gravity
- **Gyroscope**: Angular velocity (rad/s) for rotation detection

### Additional Sensors (Platform Dependent)

#### Android (6 sensors)

- Magnetometer (magnetic field in µT)
- Rotation Vector (quaternion orientation)
- Linear Acceleration (acceleration without gravity)
- Gravity (gravity component only)

#### iOS (5 sensors)

- Rotation Vector (quaternion from attitude)
- Linear Acceleration (userAcceleration)
- Gravity (gravity vector)

## Architecture

### Shared Module (`composeApp/src/commonMain`)

- **`data/`**: Data models for all sensor types (SensorData, Accelerometer, Gyroscope, Magnetometer,
  etc.)
- **`sensor/`**: Sensor manager interface with expect/actual implementations
- **`engine/`**:
    - TrickEngine: Manages sensor data collection
    - RingBuffer: Efficient circular buffer for historical data
- **`ui/`**:
    - SensorScreen: Main scrollable dashboard
    - SensorViewModel: State management
    - **`components/`**:
        - SensorGraph: Time-series graph with adaptive scaling
        - AxisVisualization: 3D real-time visualization

### Platform-Specific Implementations

#### Android (`composeApp/src/androidMain`)

- Uses Android's `SensorManager` API for all 6 sensor types
- Implements sensor data collection with `SensorEventListener`
- Registers: ACCELEROMETER, GYROSCOPE, MAGNETIC_FIELD, ROTATION_VECTOR, LINEAR_ACCELERATION, GRAVITY

#### iOS (`composeApp/src/iosMain`)

- Uses CoreMotion's `CMMotionManager` with `deviceMotion` API
- Collects comprehensive motion data from single unified source
- Accesses: userAcceleration, gravity, rotationRate, attitude (quaternion)

## UI Components

### Dashboard Layout

1. **Fixed Header**
    - Title and recording status
    - Pause/Resume button with live red dot indicator

2. **Core Sensors Section**
    - Accelerometer graph (m/s²)
    - Gyroscope graph (rad/s)

3. **Additional Sensors Section** (when available)
    - Magnetometer graph (µT) - Android only
    - Linear Acceleration graph (m/s²)
    - Gravity graph (m/s²)
    - Rotation Vector graph (Quaternion)

4. **Real-Time Visualization**
    - 3D axis display showing current acceleration direction
    - Red (X), Green (Y), Blue (Z) color-coded axes
    - Blue circle for Z-axis magnitude

### Graph Features

- **Adaptive Scaling**: Automatically adjusts to min/max values
- **Color-Coded Lines**: Red (X), Green (Y), Blue (Z)
- **Legend**: Shows which line represents which axis
- **Zero Reference**: Gray line when zero is in range
- **Smooth Rendering**: 60Hz updates with efficient Canvas drawing

## Data Storage

### Ring Buffer Implementation

- **Capacity**: 600 samples (~10 seconds at 60Hz)
- **Circular Storage**: Automatically overwrites oldest data
- **O(1) Operations**: Constant-time insertions
- **Chronological Order**: `toList()` returns oldest-to-newest
- **Memory Efficient**: No list resizing or garbage collection

## Building and Running

### Android
1. Open the project in Android Studio
2. Select the `composeApp` configuration
3. Run on an Android device or emulator
4. **Note**: Real hardware recommended for accurate sensor data

### iOS

1. Run the `iosApp` configuration from Android Studio for simulator
2. For device: Open `iosApp/iosApp.xcodeproj` in Xcode
3. Run on a physical iOS device
4. **Note**: Simulator has limited/simulated sensor support

## Performance Characteristics

- **Update Rate**: 60Hz (60 samples per second)
- **History Buffer**: 10 seconds rolling window
- **Memory**: Fixed allocation, no dynamic growth
- **CPU**: Efficient Canvas drawing, no unnecessary allocations
- **Startup**: Auto-begins recording immediately

## Future Enhancements

- Trick detection algorithms (spins, flips, combos)
- 10-second playback visualization with animated phone representation
- Pattern recognition for specific tricks
- Recording and replay of sensor data sessions
- Gesture recognition and scoring system
- Export sensor data for analysis

## Technical Stack

- **Kotlin Multiplatform Mobile (KMM)**
- **Compose Multiplatform** for shared UI
- **Kotlin Coroutines & Flow** for reactive data streams
- **Platform Sensors**:
    - Android: SensorManager API
    - iOS: CoreMotion (CMMotionManager, deviceMotion)
- **Custom Ring Buffer** for efficient data storage

## Sensor Details

See [SENSORS.md](SENSORS.md) for detailed sensor information and platform support matrix.

## License

This is a sample project for learning KMM development.