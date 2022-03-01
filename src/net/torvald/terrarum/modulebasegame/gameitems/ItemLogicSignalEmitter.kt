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
    override val originalName = "ITEM_LOGIC_SIGNAL_EMITTER"
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_source.tga")

    override var baseToolSize: Double? = baseMass

    init {
        equipPosition = EquipPosition.HAND_GRIP
    }

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        super.effectOnUnequip(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}