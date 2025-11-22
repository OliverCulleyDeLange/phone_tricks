package ocd.phonetricks.engine

import ocd.phonetricks.data.*
import phonetricks.composeapp.generated.resources.Res
import kotlin.math.abs
import kotlin.math.sqrt

class MLTapDetector {
    private val tapThreshold = 1f
    private val tapCooldownMs = 50
    private val mlConfidenceThreshold = 0.4f
    private var lastTapTimeMs = 0L
    private var previousMagnitude = 0f

    private var model: RandomForestModel? = null
    private val featureExtractor = TapFeatureExtractor()

    private var isModelLoaded = false

    suspend fun loadModel() {
        try {
            val modelJson = Res.readBytes("files/tap_classifier_kotlin.json").decodeToString()
            model = RandomForestModel.loadFromJson(modelJson)
            isModelLoaded = true
            println("ML model loaded successfully")
        } catch (e: Exception) {
            println("Failed to load ML model: ${e.message}")
            isModelLoaded = false
        }
    }

    fun processSensorData(
        accelerometerBuffer: RingBuffer<Accelerometer>,
        gyroscopeBuffer: RingBuffer<Gyroscope>,
        linearAccelerationBuffer: RingBuffer<LinearAcceleration>,
        magnetometerBuffer: RingBuffer<Magnetometer>,
        gravityBuffer: RingBuffer<Gravity>,
        rotationVectorBuffer: RingBuffer<RotationVector>
    ): List<TrickEvent> {
        if (linearAccelerationBuffer.isEmpty()) {
            return emptyList()
        }

        val detectedTaps = mutableListOf<TrickEvent>()
        val current = linearAccelerationBuffer[linearAccelerationBuffer.size() - 1]
        val currentMagnitude = calculateMagnitude(current.x, current.y, current.z)

        val magnitudeDelta = currentMagnitude - previousMagnitude

        if (magnitudeDelta > tapThreshold) {
            val timeSinceLastTapMs = current.timestampMs - lastTapTimeMs
            if (timeSinceLastTapMs > tapCooldownMs) {
                if (isModelLoaded && model != null) {
                    try {
                        val features = featureExtractor.extractFeaturesFromTap(
                            accelerometerBuffer,
                            gyroscopeBuffer,
                            linearAccelerationBuffer,
                            magnetometerBuffer,
                            gravityBuffer,
                            rotationVectorBuffer,
                            current.timestampMs
                        )

                        val (predictedLabel, confidence) = model!!.predict(features)

                        if (confidence >= mlConfidenceThreshold) {
                            val tapType = mapPredictionToTrickType(
                                predictedLabel,
                                rotationVectorBuffer,
                                current
                            )
                            detectedTaps.add(TrickEvent(tapType, current.timestampMs, confidence))
                            println("ML detected: $predictedLabel -> $tapType, confidence: $confidence")
                            lastTapTimeMs = current.timestampMs
                        } else {
                            println("Low confidence ML prediction: $predictedLabel, confidence: $confidence")
                        }
                    } catch (e: Exception) {
                        println("ML inference error: ${e.message}")
                        fallbackToRuleBasedDetection(
                            rotationVectorBuffer,
                            current,
                            magnitudeDelta,
                            detectedTaps
                        )
                    }
                } else {
                    fallbackToRuleBasedDetection(
                        rotationVectorBuffer,
                        current,
                        magnitudeDelta,
                        detectedTaps
                    )
                }
            }
        }

        previousMagnitude = currentMagnitude
        return detectedTaps
    }

    private fun fallbackToRuleBasedDetection(
        rotationVectorBuffer: RingBuffer<RotationVector>,
        current: LinearAcceleration,
        magnitudeDelta: Float,
        detectedTaps: MutableList<TrickEvent>
    ) {
        val confidence = calculateConfidence(magnitudeDelta)
        if (confidence >= 0.5f) {
            val rotationVector = findClosestRotationVector(rotationVectorBuffer, current.timestampMs)
            if (rotationVector != null) {
                val tapType = determineTapSurface(
                    rotationVector,
                    current.x,
                    current.y,
                    current.z
                )
                detectedTaps.add(TrickEvent(tapType, current.timestampMs, confidence))
                println("Rule-based tap: $tapType, confidence: $confidence")
                lastTapTimeMs = current.timestampMs
            }
        }
    }

    private fun mapPredictionToTrickType(
        label: String,
        rotationVectorBuffer: RingBuffer<RotationVector>,
        current: LinearAcceleration
    ): TrickType {
        val explicitType = when {
            label.contains("TAP_FRONT", ignoreCase = true) -> TrickType.TAP_FRONT
            label.contains("TAP_BACK", ignoreCase = true) -> TrickType.TAP_BACK
            label.contains("TAP_TOP", ignoreCase = true) -> TrickType.TAP_TOP
            label.contains("TAP_BOTTOM", ignoreCase = true) -> TrickType.TAP_BOTTOM
            label.contains("TAP_LEFT", ignoreCase = true) -> TrickType.TAP_LEFT
            label.contains("TAP_RIGHT", ignoreCase = true) -> TrickType.TAP_RIGHT
            else -> null
        }

        if (explicitType != null) {
            return explicitType
        }

        val rotationVector = findClosestRotationVector(rotationVectorBuffer, current.timestampMs)
        if (rotationVector != null) {
            return determineTapSurface(rotationVector, current.x, current.y, current.z)
        }

        return TrickType.TAP_FRONT
    }

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

    private fun calculateMagnitude(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z)
    }

    private fun determineTapSurface(
        rotationVector: RotationVector,
        accelX: Float,
        accelY: Float,
        accelZ: Float
    ): TrickType {
        val qx = rotationVector.x
        val qy = rotationVector.y
        val qz = rotationVector.z
        val qw = rotationVector.scalar ?: computeScalar(qx.toDouble(), qy.toDouble(), qz.toDouble())

        val absX = abs(accelX)
        val absY = abs(accelY)
        val absZ = abs(accelZ)

        return when {
            absZ > absX && absZ > absY -> {
                if (accelZ > 0) TrickType.TAP_FRONT else TrickType.TAP_BACK
            }

            absY > absX && absY > absZ -> {
                if (accelY > 0) TrickType.TAP_TOP else TrickType.TAP_BOTTOM
            }

            else -> {
                if (accelX > 0) TrickType.TAP_RIGHT else TrickType.TAP_LEFT
            }
        }
    }

    private fun computeScalar(x: Double, y: Double, z: Double): Float {
        val sumSquares = x * x + y * y + z * z
        return if (sumSquares < 1.0) {
            sqrt(1.0 - sumSquares).toFloat()
        } else {
            0f
        }
    }

    private fun calculateConfidence(magnitudeDelta: Float): Float {
        val normalized = (magnitudeDelta - tapThreshold) / (tapThreshold * 2)
        return normalized.coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        lastTapTimeMs = 0L
        previousMagnitude = 0f
    }
}
