# PhoneTricks Tap Detection Training

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Collect Training Data

Use the PhoneTricks app to collect tap data:

**Minimum for testing:**

- 3-5 sessions of front taps (100-200 taps total)
- 3-5 sessions of back taps (100-200 taps total)
- Optional: side taps or other locations

**Better baseline:**

- 200-300 taps per location
- Various conditions (different speeds, intensities)

Files will be saved to `training_data/` directory as:

- `tap_collection_TAP_FRONT_36taps_1763840002702.json`
- `tap_collection_TAP_BACK_42taps_1763840123456.json`
- etc.

### 3. Visualize Your Data

```bash
python visualize_taps.py
```

This processes all JSON files in `training_data/` and saves visualizations to `visualizations/`
directory.

To visualize a single file:

```bash
python visualize_taps.py training_data/tap_collection_TAP_FRONT_36taps_1763840002702.json
```

### 4. Train the Model

```bash
python train_tap_model.py
```

This will:

- Load all training data from `training_data/`
- Extract features from sensor data (200ms window around each tap)
- Train a Random Forest classifier
- Show accuracy metrics and feature importance
- Save model to `models/tap_classifier.pkl` and `models/scaler.pkl`

Expected output:

```
Found 6 training data file(s)
...
Total samples: 250
Train samples: 200
Test samples: 50

✓ Training complete!
  Training accuracy: 0.985
  Test accuracy: 0.920
```

### 5. Export Model for Kotlin App

```bash
python export_model_to_kotlin.py
```

This will:

- Convert the trained .pkl model to Kotlin-friendly JSON format
- Export decision tree structures and scaler parameters
- Automatically copy the model to `composeApp/src/commonMain/composeResources/files/`
- The app will load this model on next build

Expected output:

```
Loading model and scaler...
Model classes: ['TAP_FRONT' 'TAP_BACK' ...]
Number of features: 90
Number of trees: 100

✓ Model exported to: models/tap_classifier_kotlin.json
  File size: 863.1 KB

✓ Model copied to: ../composeApp/src/commonMain/composeResources/files/tap_classifier_kotlin.json
```

### 6. Rebuild the App

```bash
cd ..
./gradlew :composeApp:assembleDebug
```

The app will now use your updated ML model for tap detection!

### 7. Test the Model (Optional)

Before exporting, you can test the model on specific files:

```bash
python test_tap_model.py training_data/tap_collection_TAP_FRONT_36taps_1763840002702.json
```

This will predict the tap type for each tap in the file and show accuracy.

## Feature Engineering

The model extracts the following features from each tap (200ms window):

For each sensor (Accelerometer, Gyroscope, LinearAcceleration, Magnetometer, Gravity,
RotationVector):

- Mean of X, Y, Z axes
- Standard deviation of X, Y, Z axes
- Max of X, Y, Z axes
- Min of X, Y, Z axes
- Peak-to-peak (range) of X, Y, Z axes

Total: 6 sensors × 15 features = 90 features per tap

## Data Quality Tips

**Good training data has:**

- Consistent tap technique within each session
- Clear, distinct taps (not dragging or sliding)
- Variety across sessions (different times, slight grip variations)
- Balanced classes (similar number of taps for each location)

**Signs you need more data:**

- Test accuracy < 80%
- High training accuracy (>95%) but low test accuracy (overfitting)
- Model confuses specific tap types

**To improve:**

- Collect 2-3x more data for confused classes
- Add more variation (different speeds, intensities)
- Ensure taps are clearly separated in time
- Check visualizations to verify sensor signals are distinct

## Files

- `visualize_taps.py` - Visualize sensor data and tap timestamps
- `train_tap_model.py` - Train the tap classification model
- `test_tap_model.py` - Test model on new data
- `export_model_to_kotlin.py` - Export model to Kotlin app format
- `convert_model_to_onnx.py` - Alternative ONNX export (optional)
- `requirements.txt` - Python dependencies
- `training_data/` - JSON files with tap collection sessions
- `visualizations/` - PNG charts of sensor data
- `models/` - Trained model files (generated after training)

## Model Architecture

Using Random Forest Classifier:

- 100 trees
- Max depth: 20
- Simple, fast, interpretable
- Works well with limited data
- No need for GPU
- Pure Kotlin inference (no TensorFlow/ONNX needed)

For production, consider:

- Collecting 500-1000+ taps per class
- Testing with different users
- Adding more tap types (side, edge, corner)
- Using deep learning (LSTM/CNN) for temporal patterns

## ML Integration

The trained model is integrated into the app via:

- **MLTapDetector** - Combines spike detection with ML classification
- **RandomForestModel** - Pure Kotlin Random Forest inference
- **TapFeatureExtractor** - Extracts 90 features matching training code

See `../QUICK_START.md` and `ML_INTEGRATION_GUIDE.md` for details.
