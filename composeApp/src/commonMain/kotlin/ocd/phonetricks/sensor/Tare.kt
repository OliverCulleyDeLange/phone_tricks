package ocd.phonetricks.sensor

import ocd.phonetricks.data.RotationVector
import kotlin.math.sqrt

/**
 * Multiply [reading] by the inverse of [tareQuat] (Hamilton product) so that
 * the device's current pose is treated as the identity rotation. Returns the
 * raw reading unchanged if [tareQuat] is null.
 */
fun applyTare(reading: RotationVector, tareQuat: RotationVector?): RotationVector {
    if (tareQuat == null) return reading

    val x = reading.x
    val y = reading.y
    val z = reading.z
    val w = reading.scalar ?: computeScalar(x.toDouble(), y.toDouble(), z.toDouble())

    val tareX = tareQuat.x
    val tareY = tareQuat.y
    val tareZ = tareQuat.z
    val tareW = tareQuat.scalar ?: computeScalar(tareX.toDouble(), tareY.toDouble(), tareZ.toDouble())

    val tareInvX = -tareX
    val tareInvY = -tareY
    val tareInvZ = -tareZ
    val tareInvW = tareW

    val resultW = tareInvW * w - tareInvX * x - tareInvY * y - tareInvZ * z
    val resultX = tareInvW * x + tareInvX * w + tareInvY * z - tareInvZ * y
    val resultY = tareInvW * y - tareInvX * z + tareInvY * w + tareInvZ * x
    val resultZ = tareInvW * z + tareInvX * y - tareInvY * x + tareInvZ * w

    return RotationVector(
        timestampMs = reading.timestampMs,
        x = resultX,
        y = resultY,
        z = resultZ,
        scalar = resultW,
    )
}

/**
 * Compute the scalar (w) component of a unit quaternion given its (x, y, z)
 * components, assuming sum-of-squares ≤ 1. Returns 0 if the inputs are
 * not normalized.
 */
fun computeScalar(x: Double, y: Double, z: Double): Float {
    val sumSquares = x * x + y * y + z * z
    return if (sumSquares < 1.0) {
        sqrt(1.0 - sumSquares).toFloat()
    } else {
        0f
    }
}
