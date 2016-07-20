package net.torvald.terrarum.ui

import com.jme3.math.FastMath
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
    private val cellSize = 32

    private val slotCount = UIQuickBar.SLOT_COUNT
    private val roundRectRadius = 6

    override var width: Int = cellSize * 7
    override var height: Int = width
    /**
     * In milliseconds
     */
    override var openCloseTime: Int = 160
    override var openCloseTimer: Int = 0

    override var handler: UIHandler? = null

    private val smallenSize = 0.93f

    var menuSelection: Int = -1

    override fun update(gc: GameContainer, delta: Int) {
        if (menuSelection >= 0)
            Terrarum.ingame.player.actorValue[AVKey._PLAYER_QUICKBARSEL] =
                    menuSelection % quickbarSlots
    }

    override fun render(gc: GameContainer, g: Graphics) {
        val centrePoint = Vector2(width / 2.0, height / 2.0)

        // draw radial thingies
        for (i in 0..slotCount - 1) {
            // set position
            val angle = Math.PI * 2.0 * (i.toDouble() / slotCount) + Math.PI // 180 deg monitor-wise
            val slotCentrePoint = Vector2(0.0, cellSize * 3.0).setDirection(angle) + centrePoint

            // draw cells
            g.color = if (menuSelection == i) Color(0xC0C0C0) else Color(0x404040)
            g.drawImage(ItemSlotImageBuilder.produce(
                    if (menuSelection == i)
                        ItemSlotImageBuilder.COLOR_WHITE
                    else
                        ItemSlotImageBuilder.COLOR_BLACK,
                    i + 1
            ),
                    slotCentrePoint.x.toFloat() - (cellSize / 2f),
                    slotCentrePoint.y.toFloat() - (cellSize / 2f)
            )

            // TODO draw item
        }
    }

    override fun processInput(input: Input) {
        if (handler!!.isOpened || handler!!.isOpening) {
            val cursorPos = Vector2(input.mouseX.toDouble(), input.mouseY.toDouble())
            val centre = Vector2(Terrarum.WIDTH / 2.0, Terrarum.HEIGHT / 2.0)
            val deg = (centre - cursorPos).direction.toFloat()

            menuSelection = Math.round(deg * slotCount / FastMath.TWO_PI)
            if (menuSelection < 0) menuSelection += 10
        }
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        if (openCloseTimer < openCloseTime) {
            openCloseTimer += delta

            handler!!.opacity = openCloseTimer.toFloat() / openCloseTime
            handler!!.scale = smallenSize + (1f.minus(smallenSize) * handler!!.opacity)
        }
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        if (openCloseTimer < openCloseTime) {
            openCloseTimer += delta
            handler!!.isOpened = false

            handler!!.opacity = (openCloseTime - openCloseTimer.toFloat()) / openCloseTime
            handler!!.scale = smallenSize + (1f.minus(smallenSize) * handler!!.opacity)
        }
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        openCloseTimer = 0
        handler!!.opacity = 1f
        handler!!.scale = 1f
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        openCloseTimer = 0
        handler!!.opacity = 0f
        handler!!.scale = 1f
    }
}