package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameitem.InventoryItem

/**
 * Created by minjaesong on 16-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory

    /** Item currentry holding, like tools/weapons/scrolls/magic/etc. */
    var itemHolding: InventoryItem

}