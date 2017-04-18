package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.TileAddress

/**
 * Created by minjaesong on 16-03-27.
 */
object LandUtil {
    fun getTileAddr(x: Int, y: Int): TileAddress =
            (Terrarum.ingame!!.world.width * y).toLong() + x

    fun resolveAbsoluteTileNumber(t: TileAddress): Pair<Int, Int> =
            Pair((t % Terrarum.ingame!!.world.width).toInt(), (t / Terrarum.ingame!!.world.width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: TileAddress): Any =
            if (id < 0x80000000L)
                Terrarum.ingame!!.getActorByID(id.toInt())
            else
                FactionCodex.getFactionByID(id)
}