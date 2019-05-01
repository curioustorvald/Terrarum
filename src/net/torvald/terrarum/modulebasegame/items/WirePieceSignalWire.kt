package net.torvald.terrarum.modulebasegame.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.blockproperties.Wire
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-03-10.
 */
class WirePieceSignalWire(override val originalID: ItemID) : GameItem() {

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
        get() = (AppLoader.resourcePool["basegame.items16"] as TextureRegionPack).get(1,9)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP

    }

    override fun startPrimaryUse(delta: Float): Boolean {
        println("Wire!")

        return true
    }

    override fun effectWhenEquipped(delta: Float) {
        IngameRenderer.selectedWireBitToDraw = Wire.BIT_SIGNAL_RED
    }
}