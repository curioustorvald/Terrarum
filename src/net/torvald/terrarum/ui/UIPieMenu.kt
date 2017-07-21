package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UIQuickBar.Companion.CELL_SIZE
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-07-20.
 */
class UIPieMenu : UICanvas() {
    private val cellSize = UIQuickBar.CELL_SIZE

    private val slotCount = UIQuickBar.SLOT_COUNT

    private val slotDistanceFromCentre: Double
            get() = cellSize * 2.8 * handler.scale
    override var width: Int = cellSize * 7
    override var height: Int = width


    /**
     * In milliseconds
     */
    override var openCloseTime: Second = 0.16f

    private val smallenSize = 0.93f

    var selection: Int = -1

    override fun update(delta: Float) {
        if (selection >= 0)
            Terrarum.ingame!!.player.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] =
                    selection % slotCount


        // update controls
        if (handler.isOpened || handler.isOpening) {
            val cursorPos = Vector2(Terrarum.mouseScreenX.toDouble(), Terrarum.mouseScreenY.toDouble())
            val centre = Vector2(Terrarum.HALFW.toDouble(), Terrarum.HALFH.toDouble())
            val deg = -(centre - cursorPos).direction.toFloat()

            selection = Math.round(deg * slotCount / FastMath.TWO_PI)
            if (selection < 0) selection += 10

            // TODO add gamepad support
        }
    }

    override fun render(batch: SpriteBatch) {
        // draw radial thingies
        for (i in 0..slotCount - 1) {
            // set position
            val angle = Math.PI * 2.0 * (i.toDouble() / slotCount) + Math.PI // 180 deg monitor-wise
            val slotCentrePoint = Vector2(0.0, slotDistanceFromCentre).setDirection(-angle) // NOTE: NOT a center of circle!

            // draw cells
            val image = if (i == selection)
                ItemSlotImageBuilder.produceLarge(false, i + 1)
            else
                ItemSlotImageBuilder.produce(true, i + 1)

            val slotSize = image.width

            val slotX = slotCentrePoint.x.toFloat() - (slotSize / 2)
            val slotY = slotCentrePoint.y.toFloat() - (slotSize / 2)

            batch.color = Color(1f, 1f, 1f, handler.opacity * UIQuickBar.finalOpacity)
            batch.draw(
                    image,
                    slotX,
                    slotY
            )


            // draw item
            val itemPair = Terrarum.ingame!!.player.inventory.getQuickBar(i)

            if (itemPair != null) {
                val itemImage = ItemCodex.getItemImage(itemPair.item)
                val itemW = itemImage.regionWidth
                val itemH = itemImage.regionHeight

                batch.color = Color(1f, 1f, 1f, handler.opacity)
                batch.draw(
                        itemImage, // using fixed CELL_SIZE for reasons
                        slotX + (CELL_SIZE - itemW) / 2f,
                        slotY + (CELL_SIZE - itemH) / 2f
                )
            }
        }
    }

    override fun doOpening(delta: Float) {
        UICanvas.doOpeningFade(handler, openCloseTime)
        handler.scale = smallenSize + (1f.minus(smallenSize) * handler.opacity)
    }

    override fun doClosing(delta: Float) {
        UICanvas.doClosingFade(handler, openCloseTime)
        handler.scale = smallenSize + (1f.minus(smallenSize) * handler.opacity)
    }

    override fun endOpening(delta: Float) {
        UICanvas.endOpeningFade(handler)
        handler.scale = 1f
    }

    override fun endClosing(delta: Float) {
        UICanvas.endClosingFade(handler)
        handler.scale = 1f
    }

    override fun dispose() {
    }
}