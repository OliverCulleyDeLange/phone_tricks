package ocd.phonetricks.engine

import ocd.phonetricks.data.*
import phonetricks.composeapp.generated.resources.Res
import kotlin.math.abs
import kotlin.math.sqrt

data class InferenceResult(
    val timestampMs: Long,
    val predictedLabel: String,
    val tapConfidence: Float,
    val negativeConfidence: Float
)

data class DetectionResult(
    val trickEvents: List<TrickEvent>,
    val inferenceResult: InferenceResult?
)

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
    ): DetectionResult {
        if (!isModelLoaded || model == null) {
            return DetectionResult(emptyList(), null)
        }

        if (linearAccelerationBuffer.isEmpty()) {
            return DetectionResult(emptyList(), null)
        }

        val detectedTaps = mutableListOf<TrickEvent>()
        val current = linearAccelerationBuffer[linearAccelerationBuffer.size() - 1]

        val timeSinceLastInferenceMs = current.timestampMs - lastInferenceTimeMs
        if (timeSinceLastInferenceMs <= inferenceThrottleMs) {
            return DetectionResult(emptyList(), null)
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

            val probabilities = model!!.predictProba(features)
            val tapConfidence = probabilities["TAP"] ?: 0f
            val negativeConfidence = probabilities["NEGATIVE"] ?: 0f

            val (predictedLabel, _) = model!!.predict(features)

            val inferenceResult = InferenceResult(
                timestampMs = current.timestampMs,
                predictedLabel = predictedLabel,
                tapConfidence = tapConfidence,
                negativeConfidence = negativeConfidence
            )

            val timeSinceLastTapMs = current.timestampMs - lastTapTimeMs
            val canEmitTap = timeSinceLastTapMs > tapCooldownMs

            if (predictedLabel != "NEGATIVE" && tapConfidence >= mlConfidenceThreshold && canEmitTap) {
                val tapType = TrickType.TAP_FRONT
                detectedTaps.add(TrickEvent(tapType, current.timestampMs, tapConfidence))
                lastTapTimeMs = current.timestampMs
            }
            return DetectionResult(detectedTaps, inferenceResult)
        } catch (e: Exception) {
            println("ML inference error: ${e.message}")
            e.printStackTrace()
            return DetectionResult(emptyList(), null)
        }
    }

    fun reset() {
        lastTapTimeMs = 0L
        lastInferenceTimeMs = 0L
    }
}
