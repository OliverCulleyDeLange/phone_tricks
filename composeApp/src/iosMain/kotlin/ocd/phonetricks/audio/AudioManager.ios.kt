package ocd.phonetricks.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import kotlin.math.*

actual fun createAudioManager(): AudioManager = IOSAudioManager()

class IOSAudioManager : AudioManager {
    // Temporary implementation using AVAudioEngine
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()

    private val sampleRate = 44100.0
    private var isPlaying = false

    // Current sound parameters
    private var currentFrequency = 440f
    private var currentAmplitude = 0.5f
    private var currentWaveform = Waveform.SINE

    // Buffer size for audio generation
    private val bufferSize = 2048
    private var phase = 0.0

    init {
        NSLog("Initializing iOS Audio Manager")
        setupAudioEngine()
    }

    private fun setupAudioEngine() {
        audioEngine.attachNode(playerNode)

        val format = AVAudioFormat(
            standardFormatWithSampleRate = sampleRate,
            channels = 1u
        )

        if (format != null) {
            audioEngine.connect(playerNode, audioEngine.mainMixerNode, format)
            try {
                audioEngine.startAndReturnError(null)
                NSLog("Audio engine started successfully")
            } catch (e: Throwable) {
                NSLog("Failed to start audio engine: ${e.message}")
            }
        } else {
            NSLog("Failed to create audio format")
        }
    }

    override fun playSynthSound(frequency: Float, amplitude: Float, waveform: Waveform) {
        currentFrequency = frequency
        currentAmplitude = amplitude
        currentWaveform = waveform

        if (!isPlaying) {
            isPlaying = true
            generateAndPlaySound()
        }
    }

    override fun stopSound() {
        isPlaying = false
        playerNode.stop()
    }

    override fun release() {
        stopSound()
        audioEngine.stop()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun generateAndPlaySound() {
        val format = AVAudioFormat(
            standardFormatWithSampleRate = sampleRate,
            channels = 1u
        ) ?: return

        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = bufferSize.toUInt())
        if (buffer == null) {
            NSLog("Failed to create audio buffer")
            return
        }

        // Set the buffer length
        buffer.frameLength = bufferSize.toUInt()

        // Get the audio buffer data
        val channelData = buffer.floatChannelData
        if (channelData == null) {
            NSLog("Failed to get channel data")
            return
        }

        val samples = channelData[0] ?: return

        // Generate the waveform
        val phaseIncrement = 2.0 * PI * currentFrequency.toDouble() / sampleRate

        for (i in 0 until bufferSize) {
            val sample = when (currentWaveform) {
                Waveform.SINE -> sin(phase)
                Waveform.SQUARE -> if (phase < PI) 1.0 else -1.0
                Waveform.TRIANGLE -> {
                    val normalizedPhase = (phase / (2.0 * PI))
                    when {
                        normalizedPhase < 0.25 -> normalizedPhase * 4.0
                        normalizedPhase < 0.75 -> 2.0 - (normalizedPhase * 4.0)
                        else -> (normalizedPhase * 4.0) - 4.0
                    }
                }

                Waveform.SAWTOOTH -> {
                    val normalizedPhase = (phase / (2.0 * PI))
                    2.0 * (normalizedPhase - floor(0.5 + normalizedPhase))
                }
            }

            samples[i] = (sample * currentAmplitude).toFloat()
            phase += phaseIncrement
            if (phase > 2.0 * PI) {
                phase -= 2.0 * PI
            }
        }

        // Schedule the buffer to play
        playerNode.scheduleBuffer(buffer, completionCallbackType = AVAudioPlayerNodeCompletionDataConsumed) {
            // Recursive play for continuous sound
            if (this.isPlaying) {
                this.generateAndPlaySound()
            }
        }

        // Start playback if not already playing
        if (!playerNode.playing) {
            playerNode.play()
        }
    }
}
