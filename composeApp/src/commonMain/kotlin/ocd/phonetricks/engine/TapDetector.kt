package ocd.phonetricks.engine

import ocd.phonetricks.data.LinearAcceleration
import ocd.phonetricks.data.RotationVector
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
    private val tapThreshold = 1f
    private val tapCooldownMs = 100L
    private val tapWindowMs = 100L
    private val confidenceThreshold = 0.5f
    private var lastTapTimeMs = 0L

    private enum class TapState {
        IDLE,
        ABOVE_THRESHOLD,
        COMPLETED
    }

    private var currentState = TapState.IDLE
    private var windowStartMs = 0L
    private var peakMagnitude = 0f
    private var peakAccelX = 0f
    private var peakAccelY = 0f
    private var peakAccelZ = 0f
    private var peakTimestampMs = 0L

    /**
     * Process sensor data from the ring buffer and detect taps.
     * Returns a list of newly detected tap events.
     */
    fun processSensorData(
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        rotationVectorBuffer: RingBuffer<RotationVector>
    ): List<TrickEvent> {
        if (linearAccelerationBuffer.isEmpty()) {
            return emptyList()
        }

        val detectedTaps = mutableListOf<TrickEvent>()
        val current = linearAccelerationBuffer[linearAccelerationBuffer.size() - 1]
        val currentMagnitude = calculateMagnitude(current.x, current.y, current.z)

        when (currentState) {
            TapState.IDLE -> {
                if (currentMagnitude > tapThreshold) {
                    currentState = TapState.ABOVE_THRESHOLD
                    windowStartMs = current.timestampMs
                    peakMagnitude = currentMagnitude
                    peakAccelX = current.x
                    peakAccelY = current.y
                    peakAccelZ = current.z
                    peakTimestampMs = current.timestampMs
                }
            }

            TapState.ABOVE_THRESHOLD -> {
                val elapsedMs = current.timestampMs - windowStartMs

                if (elapsedMs > tapWindowMs) {
                    currentState = TapState.IDLE
                    resetPeakData()
                } else if (currentMagnitude > tapThreshold) {
                    if (currentMagnitude > peakMagnitude) {
                        peakMagnitude = currentMagnitude
                        peakAccelX = current.x
                        peakAccelY = current.y
                        peakAccelZ = current.z
                        peakTimestampMs = current.timestampMs
                    }
                } else {
                    val timeSinceLastTapMs = current.timestampMs - lastTapTimeMs
                    if (timeSinceLastTapMs > tapCooldownMs) {
                        val rotationVector = findClosestRotationVector(rotationVectorBuffer, peakTimestampMs)
                        if (rotationVector != null) {
                            val confidence = calculateConfidence(peakMagnitude)
                            if (confidence >= confidenceThreshold) {
                                val tapType = determineTapSurface(
                                    rotationVector,
                                    peakAccelX,
                                    peakAccelY,
                                    peakAccelZ
                                )
                                detectedTaps.add(TrickEvent(tapType, current.timestampMs, confidence))
                                println("Added tap event: $tapType, confidence: $confidence, peak magnitude: $peakMagnitude")
                                lastTapTimeMs = current.timestampMs
                            } else {
                                println("Filtered low confidence tap: confidence: $confidence, peak magnitude: $peakMagnitude")
                            }
                        }
                    }

                    currentState = TapState.IDLE
                    resetPeakData()
                }
            }

            TapState.COMPLETED -> {
                currentState = TapState.IDLE
                resetPeakData()
            }
        }

        return detectedTaps
    }

    private fun resetPeakData() {
        peakMagnitude = 0f
        peakAccelX = 0f
        peakAccelY = 0f
        peakAccelZ = 0f
        peakTimestampMs = 0L
    }

    /**
     * Find the closest rotation vector to a given timestamp.
     */
    private fun findClosestRotationVector(
        rotationVectorBuffer: RingBuffer<RotationVector>,
        timestampMs: Long
    ): RotationVector? {
        if (rotationVectorBuffer.isEmpty()) return null

        var closest: RotationVector? = null
        var minDiff = Long.MAX_VALUE

        for (i in 0 until rotationVectorBuffer.size()) {
            val reading = rotationVectorBuffer[i]
            val diff = abs(reading.timestampMs - timestampMs)
            if (diff < minDiff) {
                minDiff = diff
                closest = reading
            }
        }

        return closest
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
    private fun determineTapSurface(
        rotationVector: RotationVector,
        accelX: Float,
        accelY: Float,
        accelZ: Float
    ): TrickType {
        // Get the rotation vector (quaternion)
        val qx = rotationVector.x
        val qy = rotationVector.y
        val qz = rotationVector.z
        val qw = rotationVector.scalar ?: computeScalar(qx.toDouble(), qy.toDouble(), qz.toDouble())

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
        val normalized = (magnitude - tapThreshold) / (tapThreshold * 2)
        return normalized.coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        lastTapTimeMs = 0L
        currentState = TapState.IDLE
        resetPeakData()
    }
}
