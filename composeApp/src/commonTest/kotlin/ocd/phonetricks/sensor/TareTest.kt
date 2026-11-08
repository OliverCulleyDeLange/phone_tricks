package ocd.phonetricks.sensor

import ocd.phonetricks.data.RotationVector
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TareTest {

    // 0.1² + 0.2² + 0.3² + 0.92736² ≈ 1, so this is a unit quaternion.
    private val unit = RotationVector(timestampMs = 1L, x = 0.1f, y = 0.2f, z = 0.3f, scalar = 0.92736f)

    @Test
    fun nullTareReturnsReadingUnchanged() {
        assertEquals(unit, applyTare(unit, null))
    }

    @Test
    fun selfTareYieldsIdentity() {
        // Applying the inverse of a unit quaternion to itself yields the identity (0, 0, 0, 1).
        val tared = applyTare(unit, unit)
        assertNear(0f, tared.x)
        assertNear(0f, tared.y)
        assertNear(0f, tared.z)
        assertNear(1f, tared.scalar ?: 0f)
    }

    @Test
    fun tarePreservesTimestamp() {
        val tare = RotationVector(timestampMs = 0L, x = 0f, y = 0f, z = 0f, scalar = 1f)
        val reading = RotationVector(timestampMs = 4242L, x = 0.1f, y = 0f, z = 0f, scalar = 0.99f)
        assertEquals(4242L, applyTare(reading, tare).timestampMs)
    }

    @Test
    fun identityTareReturnsReadingUnchanged() {
        // Tare = identity (0,0,0,1). The result should equal the input.
        val tare = RotationVector(timestampMs = 0L, x = 0f, y = 0f, z = 0f, scalar = 1f)
        val tared = applyTare(unit, tare)
        assertNear(unit.x, tared.x)
        assertNear(unit.y, tared.y)
        assertNear(unit.z, tared.z)
        assertNear(unit.scalar ?: 0f, tared.scalar ?: 0f)
    }

    @Test
    fun computeScalarRecoversWFromUnitQuat() {
        // For a unit quaternion (0.1, 0.2, 0.3, w): w = sqrt(1 - 0.14)
        val w = computeScalar(0.1, 0.2, 0.3)
        assertNear(0.92736f, w)
    }

    @Test
    fun computeScalarReturnsZeroWhenNotNormalized() {
        // Sum of squares > 1 → non-normalized → returns 0 by contract.
        assertEquals(0f, computeScalar(1.0, 1.0, 1.0))
    }

    @Test
    fun missingScalarUsesComputedW() {
        // If the reading omits w, applyTare reconstructs it from the unit
        // constraint. Self-tare should still collapse to identity.
        val q = RotationVector(timestampMs = 0L, x = 0.1f, y = 0.2f, z = 0.3f, scalar = null)
        val tared = applyTare(q, q)
        assertNear(0f, tared.x)
        assertNear(0f, tared.y)
        assertNear(0f, tared.z)
        assertNear(1f, tared.scalar ?: 0f)
    }

    private fun assertNear(expected: Float, actual: Float, tol: Float = 1e-4f) {
        assertTrue(abs(expected - actual) < tol, "expected ~$expected, got $actual")
    }
}
