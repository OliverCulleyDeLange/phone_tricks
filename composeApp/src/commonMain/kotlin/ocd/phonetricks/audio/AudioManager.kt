package ocd.phonetricks.audio

import kotlinx.serialization.Serializable

/**
 * Platform-specific audio manager for playing synthesized sounds.
 */
interface AudioManager {
    /**
     * Play a sound with the specified parameters.
     *
     * @param frequency The base frequency in Hz (20-20000)
     * @param amplitude The volume level (0.0-1.0)
     * @param waveformA The starting waveform
     * @param waveformB The ending waveform to blend toward
     * @param blend Crossfade factor: 0.0 = fully waveformA, 1.0 = fully waveformB
     */
    fun playSynthSound(
        frequency: Float,
        amplitude: Float,
        waveformA: Waveform = Waveform.SINE,
        waveformB: Waveform = Waveform.SINE,
        blend: Float = 0f,
    )

    /**
     * Stop all sounds.
     */
    fun stopSound()

    /**
     * Release any audio resources.
     */
    fun release()

    /**
     * Set an audio effect.
     *
     * @param effect The audio effect to apply
     * @param wetDry The mix level of the effect (0.0-1.0)
     */
    fun setEffect(effect: AudioEffect, wetDry: Float)

    /**
     * Set a filter preset.
     *
     * @param preset The filter preset to apply
     * @param frequency The cutoff frequency for the filter
     * @param wetDry The mix level of the filter (0.0-1.0)
     */
    fun setFilter(preset: FilterPreset, frequency: Float, wetDry: Float)

    fun getSpectrumData(): FloatArray

    fun setEqBands(bands: List<EqBand>)
}

@Serializable
data class EqBand(
    val id: Int,
    val frequency: Float,
    val gainDb: Float,
    val q: Float = 1.4f,
)

/**
 * Waveform types for the synthesizer.
 */
@Serializable
enum class Waveform {
    SINE,
    SQUARE,
    TRIANGLE,
    SAWTOOTH
}

/**
 * Audio effects that can be applied to sounds.
 */
@Serializable
enum class AudioEffect {
    ECHO,
    DELAY,
    BITCRUSHER,
    REVERB,
    WHOOSH,
    GUITAR_DISTORTION;

    /**
     * Get the display name for the effect.
     */
    fun displayName(): String = when (this) {
        ECHO -> "Echo"
        DELAY -> "Delay"
        BITCRUSHER -> "Bitcrusher"
        REVERB -> "Reverb"
        WHOOSH -> "Whoosh"
        GUITAR_DISTORTION -> "Distortion"
    }
}

/**
 * Filter presets for shaping audio frequency response.
 */
@Serializable
enum class FilterPreset {
    LOW_PASS,
    HIGH_PASS,
    LOW_SHELF,
    HIGH_SHELF,
    BANDPASS,
    NOTCH,
    PARAMETRIC;

    /**
     * Get the display name for the filter preset.
     */
    fun displayName(): String = when (this) {
        LOW_PASS -> "Low-Pass"
        HIGH_PASS -> "High-Pass"
        LOW_SHELF -> "Low-Shelf"
        HIGH_SHELF -> "High-Shelf"
        BANDPASS -> "Bandpass"
        NOTCH -> "Notch"
        PARAMETRIC -> "Parametric"
    }
}

/**
 * Expect function to get the platform-specific audio manager implementation.
 */
expect fun createAudioManager(): AudioManager
