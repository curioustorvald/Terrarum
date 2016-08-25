package net.torvald.terrarum.ui

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-07-20.
 */
class UIQuickBar : UICanvas, MouseControlled {
    private val gutter = 8
    override var width: Int = (ItemSlotImageBuilder.slotImageSize + gutter) * SLOT_COUNT
    override var height: Int = ItemSlotImageBuilder.slotImageSize + 4 + Terrarum.fontGame.lineHeight
    /**
     * In milliseconds
     */
    override var openCloseTime: Int = 160

    private val startPointX = ItemSlotImageBuilder.slotLarge.width / 2
    private val startPointY = ItemSlotImageBuilder.slotLarge.height / 2

    override var handler: UIHandler? = null

    private var selection: Int
        get() = Terrarum.ingame.player.actorValue.getAsInt(AVKey._PLAYER_QUICKBARSEL)!!
        set(value) { Terrarum.ingame.player.actorValue[AVKey._PLAYER_QUICKBARSEL] = value }

    override fun update(gc: GameContainer, delta: Int) {
    }

    override fun render(gc: GameContainer, g: Graphics) {
        for (i in 0..SLOT_COUNT - 1) {
            val color = if (i == selection)
                ItemSlotImageBuilder.COLOR_WHITE
            else
                ItemSlotImageBuilder.COLOR_BLACK

            // draw slots
            g.drawImage(
                    if (i == selection)
                        ItemSlotImageBuilder.produceLarge(color, i + 1)
                    else
                        ItemSlotImageBuilder.produce(color, i + 1),
                    startPointX + (CELL_SIZE + gutter).times(i).toFloat(),
                    startPointY.toFloat()
            )
            // draw items

        }
    }

    override fun processInput(input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        handler!!.opacity = handler!!.openCloseCounter.toFloat() / openCloseTime
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        handler!!.opacity = (openCloseTime - handler!!.openCloseCounter.toFloat()) / openCloseTime
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        handler!!.opacity = 1f
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        handler!!.opacity = 0f
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
    }

    override fun mouseWheelMoved(change: Int) {
        selection = selection.plus(if (change > 1) 1 else if (change < -1) -1 else 0).mod(SLOT_COUNT)
        if (selection < 0) selection += SLOT_COUNT
    }

    companion object {
        const val SLOT_COUNT = 10
        const val CELL_SIZE = 32
    }
}