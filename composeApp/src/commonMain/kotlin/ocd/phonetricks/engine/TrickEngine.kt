package ocd.phonetricks.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.*
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.training.Labels
import ocd.phonetricks.training.TrainingDataRecorder

class TrickEngine(
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) {
    private val maxHistorySize = 1000
    private val accelerometerBuffer = RingBuffer<Accelerometer>(maxHistorySize)
    private val gyroscopeBuffer = RingBuffer<Gyroscope>(maxHistorySize)
    private val magnetometerBuffer = RingBuffer<Magnetometer>(maxHistorySize)
    private val rotationVectorBuffer = RingBuffer<RotationVector>(maxHistorySize)
    private val linearAccelerationBuffer = RingBuffer<LinearAcceleration>(maxHistorySize)
    private val gravityBuffer = RingBuffer<Gravity>(maxHistorySize)

    private val trickDetector = TrickDetector()
    private val tapDetector = MLTapDetector()

    private val trainingRecorder = TrainingDataRecorder()

    private val _trickEvents = MutableSharedFlow<TrickEvent>()
    val trickEvents: SharedFlow<TrickEvent> = _trickEvents.asSharedFlow()

    private val _inferenceResults = MutableSharedFlow<InferenceResult>()
    val inferenceResults: SharedFlow<InferenceResult> = _inferenceResults.asSharedFlow()

    init {
        listenToSensors()
        scope.launch {
            tapDetector.loadModel()
        }
    }

    private fun detectEvents() {
        val newTricks = trickDetector.processSensorData(
            gyroscopeBuffer,
            rotationVectorBuffer
        )

        val detectionResult = tapDetector.processSensorData(
            accelerometerBuffer,
            gyroscopeBuffer,
            linearAccelerationBuffer,
            magnetometerBuffer,
            gravityBuffer,
            rotationVectorBuffer
        )

        val allEvents = newTricks + detectionResult.trickEvents
        scope.launch {
            for (event in allEvents) {
                _trickEvents.emit(event)
            }

            detectionResult.inferenceResult?.let { result ->
                _inferenceResults.emit(result)
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

    fun exportTrainingDataWithTimestamps(
        labels: Labels,
        tapTimestamps: List<Long>,
        recordingStartMs: Long,
        recordingEndMs: Long
    ): String {
        return trainingRecorder.serializeWithTimestamps(
            accelerometerBuffer,
            gyroscopeBuffer,
            magnetometerBuffer,
            rotationVectorBuffer,
            linearAccelerationBuffer,
            gravityBuffer,
            labels,
            tapTimestamps,
            recordingStartMs,
            recordingEndMs
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

    fun getCurrentRotationVector(): RotationVector? =
        if (rotationVectorBuffer.isEmpty()) null else rotationVectorBuffer[rotationVectorBuffer.size() - 1]

    fun getAccelerometerHistory(): List<Accelerometer> = accelerometerBuffer.toList()

    fun getGyroscopeHistory(): List<Gyroscope> = gyroscopeBuffer.toList()
    fun getMagnetometerHistory(): List<Magnetometer> = magnetometerBuffer.toList()
    fun getRotationVectorHistory(): List<RotationVector> = rotationVectorBuffer.toList()
    fun getLinearAccelerationHistory(): List<LinearAcceleration> = linearAccelerationBuffer.toList()
    fun getGravityHistory(): List<Gravity> = gravityBuffer.toList()

    private fun listenToSensors() {
        scope.launch {
            sensorManager.accelerometerFlow.collect { reading ->
                accelerometerBuffer.add(reading)
                detectEvents()
            }
        }

        scope.launch {
            sensorManager.gyroscopeFlow.collect { reading ->
                gyroscopeBuffer.add(reading)
                detectEvents()
            }
        }

        scope.launch {
            sensorManager.magnetometerFlow.collect { reading ->
                magnetometerBuffer.add(reading)
            }
        }

        scope.launch {
            sensorManager.rotationVectorFlow.collect { reading ->
                rotationVectorBuffer.add(reading)
            }
        }

        scope.launch {
            sensorManager.linearAccelerationFlow.collect { reading ->
                linearAccelerationBuffer.add(reading)
                detectEvents()
            }
        }

        scope.launch {
            sensorManager.gravityFlow.collect { reading ->
                gravityBuffer.add(reading)
            }
        }
    }
}
