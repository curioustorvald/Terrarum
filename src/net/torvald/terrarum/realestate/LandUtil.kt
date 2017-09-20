package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-03-27.
 */
object LandUtil {
    fun getBlockAddr(world: GameWorld, x: Int, y: Int): BlockAddress =
            (world.width * y).toLong() + x

    fun resolveBlockAddr(world: GameWorld, t: BlockAddress): Pair<Int, Int> =
            Pair((t % world.width).toInt(), (t / world.width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Int): Any =
            if (id >= 0)
                Terrarum.ingame!!.getActorByID(id)
            else
                FactionCodex.getFactionByID(id)
}