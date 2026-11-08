package ocd.phonetricks.utils

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Format a Float to one decimal place. Multiplatform replacement for
 * `"%.1f".format(value)`, which is JVM-only.
 */
fun formatOneDecimal(value: Float): String {
    val rounded = (value * 10f).roundToInt()
    val sign = if (rounded < 0) "-" else ""
    val abs = rounded.absoluteValue
    val whole = abs / 10
    val frac = abs % 10
    return "$sign$whole.$frac"
}
