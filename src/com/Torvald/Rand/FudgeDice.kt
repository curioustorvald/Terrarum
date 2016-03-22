package com.Torvald.Rand

import java.util.Random

/**
 * Created by minjaesong on 16-02-03.
 */
open class FudgeDice
/**
 * Define new set of fudge dice with given counts.
 * @param randfunc java.util.Random or its extension
 * *
 * @param counts amount of die
 */
(private val randfunc: Random, val diceCounts: Int) {

    /**
     * Roll dice and get result.
     * @return Normal distributed integer [-N , N] for diceCount of N. 0 is the most frequent return.
     */
    fun roll(): Int {
        var diceResult = 0
        for (c in 0..diceCounts - 1) {
            diceResult += rollSingleDie()
        }

        return diceResult
    }

    /**
     * Roll dice and get result, for array index
     * @return Normal distributed integer [0 , N] for N = 2 × DiceCounts + 1. 0 is the most frequent return.
     */
    fun rollForArray(): Int {
        return roll() + diceCounts
    }

    val sizeOfProbabilityRange: Int
        get() = 2 * diceCounts + 1

    /**
     * @return integer randomly picked from {-1, 0, 1}
     */
    private fun rollSingleDie(): Int {
        return randfunc.nextInt(3) - 1
    }
}
