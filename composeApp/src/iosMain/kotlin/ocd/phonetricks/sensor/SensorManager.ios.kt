package ocd.phonetricks.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gravity
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.LinearAcceleration
import ocd.phonetricks.data.Magnetometer
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.data.SensorData
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
class IOSSensorManager : SensorManager {
    private val motionManager = CMMotionManager()
    
    override val sensorDataFlow: Flow<SensorData> = callbackFlow {
        val available = motionManager.accelerometerAvailable && 
                       motionManager.gyroAvailable
        
        if (available) {
            // Set update intervals
            motionManager.deviceMotionUpdateInterval = 1.0 / 60.0 // 60Hz
            
            var lastAccelerometer = Accelerometer(0f, 0f, 0f)
            var lastGyroscope = Gyroscope(0f, 0f, 0f)
            var lastRotationVector: RotationVector? = null
            var lastLinearAcceleration: LinearAcceleration? = null
            var lastGravity: Gravity? = null
            
            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
            
            // Use device motion for comprehensive sensor data
            motionManager.startDeviceMotionUpdatesToQueue(queue) { motion, error ->
                motion?.let { deviceMotion ->
                    // User acceleration (linear acceleration without gravity)
                    deviceMotion.userAcceleration.useContents {
                        lastLinearAcceleration = LinearAcceleration(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat()
                        )
                    }
                    
                    // Gravity
                    deviceMotion.gravity.useContents {
                        lastGravity = Gravity(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat()
                        )
                        
                        // Calculate total acceleration (linear + gravity)
                        lastLinearAcceleration?.let { linear ->
                            lastAccelerometer = Accelerometer(
                                x = linear.x + x.toFloat(),
                                y = linear.y + y.toFloat(),
                                z = linear.z + z.toFloat()
                            )
                        }
                    }
                    
                    // Rotation rate (gyroscope)
                    deviceMotion.rotationRate.useContents {
                        lastGyroscope = Gyroscope(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat()
                        )
                    }
                    
                    // Attitude (rotation vector as quaternion)
                    deviceMotion.attitude.quaternion.useContents {
                        lastRotationVector = RotationVector(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            z = z.toFloat(),
                            scalar = w.toFloat()
                        )
                    }
                    
                    trySend(
                        SensorData(
                            timestamp = (time(null) * 1000),
                            accelerometer = lastAccelerometer,
                            gyroscope = lastGyroscope,
                            magnetometer = null,
                            rotationVector = lastRotationVector,
                            linearAcceleration = lastLinearAcceleration,
                            gravity = lastGravity
                        )
                    )
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
