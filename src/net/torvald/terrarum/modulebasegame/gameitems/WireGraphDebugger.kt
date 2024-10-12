package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.unicode.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.GameItem.EquipPosition.HAND_GRIP
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes

class WireGraphDebugger(originalID: ItemID) : GameItem(originalID) {
    override var baseToolSize: Double? = PickaxeCore.BASE_MASS_AND_SIZE
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = "CUPR"
    override var baseMass = 2.0
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(2,3)
    }
    override var equipPosition = HAND_GRIP
    override var originalName = "ITEM_MULTIMETER"

    init {
        stackable = false
        isUnique = true
    }

    private val sb = StringBuilder()
    private val blockMarker = CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"

        blockMarker.showMarker(3)
        blockMarker.markerColour = Color.YELLOW
        blockMarker.update(delta)

        val mx = Terrarum.mouseTileX
        val my = Terrarum.mouseTileY

        sb.clear()

        INGAME.world.getAllWiringGraph(mx, my)?.forEach { (itemID, simCell) ->
            if (sb.isNotEmpty()) sb.append('\n')

            val connexionIcon = (simCell.cnx + 0xE0A0).toChar()
            val wireName = WireCodex[itemID].nameKey

            val emit = simCell.emt
            val recv = simCell.rcp

            sb.append("$connexionIcon $wireName")
            sb.append("\nE: $emit")
            recv.forEach {
                val src = INGAME.world.getWireEmitStateOf(it.src.x, it.src.y, itemID)!!
                sb.append("\nR: $src $EMDASH d ${it.dist}")
            }
        }

        if (sb.isNotEmpty()) {
            acquireTooltip(sb.toString())
        }
        else {
            releaseTooltip()
        }
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
        blockMarker.hideMarker()
        removeFromTooltipRecord()
    }
}