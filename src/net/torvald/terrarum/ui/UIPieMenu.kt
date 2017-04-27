package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UIQuickBar.Companion.CELL_SIZE
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-07-20.
 */
class UIPieMenu : UICanvas {
    private val cellSize = UIQuickBar.CELL_SIZE

    private val slotCount = UIQuickBar.SLOT_COUNT

    private val slotDistanceFromCentre: Double
            get() = cellSize * 2.7 * handler!!.scale
    override var width: Int = cellSize * 7
    override var height: Int = width

    override var handler: UIHandler? = null

    /**
     * In milliseconds
     */
    override var openCloseTime: Millisec = 160

    private val smallenSize = 0.93f

    var selection: Int = -1

    override fun update(gc: GameContainer, delta: Int) {
        if (Terrarum.ingame!!.player != null) {
            if (selection >= 0)
                Terrarum.ingame!!.player!!.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] =
                        selection % slotCount
        }
    }

    override fun render(gc: GameContainer, g: Graphics) {
        // draw radial thingies
        for (i in 0..slotCount - 1) {
            // set position
            val angle = Math.PI * 2.0 * (i.toDouble() / slotCount) + Math.PI // 180 deg monitor-wise
            val slotCentrePoint = Vector2(0.0, slotDistanceFromCentre).setDirection(-angle)// + centrePoint

            // draw cells
            val color = if (i == selection)
                ItemSlotImageBuilder.COLOR_WHITE
            else
                ItemSlotImageBuilder.COLOR_BLACK

            val image = if (i == selection)
                ItemSlotImageBuilder.produceLarge(color, i + 1)
            else
                ItemSlotImageBuilder.produce(color, i + 1)

            val slotSize = image.width

            val slotX = slotCentrePoint.x.toFloat() - (slotSize / 2) + Terrarum.HALFW
            val slotY = slotCentrePoint.y.toFloat() - (slotSize / 2) + Terrarum.HALFH

            g.drawImage(
                    image,
                    slotX,
                    slotY,
                    Color(1f, 1f, 1f, handler!!.opacity * UIQuickBar.finalOpacity)
            )


            // draw item
            val itemPair = Terrarum.ingame!!.player!!.inventory.getQuickBar(i)

            if (itemPair != null) {
                val itemImage = ItemCodex.getItemImage(itemPair.item)
                val itemW = itemImage.width
                val itemH = itemImage.height

                g.drawImage(
                        itemImage, // using fixed CELL_SIZE for reasons
                        slotX + (CELL_SIZE - itemW) / 2f,
                        slotY + (CELL_SIZE - itemH) / 2f,
                        Color(1f, 1f, 1f, handler!!.opacity * UIQuickBar.finalOpacity)
                )
            }
        }
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
        if (handler!!.isOpened || handler!!.isOpening) {
            val cursorPos = Vector2(input.mouseX.toDouble(), input.mouseY.toDouble())
            val centre = Vector2(Terrarum.HALFW.toDouble(), Terrarum.HALFH.toDouble())
            val deg = -(centre - cursorPos).direction.toFloat()

            selection = Math.round(deg * slotCount / FastMath.TWO_PI)
            if (selection < 0) selection += 10

            // TODO add gamepad support
        }
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        UICanvas.doOpeningFade(handler, openCloseTime)
        handler!!.scale = smallenSize + (1f.minus(smallenSize) * handler!!.opacity)
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        UICanvas.doClosingFade(handler, openCloseTime)
        handler!!.scale = smallenSize + (1f.minus(smallenSize) * handler!!.opacity)
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        UICanvas.endOpeningFade(handler)
        handler!!.scale = 1f
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        UICanvas.endClosingFade(handler)
        handler!!.scale = 1f
    }
}