package net.torvald.terrarum.modulebasegame.ui

interface HasInventory {

    fun getNegotiator(): InventoryNegotiator
    fun getFixtureInventory()
    fun getPlayerInventory()

}