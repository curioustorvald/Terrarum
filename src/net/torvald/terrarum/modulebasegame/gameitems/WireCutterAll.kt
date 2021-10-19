package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.WireCodex
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameitem.inInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem

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
    override var stackable = false
    override var inventoryCategory = Category.TOOL
    override val isUnique = true
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegionPack("basegame.items16").get(0, 9)

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = inInteractableRange(actor) {
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTile = Point2i(Terrarum.mouseTileX, Terrarum.mouseTileY)
        val wires = ingame.world.getAllWiresFrom(mouseTile.x, mouseTile.y)?.cloneToList()

        wires?.forEach {
            ingame.world.removeTileWire(mouseTile.x, mouseTile.y, it, false)
            ingame.addNewActor(DroppedItem(it, mouseTile.x * TILE_SIZE, mouseTile.y * TILE_SIZE))
        } ?: return@inInteractableRange false

        true
    }

    override fun effectWhenEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }
}