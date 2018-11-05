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

    val buffer: Array<T> = arrayOfNulls<Any>(size) as Array<T>
    var tail: Int = 0
    var head: Int = 0

    val lastIndex = size - 1

    val elemCount: Int
        get() = if (tail >= head) tail - head else size

    fun add(item: T) {
        buffer[tail] = item // overwrites oldest item when eligible
        tail = (tail + 1) % size
        if (tail == head) {
            head = (head + 1) % size
        }
    }

    inline fun forEach(action: (T) -> Unit) {
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

    // FIXME not working as intended
    inline fun <R> fold(initial: R, operation: (R, T) -> R): R {
        var accumulator = initial
        //for (element in buffer) accumulator = operation(accumulator, element)
        if (tail >= head) {
            for (i in head..tail - 1)
                operation(accumulator, buffer[i])
        }
        else {
            for (i in 0..size - 1)
                operation(accumulator, buffer[(i + head) % size])
        }

        return accumulator
    }

    inline fun forEachConcurrent(action: (T) -> Unit) {
        TODO()
    }

    inline fun forEachConcurrentWaitFor(action: (T) -> Unit) {
        TODO()
    }

    override fun toString(): String {
        return "CircularArray(size=" + buffer.size + ", head=" + head + ", tail=" + tail + ")"
    }
}