package ocd.phonetricks.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive

/**
 * Configure the shared AVAudioSession for the app. Without this:
 *
 * - Playback can be silenced by the ringer switch.
 * - The synth refuses to play while another app holds the audio focus.
 * - Recording fails because the default category is playback-only.
 *
 * Both IOSAudioManager and IOSSamplePlayer must call this before
 * starting their AVAudioEngine. Repeated calls are safe.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun configureSharedAudioSession() {
    val session = AVAudioSession.sharedInstance()
    try {
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        session.setActive(true, error = null)
    } catch (_: Throwable) {
        // Best-effort. If the session cannot be configured (e.g. another
        // app holds an exclusive category) we let the engine attempt to
        // start anyway — failures surface through startAndReturnError.
    }
}

/**
 * Trigger the mic permission prompt if the user hasn't decided yet.
 * Repeated calls are safe — iOS only shows the system alert on the
 * first call. The callback fires immediately if the permission state
 * is already known.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun requestMicrophonePermission(onResult: (Boolean) -> Unit) {
    AVAudioSession.sharedInstance().requestRecordPermission { granted ->
        onResult(granted)
    }
}
