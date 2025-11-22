package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ocd.phonetricks.audio.createAudioManager
import ocd.phonetricks.data.*
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.sensor.SensorManager
import kotlin.math.sqrt

class SensorViewModel(sensorManager: SensorManager) : ViewModel() {
    private val engine = TrickEngine(sensorManager, viewModelScope)
    private val audioManager = createAudioManager()

    // Tare quaternion - stores the offset rotation to align the model with the device
    private val _tareQuaternion = MutableStateFlow<RotationVector?>(null)

    val accelerometerData: StateFlow<Accelerometer?> = engine.bufferUpdate.map {
        engine.getCurrentAccelerometer()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gyroscopeData: StateFlow<Gyroscope?> = engine.bufferUpdate.map {
        engine.getCurrentGyroscope()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val magnetometerData: StateFlow<Magnetometer?> = engine.bufferUpdate.map {
        engine.getCurrentMagnetometer()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val rotationVectorData: StateFlow<RotationVector?> = engine.bufferUpdate.map {
        engine.getCurrentRotationVector()?.let { reading -> applyTare(reading, _tareQuaternion.value) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val linearAccelerationData: StateFlow<LinearAcceleration?> = engine.bufferUpdate.map {
        engine.getCurrentLinearAcceleration()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val gravityData: StateFlow<Gravity?> = engine.bufferUpdate.map {
        engine.getCurrentGravity()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val accelerometerHistory: StateFlow<List<Accelerometer>> = engine.bufferUpdate.map {
        engine.getAccelerometerHistory()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gyroscopeHistory: StateFlow<List<Gyroscope>> = engine.bufferUpdate.map {
        engine.getGyroscopeHistory()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val magnetometerHistory: StateFlow<List<Magnetometer>> = engine.bufferUpdate.map {
        engine.getMagnetometerHistory()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rotationVectorHistory: StateFlow<List<RotationVector>> = engine.bufferUpdate.map {
        val history = engine.getRotationVectorHistory()
        val tare = _tareQuaternion.value
        if (tare == null) {
            history
        } else {
            history.map { reading -> applyTare(reading, tare) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val linearAccelerationHistory: StateFlow<List<LinearAcceleration>> = engine.bufferUpdate.map {
        engine.getLinearAccelerationHistory()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gravityHistory: StateFlow<List<Gravity>> = engine.bufferUpdate.map {
        engine.getGravityHistory()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _detectedTricks = MutableStateFlow<List<TrickEvent>>(emptyList())
    val detectedTricks: StateFlow<List<TrickEvent>> = _detectedTricks.asStateFlow()

    fun tare() {
        val currentData = engine.getCurrentRotationVector()
        if (currentData != null) {
            _tareQuaternion.value = currentData
        }
    }

    /**
     * Apply tare quaternion to sensor data's rotation vector
     */
    private fun applyTare(reading: RotationVector, tareQuat: RotationVector?): RotationVector {
        if (tareQuat == null) return reading

        val x = reading.x
        val y = reading.y
        val z = reading.z
        val w = reading.scalar ?: computeScalar(x.toDouble(), y.toDouble(), z.toDouble())

        val tareX = tareQuat.x
        val tareY = tareQuat.y
        val tareZ = tareQuat.z
        val tareW = tareQuat.scalar ?: computeScalar(tareX.toDouble(), tareY.toDouble(), tareZ.toDouble())

        val tareInvX = -tareX
        val tareInvY = -tareY
        val tareInvZ = -tareZ
        val tareInvW = tareW

        val resultW = tareInvW * w - tareInvX * x - tareInvY * y - tareInvZ * z
        val resultX = tareInvW * x + tareInvX * w + tareInvY * z - tareInvZ * y
        val resultY = tareInvW * y - tareInvX * z + tareInvY * w + tareInvZ * x
        val resultZ = tareInvW * z + tareInvX * y - tareInvY * x + tareInvZ * w

        return RotationVector(
            timestampMs = reading.timestampMs,
            x = resultX,
            y = resultY,
            z = resultZ,
            scalar = resultW
        )
    }

    /**
     * Compute the scalar (w) component if not provided
     */
    private fun computeScalar(x: Double, y: Double, z: Double): Float {
        val sumSquares = x * x + y * y + z * z
        return if (sumSquares < 1.0) {
            sqrt(1.0 - sumSquares).toFloat()
        } else {
            0f
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    init {
        // Listen to trick events and play sounds for taps
        viewModelScope.launch {
            engine.trickEvents.collect { event ->
                // Add to accumulated list for UI
                _detectedTricks.value = _detectedTricks.value + event

                // Play sound if it's a tap
                if (event.type.isTap()) {
                    audioManager.playTapSound()
                }
            }
        }
    }
}
