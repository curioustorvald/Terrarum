package net.torvald.terrarum.modulecomputers.gameitems

import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase

/**
 * Created by minjaesong on 2025-03-30.
 */
class ItemComputerConsole(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulecomputers.gameactors.FixtureComputerConsole") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 80.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
//        itemImage = FixtureItemBase.getItemImageFromSheet("dwarventech", "sprites/fixtures/desktop_computer.tga", TILE_SIZE, TILE_SIZE)
    }
    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_COMPUTER_CONSOLE"

}