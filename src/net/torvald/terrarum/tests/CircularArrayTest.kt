package net.torvald.terrarum.tests

import net.torvald.util.CircularArray

/**
 * Created by minjaesong on 2019-01-09.
 */
class CircularArrayTest {

    operator fun invoke() {
        val testSet = CircularArray<Int?>(5)
        val testSet2 = CircularArray<Int?>(5)

        for (i in 1..5) {
            testSet.add(i)
        }

        println("Metadata:")
        println(testSet)
        println("forEach():")
        testSet.forEach { print("$it ") }
        println("\nfold(0, sum):")
        println(testSet.fold(0) { acc, v -> acc + (v ?: 0) })
        println("Raw:")
        testSet.buffer.forEach { print("$it ") }
        println()

        println()
        for (i in 1..6) {
            testSet2.add(i)
        }

        println("Metadata:")
        println(testSet2)
        println("forEach():")
        testSet2.forEach { print("$it ") }
        println("\nfold(0, sum):")
        println(testSet2.fold(0) { acc, v -> acc + (v ?: 0) })
        println("Raw:")
        testSet2.buffer.forEach { print("$it ") }
        println()

    }

}

fun main(args: Array<String>) {
    CircularArrayTest().invoke()
}