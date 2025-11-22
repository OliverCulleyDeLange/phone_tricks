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
import ocd.phonetricks.utils.currentTimeMillis

class TrickEngine(
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) {
    private val maxHistorySize = 1000
    private val accelerometerBuffer = RingBuffer<AccelerometerReading>(maxHistorySize)
    private val gyroscopeBuffer = RingBuffer<GyroscopeReading>(maxHistorySize)
    private val magnetometerBuffer = RingBuffer<MagnetometerReading>(maxHistorySize)
    private val rotationVectorBuffer = RingBuffer<RotationVectorReading>(maxHistorySize)
    private val linearAccelerationBuffer = RingBuffer<LinearAccelerationReading>(maxHistorySize)
    private val gravityBuffer = RingBuffer<GravityReading>(maxHistorySize)

    private val _bufferUpdate = MutableStateFlow(0L)
    val bufferUpdate: StateFlow<Long> = _bufferUpdate.asStateFlow()

    private val trickDetector = TrickDetector()
    private val tapDetector = TapDetector()

    private val trainingRecorder = TrainingDataRecorder()

    private val _trickEvents = MutableSharedFlow<TrickEvent>()
    val trickEvents: SharedFlow<TrickEvent> = _trickEvents.asSharedFlow()

    init {
        listenToSensors()
    }

    private fun notifyBufferUpdate() {
        _bufferUpdate.value = currentTimeMillis()
    }

    private fun detectTricks() {
        val newTricks = trickDetector.processSensorData(
            gyroscopeBuffer,
            rotationVectorBuffer
        )

        val newTaps = tapDetector.processSensorData(
            linearAccelerationBuffer,
            rotationVectorBuffer
        )

        val allEvents = newTricks + newTaps
        scope.launch {
            for (event in allEvents) {
                _trickEvents.emit(event)
            }
        }
    }

    fun clearHistory() {
        accelerometerBuffer.clear()
        gyroscopeBuffer.clear()
        magnetometerBuffer.clear()
        rotationVectorBuffer.clear()
        linearAccelerationBuffer.clear()
        gravityBuffer.clear()

        notifyBufferUpdate()

        trickDetector.reset()
        tapDetector.reset()
    }

    fun exportTrainingData(label: TrickType): String {
        return trainingRecorder.serializeToJson(
            accelerometerBuffer,
            gyroscopeBuffer,
            magnetometerBuffer,
            rotationVectorBuffer,
            linearAccelerationBuffer,
            gravityBuffer,
            label
        )
    }

    fun getBufferStats() = trainingRecorder.getBufferStats(
        accelerometerBuffer,
        gyroscopeBuffer,
        magnetometerBuffer,
        rotationVectorBuffer,
        linearAccelerationBuffer,
        gravityBuffer
    )

    fun getCurrentAccelerometer(): AccelerometerReading? =
        if (accelerometerBuffer.isEmpty()) null else accelerometerBuffer[accelerometerBuffer.size() - 1]

    fun getCurrentGyroscope(): GyroscopeReading? =
        if (gyroscopeBuffer.isEmpty()) null else gyroscopeBuffer[gyroscopeBuffer.size() - 1]

    fun getCurrentMagnetometer(): MagnetometerReading? =
        if (magnetometerBuffer.isEmpty()) null else magnetometerBuffer[magnetometerBuffer.size() - 1]

    fun getCurrentRotationVector(): RotationVectorReading? =
        if (rotationVectorBuffer.isEmpty()) null else rotationVectorBuffer[rotationVectorBuffer.size() - 1]

    fun getCurrentLinearAcceleration(): LinearAccelerationReading? =
        if (linearAccelerationBuffer.isEmpty()) null else linearAccelerationBuffer[linearAccelerationBuffer.size() - 1]

    fun getCurrentGravity(): GravityReading? =
        if (gravityBuffer.isEmpty()) null else gravityBuffer[gravityBuffer.size() - 1]

    fun getAccelerometerHistory(): List<AccelerometerReading> = accelerometerBuffer.toList()

    fun getGyroscopeHistory(): List<GyroscopeReading> = gyroscopeBuffer.toList()
    fun getMagnetometerHistory(): List<MagnetometerReading> = magnetometerBuffer.toList()
    fun getRotationVectorHistory(): List<RotationVectorReading> = rotationVectorBuffer.toList()
    fun getLinearAccelerationHistory(): List<LinearAccelerationReading> = linearAccelerationBuffer.toList()
    fun getGravityHistory(): List<GravityReading> = gravityBuffer.toList()

    private fun listenToSensors() {
        scope.launch {
            sensorManager.accelerometerFlow.collect { reading ->
                accelerometerBuffer.add(reading)
                notifyBufferUpdate()
                detectTricks()
            }
        }

        scope.launch {
            sensorManager.gyroscopeFlow.collect { reading ->
                gyroscopeBuffer.add(reading)
                notifyBufferUpdate()
                detectTricks()
            }
        }

        sensorManager.magnetometerFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    magnetometerBuffer.add(reading)
                    notifyBufferUpdate()
                }
            }
        }

        scope.launch {
            sensorManager.rotationVectorFlow.collect { reading ->
                rotationVectorBuffer.add(reading)
                notifyBufferUpdate()
            }
        }

        sensorManager.linearAccelerationFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    linearAccelerationBuffer.add(reading)
                    notifyBufferUpdate()
                    detectTricks()
                }
            }
        }

        sensorManager.gravityFlow?.let { flow ->
            scope.launch {
                flow.collect { reading ->
                    gravityBuffer.add(reading)
                    notifyBufferUpdate()
                }
            }
        }
    }
}
