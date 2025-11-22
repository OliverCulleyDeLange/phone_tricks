package ocd.phonetricks.data

import kotlinx.serialization.Serializable

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
    val scalar: Float? = null
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

@Serializable
data class AccelerometerReading(
    val timestampMs: Long,
    val data: Accelerometer
)

@Serializable
data class GyroscopeReading(
    val timestampMs: Long,
    val data: Gyroscope
)

@Serializable
data class MagnetometerReading(
    val timestampMs: Long,
    val data: Magnetometer
)

@Serializable
data class RotationVectorReading(
    val timestampMs: Long,
    val data: RotationVector
)

@Serializable
data class LinearAccelerationReading(
    val timestampMs: Long,
    val data: LinearAcceleration
)

@Serializable
data class GravityReading(
    val timestampMs: Long,
    val data: Gravity
)
