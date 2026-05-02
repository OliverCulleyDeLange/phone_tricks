@file:OptIn(kotlin.time.ExperimentalTime::class)

package ocd.phonetricks.utils

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Get the current time in milliseconds since epoch.
 * Platform-specific implementation.
 */
expect fun currentTimeMillis(): Long

fun formatTimestampForFilename(timestampMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestampMs)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val year = localDateTime.year
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')

    return "${year}-${month}-${day}_${hour}-${minute}-${second}"
}
