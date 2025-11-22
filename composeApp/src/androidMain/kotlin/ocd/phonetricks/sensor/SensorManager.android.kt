package ocd.phonetricks.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ocd.phonetricks.data.*

private const val SENSOR_DELAY = AndroidSensorManager.SENSOR_DELAY_GAME

class AndroidSensorManager(context: Context) : SensorManager {
    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    private val accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector = androidSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val linearAcceleration = androidSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravity = androidSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    override val accelerometerFlow: Flow<AccelerometerReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(
                        AccelerometerReading(
                            timestampMs = System.currentTimeMillis(),
                            data = Accelerometer(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)
        }

        awaitClose {
            androidSensorManager.unregisterListener(listener)
        }
    }

    override val gyroscopeFlow: Flow<GyroscopeReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(
                        GyroscopeReading(
                            timestampMs = System.currentTimeMillis(),
                            data = Gyroscope(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2]
                            )
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        gyroscope?.let {
            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)
        }

        awaitClose {
            androidSensorManager.unregisterListener(listener)
        }
    }

    override val magnetometerFlow: Flow<MagnetometerReading>? = magnetometer?.let {
        callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        trySend(
                            MagnetometerReading(
                                timestampMs = System.currentTimeMillis(),
                                data = Magnetometer(
                                    x = it.values[0],
                                    y = it.values[1],
                                    z = it.values[2]
                                )
                            )
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)

            awaitClose {
                androidSensorManager.unregisterListener(listener)
            }
        }
    }

    override val rotationVectorFlow: Flow<RotationVectorReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(
                        RotationVectorReading(
                            timestampMs = System.currentTimeMillis(),
                            data = RotationVector(
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2],
                                scalar = if (it.values.size > 3) it.values[3] else null
                            )
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationVector?.let {
            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)
        }

        awaitClose {
            androidSensorManager.unregisterListener(listener)
        }
    }

    override val linearAccelerationFlow: Flow<LinearAccelerationReading>? = linearAcceleration?.let {
        callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        trySend(
                            LinearAccelerationReading(
                                timestampMs = System.currentTimeMillis(),
                                data = LinearAcceleration(
                                    x = it.values[0],
                                    y = it.values[1],
                                    z = it.values[2]
                                )
                            )
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)

            awaitClose {
                androidSensorManager.unregisterListener(listener)
            }
        }
    }

    override val gravityFlow: Flow<GravityReading>? = gravity?.let {
        callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        trySend(
                            GravityReading(
                                timestampMs = System.currentTimeMillis(),
                                data = Gravity(
                                    x = it.values[0],
                                    y = it.values[1],
                                    z = it.values[2]
                                )
                            )
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            androidSensorManager.registerListener(listener, it, SENSOR_DELAY)

            awaitClose {
                androidSensorManager.unregisterListener(listener)
            }
        }
    }
}
actual fun createSensorManager(): SensorManager {
    throw IllegalStateException("Context required for Android. Use createSensorManager(context) instead")
}

fun createSensorManager(context: Context): SensorManager {
    return AndroidSensorManager(context)
}
