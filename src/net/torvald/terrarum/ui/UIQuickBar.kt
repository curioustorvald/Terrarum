package net.torvald.terrarum.ui

import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.itemproperties.ItemCodex
import org.newdawn.slick.Color
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
    override var openCloseTime: Millisec = 160

    private val startPointX = ItemSlotImageBuilder.slotLarge.width / 2
    private val startPointY = ItemSlotImageBuilder.slotLarge.height / 2

    override var handler: UIHandler? = null

    private var selection: Int
        get() = Terrarum.ingame!!.player?.actorValue?.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL) ?: 0
        set(value) { Terrarum.ingame!!.player?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, value) }


    override fun update(gc: GameContainer, delta: Int) {
    }

    override fun render(gc: GameContainer, g: Graphics) {

        for (i in 0..SLOT_COUNT - 1) {
            val color = if (i == selection)
                ItemSlotImageBuilder.COLOR_WHITE
            else
                ItemSlotImageBuilder.COLOR_BLACK

            val image = if (i == selection)
                ItemSlotImageBuilder.produceLarge(color, i + 1)
            else
                ItemSlotImageBuilder.produce(color, i + 1)

            val slotX = startPointX + (CELL_SIZE + gutter).times(i).toFloat()
            val slotY = startPointY.toFloat()

            // draw slots
            g.drawImage(
                    image,
                    slotX,
                    slotY,
                    Color(1f, 1f, 1f, handler!!.opacity * finalOpacity)
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
        selection = selection.plus(if (change > 1) 1 else if (change < -1) -1 else 0).rem(SLOT_COUNT)
        if (selection < 0) selection += SLOT_COUNT
    }

    companion object {
        val finalOpacity = 0.8f

        const val SLOT_COUNT = 10
        const val CELL_SIZE = 32
    }
}