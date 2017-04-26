package net.torvald.terrarum.realestate

import java.util.*

/**
 * Created by minjaesong on 16-03-27.
 */
object RealEstateCodex {
    /**
     * HashMap<Absolute block number, Actor/Faction ID>
     *
     * Note that a block can have only ONE owner (as an Actor or Faction ID)
     */
    private var ownershipRegistry: HashMap<Long, Int> = HashMap()

    fun setOwner(tileX: Int, tileY: Int, refID: Int) {
        ownershipRegistry[LandUtil.getBlockAddr(tileX, tileY)] = refID
    }

    fun removeOwner(tileX: Int, tileY: Int) {
        ownershipRegistry.remove(LandUtil.getBlockAddr(tileX, tileY))
    }

    fun getOwner(tileX: Int, tileY: Int): Int? =
            ownershipRegistry[LandUtil.getBlockAddr(tileX, tileY)]
}