package net.torvald.terrarum.tests

import net.torvald.util.SortedArrayList

fun main(args: Array<String>) {
    val t = SortedArrayList<Int>()

    t.add(5)
    t.add(1)
    t.add(4)
    t.add(2)
    t.add(3)

    t.forEach { print(it) }
}