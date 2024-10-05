package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInductionMotor

/**
 * Created by minjaesong on 2024-10-03.
 */
class ItemInductionMotor(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureInductionMotor") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureInductionMotor.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSheet("basegame", "sprites/fixtures/induction_motor.tga", TILE_SIZE, TILE_SIZE)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_INDUCTION_MOTOR"
    override var inventoryCategory = Category.FIXTURE

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "axle"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}

/**
 * Created by minjaesong on 2024-10-05.
 */
class ItemGearbox(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureGearbox") {

    override var dynamicID: ItemID = originalID
    override var baseMass = FixtureInductionMotor.MASS
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSheet("basegame", "sprites/fixtures/gearbox.tga", TILE_SIZE, TILE_SIZE+1)
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_GEARBOX"
    override var inventoryCategory = Category.FIXTURE

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        super.effectWhileEquipped(actor, delta)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = "axle"
    }

    override fun effectOnUnequip(actor: ActorWithBody) {
        super.effectOnUnequip(actor)
        (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass = ""
    }

}