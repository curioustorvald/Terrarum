package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2016-03-15.
 */
open class DroppedItem(private val itemID: ItemID, topLeftX: Int, topLeftY: Int) : ActorWithBody(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT) {

    private val textureRegion = ItemCodex.getItemImage(itemID)

    var itemCount = 1

    init {
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
        // copy-pasted from ActorWithBody.drawSpriteInGoodPosition()

        if (world == null) return


        val leftsidePadding = world!!.width.times(TILE_SIZE) - WorldCamera.width.ushr(1)
        val rightsidePadding = WorldCamera.width.ushr(1)

        val offsetX = hitboxTranslateX * scale
        val offsetY = (textureRegion?.regionHeight ?: TILE_SIZE) * scale - hitbox.height - hitboxTranslateY * scale - 1

        textureRegion?.let {
            // FIXME test me: this extra IF statement is supposed to not draw actors that's outside of the camera.
            //                basic code without offsetX/Y DOES work, but obviously offsets are not tested.
            if (WorldCamera.xCentre > leftsidePadding && centrePosPoint.x <= rightsidePadding) {
                // camera center neg, actor center pos
                batch.draw(it,
                        (hitbox.startX - offsetX).toFloat() + world!!.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        TILE_SIZEF * scale.toFloat(), TILE_SIZEF * scale.toFloat()
                )
            }
            else if (WorldCamera.xCentre < rightsidePadding && centrePosPoint.x >= leftsidePadding) {
                // camera center pos, actor center neg
                batch.draw(it,
                        (hitbox.startX - offsetX).toFloat() - world!!.width * TILE_SIZE,
                        (hitbox.startY - offsetY).toFloat(),
                        TILE_SIZEF * scale.toFloat(), TILE_SIZEF * scale.toFloat()
                )
            }
            else {
                batch.draw(it,
                        (hitbox.startX - offsetX).toFloat(),
                        (hitbox.startY - offsetY).toFloat(),
                        TILE_SIZEF * scale.toFloat(), TILE_SIZEF * scale.toFloat()
                )
            }
        }
    }

    override fun update(delta: Float) {
        super.update(delta)

        // TODO merge into the already existing droppeditem with isStationary==true if one is detected
    }
}