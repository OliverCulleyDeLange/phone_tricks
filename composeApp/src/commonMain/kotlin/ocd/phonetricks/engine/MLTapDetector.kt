package ocd.phonetricks.engine

import ocd.phonetricks.data.*
import phonetricks.composeapp.generated.resources.Res
import kotlin.math.abs
import kotlin.math.sqrt

class MLTapDetector {
    private val tapCooldownMs = 100
    private val mlConfidenceThreshold = 0.8f
    private val inferenceThrottleMs = 20
    private var lastTapTimeMs = 0L
    private var lastInferenceTimeMs = 0L

    private var model: RandomForestModel? = null
    private val featureExtractor = TapFeatureExtractor()

    private var isModelLoaded = false

    suspend fun loadModel() {
        try {
            val modelJson = Res.readBytes("files/tap_classifier_kotlin.json").decodeToString()
            model = RandomForestModel.loadFromJson(modelJson)
            isModelLoaded = true
            println("✓ ML model loaded successfully!")
            println("  Model classes: ${model?.getClasses()?.joinToString(", ")}")
        } catch (e: Exception) {
            println("✗ Failed to load ML model: ${e.message}")
            e.printStackTrace()
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
        if (!isModelLoaded || model == null) {
            return emptyList()
        }

        if (linearAccelerationBuffer.isEmpty()) {
            return emptyList()
        }

        val detectedTaps = mutableListOf<TrickEvent>()
        val current = linearAccelerationBuffer[linearAccelerationBuffer.size() - 1]

        val timeSinceLastTapMs = current.timestampMs - lastTapTimeMs
        if (timeSinceLastTapMs <= tapCooldownMs) {
            return emptyList()
        }

        val timeSinceLastInferenceMs = current.timestampMs - lastInferenceTimeMs
        if (timeSinceLastInferenceMs <= inferenceThrottleMs) {
            return emptyList()
        }

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

            lastInferenceTimeMs = current.timestampMs

            val (predictedLabel, confidence) = model!!.predict(features)

            if (predictedLabel != "NEGATIVE" && confidence >= mlConfidenceThreshold) {
                val tapType = TrickType.TAP_FRONT

                detectedTaps.add(TrickEvent(tapType, current.timestampMs, confidence))
                lastTapTimeMs = current.timestampMs
                println("Confidence: ${(confidence * 100).toInt()}%")
            }
        } catch (e: Exception) {
            println("ML inference error: ${e.message}")
            e.printStackTrace()
        }

        return detectedTaps
    }

    fun reset() {
        lastTapTimeMs = 0L
        lastInferenceTimeMs = 0L
    }
}
