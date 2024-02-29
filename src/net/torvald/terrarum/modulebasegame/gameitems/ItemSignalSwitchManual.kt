package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter

/**
 * Created by minjaesong on 2024-03-01.
 */
class ItemSignalSwitchManual(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSignalSwitchManual") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(8, 3)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_LOGIC_SIGNAL_SWITCH"
    override var inventoryCategory = Category.MISC

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}