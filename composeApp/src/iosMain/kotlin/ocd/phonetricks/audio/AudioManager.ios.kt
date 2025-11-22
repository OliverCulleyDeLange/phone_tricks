package ocd.phonetricks.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.*
import kotlin.math.PI
import kotlin.math.sin

actual fun createAudioManager(): AudioManager = IOSAudioManager()

@OptIn(ExperimentalForeignApi::class)
class IOSAudioManager : AudioManager {
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()

    private val sampleRate = 44100.0
    private val duration = 0.05 // 50ms
    private val frequency = 800.0 // Hz

    private var audioBuffer: AVAudioPCMBuffer? = null

    init {
        setupAudioEngine()
        generateTapSound()
    }

    private fun setupAudioEngine() {
        audioEngine.attachNode(playerNode)

        val format = AVAudioFormat(
            standardFormatWithSampleRate = sampleRate,
            channels = 1u
        )

        format?.let {
            audioEngine.connect(playerNode, audioEngine.mainMixerNode, it)
        }

        try {
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            println("Error starting audio engine: ${e.message}")
        }
    }

    private fun generateTapSound() {
        val format = AVAudioFormat(
            standardFormatWithSampleRate = sampleRate,
            channels = 1u
        ) ?: return

        val numSamples = (duration * sampleRate).toInt()
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = numSamples.toUInt())
        buffer.frameLength = numSamples.toUInt()

        val channelData = buffer.floatChannelData ?: return
        val samples = channelData[0] ?: return

        // Generate a sine wave with envelope (fade in/out to avoid clicks)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate / frequency)
            val envelope = when {
                i < numSamples * 0.1 -> i / (numSamples * 0.1) // Fade in (10%)
                i > numSamples * 0.7 -> (numSamples - i) / (numSamples * 0.3) // Fade out (30%)
                else -> 1.0
            }
            samples[i] = (sin(angle) * envelope * 0.3).toFloat() // 30% volume
        }

        audioBuffer = buffer
    }

    override fun playTapSound() {
        audioBuffer?.let { buffer ->
            if (playerNode.playing) {
                playerNode.stop()
            }
            playerNode.scheduleBuffer(buffer, completionHandler = null)
            playerNode.play()
        }
    }

    override fun release() {
        playerNode.stop()
        audioEngine.stop()
    }
}
