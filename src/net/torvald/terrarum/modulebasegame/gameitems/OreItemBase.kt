package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2023-10-11.
 */
open class OreItemBase(originalID: ItemID) : GameItem(originalID) {
    override var baseMass = 10.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.GENERIC
    override val isDynamic = false
    override val materialId = "OORE"
    override var equipPosition = EquipPosition.HAND_GRIP
}

/* Wooden Log is a block */
class OreWood(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_WOOD"
    override val materialId = "WOOD"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,6)
}
class OreStick(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_WOOD_STICK"
    override val materialId = "WOOD"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1,6)
}
class OreCopper(originalID: ItemID) : OreItemBase(originalID) {
    override var originalName = "ITEM_ORE_MALACHITE"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(2,6)
}