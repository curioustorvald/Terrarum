package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.Reorientable

/**
 * Created by minjaesong on 2024-03-08.
 */
class ItemWrench(originalID: ItemID) : GameItem(originalID), FixtureInteractionBlocked {

    override val disallowToolDragging = true
    override var dynamicID: ItemID = originalID
    override var baseMass = 0.1
    override var baseToolSize: Double? = baseMass
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = "STAL" // this is to just increase the reach
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(5, 2)

    init {
        stackable = false
        isUnique = true
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        originalName = "ITEM_WRENCH"
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
        (INGAME as TerrarumIngame).getActorsUnderMouse(mwx, mwy).filterIsInstance<Reorientable>().firstOrNull()?.let { fixture ->
            fixture.orientClockwise()
            0L
        } ?: -1L
    }

    override fun startSecondaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
        (INGAME as TerrarumIngame).getActorsUnderMouse(mwx, mwy).filterIsInstance<Reorientable>().firstOrNull()?.let { fixture ->
            fixture.orientAnticlockwise()
            0L
        } ?: -1L
    }
}