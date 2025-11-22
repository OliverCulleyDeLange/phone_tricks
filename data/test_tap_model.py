import json
import numpy as np
from pathlib import Path
import sys
import joblib
from feature_extraction import extract_features_from_tap, extract_negative_samples


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

    if 'labels' in data and isinstance(data['labels'], dict):
        labels_obj = data['labels']
        label_parts = []
        if labels_obj.get('sessionTag'):
            label_parts.append(labels_obj['sessionTag'])
        if labels_obj.get('surface'):
            label_parts.extend(labels_obj['surface'])
        if labels_obj.get('taps'):
            label_parts.extend(labels_obj['taps'])
        actual_label = '_'.join(label_parts) if label_parts else 'unlabeled'
    else:
        actual_label = data.get('label', 'unlabeled')

    tap_timestamps = data['tapTimestamps']
    is_negative = len(tap_timestamps) == 0 or 'negative' in Path(json_file).name.lower()

    if is_negative:
        print(f"Actual label: {actual_label} [NEGATIVE SAMPLE]")
        print(f"Tap count: {len(tap_timestamps)}")
        print("\nExtracting random windows for negative sample testing...")
        print("=" * 60)

        negative_features = extract_negative_samples(data, num_samples=20)

        if not negative_features:
            print("Error: Could not extract features from negative sample")
            return

        predictions = []
        for i, features in enumerate(negative_features):
            features_scaled = scaler.transform([features])
            prediction = model.predict(features_scaled)[0]
            probability = model.predict_proba(features_scaled)[0]
            predictions.append(prediction)

            max_prob = np.max(probability)
            print(f"Window {i + 1:3d}: Predicted={prediction:20s} Confidence={max_prob:.3f}")

        print("=" * 60)

        correct = sum(1 for pred in predictions if pred == 'NEGATIVE')
        accuracy = correct / len(predictions) if predictions else 0

        print(f"\nResults:")
        print(f"  Correctly identified as NEGATIVE: {correct}/{len(predictions)}")
        print(f"  Accuracy: {accuracy:.1%}")

        from collections import Counter
        pred_counts = Counter(predictions)
        print(f"\nPrediction distribution:")
        for pred, count in pred_counts.most_common():
            print(f"  {pred}: {count} ({count / len(predictions):.1%})")
    else:
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
            print(f"Tap {i + 1:3d}: Predicted={prediction:20s} Confidence={max_prob:.3f}")

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
