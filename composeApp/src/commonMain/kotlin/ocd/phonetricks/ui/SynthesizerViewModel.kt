package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import ocd.phonetricks.audio.AudioManager
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import ocd.phonetricks.audio.FilterPreset
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.ControlMapping
import ocd.phonetricks.data.ControlParameter
import ocd.phonetricks.data.ControlSurface
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.data.computeVolumeAmplitude
import ocd.phonetricks.data.normalizeSurfaceValue
import ocd.phonetricks.sensor.SensorManager

class SynthesizerViewModel(
    sensorManager: SensorManager,
    private val audioManager: AudioManager,
    private val settingsViewModel: SettingsViewModel,
    private val noteSettingsViewModel: NoteSettingsViewModel,
) : ViewModel() {

    private val _rotationVector = MutableStateFlow<RotationVector?>(null)
    val rotationVector: StateFlow<RotationVector?> = _rotationVector.asStateFlow()

    private val _acceleration = MutableStateFlow<Accelerometer?>(null)
    val acceleration: StateFlow<Accelerometer?> = _acceleration.asStateFlow()

    private val _baseFrequency = MutableStateFlow(440f)
    val baseFrequency: StateFlow<Float> = _baseFrequency.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveformA = MutableStateFlow(Waveform.SINE)
    private val _waveformB = MutableStateFlow(Waveform.SINE)
    private val _waveformBlend = MutableStateFlow(0f)
    val waveformA: StateFlow<Waveform> = _waveformA.asStateFlow()
    val waveformB: StateFlow<Waveform> = _waveformB.asStateFlow()
    val waveformBlend: StateFlow<Float> = _waveformBlend.asStateFlow()

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

    // Trigger fired when surface values change. Sampled at ~60Hz so the
    // audio thread isn't hit by every individual sensor emission — at
    // SENSOR_DELAY_GAME the three sensors together produce hundreds of
    // mutex acquisitions per second otherwise.
    private val recomputeTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        sensorManager.rotationVectorFlow
            .onEach { r ->
                _rotationVector.value = r
                _quatX.value = r.x
                _quatY.value = r.y
                _quatZ.value = r.z
                _quatW.value = r.scalar ?: 1f
                recomputeTrigger.tryEmit(Unit)
            }
            .launchIn(viewModelScope)

        sensorManager.accelerometerFlow
            .onEach { a ->
                _acceleration.value = a
                _accelX.value = a.x
                _accelY.value = a.y
                _accelZ.value = a.z
                recomputeTrigger.tryEmit(Unit)
            }
            .launchIn(viewModelScope)

        sensorManager.gyroscopeFlow
            .onEach { g ->
                _gyroX.value = g.x
                _gyroY.value = g.y
                _gyroZ.value = g.z
                recomputeTrigger.tryEmit(Unit)
            }
            .launchIn(viewModelScope)

        @OptIn(FlowPreview::class)
        recomputeTrigger
            .sample(16.milliseconds)
            .onEach { recompute(settingsViewModel.mappings.value) }
            .launchIn(viewModelScope)

        // Mappings change is rare and user-driven — apply immediately.
        settingsViewModel.mappings
            .onEach { recompute(it) }
            .launchIn(viewModelScope)
    }

    private fun recompute(mappings: List<ControlMapping>) {
        val pitchMappings = mappings.filter { it.parameter is ControlParameter.Pitch }
        val volumeMappings = mappings.filter { it.parameter is ControlParameter.Volume }
        val waveformMappings = mappings.filter { it.parameter is ControlParameter.Waveform }

        val scale = noteSettingsViewModel.scale.value
        val frequency = if (pitchMappings.isEmpty()) _baseFrequency.value else {
            val first = pitchMappings.first()
            val fp = first.parameter as ControlParameter.Pitch
            val ft = normalize(first).coerceIn(0f, 1f)
            val baseHz = fp.min + ft * (fp.max - fp.min)
            val snappedBaseHz = if (fp.snapToScale) scale.snapFrequency(baseHz) else baseHz
            val baseSemitones = 12.0 * ln(snappedBaseHz.toDouble() / 16.35) / ln(2.0)
            val totalSemitones = pitchMappings.drop(1).fold(baseSemitones) { acc, mapping ->
                val p = mapping.parameter as ControlParameter.Pitch
                val t = normalize(mapping).coerceIn(0f, 1f)
                val rangeInSemitones = 12.0 * ln(p.max.toDouble() / p.min) / ln(2.0)
                val offsetSemitones = (t - 0.5) * rangeInSemitones
                val offsetHz = 16.35 * 2.0.pow((acc + offsetSemitones) / 12.0)
                val finalHz = if (p.snapToScale) scale.snapFrequency(offsetHz.toFloat()).toDouble() else offsetHz
                12.0 * ln(finalHz / 16.35) / ln(2.0)
            }
            (16.35 * 2.0.pow(totalSemitones / 12.0)).toFloat()
        }

        val amplitude = computeVolumeAmplitude(
            volumeMappings.map { normalize(it) to (it.parameter as ControlParameter.Volume) }
        ) ?: _amplitude.value

        val waveformA: Waveform
        val waveformB: Waveform
        val blend: Float
        if (waveformMappings.isEmpty()) {
            waveformA = _waveformA.value
            waveformB = _waveformB.value
            blend = _waveformBlend.value
        } else {
            val p = waveformMappings.first().parameter as ControlParameter.Waveform
            val t = waveformMappings.map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
            waveformA = p.startWaveform
            waveformB = p.endWaveform
            blend = t
        }

        _baseFrequency.value = frequency
        _amplitude.value = amplitude
        _waveformA.value = waveformA
        _waveformB.value = waveformB
        _waveformBlend.value = blend

        mappings.filter { it.parameter is ControlParameter.EffectWetDry }
            .groupBy { (it.parameter as ControlParameter.EffectWetDry).effect }
            .forEach { (effect, effectMappings) ->
                val wetDry = effectMappings.map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
                audioManager.setEffect(effect, wetDry)
            }

        val filterFreqMappings = mappings.filter { it.parameter is ControlParameter.FilterFrequency }
        val filterWetDryMappings = mappings.filter { it.parameter is ControlParameter.FilterWetDry }
        val allFilterPresets: Set<FilterPreset> = (
            filterFreqMappings.map { (it.parameter as ControlParameter.FilterFrequency).preset } +
            filterWetDryMappings.map { (it.parameter as ControlParameter.FilterWetDry).preset }
        ).toSet()

        allFilterPresets.forEach { preset ->
            val freqParam = filterFreqMappings
                .firstOrNull { (it.parameter as ControlParameter.FilterFrequency).preset == preset }
                ?.parameter as? ControlParameter.FilterFrequency
            val wetDryParam = filterWetDryMappings
                .firstOrNull { (it.parameter as ControlParameter.FilterWetDry).preset == preset }
                ?.parameter as? ControlParameter.FilterWetDry

            val frequency2 = if (freqParam != null) {
                val t = filterFreqMappings
                    .filter { (it.parameter as ControlParameter.FilterFrequency).preset == preset }
                    .map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
                freqParam.min + t * (freqParam.max - freqParam.min)
            } else 1000f

            val wetDry = if (wetDryParam != null) {
                filterWetDryMappings
                    .filter { (it.parameter as ControlParameter.FilterWetDry).preset == preset }
                    .map { normalize(it) }.average().toFloat().coerceIn(0f, 1f)
            } else 0f

            audioManager.setFilter(preset, frequency2, wetDry)
        }

        if (_isTouchInBox.value) {
            audioManager.playSynthSound(frequency, amplitude, waveformA, waveformB, blend)
        }
    }

    private fun normalize(mapping: ControlMapping): Float {
        val raw = surfaceValues[mapping.surface]?.value ?: 0f
        val p = mapping.parameter
        return normalizeSurfaceValue(raw, p.inputMin, p.inputMax)
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
}
