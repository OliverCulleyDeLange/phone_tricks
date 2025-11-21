package ocd.phonetricks.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.SensorData
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
class IOSSensorManager : SensorManager {
    private val motionManager = CMMotionManager()
    
    override val sensorDataFlow: Flow<SensorData> = callbackFlow {
        if (motionManager.accelerometerAvailable && motionManager.gyroAvailable) {
            motionManager.accelerometerUpdateInterval = 1.0 / 60.0 // 60Hz
            motionManager.gyroUpdateInterval = 1.0 / 60.0 // 60Hz
            
            var lastAccelerometer = Accelerometer(0f, 0f, 0f)
            var lastGyroscope = Gyroscope(0f, 0f, 0f)
            
            val queue = NSOperationQueue.currentQueue() ?: NSOperationQueue.mainQueue
            
            motionManager.startAccelerometerUpdatesToQueue(queue) { data, error ->
                data?.acceleration?.useContents {
                    lastAccelerometer = Accelerometer(
                        x = x.toFloat(),
                        y = y.toFloat(),
                        z = z.toFloat()
                    )
                    
                    trySend(
                        SensorData(
                            timestamp = (time(null) * 1000),
                            accelerometer = lastAccelerometer,
                            gyroscope = lastGyroscope
                        )
                    )
                }
            }
            
            motionManager.startGyroUpdatesToQueue(queue) { data, error ->
                data?.rotationRate?.useContents {
                    lastGyroscope = Gyroscope(
                        x = x.toFloat(),
                        y = y.toFloat(),
                        z = z.toFloat()
                    )
                    
                    trySend(
                        SensorData(
                            timestamp = (time(null) * 1000),
                            accelerometer = lastAccelerometer,
                            gyroscope = lastGyroscope
                        )
                    )
                }
            }
        }
        
        awaitClose {
            motionManager.stopAccelerometerUpdates()
            motionManager.stopGyroUpdates()
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
