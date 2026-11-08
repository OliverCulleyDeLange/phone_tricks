package ocd.phonetricks.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine

/**
 * The app has one hardware audio graph. Synth playback, sample playback, and
 * microphone capture attach nodes to this engine instead of competing with
 * separate AVAudioEngine instances for the same input/output route.
 */
@OptIn(ExperimentalForeignApi::class)
internal object SharedIosAudioEngine {
    val engine = AVAudioEngine()

    private var ownerCount = 0

    fun retain() {
        if (ownerCount == 0) configureSharedAudioSession()
        ownerCount++
    }

    fun ensureRunning() {
        try {
            engine.startAndReturnError(null)
        } catch (_: Throwable) {
            // Callers keep their state recoverable if the route is temporarily
            // unavailable. A later play/record attempt retries the start.
        }
    }

    fun release() {
        if (ownerCount > 0) ownerCount--
        if (ownerCount == 0) engine.stop()
    }
}
