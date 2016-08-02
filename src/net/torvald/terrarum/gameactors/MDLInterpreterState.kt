package net.torvald.terrarum.gameactors

import net.torvald.terrarum.mapgenerator.RoguelikeRandomiser
import java.util.*

/**
 * Provides MDL interpretation, pre-compilation and stores state of the interpreter
 *
 * Created by minjaesong on 16-07-30.
 */
class MDLInterpreterState {
    val stack = MagicArrayStack(20)

    fun interpret(line: String) {

    }

    fun execute(property: MagicWords, power: MagicWords? = null, arg: Int? = null) {

    }














    enum class MagicWords {
        // properties
        ELDR, IS, STORMR, HREYFING, LAEKNING, GLEYPI, TJON,
        //fire, ice, storm, kinesis,    heal, absorb, harm
        // reserved words
        LAEKNINGHRADI, HREYFINGHRADI, LAEKNINGAUKI, HREYFINGAUKI, STOEKKAUKI, HEILSASTIG,
        //  heal rate,movement speed, healratemult,movespeedmult, jump boost, health point
        // adjectives (power)

        // operators
        ITA, POP, PLUS, MINUS, SINNUM, DEILING, LEIFASTOFN, AFRIT, TALASKIPTI, HENDA
        // push, pop, +,    -,      *,       /,          %,   dup,       swap,  drop
    }

    class MagicOrInt() {
        private var magic: MagicWords? = null
        private var number: Int? = null

        constructor(kynngi: MagicWords): this() {
            magic = kynngi
        }

        constructor(integer: Int) : this() {
            number = integer
        }

        fun toMagic() = if (magic != null) magic!! else throw TypeCastException("$this: cannot be cast to MagicWord")
        fun toInt() = if (number != null) number!! else throw TypeCastException("$this: cannot be cast to MagicWord")

        fun isMagic() = (magic != null)
        fun isInt() = (number != null)

        override fun toString(): String = if (magic != null && number == null) "$magic" else if (magic == null && number != null) "$number" else "INVALID"
    }

    class MagicArrayStack {
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

        private lateinit var data: Array<MagicOrInt?>

        constructor(stackSize: Int) {
            data = Array(stackSize, { null })
        }

        constructor(arr: Array<MagicOrInt?>) {
            data = arr.copyOf()
            depth = size
        }

        fun push(v: MagicOrInt) {
            if (depth >= data.size) throw StackOverflowError()
            data[depth++] = v
        }

        fun pop(): MagicOrInt {
            if (depth == 0) throw EmptyStackException()
            return data[--depth]!!
        }

        fun peek(): MagicOrInt? {
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

        fun defineFromArray(arr: Array<MagicOrInt?>) { data = arr.copyOf() }

        /**
         * Increase the stack size by a factor.
         */
        fun inflate(sizeToAdd: Int) {
            if (sizeToAdd < 0) throw UnsupportedOperationException("$sizeToAdd: Cannot deflate the stack with this function. Use deflate(int) instead.")
            size += sizeToAdd
            val oldStack = this.asArray()
            data = Array(size, { if (it < oldStack.size) oldStack[it] else null })
        }

        /**
         * Decrease the stack size by a factor. Overflowing data will be removed.
         */
        fun deflate(sizeToTake: Int) {
            if (size - sizeToTake < 1) throw UnsupportedOperationException("$sizeToTake: Cannot deflate the stack to the size of zero or negative.")
            size -= sizeToTake
            val oldStack = this.asArray()
            data = Array(size, { oldStack[it] })
            if (depth > data.size) depth = data.size
        }

        /**
         * Convert stack as array. Index zero is the bottommost element.
         * @return array of data, with array size equivalent to the stack depth.
         */
        fun asArray() = data.copyOfRange(0, depth - 1)

        fun equalTo(other: MagicArrayStack) = (this.asArray() == other.asArray())

        fun plus()  { if (data[depth - 2]!!.isInt() && peek()!!.isInt()) data[depth - 2] = MagicOrInt(data[depth - 2]!!.toInt() + (pop().toInt())) else throw RuntimeException("${data[depth - 2]}: Cannot do arithmetic operation on non-numeric type.") }
        fun minus() { if (data[depth - 2]!!.isInt() && peek()!!.isInt()) data[depth - 2] = MagicOrInt(data[depth - 2]!!.toInt() - (pop().toInt())) else throw RuntimeException("${data[depth - 2]}: Cannot do arithmetic operation on non-numeric type.") }
        fun times() { if (data[depth - 2]!!.isInt() && peek()!!.isInt()) data[depth - 2] = MagicOrInt(data[depth - 2]!!.toInt() * (pop().toInt())) else throw RuntimeException("${data[depth - 2]}: Cannot do arithmetic operation on non-numeric type.") }
        fun div()   { if (data[depth - 2]!!.isInt() && peek()!!.isInt()) data[depth - 2] = MagicOrInt(data[depth - 2]!!.toInt() / (pop().toInt())) else throw RuntimeException("${data[depth - 2]}: Cannot do arithmetic operation on non-numeric type.") }
        fun mod()   { if (data[depth - 2]!!.isInt() && peek()!!.isInt()) data[depth - 2] = MagicOrInt(data[depth - 2]!!.toInt() % (pop().toInt())) else throw RuntimeException("${data[depth - 2]}: Cannot do arithmetic operation on non-numeric type.") }
    }
}