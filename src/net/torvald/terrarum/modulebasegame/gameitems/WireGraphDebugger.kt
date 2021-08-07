package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import kotlin.math.roundToInt

class WireGraphDebugger(originalID: ItemID) : GameItem(originalID) {

    override val originalName = "WIRE_DEBUGGER"
    override var baseToolSize: Double? = PickaxeCore.BASE_MASS_AND_SIZE
    override var stackable = false
    override var inventoryCategory = Category.TOOL
    override val isUnique = true
    override val isDynamic = false
    override val material = MaterialCodex["CUPR"]
    override var baseMass = 2.0
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_24")

    init {
        super.equipPosition = GameItem.EquipPosition.HAND_GRIP
        super.name = "Wire Debugger"
    }

    private val sb = StringBuilder()

    override fun effectWhenEquipped(delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "wire_render_all"


        val mx = Terrarum.mouseTileX
        val my = Terrarum.mouseTileY

        sb.clear()

        Terrarum.ingame!!.world.getAllWiringGraph(mx, my)?.let {
            it.forEachIndexed { index, (itemID, simCell) ->
                if (sb.isNotEmpty()) sb.append('\n')


                val connexionIcon = (simCell.con + 0xE0A0).toChar()
                val wireName = WireCodex[itemID].nameKey

                // todo

                sb.append("$connexionIcon $wireName")
            }
        }

        if (sb.isNotEmpty()) {
            (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(sb.toString())
        }
        else {
            (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(null)
        }
    }

    override fun effectOnUnequip(delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
        (Terrarum.ingame!! as TerrarumIngame).setTooltipMessage(null)
    }
}