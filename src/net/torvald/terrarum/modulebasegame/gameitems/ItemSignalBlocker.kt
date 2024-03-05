package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureLogicSignalEmitter

/**
 * Created by minjaesong on 2024-03-04.
 */
class ItemSignalBlocker(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSignalBlocker") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(10, 3)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_LOGIC_SIGNAL_BLOCKER"

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}

/**
 * Created by minjaesong on 2024-03-05.
 */
class ItemSignalLatch(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureSignalLatch") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureLogicSignalEmitter.MASS
    override val canBeDynamic = false
    override val materialId = ""
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(11, 3)

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_LOGIC_SIGNAL_LATCH"

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "signal"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}