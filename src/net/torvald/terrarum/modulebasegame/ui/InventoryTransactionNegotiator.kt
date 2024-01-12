package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.ui.UIItemCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory

/**
 * Created by minjaesong on 2021-03-12.
 */
abstract class InventoryTransactionNegotiator {
    /** Retrieve item filter to be used to show only the acceptable items when player's own inventory is being displayed */
    open fun getItemFilter(): List<String> = listOf(CAT_ALL) // GameItem.Category
    /** Called when the item is being accepted from the player and moved to the right inventory (object), slot (UI), etc... */
    abstract fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long = 1L)
    /** Called when the item is being removed from the fixture to returns it back to the player, or make explosion, etc... */
    abstract fun refund(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long = 1L)
}