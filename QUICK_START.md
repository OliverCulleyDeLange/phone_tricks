# Quick Start: ML-Powered Tap Detection

## What Changed?

Your `TapDetector` has been upgraded to use machine learning! The app now uses a trained Random
Forest model that analyzes 90 features from 6 sensors to accurately classify tap events.

## Files Added

### Kotlin Implementation

- `composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/MLTapDetector.kt` - ML-powered tap
  detector
- `composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/RandomForestModel.kt` - Random Forest
  inference
- `composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/TapFeatureExtractor.kt` - Feature
  extraction

### Python Tools

- `data/export_model_to_kotlin.py` - Converts pkl model to Kotlin-friendly JSON
- `data/convert_model_to_onnx.py` - Alternative ONNX converter (optional)

### Documentation

- `data/ML_INTEGRATION_GUIDE.md` - Comprehensive integration guide

### Model File

- `composeApp/src/commonMain/composeResources/files/tap_classifier_kotlin.json` - The trained
  model (860 KB)

## How It Works

1. **Spike Detection**: Detects sudden acceleration changes (potential taps)
2. **Feature Extraction**: Extracts 90 statistical features from a 200ms window
3. **ML Prediction**: Runs Random Forest inference to classify the tap
4. **Confidence Filtering**: Only accepts predictions above 40% confidence
5. **Fallback**: Uses rule-based detection if ML fails

## Using the Current Model

The model is already loaded and active! Just rebuild and run your app:

```bash
./gradlew :composeApp:assembleDebug
```

The `TrickEngine` now uses `MLTapDetector` automatically.

## Improving the Model

### Step 1: Collect Training Data

Use the "Tap Collection" screen to record taps with different labels.

### Step 2: Train

```bash
cd data
python train_tap_model.py
```

### Step 3: Export to Kotlin

```bash
cd data
python export_model_to_kotlin.py
```

This will automatically copy the model to the app resources.

### Step 4: Rebuild App

```bash
./gradlew clean
./gradlew :composeApp:assembleDebug
```

## Configuration

Edit `MLTapDetector.kt` to tune parameters:

```kotlin
private val tapThreshold = 1f                   // Sensitivity (lower = more sensitive)
private val tapCooldownMs = 50                  // Min time between taps
private val mlConfidenceThreshold = 0.4f        // Min confidence to accept (0-1)
```

## Current Model Performance

Your trained model recognizes:

- `TAP_FRONT` - Taps on screen ✓
- `held` - Phone being held
- Various tap scenarios (hard surface, soft surface, etc.)

Only "TAP_*" predictions are mapped to tap events. Other predictions are logged but ignored.

## Troubleshooting

### "Failed to load ML model"

- Check that `tap_classifier_kotlin.json` exists in resources
- Rebuild the project completely
- Check console for detailed error messages

### Model is too sensitive/not sensitive enough

Adjust `mlConfidenceThreshold`:

- Higher (0.6-0.8) = fewer false positives, might miss real taps
- Lower (0.2-0.4) = more detections, might include noise

### Wrong tap surface detected

- Collect more training data for that surface
- Retrain the model
- Ensure phone orientation is varied in training data

## Next Steps

1. Test the current ML detector
2. Collect diverse training data for better accuracy
3. Retrain and redeploy the model
4. Adjust confidence thresholds based on real-world performance

For detailed information, see `data/ML_INTEGRATION_GUIDE.md`.
