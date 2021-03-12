package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.gameitem.GameItem

/**
 * Created by minjaesong on 2021-03-12.
 */
interface InventoryNegotiator {
    /** Retrieve item filter to be used to show only the acceptable items when player's own inventory is being displayed */
    fun getItemFilter(): List<String> // GameItem.Category
    /** Accepts item from the player and pass it to right inventory (object), slot (UI), etc... */
    fun accept(item: GameItem, amount: Int = 1)
    /** Rejects item and perhaps returns it back to the player, or make explosion, etc... */
    fun reject(item: GameItem, amount: Int = 1)
}