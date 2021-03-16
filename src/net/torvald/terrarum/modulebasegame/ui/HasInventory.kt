package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory

interface HasInventory {

    fun getNegotiator(): InventoryNegotiator
    fun getFixtureInventory(): FixtureInventory
    fun getPlayerInventory(): FixtureInventory

}