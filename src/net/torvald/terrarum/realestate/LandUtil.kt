package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.BlockAddress

/**
 * Created by minjaesong on 16-03-27.
 */
object LandUtil {
    fun getBlockAddr(x: Int, y: Int): BlockAddress =
            (Terrarum.ingame!!.world.width * y).toLong() + x

    fun resolveAbsoluteBlockNumber(t: BlockAddress): Pair<Int, Int> =
            Pair((t % Terrarum.ingame!!.world.width).toInt(), (t / Terrarum.ingame!!.world.width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Int): Any =
            if (id >= 0)
                Terrarum.ingame!!.getActorByID(id)
            else
                FactionCodex.getFactionByID(id)
}