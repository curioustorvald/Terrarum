package net.torvald.terrarum.realestate

import java.io.Serializable
import java.util.*

/**
 * Created by minjaesong on 16-03-27.
 */
object RealEstateCodex {
    /**
     * HashMap<Absolute tile number, Actor/Faction ID>
     *
     * Note that a tile can have only ONE owner (as an Actor or Faction ID)
     */
    private var ownershipRegistry: HashMap<Long, Int> = HashMap()

    fun setOwner(tileX: Int, tileY: Int, refID: Int) {
        ownershipRegistry[LandUtil.getTileAddr(tileX, tileY)] = refID
    }

    fun removeOwner(tileX: Int, tileY: Int) {
        ownershipRegistry.remove(LandUtil.getTileAddr(tileX, tileY))
    }

    fun getOwner(tileX: Int, tileY: Int): Int? =
            ownershipRegistry[LandUtil.getTileAddr(tileX, tileY)]
}