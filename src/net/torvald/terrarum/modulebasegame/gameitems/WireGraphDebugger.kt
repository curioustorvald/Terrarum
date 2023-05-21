package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.unicode.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame

class WireGraphDebugger(originalID: ItemID) : GameItem(originalID) {

    override val originalName = "WIRE_DEBUGGER"
    override var baseToolSize: Double? = PickaxeCore.BASE_MASS_AND_SIZE
    override var stackable = false
    override var inventoryCategory = Category.TOOL
    override val isUnique = true
    override val isDynamic = false
    override val materialId = "CUPR"
    override var baseMass = 2.0
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_24")

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.name = "Wire Debugger"
    }

    private val sb = StringBuilder()
    private val blockMarker = CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"

        blockMarker.markerShape = 3
        blockMarker.markerColour = Color.YELLOW
        blockMarker.isVisible = true
        blockMarker.update(delta)

        val mx = Terrarum.mouseTileX
        val my = Terrarum.mouseTileY

        sb.clear()

        INGAME.world.getAllWiringGraph(mx, my)?.forEach { (itemID, simCell) ->
            if (sb.isNotEmpty()) sb.append('\n')

            val connexionIcon = (simCell.cnx + 0xE0A0).toChar()
            val wireName = WireCodex[itemID].nameKey

            val emit = simCell.emt
            val recv = simCell.rcv

            sb.append("$connexionIcon $wireName")
            sb.append("\nE: $emit")
            recv.forEach {
                val src = INGAME.world.getWireEmitStateOf(it.src.x, it.src.y, itemID)!!
                sb.append("\nR: $src $EMDASH d ${it.dist}")
            }
        }

        if (sb.isNotEmpty()) {
            (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(sb.toString())
        }
        else {
            (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(null)
        }
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
        (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(null)
        blockMarker.isVisible = false
    }
}