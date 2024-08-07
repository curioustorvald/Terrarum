package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2023-05-28.
 */
class ItemWorldPortal(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWorldPortal") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 6000.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(3,3)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_WORLD_PORTAL"
}