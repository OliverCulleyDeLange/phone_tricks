package ocd.phonetricks.audio

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
}

/**
 * Waveform types for the synthesizer.
 */
enum class Waveform {
    SINE,
    SQUARE,
    TRIANGLE,
    SAWTOOTH
}

/**
 * Expect function to get the platform-specific audio manager implementation.
 */
expect fun createAudioManager(): AudioManager
