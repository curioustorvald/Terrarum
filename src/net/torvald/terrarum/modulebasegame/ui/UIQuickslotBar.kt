package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.UICanvas

/**
 * A bar-shaped representation of the Quickslot.
 *
 * Created by minjaesong on 2016-07-20.
 */
class UIQuickslotBar : UICanvas() {
    private val cellSize = ItemSlotImageFactory.slotImage.tileW // 38

    private val gutter = 10 - 6 // do -6 to get a gutter size of not-enlarged cells
    override var width: Int = cellSize * SLOT_COUNT + gutter * (SLOT_COUNT - 1) // 452
    override var height: Int = ItemSlotImageFactory.slotImage.tileH + 4 + AppLoader.fontGame.lineHeight.toInt()
    /**
     * In milliseconds
     */
    override var openCloseTime: Second = COMMON_OPEN_CLOSE

    private var selection: Int
        get() = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.actorValue?.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL) ?: 0
        set(value) { (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, value.fmod(SLOT_COUNT)) }


    companion object {
        const val SLOT_COUNT = 10
        const val DISPLAY_OPACITY = 0.8f
        const val COMMON_OPEN_CLOSE = 0.12f
    }


    override fun updateUI(delta: Float) {
    }

    private val drawColor = Color(1f, 1f, 1f, 1f)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        for (i in 0..SLOT_COUNT - 1) {
            val item = ItemCodex[(Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.inventory?.getQuickslot(i)?.itm]

            val image = if (i == selection)
                ItemSlotImageFactory.produceLarge(false, (i + 1) % SLOT_COUNT, item)
            else
                ItemSlotImageFactory.produce(true, (i + 1) % SLOT_COUNT, item)

            val slotX = cellSize / 2 + (cellSize + gutter) * i
            val slotY = cellSize / 2

            // draw slots
            drawColor.a = handler.opacity * DISPLAY_OPACITY
            batch.color = drawColor
            image.draw(batch, slotX, slotY)

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

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        // super.scrolled(amount) // no UIItems here

        selection = selection.plus(if (amountX > 1) 1 else if (amountX < -1) -1 else 0).fmod(SLOT_COUNT)

        return true
    }

    override fun dispose() {
    }

}