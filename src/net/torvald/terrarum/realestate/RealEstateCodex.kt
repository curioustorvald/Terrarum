package net.torvald.terrarum.realestate

import net.torvald.terrarum.gameworld.GameWorld
import java.util.*

/**
 * Created by minjaesong on 2016-03-27.
 */
class RealEstateCodex {
    /**
     * HashMap<Absolute block number, Actor/Faction ID>
     *
     * Note that a block can have only ONE owner (as an Actor or Faction ID)
     */
    private val ownershipRegistry: HashMap<Long, Int> = HashMap()

    fun clear() = ownershipRegistry.clear()

    internal constructor()

    fun setOwner(world: GameWorld, tileX: Int, tileY: Int, refID: Int) {
        ownershipRegistry[LandUtil.getBlockAddr(world, tileX, tileY)] = refID
    }

    fun removeOwner(world: GameWorld, tileX: Int, tileY: Int) {
        ownershipRegistry.remove(LandUtil.getBlockAddr(world, tileX, tileY))
    }

    fun getOwner(world: GameWorld, tileX: Int, tileY: Int): Int? =
            ownershipRegistry[LandUtil.getBlockAddr(world, tileX, tileY)]
}