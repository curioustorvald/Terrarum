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
class ItemLogicSignalBulb(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalBulb") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(9, 3)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_COPPER_BULB"

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}