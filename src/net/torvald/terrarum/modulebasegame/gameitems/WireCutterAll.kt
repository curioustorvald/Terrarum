package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * TEST ITEM; this item cuts every wire on a cell, and has no durability drop
 *
 * Created by minjaesong on 2021-09-18.
 */
class WireCutterAll(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_WIRE_CUTTER"
    override var baseMass = 0.1
    override var baseToolSize: Double? = baseMass
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items16").get(0, 9)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
    }

    override fun startPrimaryUse(delta: Float): Boolean {
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)
        val wires = ingame.world.getAllWiresFrom(mouseTile.x, mouseTile.y)

        wires?.forEach {
            ingame.world.removeTileWire(mouseTile.x, mouseTile.y, it, false)
        } ?: return false
        return true
    }

    override fun effectWhenEquipped(delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"
    }

    override fun effectOnUnequip(delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }
}