package ocd.phonetricks.training

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.data.TrickType
import ocd.phonetricks.engine.RingBuffer
import ocd.phonetricks.utils.currentTimeMillis

/**
 * Represents a single training sample with sensor data and its label.
 */
@Serializable
data class TrainingSample(
    val label: String,
    val sensorData: List<SensorData>,
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
     * Serialize a ring buffer of sensor data to a JSON string with the given label.
     *
     * @param sensorBuffer The ring buffer containing sensor data
     * @param label The tap type label for this data (e.g., "TAP_FRONT", "TAP_BACK")
     * @return JSON string representation of the training sample
     */
    fun serializeToJson(sensorBuffer: RingBuffer<SensorData>, label: TrickType): String {
        val sample = TrainingSample(
            label = label.name,
            sensorData = sensorBuffer.toList(),
            recordingTimestampMs = currentTimeMillis()
        )
        return json.encodeToString(sample)
    }

    /**
     * Serialize a ring buffer of sensor data to a JSON string with a custom string label.
     *
     * @param sensorBuffer The ring buffer containing sensor data
     * @param labelString Custom label string
     * @return JSON string representation of the training sample
     */
    fun serializeToJson(sensorBuffer: RingBuffer<SensorData>, labelString: String): String {
        val sample = TrainingSample(
            label = labelString,
            sensorData = sensorBuffer.toList(),
            recordingTimestampMs = currentTimeMillis()
        )
        return json.encodeToString(sample)
    }

    /**
     * Get statistics about the current buffer for display purposes.
     */
    fun getBufferStats(sensorBuffer: RingBuffer<SensorData>): BufferStats {
        val data = sensorBuffer.toList()
        if (data.isEmpty()) {
            return BufferStats(0, 0, 0f)
        }

        val duration = if (data.size > 1) {
            data.last().timestampMs - data.first().timestampMs
        } else {
            0L
        }

        val sampleRate = if (duration > 0) {
            (data.size - 1) * 1000f / duration
        } else {
            0f
        }

        return BufferStats(
            sampleCount = data.size,
            durationMs = duration,
            sampleRate = sampleRate
        )
    }
}

/**
 * Statistics about a sensor data buffer.
 */
data class BufferStats(
    val sampleCount: Int,
    val durationMs: Long,
    val sampleRate: Float
)
