# Tap Detection Feature

## Overview

The tap detection feature identifies when the phone is tapped on different surfaces (front, back, or
any of the 4 edges) using accelerometer data. This extends the existing trick detection system which
detects spins and flips.

## Implementation

### Components

1. **TapDetector** (`composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/TapDetector.kt`)
    - Core detection algorithm
    - Analyzes accelerometer data for impact events
    - Determines which surface was tapped based on acceleration direction

2. **TrickType Extensions** (`composeApp/src/commonMain/kotlin/ocd/phonetricks/data/TrickEvent.kt`)
    - Added tap types: `TAP_FRONT`, `TAP_BACK`, `TAP_TOP`, `TAP_BOTTOM`, `TAP_LEFT`, `TAP_RIGHT`
    - Helper functions: `isTap()`, `getLabel()`, `getShortLabel()`

3. **TrickEngine Integration** (
   `composeApp/src/commonMain/kotlin/ocd/phonetricks/engine/TrickEngine.kt`)
    - Instantiates TapDetector alongside TrickDetector
    - Processes both trick and tap events together
    - Manages detector state and resets

4. **UI Updates** (
   `composeApp/src/commonMain/kotlin/ocd/phonetricks/ui/components/TrickTimeline.kt`)
    - Displays tap events with yellow color
    - Shows surface labels (F/B/T/Bo/L/R) above tap dots
    - Counts and displays total taps

## Detection Algorithm

### Impact Detection

The TapDetector monitors acceleration magnitude for sudden spikes:

```kotlin
// Threshold for tap detection
private val tapThreshold = 15.0f // m/s²

// Cooldown to avoid double-detections
private val tapCooldownMs = 300L

// Uses linear acceleration if available (excludes gravity)
val accel = current.linearAcceleration ?: current.accelerometer
val accelMagnitude = sqrt(accel.x² + accel.y² + accel.z²)

if (accelMagnitude > tapThreshold) {
    // Tap detected!
}
```

### Surface Identification

The detector determines which surface was tapped by analyzing the acceleration vector:

```
Phone Coordinate System (portrait mode):
  +Y (top)
   ↑
   |
   |
   +----→ +X (right)
  /
 ↙
+Z (screen toward user)
```

**Decision Logic:**

- Find the axis with maximum absolute acceleration
- Determine direction (positive or negative)
- Map to surface:
    - **Z-axis**: Front (screen) or Back
    - **Y-axis**: Top or Bottom edge
    - **X-axis**: Left or Right edge

### Confidence Score

Based on impact magnitude:

```kotlin
confidence = (magnitude - threshold) / (threshold * 2)
// Clamped to [0.0, 1.0]
```

Higher impact = higher confidence (up to 3x threshold = 100% confidence)

## Usage

### Running the App

1. Build and run on a physical device (simulators have limited sensor support)
2. The app auto-starts recording sensor data
3. Tap the phone on different surfaces:
    - Tap the screen (front)
    - Tap the back
    - Tap any of the 4 edges

### Viewing Results

The **Trick Timeline** at the top of the screen shows:

- Yellow dots for tap events
- Surface labels above each dot (F, B, T, Bo, L, R)
- Total tap count in the counter badge
- Scrolling timeline showing last 10 seconds

## Tuning Parameters

You can adjust these values in `TapDetector.kt`:

```kotlin
// Sensitivity: lower = more sensitive to light taps
private val tapThreshold = 15.0f

// Cooldown: minimum time between tap detections
private val tapCooldownMs = 300L
```

**Recommended Values:**

- **Light taps**: threshold = 10-12 m/s²
- **Normal taps**: threshold = 15 m/s² (default)
- **Hard taps only**: threshold = 20-25 m/s²

## Testing Tips

1. **Front/Back Detection:**
    - Lay phone flat on a soft surface
    - Tap firmly on the screen or back
    - Should detect correctly regardless of orientation

2. **Edge Detection:**
    - Hold phone vertically
    - Tap on the top, bottom, left, or right edge
    - May need firmer taps for edges

3. **Avoiding False Positives:**
    - Walking/movement shouldn't trigger taps (threshold is set high enough)
    - Dropping phone will likely trigger a tap
    - Phone vibration may trigger false positives

## Integration with Existing Features

- **Replay Mode**: Tap events are captured in the 10-second replay buffer
- **Trick Detection**: Runs alongside spin/flip detection without interference
- **Tare Function**: Doesn't affect tap detection (uses accelerometer, not rotation)
- **Sensor Graphs**: Tap events are visible as spikes in the accelerometer/linear acceleration
  graphs

## Future Enhancements

Possible improvements:

- Double-tap detection
- Tap pattern recognition (e.g., "shave and a haircut")
- Tap strength/intensity classification (soft, medium, hard)
- Haptic feedback on tap detection
- Tap-based controls or Easter eggs
- Machine learning for more accurate surface detection
