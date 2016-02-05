package com.Torvald.Rand;

import java.util.Random;

/**
 * Created by minjaesong on 16-02-03.
 */
public class Fudge3 {

    public FudgeDice create(Random rand) {
        return new FudgeDice(rand, 3);
    }

}
