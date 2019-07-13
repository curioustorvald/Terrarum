package net.torvald.terrarum.tests

import net.torvald.util.CircularArray

/**
 * Created by minjaesong on 2019-01-09.
 */
class CircularArrayTest {

    operator fun invoke() {
        val testSet = CircularArray<Int?>(5, true)
        val testSet2 = CircularArray<Int?>(5, true)
        testSet2.overwritingPolicy = { println("Overwritten: $it") }
        val testSet3 = CircularArray<Int?>(5, true)

        for (i in 1..5) {
            testSet.appendHead(i)
        }

        println("Metadata:")
        println(testSet)
        println("forEach():")
        testSet.forEach { print("$it ") } // 1 2 3 4 5
        println("\nfold(0, sum):")
        println(testSet.fold(0) { acc, v -> acc + (v ?: 0) }) // 15
        println("Raw:")
        testSet.buffer.forEach { print("$it ") } // 1 2 3 4 5
        println()

        println()
        for (i in 1..6) {
            testSet2.appendHead(i) // must print "Overwritten: 1"
        }

        println("Metadata:")
        println(testSet2)
        println("forEach():") // 2 3 4 5 6
        testSet2.forEach { print("$it ") }
        println("\nfold(0, sum):")
        println(testSet2.fold(0) { acc, v -> acc + (v ?: 0) }) // 20
        println("Raw:")
        testSet2.buffer.forEach { print("$it ") } // 6 2 3 4 5
        println()

        println()
        for (i in 1..5) {
            testSet3.appendHead(i)
        }

        val a1 = testSet3.removeTail()
        val a2 = testSet3.removeTail()
        println("Removed elems: $a1 $a2")

        println("Metadata:")
        println(testSet3)
        println("forEach():") // 3 4 5
        testSet3.forEach { print("$it ") }
        println("\nfold(0, sum):")
        println(testSet3.fold(0) { acc, v -> acc + (v ?: 0) }) // 12
        println("Raw:")
        testSet3.buffer.forEach { print("$it ") } // 1 2 3 4 5
        println()
    }

}

fun main(args: Array<String>) {
    CircularArrayTest().invoke()
}