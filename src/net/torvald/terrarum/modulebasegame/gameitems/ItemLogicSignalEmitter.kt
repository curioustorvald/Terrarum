package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter

class ItemLogicSignalEmitter(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override val isDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_source.tga")

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_LOGIC_SIGNAL_EMITTER"
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