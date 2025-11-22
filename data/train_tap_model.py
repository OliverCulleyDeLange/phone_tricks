import json
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
import joblib
from feature_extraction import extract_features_from_tap, extract_negative_samples


def load_and_extract_features(json_files):
    all_features = []
    all_labels = []

    for json_file in json_files:
        print(f"Processing {json_file.name}...")

        with open(json_file, 'r') as f:
            data = json.load(f)

        if 'labels' in data and isinstance(data['labels'], dict):
            labels_obj = data['labels']
            sample_type = labels_obj.get('sampleType', 'positive')
            label_parts = []
            if labels_obj.get('sessionTag'):
                label_parts.append(labels_obj['sessionTag'])
            if labels_obj.get('surface'):
                label_parts.extend(labels_obj['surface'])
            if labels_obj.get('taps'):
                label_parts.extend(labels_obj['taps'])
            original_label = '_'.join(label_parts) if label_parts else 'unlabeled'
        else:
            sample_type = 'negative' if 'negative' in json_file.name.lower() else 'positive'
            original_label = data.get('label', 'unlabeled')

        tap_timestamps = data['tapTimestamps']

        is_negative = sample_type == 'negative' or len(tap_timestamps) == 0

        if is_negative:
            print(f"  Label: {original_label} [NEGATIVE], Extracting random windows...")
            negative_features = extract_negative_samples(data, num_samples=20)
            for features in negative_features:
                all_features.append(features)
                all_labels.append('NEGATIVE')
            print(f"  Extracted {len(negative_features)} negative samples")
        else:
            print(f"  Label: {original_label} -> TAP, Taps: {len(tap_timestamps)}")
            for tap_ts in tap_timestamps:
                features = extract_features_from_tap(data, tap_ts)
                all_features.append(features)
                all_labels.append('TAP')

    return np.array(all_features), np.array(all_labels)


def train_model():
    training_data_dir = Path("training_data")

    if not training_data_dir.exists():
        print("Error: training_data directory not found")
        return

    json_files = sorted(training_data_dir.glob("*.json"))

    if not json_files:
        print("No training data files found!")
        return

    print(f"Found {len(json_files)} training data file(s)\n")
    print("=" * 60)

    X, y = load_and_extract_features(json_files)

    print("\n" + "=" * 60)
    print(f"Total samples: {len(X)}")
    print(f"Feature dimensions: {X.shape[1]}")
    print(f"Label distribution:")
    unique, counts = np.unique(y, return_counts=True)
    for label, count in zip(unique, counts):
        print(f"  {label}: {count} samples")

    if len(np.unique(y)) < 2:
        print(
            "\n⚠️  Warning: Only one class detected. Need at least 2 different tap types to train.")
        return

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f"\nTrain samples: {len(X_train)}")
    print(f"Test samples: {len(X_test)}")

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    print("\nTraining Random Forest classifier...")
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=20,
        min_samples_split=5,
        random_state=42,
        n_jobs=-1
    )

    model.fit(X_train_scaled, y_train)

    train_score = model.score(X_train_scaled, y_train)
    test_score = model.score(X_test_scaled, y_test)

    print(f"\n✓ Training complete!")
    print(f"  Training accuracy: {train_score:.3f}")
    print(f"  Test accuracy: {test_score:.3f}")

    y_pred = model.predict(X_test_scaled)

    print("\n" + "=" * 60)
    print("Classification Report:")
    print("=" * 60)
    print(classification_report(y_test, y_pred))

    print("=" * 60)
    print("Confusion Matrix:")
    print("=" * 60)
    cm = confusion_matrix(y_test, y_pred)
    print(cm)
    print()
    labels = model.classes_
    print("Labels:", labels)

    feature_importance = model.feature_importances_
    sensor_names = ['Accelerometer', 'Gyroscope', 'LinearAccel', 'Magnetometer', 'Gravity',
                    'RotationVector']
    print("\n" + "=" * 60)
    print("Feature Importance by Sensor:")
    print("=" * 60)
    features_per_sensor = 15
    for i, sensor in enumerate(sensor_names):
        start_idx = i * features_per_sensor
        end_idx = start_idx + features_per_sensor
        sensor_importance = np.sum(feature_importance[start_idx:end_idx])
        print(f"{sensor:20s}: {sensor_importance:.4f}")

    models_dir = Path("models")
    models_dir.mkdir(exist_ok=True)

    model_path = models_dir / "tap_classifier.pkl"
    scaler_path = models_dir / "scaler.pkl"

    joblib.dump(model, model_path)
    joblib.dump(scaler, scaler_path)

    print("\n" + "=" * 60)
    print(f"✓ Model saved to: {model_path}")
    print(f"✓ Scaler saved to: {scaler_path}")
    print("=" * 60)

    print("\n📊 Next Steps:")
    print("  1. Collect more data for different tap types if needed")
    print("  2. Test with: python test_tap_model.py <test_file.json>")
    print("  3. If accuracy is low (<80%), collect more diverse training data")


if __name__ == "__main__":
    train_model()
