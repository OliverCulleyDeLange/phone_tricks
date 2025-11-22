package ocd.phonetricks.engine

import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.data.TrickEvent
import ocd.phonetricks.data.TrickType
import kotlin.math.abs
import kotlin.math.PI

/**
 * Detects phone tricks like spins and flips from sensor data.
 *
 * Detection Logic:
 * - SPIN: Rotation about Z-axis (phone flat, spinning like on a table)
 * - FLIP: Rotation about X or Y axis (phone flipping end-over-end)
 */
class TrickDetector {
    // Threshold for detecting significant rotation (rad/s)
    private val rotationThreshold = 2.0f // ~115 degrees/second

    // Track accumulated rotation for each axis
    private var accumulatedRotationZ = 0.0
    private var accumulatedRotationX = 0.0
    private var accumulatedRotationY = 0.0

    // Track if we're currently in a trick
    private var isInSpin = false
    private var isInFlipX = false
    private var isInFlipY = false

    // Last detection time to avoid duplicate detections
    private var lastSpinTime = 0L
    private var lastFlipTime = 0L
    private val cooldownMs = 500L // Minimum time between same trick detections

    /**
     * Process sensor data from the ring buffer and detect tricks.
     * Returns a list of newly detected tricks.
     */
    fun processSensorData(
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        rotationVectorBuffer: RingBuffer<RotationVector>
    ): List<TrickEvent> {
        // We need at least 2 readings to calculate delta time
        if (gyroscopeBuffer.size() < 2) {
            return emptyList()
        }

        val detectedTricks = mutableListOf<TrickEvent>()

        // Get the last 2 sensor readings
        val size = gyroscopeBuffer.size()
        val previous = gyroscopeBuffer[size - 2]
        val current = gyroscopeBuffer[size - 1]

        // Calculate time delta in seconds
        val deltaTime = (current.timestampMs - previous.timestampMs) / 1000.0
        if (deltaTime <= 0 || deltaTime > 0.1) return emptyList() // Ignore invalid deltas

        // Integrate angular velocity to get rotation angles
        val rotationZ = current.z * deltaTime
        val rotationX = current.x * deltaTime
        val rotationY = current.y * deltaTime

        // Detect SPIN (Z-axis rotation)
        detectedTricks.addAll(detectSpin(current, rotationZ))

        // Detect FLIP (X or Y axis rotation)
        detectedTricks.addAll(detectFlip(current, rotationX, rotationY))

        return detectedTricks
    }

    private fun detectSpin(current: Gyroscope, rotationZ: Double): List<TrickEvent> {
        val tricks = mutableListOf<TrickEvent>()

        // Check if Z-axis is spinning fast enough
        if (abs(current.z) > rotationThreshold) {
            if (!isInSpin) {
                isInSpin = true
                accumulatedRotationZ = 0.0
            }
            accumulatedRotationZ += rotationZ

            // Check if we've completed a full rotation (2π radians = 360 degrees)
            if (abs(accumulatedRotationZ) >= 2 * PI) {
                val timeSinceLastSpin = current.timestampMs - lastSpinTime
                if (timeSinceLastSpin > cooldownMs) {
                    val confidence = calculateConfidence(abs(current.z), rotationThreshold)
                    tricks.add(TrickEvent(TrickType.SPIN, current.timestampMs, confidence))
                    lastSpinTime = current.timestampMs
                }
                accumulatedRotationZ = 0.0
            }
        } else {
            // Reset if rotation slows down
            if (isInSpin && abs(current.z) < rotationThreshold * 0.5f) {
                isInSpin = false
                accumulatedRotationZ = 0.0
            }
        }

        return tricks
    }

    private fun detectFlip(current: Gyroscope, rotationX: Double, rotationY: Double): List<TrickEvent> {
        val tricks = mutableListOf<TrickEvent>()

        // Check X-axis flip
        if (abs(current.x) > rotationThreshold) {
            if (!isInFlipX) {
                isInFlipX = true
                accumulatedRotationX = 0.0
            }
            accumulatedRotationX += rotationX

            if (abs(accumulatedRotationX) >= 2 * PI) {
                val timeSinceLastFlip = current.timestampMs - lastFlipTime
                if (timeSinceLastFlip > cooldownMs) {
                    val confidence = calculateConfidence(abs(current.x), rotationThreshold)
                    tricks.add(TrickEvent(TrickType.FLIP, current.timestampMs, confidence))
                    lastFlipTime = current.timestampMs
                }
                accumulatedRotationX = 0.0
            }
        } else {
            if (isInFlipX && abs(current.x) < rotationThreshold * 0.5f) {
                isInFlipX = false
                accumulatedRotationX = 0.0
            }
        }

        // Check Y-axis flip
        if (abs(current.y) > rotationThreshold) {
            if (!isInFlipY) {
                isInFlipY = true
                accumulatedRotationY = 0.0
            }
            accumulatedRotationY += rotationY

            if (abs(accumulatedRotationY) >= 2 * PI) {
                val timeSinceLastFlip = current.timestampMs - lastFlipTime
                if (timeSinceLastFlip > cooldownMs) {
                    val confidence = calculateConfidence(abs(current.y), rotationThreshold)
                    tricks.add(TrickEvent(TrickType.FLIP, current.timestampMs, confidence))
                    lastFlipTime = current.timestampMs
                }
                accumulatedRotationY = 0.0
            }
        } else {
            if (isInFlipY && abs(current.y) < rotationThreshold * 0.5f) {
                isInFlipY = false
                accumulatedRotationY = 0.0
            }
        }

        return tricks
    }

    private fun calculateConfidence(angularVelocity: Float, threshold: Float): Float {
        // Higher angular velocity = higher confidence
        // Cap at 1.0
        return (angularVelocity / (threshold * 3)).coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        accumulatedRotationZ = 0.0
        accumulatedRotationX = 0.0
        accumulatedRotationY = 0.0
        isInSpin = false
        isInFlipX = false
        isInFlipY = false
    }
}
