package ocd.phonetricks.data

import kotlinx.serialization.Serializable

@Serializable
data class Accelerometer(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Gyroscope(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Magnetometer(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class RotationVector(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val scalar: Float? = null
)

@Serializable
data class LinearAcceleration(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Gravity(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)
