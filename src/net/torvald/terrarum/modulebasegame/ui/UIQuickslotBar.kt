package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
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
    override var height: Int = ItemSlotImageFactory.slotImage.tileH + 4 + App.fontGame.lineHeight.toInt()
    /**
     * In milliseconds
     */
    override var openCloseTime: Second = COMMON_OPEN_CLOSE

    private var selection: Int = -1
        set(value) {
            (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.actorValue?.set(AVKey.__PLAYER_QUICKSLOTSEL, value.fmod(SLOT_COUNT))
            field = value
        }


    companion object {
        const val SLOT_COUNT = 10
        const val DISPLAY_OPACITY = 0.8f
        const val COMMON_OPEN_CLOSE = 0.12f
    }


    override fun updateUI(delta: Float) {
        val newSelection = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.actorValue?.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL) ?: 0

        if (selection != newSelection) {
            nameShowupFired = true
            nameShowupTimer = 0f
            nameShowupAlpha = 1f
            showupTime = App.getConfigInt("selecteditemnameshowuptime").div(1000f) // refresh game config
        }

        if (nameShowupFired) {

            nameShowupTimer += delta

            if (nameShowupTimer >= NAME_SHOWUP_DECAY + showupTime) {
                nameShowupAlpha = 0f
                nameShowupFired = false
            }
            else if (nameShowupTimer >= showupTime) {
                val a = (nameShowupTimer - showupTime) / NAME_SHOWUP_DECAY
                nameShowupAlpha = FastMath.interpolateLinear(a, 1f, 0f)
            }
        }

        selection = newSelection
    }

    private val NAME_SHOWUP_DECAY = COMMON_OPEN_CLOSE
    private var nameShowupAlpha = 0f
    private var nameShowupTimer = 0f
    private var nameShowupFired = false
    private var showupTime = App.getConfigInt("selecteditemnameshowuptime").div(1000f)

    private val drawColor = Color(1f, 1f, 1f, 1f)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.let { actor ->
            for (i in 0..SLOT_COUNT - 1) {
                val item = ItemCodex[actor.inventory.getQuickslotItem(i)?.itm]

                val image = if (i == selection)
                    ItemSlotImageFactory.produceLarge(false, (i + 1) % SLOT_COUNT, item)
                else
                    ItemSlotImageFactory.produce(true, (i + 1) % SLOT_COUNT, item)

                val slotX = cellSize / 2 + (cellSize + gutter) * i
                val slotY = cellSize / 2

                // draw slots
                drawColor.set(-1)
                drawColor.a = handler.opacity * DISPLAY_OPACITY
                batch.color = drawColor
                image.draw(batch, slotX, slotY)
            }

            if (nameShowupAlpha > 0f) {
                val selection = actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL) ?: return
                actor.inventory.getQuickslotItem(selection)?.let {
                    val item = ItemCodex[it.itm]
                    val quantity = it.qty
                    val text = "${item?.name}" + (if (item?.isUnique == true) "" else  " ($quantity)")
                    val textWidth = App.fontGame.getWidth(text)

                    drawColor.set(item?.nameColour ?: Color.WHITE)
                    drawColor.a = nameShowupAlpha
                    batch.color = drawColor
                    App.fontGame.draw(batch, text, (width - textWidth) / 2, height - 20)
                }
            }
        }

    }


    override fun doOpening(delta: Float) {
        handler.opacity = handler.openCloseCounter / openCloseTime
    }

    override fun doClosing(delta: Float) {
        handler.opacity = (openCloseTime - handler.openCloseCounter) / openCloseTime
    }

    override fun endOpening(delta: Float) {
        handler.opacity = 1f
    }

    override fun endClosing(delta: Float) {
        handler.opacity = 0f
    }

    override fun dispose() {
    }

}