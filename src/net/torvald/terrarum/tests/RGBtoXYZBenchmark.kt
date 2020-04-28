package net.torvald.terrarum.tests

import net.torvald.colourutil.ColourUtil.getLuminosity
import net.torvald.colourutil.ColourUtil.getLuminosityQuick
import net.torvald.colourutil.RGB
import net.torvald.colourutil.linearise
import net.torvald.colourutil.unLinearise
import net.torvald.random.HQRNG
import kotlin.system.measureNanoTime

/**
 * Created by minjaesong on 2019-01-03.
 */
class RGBtoXYZBenchmark {

    private val TEST_SIZE = 100000
    private val TEST_CHUNK = 1000

    operator fun invoke() {
        val rng = HQRNG()
        // prepare test sets
        val testSets = Array(TEST_SIZE) {
            RGB(rng.nextFloat(), rng.nextFloat(), rng.nextFloat())
        }
        // make sure to initialise Util's LUT
        testSets[rng.nextInt(0, testSets.size)].linearise()
        testSets[rng.nextInt(0, testSets.size)].unLinearise()


        // conduct the experiment
        val timer1 = ArrayList<Long>()
        val timer2 = ArrayList<Long>()


        for (i in 0 until TEST_SIZE step TEST_CHUNK) {

            val time1 = measureNanoTime {
                for (c in i until i + TEST_CHUNK) {
                    testSets[c].getLuminosity()
                }
            }

            val time2 = measureNanoTime {
                for (c in i until i + TEST_CHUNK) {
                    testSets[c].getLuminosityQuick()
                }
            }

            timer1.add(time1)
            timer2.add(time2)
        }


        // print out captured data
        println("with LUT\tno LUT\tmult")
        for (i in 0 until timer1.size) {
            println("${timer1[i]}\t${timer2[i]}\t${timer1[i].toFloat() / timer2[i]}")
        }
    }

}

fun main(args: Array<String>) {
    RGBtoXYZBenchmark().invoke()
}