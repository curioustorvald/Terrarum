package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-12-04.
 */
class ItemSmelterBasic(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSmelterBasic") {
    override var baseMass = 100.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(4,3)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_SMELTER_SMALL"
}

/**
 * Created by minjaesong on 2024-03-10.
 */
class ItemAlloyingFurnace(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureAlloyingFurnace") {
    override var baseMass = 100.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(13,3)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_ALLOYING_FURNACE"
}