package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.Terrarum.toInt
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRangeTools
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameitems.BlockBase.wireNodeMirror
import net.torvald.terrarum.notEmptyOrNull
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.WiringGraphMap

/**
 * Modular approach to the wire cutter function
 *
 * Created by minjaesong on 2022-07-13.
 */
object WireCutterBase {

    private fun disconnect(item: ItemID, mapP: WiringGraphMap, mapN: WiringGraphMap, subTile: Terrarum.SubtileVector) {
        val pBitMask = subTile.toInt() // bit mask to turn off
        val nBitMask = pBitMask.wireNodeMirror()

        mapP[item]!!.cnx = mapP[item]!!.cnx and (15 - pBitMask) // (15 - pBitMask) -> bit mask to retain
        mapN[item]!!.cnx = mapN[item]!!.cnx and (15 - nBitMask)
    }

    fun startPrimaryUse(item: GameItem, actor: ActorWithBody, delta: Float, wireFilter: (ItemID) -> Boolean) = mouseInInteractableRangeTools(actor, item) {
        val ingame = Terrarum.ingame!! as TerrarumIngame
        val mouseTile = Terrarum.getMouseSubtile4()

        if (mouseTile.vector == Terrarum.SubtileVector.INVALID) return@mouseInInteractableRangeTools false

        val (mtx, mty) = mouseTile.currentTileCoord
        val mvec = mouseTile.vector

        if (mvec == Terrarum.SubtileVector.CENTRE) {
            val wireNet = ingame.world.getAllWiresFrom(mtx, mty)
            val wireItems = wireNet.first

            wireItems?.filter(wireFilter)?.notEmptyOrNull()?.forEach {
                ingame.world.removeTileWire(mtx, mty, it, false)
                ingame.queueActorAddition(DroppedItem(it, mtx * TILE_SIZED, mty * TILE_SIZED))
            } ?: return@mouseInInteractableRangeTools false

            true
        }
        else {
            val (ntx, nty) = mouseTile.nextTileCoord

            val wireNetP = ingame.world.getAllWiresFrom(mtx, mty)
            val wireNetN = ingame.world.getAllWiresFrom(ntx, nty)
            val wireItemsP = wireNetP.first
            val wireItemsN = wireNetN.first

            // get intersection of wireItemsP and wireItemsN
            if (wireItemsP != null && wireItemsN != null) {
                val wireItems = wireItemsP intersect wireItemsN

                wireItems.filter(wireFilter).notEmptyOrNull()?.forEach {
                    disconnect(it, wireNetP.second!!, wireNetN.second!!, mouseTile.vector)
                } ?: return@mouseInInteractableRangeTools false

                true
            }
            else
                false
        }
    }.toInt().minus(1L) // 0L if successful, -1L if not


}
/**
 * TEST ITEM; this item cuts every wire on a cell, and has no durability drop
 *
 * Created by minjaesong on 2021-09-18.
 */
class WireCutterAll(originalID: ItemID) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override var baseMass = 0.1
    override var baseToolSize: Double? = baseMass
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(1, 3)

    init {
        stackable = false
        isUnique = true
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        originalName = "ITEM_WIRE_CUTTER"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) =
            WireCutterBase.startPrimaryUse(this, actor, delta) { true }

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }
}