package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blockproperties.Wire
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.Material

/**
 * Created by minjaesong on 2019-03-10.
 */
class WirePieceSignalWire(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_WIRE"
    override var baseMass = 0.001
    override var baseToolSize: Double? = null
    override var stackable = true
    override var inventoryCategory = Category.WIRE
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion?
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items16").get(1,9)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP

    }

    override fun startPrimaryUse(delta: Float): Boolean {
        return BlockBase.wireStartPrimaryUse(this, Wire.BIT_SIGNAL_RED, delta)
    }

    override fun effectWhenEquipped(delta: Float) {
        BlockBase.wireEffectWhenEquipped(Wire.BIT_SIGNAL_RED, delta)
    }
}