package net.torvald.terrarum.realestate

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod

/**
 * Created by minjaesong on 2016-03-27.
 */
object LandUtil {
    fun getBlockAddr(world: GameWorld, x: Int, y: Int): BlockAddress =
            // coercing and fmod-ing follows ROUNDWORLD rule
            (world.width * y.coerceIn(0, world.height - 1)).toLong() + x.fmod(world.width)

    fun resolveBlockAddr(world: GameWorld, t: BlockAddress): Pair<Int, Int> =
            Pair((t % world.width).toInt(), (t / world.width).toInt())

    fun resolveBlockAddr(width: Int, t: BlockAddress): Pair<Int, Int> =
            Pair((t % width).toInt(), (t / width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Int): Any =
            if (id >= 0)
                Terrarum.ingame!!.getActorByID(id)
            else
                FactionCodex.getFactionByID(id)


}