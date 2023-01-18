package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.VectorArray.Companion.NULLVEC
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitems.*

/**
 * Created by minjaesong on 2016-03-15.
 */
open class DroppedItem : ActorWithBody {

    companion object {
        const val NO_PICKUP_TIME = 1f
        const val MERGER_RANGE = 8.0 * TILE_SIZED // the wanted distance, squared
    }

    var itemID: ItemID = ""; private set

    @Transient private var visualItemID = ""

    @Transient private var textureRegion: TextureRegion? = null // deserialiser won't call setter of the fields

    var itemCount = 1L

    protected constructor()

    private var timeSinceSpawned = 0f

    fun canBePickedUp() = timeSinceSpawned > NO_PICKUP_TIME && !flagDespawn

    private val randKey1 = (Math.random() * 256).toInt()
    private val randKey2 = (Math.random() * 256).toInt()

    override var lightBoxList = listOf(Lightbox(this.hitbox.clone().setPosition(0.0, 0.0), NULLVEC))
        // the Cvec will be calculated dynamically on Update
    override var shadeBoxList = listOf(Lightbox(this.hitbox.clone().setPosition(0.0, 0.0), NULLVEC))
        // the Cvec will be calculated dynamically on Update

    /**
     * @param topLeftX world-wise coord
     * @param topLeftY world-wise coord
     */
    constructor(itemID: ItemID, topLeftX: Double, topLeftY: Double) : super(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT) {
        this.itemID = itemID

        if (itemID.isActor())
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = if (itemID.isItem() || itemID.isWire())
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
        if (visualItemID == "") {
            visualItemID = BlockCodex.getOrNull(itemID)?.world ?: itemID
        }
        if (textureRegion == null) {
            textureRegion = ItemCodex.getItemImage(visualItemID)!!
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
        if (this.itemID.isBlock() || this.itemID.isItem()) {
            BlockCodex[this.itemID].let {
                this.lightBoxList[0].light = it.getLumCol(randKey1, randKey2)
            }
        }

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