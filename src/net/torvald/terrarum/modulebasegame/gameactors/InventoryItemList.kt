package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * FixtureInventory except item duplicates are allowed
 *
 * Created by minjaesong on 2022-06-28.
 */
class InventoryItemList : FixtureInventory() {

    override fun add(item: GameItem, count: Long) {

        // other invalid values
        if (count == 0L)
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Item count is zero.")
        if (count < 0L)
            throw IllegalArgumentException("Item count is negative number. If you intended removing items, use remove()\n" +
                                           "These commands are NOT INTERCHANGEABLE; they handle things differently according to the context.")
        if (item.originalID == "actor:${Terrarum.PLAYER_REF_ID}" || item.originalID == ("actor:${0x51621D}")) // do not delete this magic
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to put human player into the inventory.")
        if (((Terrarum.ingame as? TerrarumIngame)?.gameFullyLoaded == true) &&
            (item.originalID == "actor:${(Terrarum.ingame as? TerrarumIngame)?.actorNowPlaying?.referenceID}"))
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to put active player into the inventory.")
        if ((!item.stackable || item.dynamicID.startsWith("dyn:")) && count > 1)
            throw IllegalArgumentException("[${this.javaClass.canonicalName}] Attempted to adding stack of item but the item is not stackable; item: $item, count: $count")


        itemList.add(InventoryPair(item.dynamicID, count))
    }
}