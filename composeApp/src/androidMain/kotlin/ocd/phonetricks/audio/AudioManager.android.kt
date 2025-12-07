package ocd.phonetricks.audio

import android.content.Context
import ocd.phonetricks.audio.Waveform
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.abs

actual fun createAudioManager(): AudioManager = AndroidAudioManager()

class AndroidAudioManager : AudioManager {
    companion object {
        init {
            System.loadLibrary("superpoweredSynth")
        }
    }

    // Native method declarations
    private external fun nativeInit(): Long
    private external fun nativePlaySound(synthPtr: Long, frequency: Float, amplitude: Float, waveformType: Int)
    private external fun nativeStopSound(synthPtr: Long)
    private external fun nativeRelease(synthPtr: Long)

    private var synthesizerPtr: Long = 0

    init {
        synthesizerPtr = nativeInit()
    }

    // Target parameter values
    private var targetFrequency = 440f
    private var targetAmplitude = 0.5f

    // Current parameter values (smoothed)
    private var currentFrequency = 440f
    private var currentAmplitude = 0.5f
    private var currentWaveform = Waveform.SINE

    // Smoothing factors (smaller = smoother, but less responsive)
    private val frequencySmoothingFactor = 0.2f
    private val amplitudeSmoothingFactor = 0.1f

    private var isPlaying = false

    override fun playSynthSound(frequency: Float, amplitude: Float, waveform: Waveform) {
        // Store target parameters
        targetFrequency = frequency
        targetAmplitude = amplitude
        currentWaveform = waveform  // Waveform changes immediately

        if (isPlaying) {
            // Already playing, just update parameters
            nativePlaySound(synthesizerPtr, frequency, amplitude, waveformTypeToInt(waveform))
            return
        }

        // Initialize current values to targets for first playback
        currentFrequency = targetFrequency
        currentAmplitude = targetAmplitude

        isPlaying = true
        nativePlaySound(synthesizerPtr, frequency, amplitude, waveformTypeToInt(waveform))
    }

    private fun waveformTypeToInt(waveform: Waveform): Int {
        return when (waveform) {
            Waveform.SINE -> 0
            Waveform.SQUARE -> 1
            Waveform.TRIANGLE -> 2
            Waveform.SAWTOOTH -> 3
        }
    }

    private fun smoothParameters() {
        // Apply exponential smoothing to frequency
        val freqDiff = targetFrequency - currentFrequency
        if (abs(freqDiff) > 0.01f) {
            currentFrequency += freqDiff * frequencySmoothingFactor
        }

        // Apply exponential smoothing to amplitude
        val ampDiff = targetAmplitude - currentAmplitude
        if (abs(ampDiff) > 0.001f) {
            currentAmplitude += ampDiff * amplitudeSmoothingFactor
        }
    }

    override fun stopSound() {
        if (!isPlaying) return

        isPlaying = false
        nativeStopSound(synthesizerPtr)
    }

    override fun release() {
        isPlaying = false
        if (synthesizerPtr != 0L) {
            nativeRelease(synthesizerPtr)
            synthesizerPtr = 0
        }
    }

    protected fun finalize() {
        if (synthesizerPtr != 0L) {
            release()
        }
    }
}
