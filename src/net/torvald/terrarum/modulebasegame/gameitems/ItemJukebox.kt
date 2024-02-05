package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2024-01-11.
 */
class ItemJukebox(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureJukebox") {


    override var baseMass = 200.0
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(6,3)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_JUKEBOX"

}