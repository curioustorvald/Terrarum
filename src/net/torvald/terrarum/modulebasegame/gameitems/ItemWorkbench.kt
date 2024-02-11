package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-09-20.
 */
class ItemWorkbench(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWorkbench") {


    override var baseMass = 20.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/workbench.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_WORKBENCH"

}