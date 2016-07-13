package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.faction.FactionCodex

/**
 * Created by minjaesong on 16-03-27.
 */
object RealEstateUtility {
    fun getAbsoluteTileNumber(x: Int, y: Int): Long =
            (Terrarum.ingame.world.width * y).toLong() + x

    fun resolveAbsoluteTileNumber(t: Long): Pair<Int, Int> =
            Pair((t % Terrarum.ingame.world.width).toInt(), (t / Terrarum.ingame.world.width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Long): Any =
            if (id < 0x80000000L)
                Terrarum.ingame.getActorByID(id.toInt())
            else
                FactionCodex.getFactionByID(id)
}