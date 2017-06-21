package net.torvald.terrarum.realestate

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.BlockAddress

/**
 * Created by minjaesong on 16-03-27.
 */
object LandUtil {
    fun getBlockAddr(x: Int, y: Int): BlockAddress =
            (TerrarumGDX.ingame!!.world.width * y).toLong() + x

    fun resolveBlockAddr(t: BlockAddress): Pair<Int, Int> =
            Pair((t % TerrarumGDX.ingame!!.world.width).toInt(), (t / TerrarumGDX.ingame!!.world.width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Int): Any =
            if (id >= 0)
                TerrarumGDX.ingame!!.getActorByID(id)
            else
                FactionCodex.getFactionByID(id)
}