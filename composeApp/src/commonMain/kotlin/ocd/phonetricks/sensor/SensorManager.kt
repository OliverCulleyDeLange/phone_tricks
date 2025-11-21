package ocd.phonetricks.sensor

import kotlinx.coroutines.flow.Flow
import ocd.phonetricks.data.SensorData

interface SensorManager {
    val sensorDataFlow: Flow<SensorData>
    fun startListening()
    fun stopListening()
}

expect fun createSensorManager(): SensorManager
