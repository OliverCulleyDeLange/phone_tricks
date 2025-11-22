package ocd.phonetricks.sensor

import kotlinx.coroutines.flow.Flow
import ocd.phonetricks.data.*

interface SensorManager {
    val accelerometerFlow: Flow<AccelerometerReading>
    val gyroscopeFlow: Flow<GyroscopeReading>
    val magnetometerFlow: Flow<MagnetometerReading>?
    val rotationVectorFlow: Flow<RotationVectorReading>
    val linearAccelerationFlow: Flow<LinearAccelerationReading>?
    val gravityFlow: Flow<GravityReading>?
}

expect fun createSensorManager(): SensorManager
