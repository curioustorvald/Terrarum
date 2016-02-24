package com.Torvald.Rand;

import java.util.Random;

/**
 * Created by minjaesong on 16-02-03.
 */
public class Fudge3 extends FudgeDice {

    /**
     * Define new set of fudge dice with three dice.
     * @param randfunc java.util.Random or its extension
     */
    public Fudge3(Random randfunc) {
        super(randfunc, 3);
    }


}
