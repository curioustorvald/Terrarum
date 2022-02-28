package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase

/**
 * Created by minjaesong on 2021-12-13.
 */
open class FixtureItemBase(originalID: ItemID, val fixtureClassName: String) : GameItem(originalID) {

    protected open val makeFixture: () -> FixtureBase = {
        Class.forName(fixtureClassName).getDeclaredConstructor().newInstance() as FixtureBase
    }

    init {
        ItemCodex.fixtureToSpawnerItemID[fixtureClassName] = originalID
    }

    protected var ghostItem: FixtureBase? = null
        get() {
            if (field == null)
                field = makeFixture()
            return field
        }

    override var dynamicID: ItemID = originalID
    override val originalName = "FIXTUREBASE"
    override var baseMass = 1.0
    override var stackable = true
    override var inventoryCategory = Category.MISC
    override val isUnique = false
    override val isDynamic = false
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("itemplaceholder_32")
    override var baseToolSize: Double? = baseMass

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        (INGAME as TerrarumIngame).blockMarkingActor.let {
            it.setGhost(ghostItem!!)
            it.isVisible = true
            it.update(delta)
            it.setGhostColourBlock()
            mouseInInteractableRange(actor) { it.setGhostColourAllow(); true }
        }
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        (INGAME as TerrarumIngame).blockMarkingActor.let {
            it.unsetGhost()
            it.isVisible = false
            it.setGhostColourNone()
        }
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) {
        val item = ghostItem!!//makeFixture()

        item.spawn(Terrarum.mouseTileX, Terrarum.mouseTileY - item.blockBox.height + 1)
        // return true when placed, false when cannot be placed
    }

}