package net.torvald.random

import java.util.*

/**
 * Created by minjaesong on 2016-02-03.
 */
open class FudgeDice
/**
 * Define new set of Fudge dice with given counts.
 * @param randfunc java.util.Random or its extension
 * *
 * @param counts amount of die
 */
(private val randfunc: Random, val diceCount: Int) {

    /**
     * Roll dice and get result.
     * @return Normally distributed integer [-N , N] for diceCount of N. 0 is the most frequent return.
     */
    fun roll(): Int {
        var diceResult = 0
        for (c in 0 until diceCount) {
            diceResult += rollSingleDie()
        }

        return diceResult
    }

    /**
     * Roll dice and get result, for array index
     * @return Normally distributed integer [0 , N] for N = 2 × DiceCounts + 1. (diceCount) is the most frequent return.
     */
    fun rollForArray(): Int {
        return roll() + diceCount
    }

    fun <T> rollForArray(array: Array<T>): T = array[rollForArray()]
    fun rollForArray(intArray: IntArray): Int = intArray[rollForArray()]
    fun rollForArray(longArray: LongArray): Long = longArray[rollForArray()]
    fun rollForArray(floatArray: FloatArray): Float = floatArray[rollForArray()]
    fun rollForArray(doubleArray: DoubleArray): Double = doubleArray[rollForArray()]

    val sizeOfProbabilityRange: Int
        get() = 2 * diceCount + 1

    /**
     * @return integer randomly picked from {-1, 0, 1}
     */
    private fun rollSingleDie(): Int {
        return randfunc.nextInt(3) - 1
    }
}
