package net.torvald.terrarum.tests

import net.torvald.random.HQRNG
import java.util.*

/**
 * Created by minjaesong on 2019-07-27.
 */

fun rng01(rng: Random): Double {
    return (rng.nextInt().toDouble() / 4294967295L.toDouble())
}

fun main(args: Array<String>) {
    val rng = HQRNG()

    /*repeat(512) {
        println(rng.nextDouble())
    }*/

    println()

    val rng2 = com.sudoplay.joise.generator.HQRNG()

    repeat(512) {
        println(rng2.getRange(0, 10))
    }

    // getTarget: 0..(t-1) (exclusive)
    // getRange: low..high (inclusive)
    // get01: 0.0 until 1.0 (exclusive)
}
