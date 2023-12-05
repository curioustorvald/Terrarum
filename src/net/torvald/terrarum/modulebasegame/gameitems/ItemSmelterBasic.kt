package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-12-04.
 */
class ItemSmelterBasic(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic") {


    override var baseMass = 100.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_tall.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_SMELTER_BASIC"

}