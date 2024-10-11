package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.modulebasegame.gameactors.Reorientable
import net.torvald.unicode.getMouseButton

/**
 * Created by minjaesong on 2024-03-08.
 */
class ItemWrench(originalID: ItemID) : GameItem(originalID), FixtureInteractionBlocked {
    override val tooltipHash = 10003L

    companion object {
        private val SP = "\u3000"
        private val ML = getMouseButton(App.getConfigInt("config_mouseprimary"))
        private val MR = getMouseButton(App.getConfigInt("config_mousesecondary"))
    }

    override val disallowToolDragging = true
    override var dynamicID: ItemID = originalID
    override var baseMass = 0.1
    override var baseToolSize: Double? = baseMass
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = "STAL" // this is to just increase the reach
    init {
        itemImage = CommonResourcePool.getAsItemSheet("basegame.items").get(5, 2)
    }

    init {
        stackable = false
        isUnique = true
        equipPosition = GameItem.EquipPosition.HAND_GRIP
        originalName = "ITEM_WRENCH"
    }

    override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
        // flipping 'eck this was annoying
        /*var q = -1L
        q = mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
            (INGAME as TerrarumIngame).world.getWireGraphOf(mtx, mty, "wire@basegame:256").let { cnx ->
                if (cnx != null) {
                    acquireTooltip("$ML ${Lang["GAME_ACTION_DISMANTLE"]}\n$MR ${Lang["MENU_CONTROLS_ROTATE"]}")

                }
                else {
                    releaseTooltip()
                }
            }
            0L
        }
        if (q == -1L)
            releaseTooltip()
        */
    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
        (INGAME as TerrarumIngame).getActorsUnderMouse(mwx, mwy).filterIsInstance<Reorientable>().firstOrNull()?.let { fixture ->
            fixture.orientClockwise()
            0L
        } ?: (INGAME as TerrarumIngame).world.getWireGraphOf(mtx, mty, "wire@basegame:256")?.let {
            (INGAME as TerrarumIngame).world.removeTileWireNoReconnect(mtx, mty, "wire@basegame:256", false)
            (INGAME as TerrarumIngame).queueActorAddition(DroppedItem("wire@basegame:256", mtx * TILE_SIZED, mty * TILE_SIZED))
            0L
        } ?: -1L
    }

    override fun startSecondaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
        (INGAME as TerrarumIngame).getActorsUnderMouse(mwx, mwy).filterIsInstance<Reorientable>().firstOrNull()?.let { fixture ->
            fixture.orientAnticlockwise()
            0L
        } ?: (INGAME as TerrarumIngame).world.getWireGraphOf(mtx, mty, "wire@basegame:256")?.let {
            val old = it
            val new = when (old) {
                5 -> 10
                10 -> 5
                else -> old
            }
            (INGAME as TerrarumIngame).world.setWireGraphOf(mtx, mty, "wire@basegame:256", new)
            0L
        } ?: -1L
    }
}