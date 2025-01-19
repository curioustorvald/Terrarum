package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2025-01-19.
 */
class ItemMysteriousATM(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureMysteriousATM") {

    override var baseMass = 2000.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mysterious_atm.tga")
    }
    // it's supposed to be turned off when you pick it up
//    @Transient override val itemImageEmissive = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mysterious_atm_emsv.tga")

    override var originalName = "ITEM_MYSTERIOUS_ATM"

}