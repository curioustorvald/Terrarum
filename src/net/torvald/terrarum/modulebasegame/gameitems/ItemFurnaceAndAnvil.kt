package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-12-05.
 */
class ItemFurnaceAndAnvil(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureFurnaceAndAnvil") {


    override var baseMass = 100.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/metalworking_furnace_and_anvil.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_FURNACE_AND_ANVIL"

}