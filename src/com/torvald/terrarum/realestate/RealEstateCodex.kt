package com.torvald.terrarum.realestate

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
    private var ownershipRegistry: HashMap<Long, Long> = HashMap()

    fun setOwner(tileX: Int, tileY: Int, refID: Long) {
        ownershipRegistry[RealEstateUtility.getAbsoluteTileNumber(tileX, tileY)] = refID
    }

    fun removeOwner(tileX: Int, tileY: Int) {
        ownershipRegistry.remove(RealEstateUtility.getAbsoluteTileNumber(tileX, tileY))
    }

    fun getOwner(tileX: Int, tileY: Int): Long? =
            ownershipRegistry[RealEstateUtility.getAbsoluteTileNumber(tileX, tileY)]
}