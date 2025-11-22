# ML Model Integration Guide

## Overview

The PhoneTricks app now uses a trained Random Forest machine learning model to detect and classify
tap events on different surfaces of the phone. This replaces the simple rule-based tap detection
with a more accurate ML-powered approach.

## Architecture

### Components

1. **TapFeatureExtractor** - Extracts 90 features from sensor data around tap events
    - 6 sensors × 15 features each (mean, std, max, min, range for x, y, z)
    - 200ms time window around tap timestamp

2. **RandomForestModel** - Loads and runs inference on the trained model
    - Pure Kotlin implementation (no external ML libraries)
    - Loads model from JSON file in app resources
    - Supports 100 decision trees

3. **MLTapDetector** - Combines spike detection with ML classification
    - Detects acceleration spikes (potential taps)
    - Extracts features and runs ML prediction
    - Falls back to rule-based detection if ML fails
    - Filters predictions based on confidence threshold

### Data Flow

```
Sensor Readings (6 buffers)
    ↓
MLTapDetector.processSensorData()
    ↓
Detect acceleration spike > threshold
    ↓
Extract 90 features from 200ms window
    ↓
RandomForestModel.predict(features)
    ↓
Map prediction to TrickType
    ↓
Emit TrickEvent with confidence
```

## Training the Model

### 1. Collect Training Data

Use the "Tap Collection" screen in the app to record taps:

- Record taps for different labels (TAP_FRONT, TAP_BACK, etc.)
- Export JSON files to your computer
- Place files in `data/training_data/`

### 2. Train the Model

```bash
cd data
python train_tap_model.py
```

This will:

- Load all JSON files from `training_data/`
- Extract features from each tap
- Train a Random Forest classifier
- Save `models/tap_classifier.pkl` and `models/scaler.pkl`
- Print accuracy and feature importance

### 3. Export for Kotlin

```bash
cd data
python export_model_to_kotlin.py
```

This converts the sklearn model to a Kotlin-friendly JSON format:

- Exports decision tree structures
- Exports scaler parameters
- Saves to `models/tap_classifier_kotlin.json`

### 4. Deploy to App

The export script automatically copies the file to:

```
composeApp/src/commonMain/composeResources/files/tap_classifier_kotlin.json
```

The app will load this on startup.

## Model Classes

The trained model currently recognizes these classes:

- `TAP_FRONT` - Taps on screen
- `held` - Phone being held/gripped
- `held + triple taps` - Triple taps while holding
- `held_hard_taps` - Hard taps while holding
- `moving phone + few taps` - Taps while moving
- `on hard surface` - Phone on hard surface
- `on hard surface + hard taps` - Hard taps on hard surface
- `on soft surface` - Phone on soft surface
- `on soft surface + hard taps` - Hard taps on soft surface

Only predictions containing "TAP_" are mapped to TrickType events.

## Configuration

### MLTapDetector Parameters

```kotlin
private val tapThreshold = 1f  // Acceleration delta to trigger detection
private val tapCooldownMs = 50  // Minimum time between taps
private val mlConfidenceThreshold = 0.4f  // Minimum confidence to accept prediction
```

### Feature Extraction

```kotlin
windowMs: Long = 200  // Time window around tap for feature extraction
```

## Performance

- **Model size**: ~860 KB
- **Inference time**: < 10ms on modern devices
- **Memory usage**: Minimal (model loaded once on startup)
- **Accuracy**: Depends on training data quality

## Fallback Behavior

If the ML model:

- Fails to load
- Throws an error during inference
- Returns low confidence predictions

The detector falls back to the original rule-based approach using device orientation and
acceleration direction.

## Testing

### Test a specific recording:

```bash
cd data
python test_tap_model.py training_data/your_file.json
```

### Visualize predictions:

```bash
cd data
python visualize_taps.py training_data/your_file.json
```

## Updating the Model

To improve accuracy:

1. Collect more diverse training data
2. Retrain: `python train_tap_model.py`
3. Export: `python export_model_to_kotlin.py`
4. Rebuild app

The model will automatically be included in the next build.

## Troubleshooting

### Model not loading

- Check if `tap_classifier_kotlin.json` exists in resources
- Look for errors in console during app startup
- Verify JSON is valid

### Low accuracy

- Collect more training data
- Ensure labels are consistent
- Try different tap intensities and phone orientations
- Adjust `mlConfidenceThreshold`

### High false positive rate

- Increase `tapThreshold`
- Increase `mlConfidenceThreshold`
- Train with more "non-tap" examples

## Future Improvements

- Train separate models for different tap types
- Use deep learning (TensorFlow Lite) for better accuracy
- Online learning to adapt to user patterns
- Context-aware detection (sitting vs walking)
