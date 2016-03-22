package com.Torvald.Rand

import java.util.Random

/**
 * Created by minjaesong on 16-02-03.
 */
class Fudge3
/**
 * Define new set of fudge dice with three dice.
 * @param randfunc java.util.Random or its extension
 */
(randfunc: Random) : FudgeDice(randfunc, 3)
