package ocd.phonetricks.audio

/**
 * Compute the normalized loop position (0..1) given:
 *
 * - [head]: the AudioTrack's `playbackHeadPosition`, masked to unsigned
 *   32-bit (wraps every ≈13.5 hours at 44.1 kHz).
 * - [loopStart]: the value of `head` captured at the start of the
 *   current loop iteration.
 * - [loopLenSamples]: the length of one loop in output frames.
 *
 * The naïve `head - loopStart` breaks the moment `head` wraps past
 * 2^32 — the previous implementation used `.coerceAtLeast(0L)`, which
 * silently froze the playhead at 0. Mask the difference back into
 * unsigned 32-bit so the wrap is invisible, and modulo by the loop
 * length so the position keeps cycling even if [loopStart] is stale.
 */
fun computePlayPosition(head: Long, loopStart: Long, loopLenSamples: Int): Float {
    val len = loopLenSamples.coerceAtLeast(1)
    val rawDiff = (head - loopStart) and 0xFFFFFFFFL
    val posInLoop = rawDiff % len
    return (posInLoop.toFloat() / len).coerceIn(0f, 1f)
}
