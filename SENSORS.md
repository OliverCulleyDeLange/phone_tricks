# Phone Tricks - Sensor Support

## Available Sensors

### Core Sensors (Always Available)
1. **Accelerometer** - Linear acceleration in m/s²
   - Measures device acceleration including gravity
   - X, Y, Z axes

2. **Gyroscope** - Angular velocity in rad/s
   - Measures rotation rate around each axis
   - X, Y, Z axes

### Additional Sensors (Platform Dependent)

3. **Magnetometer** - Magnetic field in µT (Android only)
   - Measures magnetic field strength
   - Useful for compass functionality
   - X, Y, Z axes
   - **Note**: Not available on iOS in current implementation

4. **Rotation Vector** - Device orientation
   - Quaternion representation (x, y, z, w)
   - Combines accelerometer, gyroscope, and magnetometer
   - Provides smooth, drift-free orientation

5. **Linear Acceleration** - Acceleration without gravity in m/s²
   - Pure device acceleration (gravity removed)
   - Useful for detecting actual movement
   - X, Y, Z axes

6. **Gravity** - Gravity component in m/s²
   - Just the gravity vector
   - X, Y, Z axes (typically [0, 9.8, 0] when device is flat)

## Platform Support

### Android
✅ Accelerometer
✅ Gyroscope
✅ Magnetometer
✅ Rotation Vector
✅ Linear Acceleration
✅ Gravity

### iOS
✅ Accelerometer
✅ Gyroscope
❌ Magnetometer (API complexity)
✅ Rotation Vector (as quaternion from attitude)
✅ Linear Acceleration (userAcceleration)
✅ Gravity

## Data Storage
- Uses efficient **Ring Buffer** with 600 samples capacity
- Stores ~10 seconds of data at 60Hz
- Auto-overwrites oldest data when full
- No manual memory management needed

## Performance
- 60Hz update rate for smooth visualization
- Ring buffer prevents memory allocation churn
- Auto-starts recording on app launch
- Pause/Resume functionality available
