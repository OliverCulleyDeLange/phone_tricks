package ocd.phonetricks.sensor

/**
 * Append [reading] to a bounded history list, capping the result at [maxSize]
 * by dropping the oldest entries. Allocates a single ArrayList per call —
 * the previous implementation (`history + reading` then `takeLast(maxSize)`)
 * allocated twice on every sensor emission.
 */
fun <T> appendBounded(history: List<T>, reading: T, maxSize: Int): List<T> {
    if (maxSize <= 0) return emptyList()
    val cur = history.size
    return if (cur < maxSize) {
        ArrayList<T>(cur + 1).apply {
            addAll(history)
            add(reading)
        }
    } else {
        ArrayList<T>(maxSize).apply {
            for (i in (cur - maxSize + 1) until cur) add(history[i])
            add(reading)
        }
    }
}
