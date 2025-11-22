package ocd.phonetricks.audio

/**
 * Platform-specific audio manager for playing synthesized sounds.
 */
interface AudioManager {
    /**
     * Play a short tap sound (synthesized sine wave).
     */
    fun playTapSound()

    /**
     * Release any audio resources.
     */
    fun release()
}

/**
 * Expect function to get the platform-specific audio manager implementation.
 */
expect fun createAudioManager(): AudioManager
