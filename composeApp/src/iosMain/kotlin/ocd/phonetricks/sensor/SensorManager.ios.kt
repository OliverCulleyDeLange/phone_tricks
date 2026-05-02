package ocd.phonetricks.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import ocd.phonetricks.data.*
import ocd.phonetricks.utils.currentTimeMillis
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
class IOSSensorManager : SensorManager {
    private val motionManager = CMMotionManager()

    override val accelerometerFlow: Flow<Accelerometer> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { deviceMotion ->
                val now = currentTimeMillis()
                deviceMotion.userAcceleration.useContents {
                    val linearX = x.toFloat()
                    val linearY = y.toFloat()
                    val linearZ = z.toFloat()
                    deviceMotion.gravity.useContents {
                        trySend(
                            Accelerometer(
                                timestampMs = now,
                                x = linearX + x.toFloat(),
                                y = linearY + y.toFloat(),
                                z = linearZ + z.toFloat(),
                            )
                        )
                    }
                }
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }

    override val gyroscopeFlow: Flow<Gyroscope> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { deviceMotion ->
                val now = currentTimeMillis()
                deviceMotion.rotationRate.useContents {
                    trySend(
                        Gyroscope(
                            timestampMs = now,
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat(),
                        )
                    )
                }
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }

    // CoreMotion does not expose a calibrated magnetic field via deviceMotion in a way that
    // matches the Android Sensor.TYPE_MAGNETIC_FIELD shape, and the rest of the app does not
    // consume this flow yet. Surface an empty flow rather than crash.
    override val magnetometerFlow: Flow<Magnetometer> = emptyFlow()

    override val rotationVectorFlow: Flow<RotationVector> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { deviceMotion ->
                val now = currentTimeMillis()
                deviceMotion.attitude.quaternion.useContents {
                    trySend(
                        RotationVector(
                            timestampMs = now,
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat(),
                            scalar = w.toFloat(),
                        )
                    )
                }
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }

    override val linearAccelerationFlow: Flow<LinearAcceleration> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { deviceMotion ->
                val now = currentTimeMillis()
                deviceMotion.userAcceleration.useContents {
                    trySend(
                        LinearAcceleration(
                            timestampMs = now,
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat(),
                        )
                    )
                }
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }

    override val gravityFlow: Flow<Gravity> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { deviceMotion ->
                val now = currentTimeMillis()
                deviceMotion.gravity.useContents {
                    trySend(
                        Gravity(
                            timestampMs = now,
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat(),
                        )
                    )
                }
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }
}

actual fun createSensorManager(): SensorManager = IOSSensorManager()
