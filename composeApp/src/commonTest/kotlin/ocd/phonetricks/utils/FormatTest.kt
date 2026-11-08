package ocd.phonetricks.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test fun positiveWhole() = assertEquals("3.0", formatOneDecimal(3f))
    @Test fun positiveFraction() = assertEquals("3.4", formatOneDecimal(3.4f))
    @Test fun positiveRoundsUp() = assertEquals("3.5", formatOneDecimal(3.45f))
    @Test fun positiveRoundsDown() = assertEquals("3.4", formatOneDecimal(3.44f))
    @Test fun zero() = assertEquals("0.0", formatOneDecimal(0f))
    @Test fun negativeWhole() = assertEquals("-2.0", formatOneDecimal(-2f))
    @Test fun negativeFraction() = assertEquals("-2.5", formatOneDecimal(-2.5f))
    @Test fun negativeRoundsTowardZero() = assertEquals("-2.4", formatOneDecimal(-2.44f))
    @Test fun smallFraction() = assertEquals("0.1", formatOneDecimal(0.1f))
    @Test fun negativeSmallFraction() = assertEquals("-0.1", formatOneDecimal(-0.1f))
}
