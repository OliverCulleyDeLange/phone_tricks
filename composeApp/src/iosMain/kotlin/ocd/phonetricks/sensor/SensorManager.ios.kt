package ocd.phonetricks.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ocd.phonetricks.data.*
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
class IOSSensorManager : SensorManager {
    private val motionManager = CMMotionManager()

    override val accelerometerFlow: Flow<AccelerometerReading> = callbackFlow {
        val available = motionManager.accelerometerAvailable && 
                       motionManager.gyroAvailable
        
        if (available) {
            // Set update intervals for maximum rate
            motionManager.deviceMotionUpdateInterval = 1.0 / 100.0 // 100Hz
            
            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
            
            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    var accel = Accelerometer(0f, 0f, 0f)

                    deviceMotion.userAcceleration.useContents {
                        val linearX = x.toFloat()
                        val linearY = y.toFloat()
                        val linearZ = z.toFloat()

                        deviceMotion.gravity.useContents {
                            accel = Accelerometer(
                                x = linearX + x.toFloat(),
                                y = linearY + y.toFloat(),
                                z = linearZ + z.toFloat()
                            )
                        }
                    }

                    trySend(
                        AccelerometerReading(
                            timestampMs = (time(null) * 1000),
                            data = accel
                        )
                    )
                }
            }
        }

        awaitClose {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    override val gyroscopeFlow: Flow<GyroscopeReading> = callbackFlow {
        val available = motionManager.accelerometerAvailable &&
            motionManager.gyroAvailable

        if (available) {
            // Set update intervals for maximum rate
            motionManager.deviceMotionUpdateInterval = 1.0 / 100.0 // 100Hz

            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue

            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    deviceMotion.rotationRate.useContents {
                        trySend(
                            GyroscopeReading(
                                timestampMs = (time(null) * 1000),
                                data = Gyroscope(
                                    x = x.toFloat(),
                                    y = y.toFloat(),
                                    z = z.toFloat()
                                )
                            )
                        )
                    }
                }
            }
        }

        awaitClose {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    override val magnetometerFlow: Flow<MagnetometerReading>? = null

    override val rotationVectorFlow: Flow<RotationVectorReading> = callbackFlow {
        val available = motionManager.accelerometerAvailable &&
            motionManager.gyroAvailable

        if (available) {
            // Set update intervals for maximum rate
            motionManager.deviceMotionUpdateInterval = 1.0 / 100.0 // 100Hz

            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue

            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    deviceMotion.attitude.quaternion.useContents {
                        trySend(
                            RotationVectorReading(
                                timestampMs = (time(null) * 1000),
                                data = RotationVector(
                                    x = x.toFloat(),
                                    y = y.toFloat(),
                                    z = z.toFloat(),
                                    scalar = w.toFloat()
                                )
                            )
                        )
                    }
                }
            }
        }

        awaitClose {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    override val linearAccelerationFlow: Flow<LinearAccelerationReading> = callbackFlow {
        val available = motionManager.accelerometerAvailable &&
            motionManager.gyroAvailable

        if (available) {
            // Set update intervals for maximum rate
            motionManager.deviceMotionUpdateInterval = 1.0 / 100.0 // 100Hz

            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue

            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    deviceMotion.userAcceleration.useContents {
                        trySend(
                            LinearAccelerationReading(
                            timestampMs = (time(null) * 1000),
                            data = LinearAcceleration(
                                x = x.toFloat(),
                                y = y.toFloat(),
                                z = z.toFloat()
                            )
                        )
                        )
                    }
                }
            }
        }
        
        awaitClose {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    override val gravityFlow: Flow<GravityReading> = callbackFlow {
        val available = motionManager.accelerometerAvailable &&
            motionManager.gyroAvailable

        if (available) {
            // Set update intervals for maximum rate
            motionManager.deviceMotionUpdateInterval = 1.0 / 100.0 // 100Hz

            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue

            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    deviceMotion.gravity.useContents {
                        trySend(
                            GravityReading(
                                timestampMs = (time(null) * 1000),
                                data = Gravity(
                                    x = x.toFloat(),
                                    y = y.toFloat(),
                                    z = z.toFloat()
                                )
                            )
                        )
                    }
                }
            }
        }

        awaitClose {
            motionManager.stopDeviceMotionUpdates()
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
    return IOSSensorManager()
}
