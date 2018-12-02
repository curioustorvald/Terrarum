package net.torvald.terrarum.tests

import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
//import org.junit.Test

/**
 * Created by minjaesong on 2018-12-02.
 */
class TestWorldTime {

    val time = WorldTime(WorldTime.DAY_LENGTH * 118L + 86398)

    //@Test
    fun testEndOfYear() {
        repeat(5) {
            println(time.getFormattedTime())
            time.addTime(1)
        }

        println()
        time.addTime(86400 - 5)

        repeat(5) {
            println(time.getFormattedTime())
            time.addTime(1)
        }
    }




}


fun main(args: Array<String>) {
    TestWorldTime().testEndOfYear()
}