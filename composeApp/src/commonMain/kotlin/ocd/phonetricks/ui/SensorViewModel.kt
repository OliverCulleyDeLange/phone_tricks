package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ocd.phonetricks.audio.createAudioManager
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gravity
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.LinearAcceleration
import ocd.phonetricks.data.Magnetometer
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.utils.currentTimeMillis
import kotlin.math.sqrt

data class ConfidenceReading(
    val timestampMs: Long,
    val positiveConfidence: Float,
    val negativeConfidence: Float
)

class SensorViewModel(private val sensorManager: SensorManager) : ViewModel() {
    private val audioManager = createAudioManager()

    // Tare quaternion - stores the offset rotation to align the model with the device
    private val _tareQuaternion = MutableStateFlow<RotationVector?>(null)

    val rotationVectorData: StateFlow<RotationVector?> = sensorManager.rotationVectorFlow.map {
        applyTare(it, _tareQuaternion.value)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val accelerometerHistory = MutableStateFlow<List<Accelerometer>>(emptyList())
    val gyroscopeHistory = MutableStateFlow<List<Gyroscope>>(emptyList())
    val magnetometerHistory = MutableStateFlow<List<Magnetometer>>(emptyList())
    val rotationVectorHistory = MutableStateFlow<List<RotationVector>>(emptyList())
    val linearAccelerationHistory = MutableStateFlow<List<LinearAcceleration>>(emptyList())
    val gravityHistory = MutableStateFlow<List<Gravity>>(emptyList())

    private val historySize = 100

    init {
        // Subscribe to sensor data
        viewModelScope.launch {
            sensorManager.accelerometerFlow.collect { reading ->
                accelerometerHistory.value = updateHistory(accelerometerHistory.value, reading)
            }
        }

        viewModelScope.launch {
            sensorManager.gyroscopeFlow.collect { reading ->
                gyroscopeHistory.value = updateHistory(gyroscopeHistory.value, reading)
            }
        }

        viewModelScope.launch {
            sensorManager.magnetometerFlow.collect { reading ->
                magnetometerHistory.value = updateHistory(magnetometerHistory.value, reading)
            }
        }

        viewModelScope.launch {
            sensorManager.rotationVectorFlow.collect { reading ->
                val taredReading = applyTare(reading, _tareQuaternion.value)
                rotationVectorHistory.value = updateHistory(rotationVectorHistory.value, taredReading)
            }
        }

        viewModelScope.launch {
            sensorManager.linearAccelerationFlow.collect { reading ->
                linearAccelerationHistory.value = updateHistory(linearAccelerationHistory.value, reading)
            }
        }

        viewModelScope.launch {
            sensorManager.gravityFlow.collect { reading ->
                gravityHistory.value = updateHistory(gravityHistory.value, reading)
            }
        }
    }

    private fun <T> updateHistory(history: List<T>, newReading: T): List<T> {
        val newHistory = history + newReading
        return if (newHistory.size > historySize) {
            newHistory.takeLast(historySize)
        } else {
            newHistory
        }
    }

    fun tare() {
        val currentData = rotationVectorHistory.value.lastOrNull()
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
}
