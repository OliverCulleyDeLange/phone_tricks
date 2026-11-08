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
    private external fun nativeSetEffect(synthPtr: Long, effectId: Int, wetDry: Float)
    private external fun nativeSetFilter(synthPtr: Long, presetId: Int, frequency: Float, wetDry: Float)
    private external fun nativeGetSpectrum(synthPtr: Long): FloatArray
    private external fun nativeSetEqBands(synthPtr: Long, frequencies: FloatArray, gains: FloatArray, qs: FloatArray)

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

    override fun setEffect(effect: AudioEffect, wetDry: Float) {
        nativeSetEffect(synthesizerPtr, effect.ordinal, wetDry)
    }

    override fun setFilter(preset: FilterPreset, frequency: Float, wetDry: Float) {
        nativeSetFilter(synthesizerPtr, preset.ordinal, frequency, wetDry)
    }

    override fun getSpectrumData(): FloatArray {
        if (synthesizerPtr == 0L) return FloatArray(512)
        return nativeGetSpectrum(synthesizerPtr)
    }

    override fun setEqBands(bands: List<EqBand>) {
        if (synthesizerPtr == 0L) return
        nativeSetEqBands(
            synthesizerPtr,
            bands.map { it.frequency }.toFloatArray(),
            bands.map { it.gainDb }.toFloatArray(),
            bands.map { it.q }.toFloatArray(),
        )
    }

    override fun release() {
        isPlaying = false
        if (synthesizerPtr != 0L) {
            nativeRelease(synthesizerPtr)
            synthesizerPtr = 0
        }
    }
}
