package ocd.phonetricks.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SamplePlayerMathTest {

    @Test
    fun startOfLoopIsZero() {
        assertEquals(0f, computePlayPosition(head = 100L, loopStart = 100L, loopLenSamples = 1000))
    }

    @Test
    fun midLoopIsHalf() {
        assertNear(0.5f, computePlayPosition(head = 600L, loopStart = 100L, loopLenSamples = 1000))
    }

    @Test
    fun zeroLoopLengthClampsToZero() {
        // loopLenSamples may be 0 during construction — the helper guards
        // against div-by-zero by treating 0 as 1, so the result is 0.
        assertEquals(0f, computePlayPosition(head = 50L, loopStart = 0L, loopLenSamples = 0))
    }

    @Test
    fun handlesIntWrapAcross32BitBoundary() {
        // playbackHeadPosition is a signed 32-bit int that wraps after
        // ≈13.5 hours at 44.1 kHz. The Android caller masks both sides
        // into unsigned-32-bit space before calling us. The expected
        // distance from a high loopStart to a low post-wrap head is
        // (2^32 - loopStart) + head.
        val twoPow32 = 1L shl 32
        val loopStart = twoPow32 - 100L  // unsigned 32-bit just before wrap
        val head = 50L                    // unsigned 32-bit just after wrap
        // Expected difference: 100 + 50 = 150 frames
        val pos = computePlayPosition(head = head, loopStart = loopStart, loopLenSamples = 1000)
        assertNear(0.15f, pos)
    }

    @Test
    fun multipleLoopsWithoutLoopStartUpdateStillCycle() {
        // If loopStartFrame is stale and the head has advanced multiple
        // loop lengths beyond it, position must wrap modulo the loop
        // length rather than saturate at 1.0.
        val pos = computePlayPosition(head = 3500L, loopStart = 0L, loopLenSamples = 1000)
        assertNear(0.5f, pos)
    }

    @Test
    fun resultIsAlwaysNormalized() {
        val pos = computePlayPosition(head = 12345L, loopStart = 1000L, loopLenSamples = 1000)
        assertTrue(pos in 0f..1f, "expected 0..1, got $pos")
    }

    private fun assertNear(expected: Float, actual: Float, tol: Float = 1e-4f) {
        assertTrue(abs(expected - actual) < tol, "expected ~$expected, got $actual")
    }
}
