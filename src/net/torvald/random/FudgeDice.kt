package net.torvald.random

import java.util.Random

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
        for (c in 0..diceCount - 1) {
            diceResult += rollSingleDie()
        }

        return diceResult
    }

    /**
     * Roll dice and get result, for array index
     * @return Normally distributed integer [0 , N] for N = 2 × DiceCounts + 1. 0 is the most frequent return.
     */
    fun rollForArray(): Int {
        return roll() + diceCount
    }

    val sizeOfProbabilityRange: Int
        get() = 2 * diceCount + 1

    /**
     * @return integer randomly picked from {-1, 0, 1}
     */
    private fun rollSingleDie(): Int {
        return randfunc.nextInt(3) - 1
    }
}
