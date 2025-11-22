package ocd.phonetricks.training

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ocd.phonetricks.data.*
import ocd.phonetricks.engine.RingBuffer
import ocd.phonetricks.utils.currentTimeMillis

/**
 * Represents a single training sample with sensor data and its label.
 */
@Serializable
data class TrainingSample(
    val label: String,
    val accelerometerData: List<Accelerometer>,
    val gyroscopeData: List<Gyroscope>,
    val magnetometerData: List<Magnetometer>,
    val rotationVectorData: List<RotationVector>,
    val linearAccelerationData: List<LinearAcceleration>,
    val gravityData: List<Gravity>,
    val recordingTimestampMs: Long,
    val sampleRate: String = ""
)

/**
 * Handles recording and serialization of sensor data for training ML models.
 */
class TrainingDataRecorder {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Serialize ring buffers of sensor data to a JSON string with the given label.
     *
     * @param accelerometerBuffer The ring buffer containing accelerometer data
     * @param gyroscopeBuffer The ring buffer containing gyroscope data
     * @param magnetometerBuffer The ring buffer containing magnetometer data
     * @param rotationVectorBuffer The ring buffer containing rotation vector data
     * @param linearAccelerationBuffer The ring buffer containing linear acceleration data
     * @param gravityBuffer The ring buffer containing gravity data
     * @param label The tap type label for this data (e.g., "TAP_FRONT", "TAP_BACK")
     * @return JSON string representation of the training sample
     */
    fun serializeToJson(
        accelerometerBuffer: RingBuffer<Accelerometer>,
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        magnetometerBuffer: RingBuffer<Magnetometer>,
        rotationVectorBuffer: RingBuffer<RotationVector>,
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        gravityBuffer: RingBuffer<Gravity>,
        label: TrickType
    ): String {
        val sample = TrainingSample(
            label = label.name,
            accelerometerData = accelerometerBuffer.toList(),
            gyroscopeData = gyroscopeBuffer.toList(),
            magnetometerData = magnetometerBuffer.toList(),
            rotationVectorData = rotationVectorBuffer.toList(),
            linearAccelerationData = linearAccelerationBuffer.toList(),
            gravityData = gravityBuffer.toList(),
            recordingTimestampMs = currentTimeMillis()
        )
        return json.encodeToString(sample)
    }

    fun serializeToJson(
        accelerometerBuffer: RingBuffer<Accelerometer>,
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        magnetometerBuffer: RingBuffer<Magnetometer>,
        rotationVectorBuffer: RingBuffer<RotationVector>,
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        gravityBuffer: RingBuffer<Gravity>,
        labelString: String
    ): String {
        val sample = TrainingSample(
            label = labelString,
            accelerometerData = accelerometerBuffer.toList(),
            gyroscopeData = gyroscopeBuffer.toList(),
            magnetometerData = magnetometerBuffer.toList(),
            rotationVectorData = rotationVectorBuffer.toList(),
            linearAccelerationData = linearAccelerationBuffer.toList(),
            gravityData = gravityBuffer.toList(),
            recordingTimestampMs = currentTimeMillis()
        )
        return json.encodeToString(sample)
    }

    /**
     * Get statistics about the current buffers for display purposes.
     */
    fun getBufferStats(
        accelerometerBuffer: RingBuffer<Accelerometer>,
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        magnetometerBuffer: RingBuffer<Magnetometer>,
        rotationVectorBuffer: RingBuffer<RotationVector>,
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        gravityBuffer: RingBuffer<Gravity>
    ): BufferStats {
        return BufferStats(
            accelerometer = getSensorStats(accelerometerBuffer),
            gyroscope = getSensorStats(gyroscopeBuffer),
            magnetometer = getSensorStats(magnetometerBuffer),
            rotationVector = getSensorStats(rotationVectorBuffer),
            linearAcceleration = getSensorStats(linearAccelerationBuffer),
            gravity = getSensorStats(gravityBuffer)
        )
    }

    private fun <T : Any> getSensorStats(buffer: RingBuffer<T>): SensorStats {
        val data = buffer.toList()

        if (data.isEmpty()) {
            return SensorStats(0, 0, 0f)
        }

        val timestamps = data.map { reading ->
            when (reading) {
                is Accelerometer -> reading.timestampMs
                is Gyroscope -> reading.timestampMs
                is Magnetometer -> reading.timestampMs
                is RotationVector -> reading.timestampMs
                is LinearAcceleration -> reading.timestampMs
                is Gravity -> reading.timestampMs
                else -> 0L
            }
        }

        val duration = if (timestamps.size > 1) {
            timestamps.last() - timestamps.first()
        } else {
            0L
        }

        val sampleRate = if (duration > 0) {
            (timestamps.size - 1) * 1000f / duration
        } else {
            0f
        }

        return SensorStats(
            sampleCount = timestamps.size,
            durationMs = duration,
            sampleRate = sampleRate
        )
    }
}

data class SensorStats(
    val sampleCount: Int,
    val durationMs: Long,
    val sampleRate: Float
)

data class BufferStats(
    val accelerometer: SensorStats,
    val gyroscope: SensorStats,
    val magnetometer: SensorStats,
    val rotationVector: SensorStats,
    val linearAcceleration: SensorStats,
    val gravity: SensorStats
)
