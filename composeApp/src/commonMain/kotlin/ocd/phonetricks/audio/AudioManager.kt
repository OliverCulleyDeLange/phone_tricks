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
     * @param waveform The type of waveform to generate (sine, square, etc.)
     */
    fun playSynthSound(frequency: Float, amplitude: Float, waveform: Waveform = Waveform.SINE)

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
