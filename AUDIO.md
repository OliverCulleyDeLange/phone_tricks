# Audio Feedback

The app now plays a short synthesized audio tone when a tap is detected.

## Architecture

### Flow-Based Event System

The `TrickEngine` has been refactored to emit trick events as they happen using a `SharedFlow`
instead of accumulating them in a list:

```kotlin
// In TrickEngine.kt
private val _trickEvents = MutableSharedFlow<TrickEvent>()
val trickEvents: SharedFlow<TrickEvent> = _trickEvents.asSharedFlow()
```

This allows multiple listeners to react to trick events in real-time, which is perfect for audio
playback.

### Audio Manager

The audio system uses a platform-specific abstraction:

```kotlin
interface AudioManager {
    fun playTapSound()
    fun release()
}

expect fun createAudioManager(): AudioManager
```

#### Android Implementation (`AndroidAudioManager`)

- Uses `AudioTrack` with `MODE_STATIC` for low-latency playback
- Generates a 50ms sine wave at 800Hz
- Applies fade-in/fade-out envelope to avoid clicks
- Pre-loads the audio buffer for instant playback

#### iOS Implementation (`IOSAudioManager`)

- Uses `AVAudioEngine` with `AVAudioPlayerNode`
- Generates the same 50ms sine wave at 800Hz
- Uses float buffers for sample generation
- Schedules buffer playback for low latency

### Integration

The `SensorViewModel` listens to the trick events flow and plays sounds:

```kotlin
init {
    viewModelScope.launch {
        engine.trickEvents.collect { event ->
            // Accumulate for UI display
            _detectedTricks.value = _detectedTricks.value + event
            
            // Play sound if it's a tap
            if (event.type.isTap()) {
                audioManager.playTapSound()
            }
        }
    }
}
```

## Sound Design

The tap sound is a simple synthesized sine wave with the following characteristics:

- **Frequency**: 800 Hz (pleasing mid-high tone)
- **Duration**: 50 ms (short and punchy)
- **Volume**: 30% (not too loud)
- **Envelope**:
    - Fade-in: 10% (first 5ms)
    - Sustain: 60% (30ms)
    - Fade-out: 30% (last 15ms)

This envelope prevents audio clicks while keeping the sound snappy and responsive.

## Benefits of the New Architecture

1. **Real-time Responsiveness**: Events are emitted immediately when detected
2. **Separation of Concerns**: Audio playback is decoupled from event detection
3. **Extensibility**: Multiple listeners can react to the same events (UI updates, audio, logging,
   etc.)
4. **Memory Efficiency**: No need to store all events in a list if only recent events matter
5. **Better UX**: Immediate audio feedback makes the app feel more responsive

## Future Enhancements

- Different sounds for different tap surfaces (front, back, edges)
- Pitch variation based on tap intensity (confidence)
- Option to toggle audio feedback on/off
- Custom sound selection
- Volume control
