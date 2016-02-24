package com.Torvald.Rand;

import java.util.Random;

/**
 * Created by minjaesong on 16-02-03.
 */
public class FudgeDice {

    private Random randfunc;
    private int diceCounts;

    /**
     * Define new set of fudge dice with given counts.
     * @param randfunc java.util.Random or its extension
     * @param counts amount of die
     */
    public FudgeDice(Random randfunc, int counts) {
        this.randfunc = randfunc;
        diceCounts = counts;
    }

    /**
     * Roll dice and get result.
     * @return Normal distributed integer [-N , N] for diceCount of N. 0 is the most frequent return.
     */
    public int roll() {
        int diceResult = 0;
        for (int c = 0; c < diceCounts; c++) {
            diceResult += rollSingleDie();
        }

        return diceResult;
    }

    /**
     * @return integer randomly picked from {-1, 0, 1}
     */
    private int rollSingleDie() {
        return (randfunc.nextInt(3)) - 1;
    }
}
