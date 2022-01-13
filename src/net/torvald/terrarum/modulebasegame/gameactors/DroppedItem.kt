package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2016-03-15.
 */
open class DroppedItem : ActorWithBody {

    companion object {
        const val NO_PICKUP_TIME = 1f
        const val MERGER_RANGE = 8.0 * TILE_SIZED // the wanted distance, squared
    }

    var itemID: ItemID = ""; private set

    @Transient private var textureRegion: TextureRegion? = null // deserialiser won't call setter of the fields

    var itemCount = 1

    protected constructor()

    private var timeSinceSpawned = 0f

    fun canBePickedUp() = timeSinceSpawned > NO_PICKUP_TIME && !flagDespawn

    constructor(itemID: ItemID, topLeftX: Int, topLeftY: Int) : super(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT) {
        this.itemID = itemID

        if (itemID.startsWith("actor@"))
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = if (itemID.startsWith("item@") || itemID.startsWith("wire@"))
            (ItemCodex[itemID]?.mass ?: 2.0).coerceAtMost(2.0)
        else
            BlockCodex[itemID].density / 1000.0 // block and wall

        actorValue[AVKey.SCALE] = ItemCodex[itemID]?.scale ?: 1.0

        setHitboxDimension(
                textureRegion?.regionWidth ?: TILE_SIZE,
                textureRegion?.regionHeight ?: TILE_SIZE,
                0, 0
        )

        setPosition(topLeftX + (hitbox.width / 2.0), topLeftY + hitbox.height)

        // random horizontal movement
        val magn = Math.random() * 1.3
        externalV.x = if (isWalled(hitbox, COLLIDING_LEFT))
            Math.cos(Math.random() * Math.PI / 2) * magn
        else if (isWalled(hitbox, COLLIDING_RIGHT))
            Math.cos(Math.random() * Math.PI / 2 + Math.PI / 2) * magn
        else
            Math.cos(Math.random() * Math.PI) * magn

    }

    override fun drawBody(batch: SpriteBatch) {
        // deserialiser won't call setter of the fields
        if (textureRegion == null) {
            textureRegion = ItemCodex.getItemImage(itemID)!!
        }

        // copy-pasted from ActorWithBody.drawSpriteInGoodPosition()
        if (world == null) return

        textureRegion?.let {
            val offsetX = (hitboxTranslateX * scale).toFloat()
            val offsetY = (it.regionHeight * scale - hitbox.height - hitboxTranslateY * scale - 1).toFloat()

            drawBodyInGoodPosition(hitbox.startX.toFloat(), hitbox.startY.toFloat()) { x, y ->
                batch.draw(it,
                        FastMath.floor(x - offsetX).toFloat(),
                        FastMath.floor(y - offsetY).toFloat(),
                        Math.floor(it.regionWidth * scale).toFloat(),
                        Math.floor(it.regionHeight * scale).toFloat()
                )
            }
        }
    }

    override fun update(delta: Float) {
        super.update(delta)

        timeSinceSpawned += delta

        // merge into the already existing droppeditem with isStationary==true if one is detected
        if (!this.isStationary) {
            INGAME.findNearestActor(this) { it is DroppedItem && it.itemID == this.itemID && it.isStationary }?.let {
                if (it.distance <= MERGER_RANGE) {
                    (it.get() as DroppedItem).itemCount += this.itemCount
                    this.flagDespawn()
                }
            }
        }
    }
}