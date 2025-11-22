import numpy as np


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

        x_values = [r['x'] for r in window_data]
        y_values = [r['y'] for r in window_data]
        z_values = [r['z'] for r in window_data]

        features.extend([
            np.mean(x_values), np.std(x_values), np.max(x_values), np.min(x_values),
            np.ptp(x_values),
            np.mean(y_values), np.std(y_values), np.max(y_values), np.min(y_values),
            np.ptp(y_values),
            np.mean(z_values), np.std(z_values), np.max(z_values), np.min(z_values),
            np.ptp(z_values),
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
