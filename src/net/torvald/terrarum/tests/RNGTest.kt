package net.torvald.terrarum.tests

import net.torvald.random.HQRNG
import net.torvald.random.XXHash32
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.serialise.toLittle
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

    /*val rng2 = com.sudoplay.joise.generator.HQRNG()

    repeat(512) {
        println(rng2.getRange(0, 10))
    }

    // getTarget: 0..(t-1) (exclusive)
    // getRange: low..high (inclusive)
    // get01: 0.0 until 1.0 (exclusive)


    */



    for (tries in 0 until 16) {
        repeat(BlockCodex.DYNAMIC_RANDOM_CASES + 12) { repeats ->
            val x = 349 + repeats
            val y = 9492 + tries


            val offset = XXHash32.hash(((x and 0xFFFF).shl(16) or (y and 0xFFFF)).toLittle(), 10000)

            //print("${offset.toString().padStart(2, '0')} ")
            print("${offset.fmod(BlockCodex.DYNAMIC_RANDOM_CASES).toString().padStart(2, '0')} ")
        }
        println()
        println()
    }
}
