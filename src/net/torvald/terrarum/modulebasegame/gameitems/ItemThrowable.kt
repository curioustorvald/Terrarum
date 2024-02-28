package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.ui.MouseLatch
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-02-14.
 */
open class ItemThrowable(originalID: ItemID, private val throwableActorClassName: String) : GameItem(originalID) {
    override var baseMass = 1.0
    override var baseToolSize: Double? = baseMass
    override var inventoryCategory = Category.TOOL
    override val canBeDynamic = false
    override val materialId = ""
    override var equipPosition = EquipPosition.HAND_GRIP
    override val disallowToolDragging = true

    init {
        ItemCodex.fixtureToSpawnerItemID[throwableActorClassName] = originalID
    }

    protected open fun setupLobbedActor(actor: ActorWithBody) {

    }

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long = mouseInInteractableRange(actor) { mx, my, mtx, mty ->
        val (throwPos, throwForce) = getThrowPosAndVector(actor)

        val lobbed = Class.forName(throwableActorClassName).getDeclaredConstructor().newInstance() as ActorWithBody
        lobbed.setPosition(throwPos)
        lobbed.externalV.set(throwForce)
        setupLobbedActor(lobbed)

        Terrarum.ingame?.queueActorAddition(lobbed)



        1L
    }


}

/**
 * @return pair of throwing start position, throwing force
 */
fun getThrowPosAndVector(actor: ActorWithBody): Pair<Vector2, Vector2> {
    val playerCentrePos = Vector2(actor.centrePosVector) // make a COPY of the actor.centrePosPoint
    val mousePos = Vector2(Terrarum.mouseX, Terrarum.mouseY)

    val actorPowMult = actor.avStrength / 2000.0
    val relativeX = relativeXposition(actor, mousePos)
    val relativeY = mousePos.y - playerCentrePos.y
    val powX = relativeX / TILE_SIZED * 3.0 * actorPowMult
    val powY = relativeY / TILE_SIZED * 3.0 * actorPowMult

    return Pair(playerCentrePos, Vector2(powX, powY))
}


/**
 * Created by minjaesong on 2024-02-14.
 */
class ItemCherryBomb(originalID: ItemID) : ItemThrowable(originalID, "net.torvald.terrarum.modulebasegame.gameactors.ActorCherryBomb") {
    override var originalName = "ITEM_CHERRY_BOMB"
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsItemSheet("basegame.items").get(0,13)
}