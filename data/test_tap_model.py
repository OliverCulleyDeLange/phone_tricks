import json
import numpy as np
from pathlib import Path
import sys
import joblib


def extract_features_from_tap(sensor_data, tap_timestamp, window_ms=200):
    features = []

    for sensor_name in ['accelerometerData', 'gyroscopeData', 'linearAccelerationData',
                        'magnetometerData', 'gravityData', 'rotationVectorData']:
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


def test_model(json_file):
    model_path = Path("models/tap_classifier.pkl")
    scaler_path = Path("models/scaler.pkl")

    if not model_path.exists() or not scaler_path.exists():
        print("Error: Model not found. Train first with: python train_tap_model.py")
        return

    print("Loading model...")
    model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)

    print(f"Loading test data from {json_file}...")
    with open(json_file, 'r') as f:
        data = json.load(f)

    actual_label = data['label']
    tap_timestamps = data['tapTimestamps']

    print(f"Actual label: {actual_label}")
    print(f"Number of taps: {len(tap_timestamps)}")
    print("\nPredicting...")
    print("=" * 60)

    predictions = []
    for i, tap_ts in enumerate(tap_timestamps):
        features = extract_features_from_tap(data, tap_ts)
        features_scaled = scaler.transform([features])
        prediction = model.predict(features_scaled)[0]
        probability = model.predict_proba(features_scaled)[0]
        predictions.append(prediction)

        max_prob = np.max(probability)
        print(f"Tap {i + 1:3d}: Predicted={prediction:15s} Confidence={max_prob:.3f}")

    print("=" * 60)

    correct = sum(1 for pred in predictions if pred == actual_label)
    accuracy = correct / len(predictions) if predictions else 0

    print(f"\nResults:")
    print(f"  Correct: {correct}/{len(predictions)}")
    print(f"  Accuracy: {accuracy:.1%}")

    from collections import Counter
    pred_counts = Counter(predictions)
    print(f"\nPrediction distribution:")
    for pred, count in pred_counts.most_common():
        print(f"  {pred}: {count} ({count / len(predictions):.1%})")


def main():
    if len(sys.argv) < 2:
        print("Usage: python test_tap_model.py <test_file.json>")
        print("\nExample:")
        print(
            "  python test_tap_model.py training_data/tap_collection_TAP_FRONT_36taps_1763840002702.json")
        return

    json_file = sys.argv[1]

    if not Path(json_file).exists():
        print(f"Error: File '{json_file}' not found")
        return

    test_model(json_file)


if __name__ == "__main__":
    main()
