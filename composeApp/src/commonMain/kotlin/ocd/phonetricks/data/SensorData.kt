package ocd.phonetricks.data

import kotlinx.serialization.Serializable

@Serializable
data class SensorData(
    val timestampMs: Long,
    val accelerometer: Accelerometer,
    val gyroscope: Gyroscope,
    val magnetometer: Magnetometer?,
    val rotationVector: RotationVector,
    val linearAcceleration: LinearAcceleration?,
    val gravity: Gravity?
)

@Serializable
data class Accelerometer(
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Gyroscope(
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Magnetometer(
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class RotationVector(
    val x: Float,
    val y: Float,
    val z: Float,
    val scalar: Float? = null // Some platforms provide a 4th component
)

@Serializable
data class LinearAcceleration(
    val x: Float,
    val y: Float,
    val z: Float
)

@Serializable
data class Gravity(
    val x: Float,
    val y: Float,
    val z: Float
)
