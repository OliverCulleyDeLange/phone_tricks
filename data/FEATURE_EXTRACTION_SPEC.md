# Feature Extraction Specification

This document defines the exact feature extraction logic used for tap detection.
Both Kotlin and Python implementations MUST follow this specification exactly.

## Implementations

- **Python:** `data/feature_extraction.py`
- **Kotlin:** `composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/TapFeatureExtractor.kt`

## Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| `window_ms` | 100 | Time window (milliseconds) on each side of tap timestamp |
| `num_sensors` | 2 | Accelerometer and Gyroscope only |
| `features_per_axis` | 5 | mean, std, max, min, peak-to-peak |
| `total_features` | 30 | 2 sensors × 3 axes × 5 features |

## Algorithm

### For Each Sensor (Accelerometer, Gyroscope)

1. **Filter data by time window:**
    - Include all readings where `abs(reading.timestamp - tap_timestamp) <= window_ms`
    - If no readings found, use 15 zeros as features

2. **Extract X, Y, Z values:**
    - Create separate lists for x, y, z values from filtered readings

3. **Calculate statistics for each axis:**
    - **mean:** Average of all values
    - **std:** Standard deviation (using sample variance: sum((x - mean)²) / n)
    - **max:** Maximum value
    - **min:** Minimum value
    - **ptp:** Peak-to-peak (max - min)

4. **Return feature vector:**
   ```
   [x_mean, x_std, x_max, x_min, x_ptp,
    y_mean, y_std, y_max, y_min, y_ptp,
    z_mean, z_std, z_max, z_min, z_ptp]
   ```

### Final Feature Vector (30 elements)

```
[accel_x_mean, accel_x_std, accel_x_max, accel_x_min, accel_x_ptp,
 accel_y_mean, accel_y_std, accel_y_max, accel_y_min, accel_y_ptp,
 accel_z_mean, accel_z_std, accel_z_max, accel_z_min, accel_z_ptp,
 gyro_x_mean, gyro_x_std, gyro_x_max, gyro_x_min, gyro_x_ptp,
 gyro_y_mean, gyro_y_std, gyro_y_max, gyro_y_min, gyro_y_ptp,
 gyro_z_mean, gyro_z_std, gyro_z_max, gyro_z_min, gyro_z_ptp]
```

## Sensor Order

**CRITICAL:** Features MUST be extracted in this exact order:

1. Accelerometer (indices 0-14)
2. Gyroscope (indices 15-29)

## Edge Cases

| Case | Behavior |
|------|----------|
| Empty buffer | Return 15 zeros for that sensor |
| No readings in window | Return 15 zeros for that sensor |
| Empty values list | Return 0 for that statistic |
| Single value in list | std = 0, ptp = 0 |

## Data Types

- **Input timestamps:** Long (milliseconds)
- **Input sensor values:** Float (x, y, z)
- **Output features:** Float[30]

## Testing

To verify implementations match:

1. Use the same sensor data JSON file
2. Extract features at the same timestamp
3. Compare output vectors element-by-element
4. Tolerance: < 0.0001 difference (accounting for floating point precision)

## Version History

- **v2 (Current):** Simplified to 30 features (accelerometer + gyroscope only, 100ms window)
- **v1:** 90 features (6 sensors, 200ms window)

## Notes

- The 100ms window means ±100ms from tap timestamp (200ms total)
- Standard deviation uses population variance (divide by n, not n-1)
- Peak-to-peak is the range (max - min)
- Feature ordering is critical for model compatibility
