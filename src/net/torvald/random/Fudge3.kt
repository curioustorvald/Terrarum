package net.torvald.random

import java.util.Random

/**
 * Created by minjaesong on 2016-02-03.
 */
class Fudge3
/**
 * Define new set of Fudge dice with three dice (3dF).
 * @param randfunc java.util.Random or its extension
 */
(randfunc: Random) : FudgeDice(randfunc, 3)
