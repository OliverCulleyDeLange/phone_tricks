package ocd.phonetricks.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class TimeUtilsTest {
    @Test
    fun formatTimestampForFilenameIsLocalAndZeroPadded() {
        // 1970-01-01T00:00:00Z, formatted in local time. Without knowing the JVM's
        // TZ we just assert structure: YYYY-MM-DD_HH-MM-SS, 19 chars, zero-padded.
        val s = formatTimestampForFilename(0L)
        assertTrue(s.length == 19, "expected 19 chars, got '$s'")
        assertTrue(s[4] == '-' && s[7] == '-' && s[10] == '_' && s[13] == '-' && s[16] == '-',
            "unexpected separators: '$s'")
        // All other characters must be digits.
        s.forEachIndexed { i, c ->
            if (i !in setOf(4, 7, 10, 13, 16)) {
                assertTrue(c.isDigit(), "non-digit '$c' at index $i in '$s'")
            }
        }
    }
}
