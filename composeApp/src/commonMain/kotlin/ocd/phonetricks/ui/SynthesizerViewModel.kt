package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ocd.phonetricks.audio.AudioManager
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.sensor.SensorManager
import kotlin.math.max
import kotlin.math.min

class SynthesizerViewModel(
    private val sensorManager: SensorManager,
    private val audioManager: AudioManager
) : ViewModel() {

    // Device position state (still keep for visualization)
    private val _rotationVector = MutableStateFlow<RotationVector?>(null)
    val rotationVector: StateFlow<RotationVector?> = _rotationVector.asStateFlow()

    private val _acceleration = MutableStateFlow<Accelerometer?>(null)
    val acceleration: StateFlow<Accelerometer?> = _acceleration.asStateFlow()

    // Synthesizer parameters
    private val _baseFrequency = MutableStateFlow(440f) // A4 note
    val baseFrequency: StateFlow<Float> = _baseFrequency.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveform = MutableStateFlow(Waveform.SINE)
    val waveform: StateFlow<Waveform> = _waveform.asStateFlow()

    // Tap box state
    private val _isTouchInBox = MutableStateFlow(false)
    val isTouchInBox: StateFlow<Boolean> = _isTouchInBox.asStateFlow()

    // Touch position in box (normalized 0.0-1.0)
    private val _touchX = MutableStateFlow(0.5f)
    private val _touchY = MutableStateFlow(0.5f)

    // Min/Max values for mapping
    private val minFrequency = 110f   // A2 note
    private val maxFrequency = 1760f  // A6 note

    init {
        // Subscribe to sensor data (still keep for visualization)
        sensorManager.rotationVectorFlow
            .onEach { rotation ->
                _rotationVector.value = rotation
            }
            .launchIn(viewModelScope)

        sensorManager.accelerometerFlow
            .onEach { accel ->
                _acceleration.value = accel
            }
            .launchIn(viewModelScope)
    }

    /**
     * Update synthesizer parameters based on touch position
     *
     * @param x Normalized X position (0.0-1.0, left to right)
     * @param y Normalized Y position (0.0-1.0, top to bottom)
     */
    private fun updateParametersFromTouch(x: Float, y: Float) {
        // Map x (0.0-1.0) to frequency range
        val frequency = minFrequency + x * (maxFrequency - minFrequency)
        _baseFrequency.value = frequency

        // Map y (0.0-1.0) to amplitude (invert y so higher is louder)
        val invertedY = 1f - y  // So bottom is quiet (0) and top is loud (1)
        _amplitude.value = max(0.1f, min(invertedY, 1.0f))

        // Update sound
        playSynthSound()
    }

    /**
     * Toggle between different waveforms
     */
    fun cycleWaveform() {
        val currentWaveform = _waveform.value
        _waveform.value = when (currentWaveform) {
            Waveform.SINE -> Waveform.SQUARE
            Waveform.SQUARE -> Waveform.TRIANGLE
            Waveform.TRIANGLE -> Waveform.SAWTOOTH
            Waveform.SAWTOOTH -> Waveform.SINE
        }

        if (_isTouchInBox.value) {
            playSynthSound()
        }
    }

    /**
     * Play synthesized sound with current parameters
     */
    private fun playSynthSound() {
        audioManager.playSynthSound(
            frequency = _baseFrequency.value,
            amplitude = _amplitude.value,
            waveform = _waveform.value
        )
    }

    /**
     * Stop all sounds
     */
    private fun stopSound() {
        audioManager.stopSound()
    }

    /**
     * Called when touch enters or moves within the box region
     *
     * @param inBox Whether the touch is in the box
     * @param x Normalized X position (0.0-1.0, left to right)
     * @param y Normalized Y position (0.0-1.0, top to bottom)
     */
    fun onTouchInBox(inBox: Boolean, x: Float = 0.5f, y: Float = 0.5f) {
        _isTouchInBox.value = inBox

        if (inBox) {
            _touchX.value = x
            _touchY.value = y
            updateParametersFromTouch(x, y)
        } else {
            // Make sure sound stops when touch ends
            stopSound()
            // Force stop again after a short delay to handle any edge cases
            viewModelScope.launch {
                kotlinx.coroutines.delay(50)
                stopSound()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
