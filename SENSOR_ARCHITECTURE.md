# Sensor Architecture

## Overview

The sensor system uses individual flows for each sensor type instead of aggregating all data into a
single flow. This eliminates data duplication and provides maximum sampling rates for each sensor
independently.

## Architecture

### Individual Sensor Flows

Each sensor emits its own timestamped readings through dedicated flows:

- `accelerometerFlow: Flow<AccelerometerReading>`
- `gyroscopeFlow: Flow<GyroscopeReading>`
- `magnetometerFlow: Flow<MagnetometerReading>?` (Android only)
- `rotationVectorFlow: Flow<RotationVectorReading>`
- `linearAccelerationFlow: Flow<LinearAccelerationReading>?` (optional)
- `gravityFlow: Flow<GravityReading>?` (optional)

### Benefits

1. **No Data Duplication**: Each sensor value is only emitted when it changes
2. **Independent Sampling**: Each sensor can emit at its own native rate
3. **Efficient Memory**: No repeated copying of unchanged sensor values
4. **Flexible Consumption**: Components can subscribe to only the sensors they need

### Data Flow

```
┌─────────────────┐
│ Hardware Sensor │
└────────┬────────┘
         │ Native sampling rate (200-500+ Hz)
         ▼
┌─────────────────────┐
│  SensorEventListener│
│   (per sensor)      │
└────────┬────────────┘
         │ Emit on change
         ▼
┌─────────────────────┐
│ Individual Flow     │
│ AccelerometerReading│
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│   TrickEngine       │
│ (aggregates latest) │
└────────┬────────────┘
         │ Reconstructs SensorData
         ▼
┌─────────────────────┐
│   Ring Buffer       │
│ (for analysis)      │
└─────────────────────┘
```

### Android Implementation

Each sensor registers its own `SensorEventListener` and emits readings via `callbackFlow`:

- Uses `SENSOR_DELAY_FASTEST` for all sensors
- Each sensor emits independently when hardware updates occur
- Automatic unregistration on flow cancellation

### iOS Implementation

Uses CoreMotion's `deviceMotion` API which provides unified sensor updates:

- Single motion manager provides all sensor data
- Each flow extracts its specific sensor reading
- All flows share the same 100Hz update rate
- Magnetometer not available on iOS

### TrickEngine Integration

The `TrickEngine` subscribes to all individual sensor flows and:

1. Collects latest value from each sensor
2. Reconstructs `SensorData` with latest values on any sensor update
3. Adds to ring buffer for historical analysis
4. Runs trick/tap detection on the buffer

This approach maintains backward compatibility with existing detection algorithms while eliminating
redundant data storage.

## Data Structures

### Individual Readings

```kotlin
data class AccelerometerReading(
    val timestampMs: Long,
    val data: Accelerometer
)
```

Each reading type wraps the sensor data with its timestamp.

### Aggregated SensorData

```kotlin
data class SensorData(
    val timestampMs: Long,
    val accelerometer: Accelerometer,
    val gyroscope: Gyroscope,
    val magnetometer: Magnetometer?,
    val rotationVector: RotationVector,
    val linearAcceleration: LinearAcceleration?,
    val gravity: Gravity?
)
```

Used in ring buffer for time-series analysis and training data export.

## Performance Characteristics

### Before (Aggregated)

- Single flow emitting at fixed 60Hz (16ms throttle)
- All sensor values copied even if unchanged
- ~60 SensorData objects/second

### After (Individual Flows)

- 6 independent flows emitting on sensor change
- Only changed values emitted
- ~200-500+ updates/second total (varies by sensor)
- TrickEngine reconstructs at actual sensor rate

## Training Data Collection

Training data export still uses the aggregated `SensorData` format:

- Ring buffer contains complete snapshots
- JSON export includes all sensors for each timestamp
- Compatible with existing ML workflows
