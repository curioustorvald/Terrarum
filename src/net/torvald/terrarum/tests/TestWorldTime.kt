package net.torvald.terrarum.tests

import net.torvald.terrarum.modulebasegame.gameworld.WorldTime

/**
 * Created by minjaesong on 2018-12-02.
 */
class TestWorldTime {

    val time = WorldTime(WorldTime.DAY_LENGTH * 118L + 86398)

    //@Test
    fun testEndOfYear() {
        repeat(3) {
            println(time.getShortTime())
            time.addTime(1)
        }
    }

}