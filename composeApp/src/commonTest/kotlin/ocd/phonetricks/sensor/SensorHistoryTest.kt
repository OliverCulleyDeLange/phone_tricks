package ocd.phonetricks.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class SensorHistoryTest {

    @Test
    fun growsBelowCap() {
        val out = appendBounded(listOf(1, 2, 3), 4, maxSize = 5)
        assertEquals(listOf(1, 2, 3, 4), out)
    }

    @Test
    fun emptyHistory() {
        assertEquals(listOf(7), appendBounded(emptyList(), 7, maxSize = 3))
    }

    @Test
    fun atCapDropsOldest() {
        val out = appendBounded(listOf(1, 2, 3), 4, maxSize = 3)
        assertEquals(listOf(2, 3, 4), out)
    }

    @Test
    fun overCapDropsAllExtras() {
        // History longer than maxSize (e.g. cap shrunk between calls): keep
        // the most recent maxSize-1 entries plus the new reading.
        val out = appendBounded(listOf(1, 2, 3, 4, 5), 6, maxSize = 3)
        assertEquals(listOf(4, 5, 6), out)
    }

    @Test
    fun zeroMaxSizeDropsEverything() {
        assertEquals(emptyList(), appendBounded(listOf(1, 2), 3, maxSize = 0))
    }

    @Test
    fun returnsNewListInstance() {
        // The Flow-based caller relies on identity inequality to publish
        // updates; a snapshot must not alias the input list.
        val src = listOf(1, 2)
        val out = appendBounded(src, 3, maxSize = 5)
        assertNotSame(src, out)
    }

    @Test
    fun rollingWindowOverManyEmissions() {
        // Simulate the per-sensor pipeline: keep emitting and assert the
        // window stays bounded at exactly maxSize after warmup.
        var history: List<Int> = emptyList()
        for (i in 1..1000) history = appendBounded(history, i, maxSize = 100)
        assertEquals(100, history.size)
        assertEquals(901, history.first())
        assertEquals(1000, history.last())
    }
}
