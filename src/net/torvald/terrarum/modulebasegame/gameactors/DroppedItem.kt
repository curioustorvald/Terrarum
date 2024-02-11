package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitems.*
import org.dyn4j.geometry.Vector2

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

    protected constructor() : super(RenderOrder.OVERLAY, PhysProperties.PHYSICS_OBJECT)

    private var timeSinceSpawned = 0f

    fun canBePickedUp() = timeSinceSpawned > NO_PICKUP_TIME && !flagDespawn

    private val randKey1 = (Math.random() * 256).toInt()
    private val randKey2 = (Math.random() * 256).toInt()

    override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TILE_SIZED, TILE_SIZED), Cvec(0)))
        // the Cvec will be calculated dynamically on Update
    override var shadeBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TILE_SIZED, TILE_SIZED), Cvec(0)))
        // the Cvec will be calculated dynamically on Update

    /**
     * @param topLeftX world-wise coord
     * @param topLeftY world-wise coord
     */
    constructor(itemID: ItemID, centreX: Double, bottomY: Double, spawnVelo: Vector2? = null) : super(RenderOrder.OVERLAY, PhysProperties.PHYSICS_OBJECT) {
        this.itemID = itemID

        if (itemID.isActor())
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = ((ItemCodex[itemID]?.mass) ?: 2.0).coerceAtLeast(2.0)

        actorValue[AVKey.SCALE] = ItemCodex[itemID]?.scale ?: 1.0

        setHitboxDimension(
                textureRegion?.regionWidth ?: TILE_SIZE,
                textureRegion?.regionHeight ?: TILE_SIZE,
                0, 0
        )

        setPosition(centreX, bottomY)

        // random horizontal movement
        if (spawnVelo == null) {
            val magn = Math.random() * 1.3
            externalV.x = if (isWalled(hitbox, COLLIDING_LEFT))
                Math.cos(Math.random() * Math.PI / 2) * magn
            else if (isWalled(hitbox, COLLIDING_RIGHT))
                Math.cos(Math.random() * Math.PI / 2 + Math.PI / 2) * magn
            else
                Math.cos(Math.random() * Math.PI) * magn
        }
        else {
            externalV.set(spawnVelo)
        }

        actorValue.set(AVKey.FALLDAMPENMULT, 0.1)

        printdbg(this, "DroppedItem with itemID '${itemID}'")
    }


    override fun reload() {
        super.reload()
        actorValue.set(AVKey.FALLDAMPENMULT, 0.1)
    }


    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        // deserialiser won't call setter of the fields
        if (visualItemID == "") {
            visualItemID = BlockCodex.getOrNull(itemID)?.world ?: itemID
        }
        if (textureRegion == null) {
            textureRegion = ItemCodex.getItemImage(visualItemID) ?: throw NullPointerException("No Item image for ${visualItemID}")
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

    private fun getLum(itemID: ItemID): Cvec {
        return if (itemID.isBlock()) {
            BlockCodex[itemID].getLumCol(randKey1, randKey2)
        }
        else if (itemID.isWall())
            BlockCodex[itemID.substringAfter('@')].getLumCol(randKey1, randKey2)
        else {
            Cvec(
                ItemCodex[itemID]?.itemProperties?.getAsFloat(AVKey.LUMR) ?: 0f,
                ItemCodex[itemID]?.itemProperties?.getAsFloat(AVKey.LUMG) ?: 0f,
                ItemCodex[itemID]?.itemProperties?.getAsFloat(AVKey.LUMB) ?: 0f,
                ItemCodex[itemID]?.itemProperties?.getAsFloat(AVKey.LUMA) ?: 0f,
            )
        }
    }

    override fun updateImpl(delta: Float) {
        renderOrder = RenderOrder.OVERLAY // for some reason the "overlaying" won't work without this

        if (this.itemID.isBlock() || this.itemID.isItem()) {
            this.lightBoxList[0].light = getLum(this.itemID)
        }

        super.updateImpl(delta)

        timeSinceSpawned += delta

        // merge into the already existing droppeditem with isStationary==true if one is detected
        if (!this.isStationary) {
            INGAME.findNearestActor(this) { it is DroppedItem && it.itemID == this.itemID && it.isStationary && !it.flagDespawn }?.let {
                if (it.distance <= MERGER_RANGE) {
//                    printdbg(this, "Dropped merger $itemID (${(it.get() as DroppedItem).itemCount} -> ${(it.get() as DroppedItem).itemCount + this.itemCount})")
                    (it.get() as DroppedItem).itemCount += this.itemCount
                    this.flagDespawn()
                }
            }
        }
    }

    fun onItemPickup(actor: ActorWithBody) {
        flagDespawn = true
        (actor as Pocketed).inventory.add(itemID, itemCount)
        Terrarum.ingame!!.sendItemPickupNoticelet(itemID, itemCount)
        ItemCodex[itemID]!!.effectOnPickup(actor)
    }
}