package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
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

    private val slotDistanceFromCentre = cellSize * 2.7
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
        if (selection >= 0)
            Terrarum.ingame!!.player.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] =
                    selection % slotCount


    }

    override fun render(gc: GameContainer, g: Graphics) {
        val centrePoint = Vector2(width / 2.0, height / 2.0)

        // draw radial thingies
        for (i in 0..slotCount - 1) {
            // set position
            val angle = Math.PI * 2.0 * (i.toDouble() / slotCount) + Math.PI // 180 deg monitor-wise
            val slotCentrePoint = Vector2(0.0, slotDistanceFromCentre).setDirection(angle) + centrePoint

            // draw cells
            val color = if (i == selection)
                ItemSlotImageBuilder.COLOR_WHITE
            else
                ItemSlotImageBuilder.COLOR_BLACK

            g.drawImage(
                    if (i == selection)
                        ItemSlotImageBuilder.produceLarge(color, i + 1)
                    else
                        ItemSlotImageBuilder.produce(color, i + 1),
                    slotCentrePoint.x.toFloat() - (cellSize / 2f),
                    slotCentrePoint.y.toFloat() - (cellSize / 2f)
            )

            // TODO draw item
        }
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
        if (handler!!.isOpened || handler!!.isOpening) {
            val cursorPos = Vector2(input.mouseX.toDouble(), input.mouseY.toDouble())
            val centre = Vector2(Terrarum.WIDTH / 2.0, Terrarum.HEIGHT / 2.0)
            val deg = (centre - cursorPos).direction.toFloat()

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