package net.torvald.terrarum.mapgenerator

import net.torvald.colourutil.Col4096
import net.torvald.random.HQRNG
import org.newdawn.slick.Color
import java.util.*

/**
 * Created by minjaesong on 16-02-23.
 */
object RoguelikeRandomiser {

    val POTION_PRIMARY_COLSET = intArrayOf(15, 15, 7, 7, 0, 0)

    var potionColours: HashMap<Int, Col4096> = HashMap()
    var coloursDiscovered: HashMap<Col4096, Boolean> = HashMap()

    val coloursTaken: ArrayList<Col4096> = ArrayList()

    var seed: Long = 0
    private val random: Random = HQRNG()

    private val POTION_HEAL_TIER1 = 0x00
    private val POTION_HEAL_TIRE2 = 0x01

    private val POTION_MAGIC_REGEN_TIER1 = 0x10

    private val POTION_BERSERK_TIER1 = 0x20



    fun setupColours() {

    }

    /**
     * For external classes/objects, does not touch COLOUR SET
     * @param arr Array of Int(0-15)
     */
    fun composeColourFrom(arr: IntArray): Color {
        val colourElements = arr.copyOf()
        shuffleArrayInt(colourElements, HQRNG())

        val colourStack = IntArrayStack(colourElements)

        return Col4096(colourStack.pop(),
                       colourStack.pop(),
                       colourStack.pop())
                .toSlickColour()
    }



    fun shuffleArrayInt(ar: IntArray, rnd: Random) {
        for (i in ar.size - 1 downTo 0) {
            val index = rnd.nextInt(i + 1);
            // Simple swap
            val a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

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

}