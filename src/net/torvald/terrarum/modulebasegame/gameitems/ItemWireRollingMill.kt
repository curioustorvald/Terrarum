package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-03-02.
 */
class ItemWireRollingMill(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWireRollingMill") {


    override var baseMass = 50.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/wire_rolling_mill.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_WIRE_ROLLING_MILL"

}