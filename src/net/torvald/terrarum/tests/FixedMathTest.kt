package net.torvald.terrarum.tests

import kotlin.system.measureNanoTime

/**
 * Created by minjaesong on 2019-01-27.
 */

class FixedMathTest {
    fun invoke() {
        val testSet = (1..100000).toList().shuffled()
        val m1 = measureNanoTime {
            testSet.forEach {
                val x = (it * 1.41421356f)
            }
        }
        val testSet2 = (1..100000).toList().shuffled()
        val m2 = measureNanoTime {
            testSet2.forEach {
                val x2 = it.mulSqrt2()
            }
        }
        val m3 = measureNanoTime {
            testSet.forEach {
                val x = (it * 1.41421356f)
            }
        }
        val m4 = measureNanoTime {
            testSet2.forEach {
                val x2 = it.mulSqrt2()
            }
        }

        println(m3)
        println(m4)
        println(m3.toDouble() / m4)
    }

    fun Int.mulSqrt2(): Int {
        val xl = this
        val yl = 92681

        val xlo = xl and 0x0000FFFF
        val xhi = xl shr 16
        val ylo = yl and 0x0000FFFF
        val yhi = yl shr 16

        val lolo = xlo * ylo
        val lohi = xlo * yhi
        val hilo = xhi * ylo
        val hihi = xhi * yhi

        val loResult = lolo shr 16
        val hiResult = hihi shl 16

        val sum = loResult + lohi + hilo + hiResult
        return sum
    }
}

fun main(args: Array<String>) {
    FixedMathTest().invoke()
}

