package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-12-04.
 */
class ItemSmelterBasic(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic") {
    override var baseMass = 100.0
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(4,3)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_SMELTER_SMALL"
}