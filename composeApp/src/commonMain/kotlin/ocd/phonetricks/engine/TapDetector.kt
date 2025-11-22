package ocd.phonetricks.engine

import ocd.phonetricks.data.SensorData
import ocd.phonetricks.data.TrickEvent
import ocd.phonetricks.data.TrickType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects taps on the phone's surfaces (front, back, or edges) using accelerometer data.
 *
 * Detection Logic:
 * - Uses linear acceleration data to detect sudden spikes (impacts)
 * - A valid tap is when acceleration goes above threshold and then back below it
 * - Uses rotation vector to determine phone orientation
 * - Combines acceleration direction with orientation to determine which surface was tapped
 *
 * Phone coordinate system:
 * - X: Positive points to the right when holding phone in portrait
 * - Y: Positive points to the top of the phone
 * - Z: Positive points out of the screen (toward user)
 */
class TapDetector {
    private val tapThreshold = 1.5f
    private val tapCooldownMs = 50L
    private var lastTapTimeMs = 0L

    /**
     * Process sensor data from the ring buffer and detect taps.
     * Returns a list of newly detected tap events.
     */
    fun processSensorData(sensorBuffer: RingBuffer<SensorData>): List<TrickEvent> {
        // We need at least 3 readings to detect a spike pattern
        if (sensorBuffer.size() < 3) {
            return emptyList()
        }

        val detectedTaps = mutableListOf<TrickEvent>()

        // Get the last 3 sensor readings
        val size = sensorBuffer.size()
        val previousPrevious = sensorBuffer[size - 3]
        val previous = sensorBuffer[size - 2]
        val current = sensorBuffer[size - 1]

        // Calculate magnitudes for each reading
        val prevPrevLinearAccel = previousPrevious.linearAcceleration
        val prevLinearAccel = previous.linearAcceleration
        val currentLinearAccel = current.linearAcceleration

        // All three must have linear acceleration data
        if (prevPrevLinearAccel == null || prevLinearAccel == null || currentLinearAccel == null) {
            return emptyList()
        }

        val prevPrevMagnitude = calculateMagnitude(prevPrevLinearAccel.x, prevPrevLinearAccel.y, prevPrevLinearAccel.z)
        val prevMagnitude = calculateMagnitude(prevLinearAccel.x, prevLinearAccel.y, prevLinearAccel.z)
        val currentMagnitude = calculateMagnitude(currentLinearAccel.x, currentLinearAccel.y, currentLinearAccel.z)

        // Check for spike pattern:
        // 1. Previous-previous was below threshold
        // 2. Previous went above threshold
        // 3. Current is below threshold again
        // This indicates a quick spike characteristic of a tap

        val wasBelow = prevPrevMagnitude < tapThreshold
        val wentAbove = prevMagnitude > tapThreshold
        val backBelow = currentMagnitude < tapThreshold

        if (wasBelow && wentAbove && backBelow) {
            val timeSinceLastTapMs = current.timestampMs - lastTapTimeMs
            if (timeSinceLastTapMs > tapCooldownMs) {
                // Use the peak (previous) data to determine tap surface
                val tapType = determineTapSurface(
                    previous,
                    prevLinearAccel.x,
                    prevLinearAccel.y,
                    prevLinearAccel.z
                )
                val confidence = calculateConfidence(prevMagnitude)
                detectedTaps.add(TrickEvent(tapType, current.timestampMs, confidence))
                println("Added tap event: $tapType, confidence: $confidence, magnitudes: $prevPrevMagnitude, $prevMagnitude, $currentMagnitude")
                lastTapTimeMs = current.timestampMs
            }
        }

        return detectedTaps
    }

    /**
     * Calculate the magnitude of a 3D vector.
     */
    private fun calculateMagnitude(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Determine which surface was tapped based on acceleration direction and device orientation.
     */
    private fun determineTapSurface(sensorData: SensorData, accelX: Float, accelY: Float, accelZ: Float): TrickType {
        // Get the rotation vector (quaternion)
        val rv = sensorData.rotationVector
        val qx = rv.x
        val qy = rv.y
        val qz = rv.z
        val qw = rv.scalar ?: computeScalar(qx.toDouble(), qy.toDouble(), qz.toDouble())

        // Transform acceleration vector from device frame to world frame
        // This helps us understand the actual direction of impact regardless of device orientation
        val worldAccel = rotateVectorByQuaternion(accelX, accelY, accelZ, qx, qy, qz, qw)

        // Find the axis with the maximum absolute acceleration
        val absX = abs(accelX)
        val absY = abs(accelY)
        val absZ = abs(accelZ)

        // Determine primary impact direction in device coordinates
        return when {
            absZ > absX && absZ > absY -> {
                // Impact along Z-axis (perpendicular to screen)
                if (accelZ > 0) TrickType.TAP_FRONT // Tap on screen
                else TrickType.TAP_BACK // Tap on back
            }

            absY > absX && absY > absZ -> {
                // Impact along Y-axis (parallel to phone length)
                if (accelY > 0) TrickType.TAP_TOP
                else TrickType.TAP_BOTTOM
            }

            else -> {
                // Impact along X-axis (parallel to phone width)
                if (accelX > 0) TrickType.TAP_RIGHT
                else TrickType.TAP_LEFT
            }
        }
    }

    /**
     * Rotate a vector by a quaternion to transform from device frame to world frame.
     */
    private fun rotateVectorByQuaternion(
        vx: Float, vy: Float, vz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float
    ): Triple<Float, Float, Float> {
        // v' = q * v * q^(-1)
        // For unit quaternions: q^(-1) = q* (conjugate)

        // First: q * v (treating v as quaternion with w=0)
        val tx = qw * vx + qy * vz - qz * vy
        val ty = qw * vy + qz * vx - qx * vz
        val tz = qw * vz + qx * vy - qy * vx
        val tw = -qx * vx - qy * vy - qz * vz

        // Then: result * q* (conjugate)
        val rx = tw * -qx + tx * qw + ty * -qz - tz * -qy
        val ry = tw * -qy + ty * qw + tz * -qx - tx * -qz
        val rz = tw * -qz + tz * qw + tx * -qy - ty * -qx

        return Triple(rx, ry, rz)
    }

    /**
     * Compute the scalar (w) component of quaternion if not provided.
     */
    private fun computeScalar(x: Double, y: Double, z: Double): Float {
        val sumSquares = x * x + y * y + z * z
        return if (sumSquares < 1.0) {
            sqrt(1.0 - sumSquares).toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate confidence based on acceleration magnitude.
     * Higher impact = higher confidence.
     */
    private fun calculateConfidence(magnitude: Float): Float {
        // Normalize between tapThreshold and 3x threshold
        val normalized = (magnitude - tapThreshold) / (tapThreshold * 2)
        return normalized.coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        lastTapTimeMs = 0L
    }
}
