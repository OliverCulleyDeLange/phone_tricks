package ocd.phonetricks.sensor

import kotlinx.coroutines.flow.Flow
import ocd.phonetricks.data.*

interface SensorManager {
    val accelerometerFlow: Flow<Accelerometer>
    val gyroscopeFlow: Flow<Gyroscope>
    val magnetometerFlow: Flow<Magnetometer>
    val rotationVectorFlow: Flow<RotationVector>
    val linearAccelerationFlow: Flow<LinearAcceleration>
    val gravityFlow: Flow<Gravity>
}

expect fun createSensorManager(): SensorManager
