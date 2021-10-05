package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import net.torvald.terrarum.gameitem.ItemID

/**
 * Created by minjaesong on 2016-03-15.
 */
open class DroppedItem : ActorWithBody {

    companion object {
        const val NO_PICKUP_TIME = 1f
    }

    var itemID: ItemID = ""; private set

    @Transient private var textureRegion: TextureRegion? = null // deserialiser won't call setter of the fields

    var itemCount = 1

    protected constructor()

    private var timeSinceSpawned = 0f

    fun canBePickedUp() = timeSinceSpawned > NO_PICKUP_TIME

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
        // TODO merge into the already existing droppeditem with isStationary==true if one is detected
    }
}