# Training Data Collection

This document explains how to collect training data for training machine learning models to detect
taps.

## Overview

The app now includes a **Training Data Collection** mode that allows you to record sensor data at
maximum sampling rates and save labeled samples for ML training.

## Features

### Maximum Sensor Sampling Rate

- **Android**: Uses `SENSOR_DELAY_FASTEST` (typically 200-500+ Hz depending on device)
- **iOS**: Set to 100 Hz update rate
- **Emission Rate**: Minimal delay (1ms) for maximum throughput

### Data Format

Each training sample is saved as a JSON file with the following structure:

```json
{
  "label": "TAP_FRONT",
  "sampleRate": "FASTEST/100Hz",
  "recordingTimestampMs": 1234567890123,
  "sensorData": [
    {
      "timestampMs": 1234567890000,
      "accelerometer": { "x": 0.1, "y": 0.2, "z": 9.8 },
      "gyroscope": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "magnetometer": { "x": 20.0, "y": 30.0, "z": -40.0 },
      "rotationVector": { "x": 0.0, "y": 0.0, "z": 0.0, "scalar": 1.0 },
      "linearAcceleration": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "gravity": { "x": 0.0, "y": 0.0, "z": 9.8 }
    },
    // ... up to 600 samples (10 seconds of data)
  ]
}
```

### Available Labels

The app supports collecting data for the following tap types:

- `TAP_FRONT` - Tap on the front face (screen)
- `TAP_BACK` - Tap on the back face
- `TAP_TOP` - Tap on the top edge
- `TAP_BOTTOM` - Tap on the bottom edge
- `TAP_LEFT` - Tap on the left edge
- `TAP_RIGHT` - Tap on the right edge

## How to Collect Training Data

### Step 1: Access Training Mode

1. Launch the PhoneTricks app
2. Tap the **"Training"** tab in the bottom navigation bar

### Step 2: Understand the Buffer

- The app maintains a **ring buffer** of the last 600 sensor readings (~10 seconds at 60Hz, less at
  higher rates)
- Buffer statistics are displayed in real-time:
    - **Sample Count**: Number of sensor readings in the buffer
    - **Duration**: Time span of the data in seconds
    - **Sample Rate**: Actual sampling frequency in Hz

### Step 3: Collect Samples

1. **Perform the tap**: Physically tap your phone on the desired surface (e.g., tap the back of the
   phone on a table)
2. **Label immediately**: Within 1-2 seconds, press the corresponding button (e.g., "Back" for a
   back tap)
3. The app will:
    - Export the current buffer contents
    - Save it as a JSON file with the label and timestamp
    - Display the filename of the saved sample

### Step 4: Repeat for Each Surface

- Collect **20-50 samples** per tap type for a robust dataset
- Vary your tapping:
    - Different strengths (light, medium, hard taps)
    - Different surfaces (table, hand, cushion)
    - Different orientations
    - Different holding positions

### Best Practices

1. **Consistent Timing**: Try to press the button quickly after tapping (within 1-2 seconds)
2. **Clear Taps**: Make distinct, clear taps on the intended surface
3. **Diverse Data**: Vary your tapping style to capture different scenarios
4. **Quality Over Quantity**: Ensure you're labeling correctly rather than rushing
5. **Regular Breaks**: Take breaks to maintain consistency

## File Storage Locations

### Android

- **Primary**: `/sdcard/Download/PhoneTricksTraining/`
- **Fallback**: Internal app storage if external storage unavailable
- Files are accessible via file manager or USB connection

### iOS

- **Location**: App's Documents directory under `training_data/`
- Access via:
    - iTunes File Sharing
    - Xcode Devices window
    - iOS Files app (if enabled)

### File Naming Convention

Files are named with the format: `sample_<LABEL>_<TIMESTAMP>.json`

Example: `sample_TAP_BACK_1701234567890.json`

## Using the Training Data

### Data Preparation

1. **Collect files** from the device storage
2. **Organize** by label into folders if needed
3. **Parse JSON** to extract sensor readings
4. **Prepare features**:
    - Extract time-series data (accelerometer, gyroscope, etc.)
    - Compute derived features (magnitude, derivatives, FFT, etc.)
    - Create sliding windows if needed

### Model Training Workflow

```python
# Example pseudocode
import json
import numpy as np

# Load samples
samples = []
labels = []

for file in training_files:
    with open(file) as f:
        data = json.load(f)
        samples.append(extract_features(data['sensorData']))
        labels.append(data['label'])

# Train model
X = np.array(samples)
y = np.array(labels)

model = train_classifier(X, y)
```

### Recommended Models

- **Random Forest**: Good baseline for time-series classification
- **LSTM/GRU**: For sequential pattern recognition
- **1D CNN**: For temporal feature extraction
- **Transformer**: For attention-based learning

### Feature Engineering Ideas

- **Raw Features**: x, y, z values from each sensor
- **Magnitude**: `sqrt(x² + y² + z²)` for each sensor
- **Derivatives**: Rate of change
- **Statistical**: Mean, std, min, max over windows
- **Frequency Domain**: FFT coefficients
- **Quaternion Features**: Rotation vector components

## Troubleshooting

### Low Sample Rate

- Check device capabilities
- Close background apps
- Restart the app

### Files Not Saving

- **Android**: Check storage permissions
- **iOS**: Ensure app has document access
- Check available storage space

### Inconsistent Data

- Ensure stable grip on device
- Tap on consistent surfaces
- Label immediately after tapping

## Next Steps

After collecting training data:

1. Export files from device
2. Analyze data distribution
3. Train your ML model
4. Evaluate performance
5. Integrate model back into the app (future feature)

## Technical Details

### Ring Buffer

- **Capacity**: 600 samples
- **Behavior**: Circular buffer that overwrites oldest data
- **Contents**: Complete sensor snapshots with all available sensors

### Serialization

- **Format**: JSON with kotlinx.serialization
- **Pretty Print**: Enabled for readability
- **Size**: Typically 200-500 KB per sample depending on buffer size

### Thread Safety

- All file I/O operations run on background threads (Dispatchers.IO on Android, Dispatchers.Default
  on iOS)
- UI updates happen on main thread
- Saving is non-blocking with progress indicator

## Future Enhancements

Potential improvements for future versions:

- [ ] Batch export multiple samples
- [ ] Export to CSV format
- [ ] Data augmentation options
- [ ] Real-time model inference
- [ ] Cloud storage integration
- [ ] Sample preview/visualization
- [ ] Data validation tools
