package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameitem.InventoryItem
import java.util.*

/**
 * Created by minjaesong on 16-01-15.
 */
interface Pocketed {

    var inventory: ActorInventory

    /** Item currentry holding, like tools/weapons/scrolls/magic/etc.
     *  Null if not holding anything
     */
    var itemHolding: InventoryItem?
    /**
     * List of all equipped items (tools, armours, rings, necklaces, etc.)
     */
    val itemEquipped: ArrayList<InventoryItem>

}