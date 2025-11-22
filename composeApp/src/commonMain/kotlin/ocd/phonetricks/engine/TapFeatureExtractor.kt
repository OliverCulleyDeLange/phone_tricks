package ocd.phonetricks.engine

import ocd.phonetricks.data.*
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

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

        val magnitudes = windowData.map { (x, y, z) ->
            sqrt(x * x + y * y + z * z)
        }

        val jerkX = if (xValues.size > 1) diff(xValues) else listOf(0f)
        val jerkY = if (yValues.size > 1) diff(yValues) else listOf(0f)
        val jerkZ = if (zValues.size > 1) diff(zValues) else listOf(0f)
        val jerkMag = jerkX.indices.map { i ->
            sqrt(jerkX[i] * jerkX[i] + jerkY[i] * jerkY[i] + jerkZ[i] * jerkZ[i])
        }

        return listOf(
            std(xValues),
            std(yValues),
            std(zValues),

            mean(magnitudes),
            max(magnitudes),
            std(magnitudes),

            if (jerkMag.isNotEmpty()) max(jerkMag) else 0f,
            if (jerkMag.isNotEmpty()) std(jerkMag) else 0f,
            if (jerkMag.isNotEmpty()) mean(jerkMag) else 0f,

            if (magnitudes.size > 2) skewness(magnitudes) else 0f,
            if (magnitudes.size > 3) kurtosis(magnitudes) else 0f,

            if (magnitudes.isNotEmpty()) argmax(magnitudes).toFloat() / magnitudes.size else 0f,

            magnitudes.sumOf { (it * it).toDouble() }.toFloat(),

            if (magnitudes.size > 1) zeroCrossingRate(magnitudes) else 0f,

            if (magnitudes.isNotEmpty() && argmax(magnitudes) > 0)
                argmax(magnitudes).toFloat() / magnitudes.size else 0f,
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

    private fun diff(values: List<Float>): List<Float> {
        return if (values.size < 2) listOf()
        else values.zipWithNext { a, b -> b - a }
    }

    private fun argmax(values: List<Float>): Int {
        return values.indices.maxByOrNull { values[it] } ?: 0
    }

    private fun skewness(values: List<Float>): Float {
        if (values.size < 3) return 0f
        val m = mean(values)
        val s = std(values)
        if (s == 0f) return 0f

        val n = values.size
        val sum = values.sumOf { ((it - m) / s).pow(3).toDouble() }
        return (n.toFloat() / ((n - 1) * (n - 2))) * sum.toFloat()
    }

    private fun kurtosis(values: List<Float>): Float {
        if (values.size < 4) return 0f
        val m = mean(values)
        val s = std(values)
        if (s == 0f) return 0f

        val n = values.size
        val sum = values.sumOf { ((it - m) / s).pow(4).toDouble() }
        val kurt = (n * (n + 1).toFloat() / ((n - 1) * (n - 2) * (n - 3))) * sum.toFloat() -
            (3 * (n - 1).toFloat().pow(2) / ((n - 2) * (n - 3)))
        return kurt
    }

    private fun zeroCrossingRate(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val m = mean(values)
        val centered = values.map { it - m }

        var crossings = 0
        for (i in 0 until centered.size - 1) {
            if (centered[i] * centered[i + 1] < 0) {
                crossings++
            }
        }
        return crossings.toFloat() / 2f
    }
}
