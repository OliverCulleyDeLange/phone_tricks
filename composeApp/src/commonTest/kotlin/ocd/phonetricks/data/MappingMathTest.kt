package ocd.phonetricks.data

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MappingMathTest {

    @Test
    fun normalizeMapsRangeToZeroOne() {
        assertEquals(0f, normalizeSurfaceValue(0f, 0f, 1f))
        assertEquals(0.5f, normalizeSurfaceValue(0.5f, 0f, 1f))
        assertEquals(1f, normalizeSurfaceValue(1f, 0f, 1f))
    }

    @Test
    fun normalizeHandlesNegativeRange() {
        assertEquals(0f, normalizeSurfaceValue(-10f, -10f, 10f))
        assertEquals(0.5f, normalizeSurfaceValue(0f, -10f, 10f))
        assertEquals(1f, normalizeSurfaceValue(10f, -10f, 10f))
    }

    @Test
    fun normalizeClampsOutOfRange() {
        assertEquals(0f, normalizeSurfaceValue(-50f, -10f, 10f))
        assertEquals(1f, normalizeSurfaceValue(50f, -10f, 10f))
    }

    @Test
    fun normalizeReturnsHalfForEmptyRange() {
        assertEquals(0.5f, normalizeSurfaceValue(7f, 5f, 5f))
    }

    @Test
    fun emptyMappingsReturnsNull() {
        assertNull(computeVolumeAmplitude(emptyList()))
    }

    @Test
    fun singleMappingScalesToItsRange() {
        val v = computeVolumeAmplitude(
            listOf(0.5f to ControlParameter.Volume(min = 0.2f, max = 0.6f))
        )
        assertNear(0.4f, v!!)
    }

    @Test
    fun multipleMappingsAverageScaledOutputs() {
        // Mapping A: t=1.0 → 0.5
        // Mapping B: t=0.0 → 0.5
        // Average: 0.5
        val v = computeVolumeAmplitude(
            listOf(
                1f to ControlParameter.Volume(min = 0f, max = 0.5f),
                0f to ControlParameter.Volume(min = 0.5f, max = 1f),
            )
        )
        assertNear(0.5f, v!!)
    }

    @Test
    fun secondMappingsRangeIsHonoured() {
        // Regression for the original bug: two mappings with very different
        // ranges should both contribute. If the first mapping's range alone
        // were used we'd never see anything beyond 0.5 here.
        val v = computeVolumeAmplitude(
            listOf(
                0f to ControlParameter.Volume(min = 0f, max = 0.5f),
                1f to ControlParameter.Volume(min = 0.8f, max = 1f),
            )
        )!!
        // (0 + 1) / 2 = 0.5, where output is (0 + 1) / 2 = 0.5. So 0.5
        // expected. But if ranges were collapsed onto the first mapping's
        // (0..0.5) we'd get the average of normalized t (0.5) → 0.25 instead.
        assertNear(0.5f, v)
        assertTrue(v > 0.25f, "second mapping's range was ignored")
    }

    private fun assertNear(expected: Float, actual: Float, tol: Float = 1e-5f) {
        assertTrue(abs(expected - actual) < tol, "expected ~$expected, got $actual")
    }
}
