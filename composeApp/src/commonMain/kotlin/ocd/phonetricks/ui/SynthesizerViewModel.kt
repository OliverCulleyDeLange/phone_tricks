package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ocd.phonetricks.audio.AudioManager
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.ControlMapping
import ocd.phonetricks.data.ControlParameter
import ocd.phonetricks.data.ControlSurface
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.sensor.SensorManager

class SynthesizerViewModel(
    sensorManager: SensorManager,
    private val audioManager: AudioManager,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {

    private val _rotationVector = MutableStateFlow<RotationVector?>(null)
    val rotationVector: StateFlow<RotationVector?> = _rotationVector.asStateFlow()

    private val _acceleration = MutableStateFlow<Accelerometer?>(null)
    val acceleration: StateFlow<Accelerometer?> = _acceleration.asStateFlow()

    private val _baseFrequency = MutableStateFlow(440f)
    val baseFrequency: StateFlow<Float> = _baseFrequency.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveform = MutableStateFlow(Waveform.SINE)
    val waveform: StateFlow<Waveform> = _waveform.asStateFlow()

    private val _isTouchInBox = MutableStateFlow(false)
    val isTouchInBox: StateFlow<Boolean> = _isTouchInBox.asStateFlow()

    private val _touchX = MutableStateFlow(0.5f)
    private val _touchY = MutableStateFlow(0.5f)
    private val _accelX = MutableStateFlow(0f)
    private val _accelY = MutableStateFlow(0f)
    private val _accelZ = MutableStateFlow(0f)
    private val _gyroX = MutableStateFlow(0f)
    private val _gyroY = MutableStateFlow(0f)
    private val _gyroZ = MutableStateFlow(0f)
    private val _quatX = MutableStateFlow(0f)
    private val _quatY = MutableStateFlow(0f)
    private val _quatZ = MutableStateFlow(0f)
    private val _quatW = MutableStateFlow(1f)

    private val surfaceValues: Map<ControlSurface, MutableStateFlow<Float>> = mapOf(
        ControlSurface.TOUCH_X to _touchX,
        ControlSurface.TOUCH_Y to _touchY,
        ControlSurface.ACCEL_X to _accelX,
        ControlSurface.ACCEL_Y to _accelY,
        ControlSurface.ACCEL_Z to _accelZ,
        ControlSurface.GYRO_X to _gyroX,
        ControlSurface.GYRO_Y to _gyroY,
        ControlSurface.GYRO_Z to _gyroZ,
        ControlSurface.QUATERNION_X to _quatX,
        ControlSurface.QUATERNION_Y to _quatY,
        ControlSurface.QUATERNION_Z to _quatZ,
        ControlSurface.QUATERNION_W to _quatW,
    )

    init {
        sensorManager.rotationVectorFlow
            .onEach { r ->
                _rotationVector.value = r
                _quatX.value = r.x
                _quatY.value = r.y
                _quatZ.value = r.z
                _quatW.value = r.scalar ?: 1f
                recompute(settingsViewModel.mappings.value)
            }
            .launchIn(viewModelScope)

        sensorManager.accelerometerFlow
            .onEach { a ->
                _acceleration.value = a
                _accelX.value = a.x
                _accelY.value = a.y
                _accelZ.value = a.z
                recompute(settingsViewModel.mappings.value)
            }
            .launchIn(viewModelScope)

        sensorManager.gyroscopeFlow
            .onEach { g ->
                _gyroX.value = g.x
                _gyroY.value = g.y
                _gyroZ.value = g.z
                recompute(settingsViewModel.mappings.value)
            }
            .launchIn(viewModelScope)

        settingsViewModel.mappings
            .onEach { recompute(it) }
            .launchIn(viewModelScope)
    }

    private fun recompute(mappings: List<ControlMapping>) {
        val pitchMappings = mappings.filterIsInstance<ControlMapping>()
            .filter { it.parameter is ControlParameter.Pitch }
        val volumeMappings = mappings.filter { it.parameter is ControlParameter.Volume }
        val waveformMappings = mappings.filter { it.parameter is ControlParameter.Waveform }

        val frequency = if (pitchMappings.isEmpty()) _baseFrequency.value else {
            val p = pitchMappings.first().parameter as ControlParameter.Pitch
            val t = pitchMappings.map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
            p.min + t * (p.max - p.min)
        }

        val amplitude = if (volumeMappings.isEmpty()) _amplitude.value else {
            val p = volumeMappings.first().parameter as ControlParameter.Volume
            val t = volumeMappings.map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
            p.min + t * (p.max - p.min)
        }

        val waveform = if (waveformMappings.isEmpty()) _waveform.value else {
            val p = waveformMappings.first().parameter as ControlParameter.Waveform
            val t = waveformMappings.map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
            mapWaveform(t, p)
        }

        _baseFrequency.value = frequency
        _amplitude.value = amplitude
        _waveform.value = waveform

        if (_isTouchInBox.value) {
            audioManager.playSynthSound(frequency, amplitude, waveform)
        }
    }

    private fun normalize(mapping: ControlMapping): Float {
        val raw = surfaceValues[mapping.surface]?.value ?: 0f
        val p = mapping.parameter
        val inputRange = p.inputMax - p.inputMin
        return if (inputRange == 0f) 0.5f else ((raw - p.inputMin) / inputRange).coerceIn(0f, 1f)
    }

    private fun mapWaveform(t: Float, config: ControlParameter.Waveform): Waveform {
        val startOrdinal = config.startWaveform.ordinal
        val endOrdinal = config.endWaveform.ordinal
        val interpolated = (startOrdinal + t * (endOrdinal - startOrdinal)).toInt()
            .coerceIn(minOf(startOrdinal, endOrdinal), maxOf(startOrdinal, endOrdinal))
        return Waveform.entries[interpolated]
    }

    fun onTouchInBox(x: Float = 0.5f, y: Float = 0.5f) {
        _isTouchInBox.value = true
        _touchX.value = x
        _touchY.value = y
        recompute(settingsViewModel.mappings.value)
    }

    fun onReleaseTouch() {
        _isTouchInBox.value = false
        audioManager.stopSound()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
