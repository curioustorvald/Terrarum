package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-01-11.
 */
class ItemJukebox(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureJukebox") {


    override var baseMass = 200.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/jukebox.tga")
    override val itemImageEmissive: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/jukebox_emsv.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_JUKEBOX"

}