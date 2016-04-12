package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-27.
 */
object RealEstateUtility {
    fun getAbsoluteTileNumber(x: Int, y: Int): Long =
            (Terrarum.game.map.width * y).toLong() + x

    fun resolveAbsoluteTileNumber(t: Long): Pair<Int, Int> =
            Pair((t % Terrarum.game.map.width).toInt(), (t / Terrarum.game.map.width).toInt())
}