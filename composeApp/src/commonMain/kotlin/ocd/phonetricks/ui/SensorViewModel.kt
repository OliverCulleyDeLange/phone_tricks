package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gravity
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.LinearAcceleration
import ocd.phonetricks.data.Magnetometer
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.sensor.applyTare

data class ConfidenceReading(
    val timestampMs: Long,
    val positiveConfidence: Float,
    val negativeConfidence: Float
)

class SensorViewModel(private val sensorManager: SensorManager) : ViewModel() {

    // Tare quaternion - stores the offset rotation to align the model with the device
    private val _tareQuaternion = MutableStateFlow<RotationVector?>(null)

    private val _rawRotationVector = MutableStateFlow<RotationVector?>(null)

    private val _rotationVectorData = MutableStateFlow<RotationVector?>(null)
    val rotationVectorData: StateFlow<RotationVector?> = _rotationVectorData.asStateFlow()

    val accelerometerHistory = MutableStateFlow<List<Accelerometer>>(emptyList())
    val gyroscopeHistory = MutableStateFlow<List<Gyroscope>>(emptyList())
    val magnetometerHistory = MutableStateFlow<List<Magnetometer>>(emptyList())
    val rotationVectorHistory = MutableStateFlow<List<RotationVector>>(emptyList())
    val quaternionHistory: StateFlow<List<RotationVector>> = rotationVectorHistory
    val linearAccelerationHistory = MutableStateFlow<List<LinearAcceleration>>(emptyList())
    val gravityHistory = MutableStateFlow<List<Gravity>>(emptyList())

    private val historySize = 100

    init {
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

        // Single subscription to the rotation flow: feed both the raw stream
        // (used as the tare reference) and the post-tare stream consumed by
        // the UI.
        viewModelScope.launch {
            sensorManager.rotationVectorFlow.collect { reading ->
                _rawRotationVector.value = reading
                val tared = applyTare(reading, _tareQuaternion.value)
                _rotationVectorData.value = tared
                rotationVectorHistory.value = updateHistory(rotationVectorHistory.value, tared)
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
        val currentData = _rawRotationVector.value
        if (currentData != null) {
            _tareQuaternion.value = currentData
        }
    }
}
