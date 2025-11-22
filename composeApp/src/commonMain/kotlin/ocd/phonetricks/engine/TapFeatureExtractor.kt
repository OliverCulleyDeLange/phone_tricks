package ocd.phonetricks.engine

import ocd.phonetricks.data.*
import kotlin.math.sqrt

class TapFeatureExtractor {

    fun extractFeaturesFromTap(
        accelerometerBuffer: RingBuffer<Accelerometer>,
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        magnetometerBuffer: RingBuffer<Magnetometer>,
        gravityBuffer: RingBuffer<Gravity>,
        rotationVectorBuffer: RingBuffer<RotationVector>,
        tapTimestamp: Long,
        windowMs: Long = 100
    ): FloatArray {
        val features = mutableListOf<Float>()

        features.addAll(extractSensorFeatures(accelerometerBuffer, tapTimestamp, windowMs))
        features.addAll(extractSensorFeatures(gyroscopeBuffer, tapTimestamp, windowMs))
        features.addAll(extractSensorFeatures(linearAccelerationBuffer, tapTimestamp, windowMs))
        features.addAll(extractSensorFeatures(magnetometerBuffer, tapTimestamp, windowMs))
        features.addAll(extractSensorFeatures(gravityBuffer, tapTimestamp, windowMs))
        features.addAll(extractSensorFeatures(rotationVectorBuffer, tapTimestamp, windowMs))

        return features.toFloatArray()
    }

    private fun <T> extractSensorFeatures(
        buffer: RingBuffer<T>,
        tapTimestamp: Long,
        windowMs: Long
    ): List<Float> {
        if (buffer.isEmpty()) {
            return List(15) { 0f }
        }

        val windowData = mutableListOf<Triple<Float, Float, Float>>()

        for (i in 0 until buffer.size()) {
            val reading = buffer[i]
            val timestamp = getTimestamp(reading)

            if (kotlin.math.abs(timestamp - tapTimestamp) <= windowMs) {
                val (x, y, z) = getXYZ(reading)
                windowData.add(Triple(x, y, z))
            }
        }

        if (windowData.isEmpty()) {
            return List(15) { 0f }
        }

        val xValues = windowData.map { it.first }
        val yValues = windowData.map { it.second }
        val zValues = windowData.map { it.third }

        return listOf(
            mean(xValues), std(xValues), max(xValues), min(xValues), ptp(xValues),
            mean(yValues), std(yValues), max(yValues), min(yValues), ptp(yValues),
            mean(zValues), std(zValues), max(zValues), min(zValues), ptp(zValues)
        )
    }

    private fun <T> getTimestamp(reading: T): Long {
        return when (reading) {
            is Accelerometer -> reading.timestampMs
            is Gyroscope -> reading.timestampMs
            is LinearAcceleration -> reading.timestampMs
            is Magnetometer -> reading.timestampMs
            is Gravity -> reading.timestampMs
            is RotationVector -> reading.timestampMs
            else -> 0L
        }
    }

    private fun <T> getXYZ(reading: T): Triple<Float, Float, Float> {
        return when (reading) {
            is Accelerometer -> Triple(reading.x, reading.y, reading.z)
            is Gyroscope -> Triple(reading.x, reading.y, reading.z)
            is LinearAcceleration -> Triple(reading.x, reading.y, reading.z)
            is Magnetometer -> Triple(reading.x, reading.y, reading.z)
            is Gravity -> Triple(reading.x, reading.y, reading.z)
            is RotationVector -> Triple(reading.x, reading.y, reading.z)
            else -> Triple(0f, 0f, 0f)
        }
    }

    private fun mean(values: List<Float>): Float {
        return if (values.isEmpty()) 0f else values.sum() / values.size
    }

    private fun std(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val m = mean(values)
        val variance = values.map { (it - m) * (it - m) }.sum() / values.size
        return sqrt(variance)
    }

    private fun max(values: List<Float>): Float {
        return values.maxOrNull() ?: 0f
    }

    private fun min(values: List<Float>): Float {
        return values.minOrNull() ?: 0f
    }

    private fun ptp(values: List<Float>): Float {
        return max(values) - min(values)
    }
}
