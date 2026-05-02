package ocd.phonetricks.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import ocd.phonetricks.data.*
import ocd.phonetricks.utils.currentTimeMillis
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * Snapshot of one CMDeviceMotion update — each `useContents` block has to be
 * resolved synchronously inside the CoreMotion callback, so we extract every
 * sensor's three (or four) Floats up front and ship a pure value class to
 * downstream flows.
 */
private data class DeviceMotionSnapshot(
    val timestampMs: Long,
    val userAccelX: Float, val userAccelY: Float, val userAccelZ: Float,
    val gravityX: Float, val gravityY: Float, val gravityZ: Float,
    val rotationRateX: Float, val rotationRateY: Float, val rotationRateZ: Float,
    val quatX: Float, val quatY: Float, val quatZ: Float, val quatW: Float,
)

@OptIn(ExperimentalForeignApi::class)
class IOSSensorManager : SensorManager {
    private val motionManager = CMMotionManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * CMMotionManager only supports a single device-motion handler at a
     * time. Previously each per-sensor flow installed its own handler and
     * the last one in won — collecting more than one flow at once silently
     * dropped data for the others. Share a single callbackFlow and fan out
     * to per-sensor flows below.
     */
    private val deviceMotion: Flow<DeviceMotionSnapshot> = callbackFlow {
        if (!motionManager.deviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.deviceMotionUpdateInterval = 1.0 / 100.0
        val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
        motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
            motion?.let { dm ->
                val now = currentTimeMillis()
                val (ux, uy, uz) = dm.userAcceleration.useContents {
                    Triple(x.toFloat(), y.toFloat(), z.toFloat())
                }
                val (gx, gy, gz) = dm.gravity.useContents {
                    Triple(x.toFloat(), y.toFloat(), z.toFloat())
                }
                val (rx, ry, rz) = dm.rotationRate.useContents {
                    Triple(x.toFloat(), y.toFloat(), z.toFloat())
                }
                val quat = dm.attitude.quaternion.useContents {
                    floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
                }
                trySend(
                    DeviceMotionSnapshot(
                        timestampMs = now,
                        userAccelX = ux, userAccelY = uy, userAccelZ = uz,
                        gravityX = gx, gravityY = gy, gravityZ = gz,
                        rotationRateX = rx, rotationRateY = ry, rotationRateZ = rz,
                        quatX = quat[0], quatY = quat[1], quatZ = quat[2], quatW = quat[3],
                    )
                )
            }
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }.shareIn(
        scope,
        // Keep the upstream alive briefly after the last collector cancels so
        // a quick UI-driven re-subscription doesn't restart CoreMotion.
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 200),
    )

    override val accelerometerFlow: Flow<Accelerometer> = deviceMotion.map { s ->
        // CoreMotion's userAcceleration is gravity-removed; combine with
        // gravity to match Android's TYPE_ACCELEROMETER which is total accel.
        Accelerometer(
            timestampMs = s.timestampMs,
            x = s.userAccelX + s.gravityX,
            y = s.userAccelY + s.gravityY,
            z = s.userAccelZ + s.gravityZ,
        )
    }

    override val gyroscopeFlow: Flow<Gyroscope> = deviceMotion.map { s ->
        Gyroscope(timestampMs = s.timestampMs, x = s.rotationRateX, y = s.rotationRateY, z = s.rotationRateZ)
    }

    // CoreMotion does not surface a calibrated magnetic field via deviceMotion
    // in a way matching Android's TYPE_MAGNETIC_FIELD, and the rest of the
    // app does not consume this flow yet.
    override val magnetometerFlow: Flow<Magnetometer> = emptyFlow()

    override val rotationVectorFlow: Flow<RotationVector> = deviceMotion.map { s ->
        RotationVector(
            timestampMs = s.timestampMs,
            x = s.quatX, y = s.quatY, z = s.quatZ,
            scalar = s.quatW,
        )
    }

    override val linearAccelerationFlow: Flow<LinearAcceleration> = deviceMotion.map { s ->
        LinearAcceleration(timestampMs = s.timestampMs, x = s.userAccelX, y = s.userAccelY, z = s.userAccelZ)
    }

    override val gravityFlow: Flow<Gravity> = deviceMotion.map { s ->
        Gravity(timestampMs = s.timestampMs, x = s.gravityX, y = s.gravityY, z = s.gravityZ)
    }
}

actual fun createSensorManager(context: Any?): SensorManager = IOSSensorManager()
