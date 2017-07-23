package net.torvald.terrarum.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 16-07-20.
 */
class UIQuickBar : UICanvas() {
    private val gutter = 8
    override var width: Int = (ItemSlotImageBuilder.slotImage.tileW + gutter) * SLOT_COUNT
    override var height: Int = ItemSlotImageBuilder.slotImage.tileH + 4 + Terrarum.fontGame.lineHeight.toInt()
    /**
     * In milliseconds
     */
    override var openCloseTime: Second = 0.16f

    private val startPointX = ItemSlotImageBuilder.slotImage.tileW / 2
    private val startPointY = ItemSlotImageBuilder.slotImage.tileH / 2

    private var selection: Int
        get() = Terrarum.ingame!!.player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL) ?: 0
        set(value) { Terrarum.ingame!!.player.actorValue.set(AVKey.__PLAYER_QUICKSLOTSEL, value.fmod(SLOT_COUNT)) }
    
    override fun update(delta: Float) {
    }

    override fun render(batch: SpriteBatch) {

        for (i in 0..SLOT_COUNT - 1) {
            val image = if (i == selection)
                ItemSlotImageBuilder.produceLarge(false, (i + 1) % SLOT_COUNT)
            else
                ItemSlotImageBuilder.produce(true, (i + 1) % SLOT_COUNT)

            val slotX = startPointX + (CELL_SIZE + gutter).times(i).toFloat()
            val slotY = startPointY.toFloat()

            // draw slots
            batch.color = Color(1f, 1f, 1f, handler.opacity * finalOpacity)
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
        handler.opacity = handler.openCloseCounter.toFloat() / openCloseTime
    }

    override fun doClosing(delta: Float) {
        handler.opacity = (openCloseTime - handler.openCloseCounter.toFloat()) / openCloseTime
    }

    override fun endOpening(delta: Float) {
        handler.opacity = 1f
    }

    override fun endClosing(delta: Float) {
        handler.opacity = 0f
    }

    override fun scrolled(amount: Int): Boolean {
        // super.scrolled(amount) // no UIItems here

        selection = selection.plus(if (amount > 1) 1 else if (amount < -1) -1 else 0).fmod(SLOT_COUNT)

        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        selection = when (keycode) {
            Input.Keys.NUM_1 -> 0
            Input.Keys.NUM_2 -> 1
            Input.Keys.NUM_3 -> 2
            Input.Keys.NUM_4 -> 3
            Input.Keys.NUM_5 -> 4
            Input.Keys.NUM_6 -> 5
            Input.Keys.NUM_7 -> 6
            Input.Keys.NUM_8 -> 7
            Input.Keys.NUM_9 -> 8
            Input.Keys.NUM_0 -> 9
            else -> return false
        }

        return true
    }

    override fun dispose() {
    }


    companion object {
        val finalOpacity = 0.8f

        const val SLOT_COUNT = 10
        const val CELL_SIZE = 32
    }
}