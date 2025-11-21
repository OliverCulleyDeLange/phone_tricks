package ocd.phonetricks.data

data class SensorData(
    val timestamp: Long,
    val accelerometer: Accelerometer,
    val gyroscope: Gyroscope
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
