package net.torvald.util

import java.util.*

class IntArrayStack {
    /**
     * Number of elements in the stack
     */
    var depth: Int = 0
        private set

    var size: Int
        get() = data.size
        set(newSize) {
            if (newSize > depth) inflate(newSize - data.size)
            else                 deflate(data.size - newSize)
        }

    private lateinit var data: IntArray

    fun isEmpty() = depth == 0
    fun isNotEmpty() = depth > 0

    constructor(stackSize: Int) {
        data = IntArray(stackSize)
    }

    constructor(arr: IntArray) {
        data = arr.copyOf()
        depth = size
    }

    fun push(v: Int) {
        if (depth >= data.size) throw StackOverflowError()
        data[depth++] = v
    }

    fun pop(): Int {
        if (depth == 0) throw EmptyStackException()
        return data[--depth]
    }

    fun peek(): Int? {
        if (depth == 0) return null
        return data[depth - 1]
    }

    fun dup() {
        if (depth == 0)         throw EmptyStackException()
        if (depth == data.size) throw StackOverflowError()
        push(peek()!!)
    }

    fun swap() {
        if (depth < 2) throw UnsupportedOperationException("Stack is empty or has only one element.")
        val up = pop()
        val dn = pop()
        push(up)
        push(dn)
    }

    fun drop() {
        if (depth == 0) throw EmptyStackException()
        --depth
    }

    fun defineFromArray(arr: IntArray) { data = arr.copyOf() }

    /**
     * Increase the stack size by a factor.
     */
    fun inflate(sizeToAdd: Int) {
        if (sizeToAdd < 0) throw UnsupportedOperationException("$sizeToAdd: Cannot deflate the stack with this function. Use deflate(int) instead.")
        size += sizeToAdd
        val oldStack = this.asArray()
        data = IntArray(size, { if (it < oldStack.size) oldStack[it] else 0 })
    }

    /**
     * Decrease the stack size by a factor. Overflowing data will be removed.
     */
    fun deflate(sizeToTake: Int) {
        if (size - sizeToTake < 1) throw UnsupportedOperationException("$sizeToTake: Cannot deflate the stack to the size of zero or negative.")
        size -= sizeToTake
        val oldStack = this.asArray()
        data = IntArray(size, { oldStack[it] })
        if (depth > data.size) depth = data.size
    }

    /**
     * Convert stack as array. Index zero is the bottommost element.
     * @return array of data, with array size equivalent to the stack depth.
     */
    fun asArray() = data.copyOfRange(0, depth - 1)

    fun equalTo(other: IntArrayStack) = (this.asArray() == other.asArray())

    fun plus()  { data[depth - 2] += pop() }
    fun minus() { data[depth - 2] -= pop() }
    fun times() { data[depth - 2] *= pop() }
    fun div()   { data[depth - 2] /= pop() }
    fun mod()   { data[depth - 2] %= pop() }
}