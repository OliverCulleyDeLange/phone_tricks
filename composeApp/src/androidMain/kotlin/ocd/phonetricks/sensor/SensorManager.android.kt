package ocd.phonetricks.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.SensorData

class AndroidSensorManager(context: Context) : SensorManager {
    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastAccelerometer: Accelerometer = Accelerometer(0f, 0f, 0f)
    private var lastGyroscope: Gyroscope = Gyroscope(0f, 0f, 0f)

    override val sensorDataFlow: Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            lastAccelerometer = Accelerometer(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        }

                        Sensor.TYPE_GYROSCOPE -> {
                            lastGyroscope = Gyroscope(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        }
                    }

                    trySend(
                        SensorData(
                            timestamp = System.currentTimeMillis(),
                            accelerometer = lastAccelerometer,
                            gyroscope = lastGyroscope
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        accelerometer?.let {
            androidSensorManager.registerListener(
                listener,
                it,
                AndroidSensorManager.SENSOR_DELAY_GAME
            )
        }

        gyroscope?.let {
            androidSensorManager.registerListener(
                listener,
                it,
                AndroidSensorManager.SENSOR_DELAY_GAME
            )
        }

        awaitClose {
            androidSensorManager.unregisterListener(listener)
        }
    }

    override fun startListening() {
        // Listening is managed by the flow
    }

    override fun stopListening() {
        // Listening is managed by the flow
    }
}

actual fun createSensorManager(): SensorManager {
    throw IllegalStateException("Context required for Android. Use createSensorManager(context) instead")
}

fun createSensorManager(context: Context): SensorManager {
    return AndroidSensorManager(context)
}
