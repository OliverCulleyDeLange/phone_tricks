import numpy as np
from scipy import stats


def extract_features_from_tap(sensor_data, tap_timestamp, window_ms=100):
    features = []

    for sensor_name in ['accelerometerData', 'gyroscopeData']:
        sensor_readings = sensor_data.get(sensor_name, [])
        if not sensor_readings:
            features.extend([0] * 15)
            continue

        window_data = [
            reading for reading in sensor_readings
            if abs(reading['timestampMs'] - tap_timestamp) <= window_ms
        ]

        if not window_data:
            features.extend([0] * 15)
            continue

        x_values = np.array([r['x'] for r in window_data])
        y_values = np.array([r['y'] for r in window_data])
        z_values = np.array([r['z'] for r in window_data])

        magnitudes = np.sqrt(x_values ** 2 + y_values ** 2 + z_values ** 2)

        jerk_x = np.diff(x_values)
        jerk_y = np.diff(y_values)
        jerk_z = np.diff(z_values)
        jerk_mag = np.sqrt(jerk_x ** 2 + jerk_y ** 2 + jerk_z ** 2) if len(
            jerk_x) > 0 else np.array([0])

        features.extend([
            np.std(x_values),
            np.std(y_values),
            np.std(z_values),

            np.mean(magnitudes),
            np.max(magnitudes),
            np.std(magnitudes),

            np.max(jerk_mag) if len(jerk_mag) > 0 else 0,
            np.std(jerk_mag) if len(jerk_mag) > 0 else 0,
            np.mean(jerk_mag) if len(jerk_mag) > 0 else 0,

            stats.skew(magnitudes) if len(magnitudes) > 2 else 0,
            stats.kurtosis(magnitudes) if len(magnitudes) > 3 else 0,

            np.argmax(magnitudes) / len(magnitudes) if len(magnitudes) > 0 else 0,

            np.sum(magnitudes ** 2),

            np.sum(np.abs(np.diff(np.sign(magnitudes - np.mean(magnitudes))))) / 2 if len(
                magnitudes) > 1 else 0,

            (np.argmax(magnitudes) / len(magnitudes)) if len(magnitudes) > 0 and np.argmax(
                magnitudes) > 0 else 0,
        ])

    return features


def extract_negative_samples(sensor_data, num_samples=20, window_ms=100):
    accel_data = sensor_data.get('accelerometerData', [])
    if not accel_data or len(accel_data) < 10:
        return []

    start_time = accel_data[0]['timestampMs']
    end_time = accel_data[-1]['timestampMs']
    duration = end_time - start_time

    if duration < window_ms * 2:
        return []

    random_timestamps = np.random.uniform(
        start_time + window_ms,
        end_time - window_ms,
        size=num_samples
    )

    features_list = []
    for timestamp in random_timestamps:
        features = extract_features_from_tap(sensor_data, timestamp, window_ms)
        features_list.append(features)

    return features_list
