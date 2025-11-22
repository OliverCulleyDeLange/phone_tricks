package ocd.phonetricks.data

data class SensorData(
    val timestamp: Long,
    val accelerometer: Accelerometer,
    val gyroscope: Gyroscope,
    val magnetometer: Magnetometer?,
    val rotationVector: RotationVector,
    val linearAcceleration: LinearAcceleration?,
    val gravity: Gravity?
)

data class Accelerometer(
    val x: Float,
    val y: Float,
    val z: Float
)

data class Gyroscope(
    val x: Float,
    val y: Float,
    val z: Float
)

data class Magnetometer(
    val x: Float,
    val y: Float,
    val z: Float
)

data class RotationVector(
    val x: Float,
    val y: Float,
    val z: Float,
    val scalar: Float? = null // Some platforms provide a 4th component
)

data class LinearAcceleration(
    val x: Float,
    val y: Float,
    val z: Float
)

data class Gravity(
    val x: Float,
    val y: Float,
    val z: Float
)
