package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory

interface HasInventory {

    fun getNegotiator(): InventoryTransactionNegotiator
    fun getFixtureInventory(): FixtureInventory
    fun getPlayerInventory(): ActorInventory

}