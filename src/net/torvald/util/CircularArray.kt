package net.torvald.util

import java.util.*


/**
 * buffer[head] contains the most recent item, whereas buffer[tail] contains the oldest one.
 *
 * Notes for particle storage:
 *      Particles does not need to be removed, just let it overwrite as their operation is rather
 *      lightweight. So, just flagDespawn = true if it need to be "deleted" so that it won't update
 *      anymore.
 *
 * Created by minjaesong on 2017-01-22.
 */
class CircularArray<T>(val size: Int, val overwriteOnOverflow: Boolean): Iterable<T> {

    /**
     * What to do RIGHT BEFORE old element is being overridden by the new element (only makes sense when ```overwriteOnOverflow = true```)
     *
     * This function will not be called when ```removeHead()``` or ```removeTail()``` is called.
     */
    var overwritingPolicy: (T) -> Unit = {
        // do nothing
    }

    val buffer: Array<T> = arrayOfNulls<Any>(size) as Array<T>

    /** Tail stands for the oldest element. The tail index points AT the tail element */
    var tail: Int = 0; private set
    /** Head stands for the youngest element. The head index points AFTER the head element */
    var head: Int = 0; private set

    private var overflow = false

    val lastIndex = size - 1

    /**
     * Number of elements that forEach() or fold() would iterate.
     */
    val elemCount: Int
        get() = if (overflow) size else head - tail
    val isEmpty: Boolean
        get() = !overflow && head == tail

    private inline fun incHead() { head = (head + 1).wrap() }
    private inline fun decHead() { head = (head - 1).wrap() }
    private inline fun incTail() { tail = (tail + 1).wrap() }
    private inline fun decTail() { tail = (tail - 1).wrap() }

    fun clear() {
        tail = 0
        head = 0
        overflow = false
    }

    /**
     * When the overflowing is enabled, tail element (ultimate element) will be changed into the penultimate element.
     */
    fun appendHead(item: T) {
        if (overflow && !overwriteOnOverflow) {
            throw StackOverflowError()
        }
        else {
            if (overflow) {
                overwritingPolicy.invoke(buffer[head])
            }

            buffer[head] = item
            incHead()
        }

        if (overflow) {
            incTail()
        }

        // must be checked AFTER the actual head increment; otherwise this condition doesn't make sense
        if (tail == head) {
            overflow = true
        }
    }

    /**
     * To just casually add items to the list, use [appendHead], **please!**
     */
    fun appendTail(item: T) {
        // even if overflowing is enabled, appending at tail causes head element to be altered, therefore such action
        // must be blocked by throwing overflow error

        // if you think this behaviour is wrong, you're confusing appendHead() with appendTail(). Use appendHead() and removeTail()
        if (overflow) {
            throw StackOverflowError()
        }
        else {
            decTail()
            buffer[tail] = item
        }

        // must be checked AFTER the actual head increment; otherwise this condition doesn't make sense
        if (tail == head) {
            overflow = true
        }
    }

    fun removeHead(): T? {
        if (isEmpty) return null

        decHead()
        overflow = false

        return buffer[head]
    }

    fun removeTail(): T? {
        if (isEmpty) return null

        val ret = buffer[tail]
        incTail()
        overflow = false

        return ret
    }

    /** Returns the youngest (last of the array) element */
    fun getHeadElem(): T? = if (isEmpty) null else buffer[(head - 1).wrap()]
    /** Returns the oldest (first of the array) element */
    fun getTailElem(): T? = if (isEmpty) null else buffer[tail]

    /**
     * Relative-indexed get. Index of zero will return the head element.
     */
    operator fun get(index: Int): T? = buffer[(head - 1 - index).wrap()]

    private fun getAbsoluteRange() =  0 until when {
        head == tail -> buffer.size
        tail >  head -> buffer.size - (((head - 1).wrap()) - tail)
        else         -> head - tail
    }

    override fun iterator(): Iterator<T> {
        if (isEmpty) {
            return object : Iterator<T> {
                override fun next(): T = throw EmptyStackException()
                override fun hasNext() = false
            }
        }

        val rangeMax = getAbsoluteRange().last
        var counter = 0
        return object : Iterator<T> {
            override fun next(): T {
                val ret = buffer[(counter + tail).wrap()]
                counter += 1
                return ret
            }

            override fun hasNext() = (counter <= rangeMax)
        }
    }

    /**
     * Iterates the array with oldest element (tail) first.
     */
    fun forEach(action: (T) -> Unit) {
        // for (element in buffer) action(element)
        // return nothing

        iterator().forEach(action)
    }

    fun <R> fold(initial: R, operation: (R, T) -> R): R {
        // accumulator = initial
        // for (element in buffer) accumulator = operation(accumulator, element)
        // return accumulator

        var accumulator = initial

        if (isEmpty)
            return initial
        else {
            iterator().forEach {
                accumulator = operation(accumulator, it)
            }
        }

        return accumulator
    }

    fun toList(): List<T> {
        val list = ArrayList<T>()
        iterator().forEach { list.add(it) }
        return list.toList()
    }

    private inline fun Int.wrap() = this fmod size

    override fun toString(): String {
        return "CircularArray(size=${buffer.size}, head=$head, tail=$tail, overflow=$overflow)"
    }

    private inline infix fun Int.fmod(other: Int) = Math.floorMod(this, other)
}