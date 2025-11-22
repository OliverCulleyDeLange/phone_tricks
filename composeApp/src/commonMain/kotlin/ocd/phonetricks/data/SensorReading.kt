package ocd.phonetricks.data

import kotlinx.serialization.Serializable

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
