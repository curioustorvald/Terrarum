package net.torvald.dataclass


/**
 * Notes for particle storage:
 *      Particles does not need to be removed, just let it overwrite as their operation is rather
 *      lightweight. So, just flagDespawn = true if it need to be "deleted" so that it won't update
 *      anymore.
 *
 * Created by minjaesong on 2017-01-22.
 */
class CircularArray<T>(val size: Int) {

    private val buffer: Array<T> = arrayOfNulls<Any>(size) as Array<T>
    private var tail: Int = 0
    private var head: Int = 0

    val elemCount: Int
        get() = if (tail >= head) tail - head else size

    fun add(item: T) {
        buffer[tail] = item // overwrites oldest item when eligible
        tail = (tail + 1) % size
        if (tail == head) {
            head = (head + 1) % size
        }
    }

    fun forEach(action: (T) -> Unit) {
        /*if (tail >= head) { // queue not full
            (head..tail - 1).map { buffer[it] }.forEach { action(it) }
        }
        else { // queue full
            (0..size - 1).map { buffer[(it + head) % size] }.forEach { action(it) }
        }*/

        // has slightly better iteration performance than lambda
        if (tail >= head) {
            for (i in head..tail - 1)
                action(buffer[i])
        }
        else {
            for (i in 0..size - 1)
                action(buffer[(i + head) % size])
        }
    }

    fun forEachConcurrent(action: (T) -> Unit) {
        TODO()
    }

    fun forEachConcurrentWaitFor(action: (T) -> Unit) {
        TODO()
    }

    override fun toString(): String {
        return "CircularArray(size=" + buffer.size + ", head=" + head + ", tail=" + tail + ")"
    }
}