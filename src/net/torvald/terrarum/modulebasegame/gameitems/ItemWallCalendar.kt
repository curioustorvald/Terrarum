package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.Terrarum.mouseOnPlayer
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.getModuleName
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer

/**
 * Created by minjaesong on 2023-08-08.
 */
class ItemWallCalendar(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWallCalendar") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 1.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/calendar.tga")
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_CALENDAR"

}


/**
 * Created by minjaesong on 2025-04-06.
 */
class ItemClipboard(originalID: ItemID) : FixtureItemBase(originalID, "net.torvald.terrarum.modulebasegame.gameactors.FixtureWallCalendar") {

    override var dynamicID: ItemID = originalID
    override var baseMass = 1.0
    override val canBeDynamic = false
    override val materialId = ""
    init {
        itemImage = getItemImageFromSingleImage("basegame", "sprites/fixtures/clipboard.tga")
    }

    override var baseToolSize: Double? = baseMass
    override var originalName = "ITEM_CLIPBOARD"

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
        // click on reachable wall: place it
        // click on player or unreachable: opens UI


        val reachable = mouseInInteractableRange(actor) { _, _, mx, my ->
            val item = ghostItem.getAndSet(makeFixture(originalID.getModuleName())) // renew the "ghost" otherwise you'll be spawning exactly the same fixture again; old ghost will be returned

            val spawnSuccessful = item.spawn(mx, my, if (actor is IngamePlayer) actor.uuid else null) // return true when placed, false when cannot be placed

            if (spawnSuccessful) 1L
            else if (mouseOnPlayer) -1L
            else 0L
        }

        if (reachable == -1L) {
            openEditorUI()
            return -1L
        }

        return if (reachable == 1L) 1L else -1L
    }


    fun openEditorUI() {
        TODO()
    }
}