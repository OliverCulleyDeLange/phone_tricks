package ocd.phonetricks.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.*
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.training.TrainingDataRecorder

class TrickEngine(
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) {
    private val _currentSensorData = MutableStateFlow<SensorData?>(null)
    val currentSensorData: StateFlow<SensorData?> = _currentSensorData.asStateFlow()

    private val maxHistorySize = 1000 // Arbitrary number of sensor data samples
    private val ringBuffer = RingBuffer<SensorData>(maxHistorySize)

    private val _sensorHistory = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorHistory: StateFlow<List<SensorData>> = _sensorHistory.asStateFlow()

    private val trickDetector = TrickDetector()
    private val tapDetector = TapDetector()

    private val trainingRecorder = TrainingDataRecorder()

    private val _trickEvents = MutableSharedFlow<TrickEvent>()
    val trickEvents: SharedFlow<TrickEvent> = _trickEvents.asSharedFlow()

    private var latestAccelerometer: Accelerometer = Accelerometer(0f, 0f, 0f)
    private var latestGyroscope: Gyroscope = Gyroscope(0f, 0f, 0f)
    private var latestMagnetometer: Magnetometer? = null
    private var latestRotationVector: RotationVector = RotationVector(0f, 0f, 0f, null)
    private var latestLinearAcceleration: LinearAcceleration? = null
    private var latestGravity: Gravity? = null

    init {
        scope.launch {
            sensorManager.accelerometerFlow.collect { reading ->
                latestAccelerometer = reading.data
                updateSensorData(reading.timestampMs)
            }
        }

        scope.launch {
            sensorManager.gyroscopeFlow.collect { reading ->
                latestGyroscope = reading.data
                updateSensorData(reading.timestampMs)
            }
        }

        sensorManager.magnetometerFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    latestMagnetometer = reading.data
                    updateSensorData(reading.timestampMs)
                }
            }
        }

        scope.launch {
            sensorManager.rotationVectorFlow.collect { reading ->
                latestRotationVector = reading.data
                updateSensorData(reading.timestampMs)
            }
        }

        sensorManager.linearAccelerationFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    latestLinearAcceleration = reading.data
                    updateSensorData(reading.timestampMs)
                }
            }
        }

        sensorManager.gravityFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    latestGravity = reading.data
                    updateSensorData(reading.timestampMs)
                }
            }
        }
    }

    private fun updateSensorData(timestampMs: Long) {
        val data = SensorData(
            timestampMs = timestampMs,
            accelerometer = latestAccelerometer,
            gyroscope = latestGyroscope,
            magnetometer = latestMagnetometer,
            rotationVector = latestRotationVector,
            linearAcceleration = latestLinearAcceleration,
            gravity = latestGravity
        )

        _currentSensorData.value = data

        // Add to ring buffer (automatically handles overflow)
        ringBuffer.add(data)

        // Update the state flow with the current buffer contents
        _sensorHistory.value = ringBuffer.toList()

        // Detect tricks - pass the ring buffer to detectors
        val newTricks = trickDetector.processSensorData(ringBuffer)

        // Detect taps - pass the ring buffer to detectors
        val newTaps = tapDetector.processSensorData(ringBuffer)

        // Combine and emit all detected events
        val allEvents = newTricks + newTaps
        scope.launch {
            for (event in allEvents) {
                _trickEvents.emit(event)
            }
        }
    }

    fun clearHistory() {
        ringBuffer.clear()
        _sensorHistory.value = emptyList()
        trickDetector.reset()
        tapDetector.reset()
    }

    /**
     * Export the current sensor buffer as a JSON string for training purposes.
     *
     * @param label The label to assign to this training sample
     * @return JSON string representation of the training data
     */
    fun exportTrainingData(label: TrickType): String {
        return trainingRecorder.serializeToJson(ringBuffer, label)
    }

    /**
     * Export the current sensor buffer as a JSON string with a custom label.
     *
     * @param labelString Custom label string
     * @return JSON string representation of the training data
     */
    fun exportTrainingData(labelString: String): String {
        return trainingRecorder.serializeToJson(ringBuffer, labelString)
    }

    /**
     * Get statistics about the current sensor buffer.
     */
    fun getBufferStats() = trainingRecorder.getBufferStats(ringBuffer)

    /**
     * Get direct access to the ring buffer for advanced use cases.
     */
    fun getSensorBuffer(): RingBuffer<SensorData> = ringBuffer
}
