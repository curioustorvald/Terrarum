package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.WireCodex
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material

/**
 * @param originalID something like `basegame:8192` (the id must exist on wires.csv)
 *
 * Created by minjaesong on 2019-03-10.
 */
class WirePieceSignalWire(originalID: ItemID, private val atlasID: String, private val sheetX: Int, private val sheetY: Int)
    : GameItem(originalID), FixtureInteractionBlocked {

    override var dynamicID: ItemID = originalID
    override var baseMass = 0.001
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.WIRE
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet(atlasID).get(sheetX, sheetY)
    }

    init {
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        originalName = "ITEM_WIRE"
        tags.addAll(WireCodex[originalID].tags)
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        return BlockBase.wireStartPrimaryUse(actor,this, delta)
    }

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        BlockBase.wireEffectWhenEquipped(this, delta)
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        BlockBase.wireEffectWhenUnequipped(this)
    }
}