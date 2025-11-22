package ocd.phonetricks.engine

/**
 * A ring buffer (circular buffer) that automatically overwrites the oldest data
 * when the buffer is full. This is more efficient than constantly removing from
 * the beginning of a list.
 */
class RingBuffer<T>(private val capacity: Int) {
    private val buffer = ArrayList<T>(capacity)
    private var head = 0
    private var size = 0

    /**
     * Add an item to the buffer. If the buffer is full, the oldest item is overwritten.
     */
    fun add(item: T) {
        if (size < capacity) {
            buffer.add(item)
            size++
        } else {
            buffer[head] = item
            head = (head + 1) % capacity
        }
    }

    /**
     * Get an item by index, where 0 is the oldest element and size-1 is the newest.
     * Throws IndexOutOfBoundsException if index is out of range.
     */
    operator fun get(index: Int): T {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for size $size")
        }

        return if (size < capacity) {
            // Buffer not yet full, direct access
            buffer[index]
        } else {
            // Buffer is full, adjust for head position
            buffer[(head + index) % capacity]
        }
    }

    /**
     * Get all items in the buffer in chronological order (oldest to newest).
     */
    fun toList(): List<T> {
        if (size < capacity) {
            return buffer.toList()
        }

        // When buffer is full, reorder so oldest element is first
        val result = ArrayList<T>(capacity)
        for (i in 0 until capacity) {
            result.add(buffer[(head + i) % capacity])
        }
        return result
    }

    /**
     * Get the current number of items in the buffer.
     */
    fun size(): Int = size

    /**
     * Check if the buffer is empty.
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * Clear all items from the buffer.
     */
    fun clear() {
        buffer.clear()
        head = 0
        size = 0
    }
}
