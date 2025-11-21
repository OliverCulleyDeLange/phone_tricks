package ocd.phonetricks.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gravity
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.LinearAcceleration
import ocd.phonetricks.data.Magnetometer
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.data.SensorData

class AndroidSensorManager(context: Context) : SensorManager {
    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    // Core sensors
    private val accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Additional sensors (may not be available on all devices)
    private val magnetometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector = androidSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val linearAcceleration = androidSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravity = androidSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private var lastAccelerometer: Accelerometer = Accelerometer(0f, 0f, 0f)
    private var lastGyroscope: Gyroscope = Gyroscope(0f, 0f, 0f)
    private var lastMagnetometer: Magnetometer? = null
    private var lastRotationVector: RotationVector? = null
    private var lastLinearAcceleration: LinearAcceleration? = null
    private var lastGravity: Gravity? = null

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

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            lastMagnetometer = Magnetometer(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        }

                        Sensor.TYPE_ROTATION_VECTOR -> {
                            lastRotationVector = RotationVector(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2],
                                scalar = if (it.values.size > 3) it.values[3] else null
                            )
                        }

                        Sensor.TYPE_LINEAR_ACCELERATION -> {
                            lastLinearAcceleration = LinearAcceleration(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        }

                        Sensor.TYPE_GRAVITY -> {
                            lastGravity = Gravity(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        }
                    }

                    // Update latest sensor values; emissions are throttled by the emitter coroutine below
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        // Register all available sensors
        val delay = AndroidSensorManager.SENSOR_DELAY_GAME

        accelerometer?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        gyroscope?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        magnetometer?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        rotationVector?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        linearAcceleration?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        gravity?.let {
            androidSensorManager.registerListener(listener, it, delay)
        }

        // Launch a coroutine that emits the latest aggregated SensorData at ~60Hz
        val emitJob = launch {
            while (isActive) {
                try {
                    kotlinx.coroutines.delay(16L) // ~60Hz
                } catch (e: Throwable) {
                    break
                }

                trySend(
                    SensorData(
                        timestamp = System.currentTimeMillis(),
                        accelerometer = lastAccelerometer,
                        gyroscope = lastGyroscope,
                        magnetometer = lastMagnetometer,
                        rotationVector = lastRotationVector,
                        linearAcceleration = lastLinearAcceleration,
                        gravity = lastGravity
                    )
                )
            }
        }

        awaitClose {
            emitJob.cancel()
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
