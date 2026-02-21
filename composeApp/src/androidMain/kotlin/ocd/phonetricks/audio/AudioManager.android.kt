package ocd.phonetricks.audio

actual fun createAudioManager(): AudioManager = AndroidAudioManager()

class AndroidAudioManager : AudioManager {
    companion object {
        init {
            System.loadLibrary("superpoweredSynth")
        }
    }

    private external fun nativeInit(): Long
    private external fun nativePlaySound(synthPtr: Long, frequency: Float, amplitude: Float, waveformA: Int, waveformB: Int, blend: Float)
    private external fun nativeStopSound(synthPtr: Long)
    private external fun nativeRelease(synthPtr: Long)

    private var synthesizerPtr: Long = 0
    private var isPlaying = false

    init {
        synthesizerPtr = nativeInit()
    }

    override fun playSynthSound(
        frequency: Float,
        amplitude: Float,
        waveformA: Waveform,
        waveformB: Waveform,
        blend: Float,
    ) {
        if (!isPlaying) isPlaying = true
        nativePlaySound(synthesizerPtr, frequency, amplitude, waveformTypeToInt(waveformA), waveformTypeToInt(waveformB), blend)
    }

    private fun waveformTypeToInt(waveform: Waveform): Int = when (waveform) {
        Waveform.SINE -> 0
        Waveform.SQUARE -> 1
        Waveform.TRIANGLE -> 2
        Waveform.SAWTOOTH -> 3
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
        if (synthesizerPtr != 0L) release()
    }
}
