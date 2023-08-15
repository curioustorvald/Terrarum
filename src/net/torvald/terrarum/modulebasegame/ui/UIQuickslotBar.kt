package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.toItemCountText
import net.torvald.terrarum.ui.UICanvas
import kotlin.math.roundToInt

/**
 * A bar-shaped representation of the Quickslot.
 *
 * Created by minjaesong on 2016-07-20.
 */
class UIQuickslotBar : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

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
        const val DISPLAY_OPACITY = 0.92f
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

    private val itemCntTextCol = Color(0x404040ff)

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying?.let { actor ->
            for (i in 0 until SLOT_COUNT) {
                val qs = actor.inventory.getQuickslotItem(i)
                val item = ItemCodex[qs?.itm]
                val itemHasGauge = ((item?.maxDurability ?: 0) > 0.0) || item?.stackable == true

                val image = if (i == selection)
                    ItemSlotImageFactory.produceLarge(false, (i + 1) % SLOT_COUNT, item, itemHasGauge)
                else
                    ItemSlotImageFactory.produce(true, (i + 1) % SLOT_COUNT, item)

                val slotX = cellSize / 2 + (cellSize + gutter) * i
                val slotY = cellSize / 2

                // draw slots
                drawColor.set(-1)
                drawColor.a = handler.opacity * DISPLAY_OPACITY
                batch.color = drawColor
                image.draw(batch, slotX, slotY)

                // durability meter/item count for the selected cell
                if (i == selection && item != null) {
                    if (item.maxDurability > 0.0) {
                        val percentage = item.durability / item.maxDurability
                        val barCol = UIItemInventoryCellCommonRes.getHealthMeterColour(percentage, 0f, 1f)
                        val barBack = barCol mul UIItemInventoryCellCommonRes.meterBackDarkening
                        val durabilityIndex = percentage.times(38).roundToInt()

                        // draw bar background
                        batch.color = barBack
                        batch.draw(ItemSlotImageFactory.slotImage.get(8,7), slotX - 19f, slotY - 19f)
                        // draw bar foreground
                        batch.color = barCol
                        batch.draw(ItemSlotImageFactory.slotImage.get(durabilityIndex % 10,4 + durabilityIndex / 10), slotX - 19f, slotY - 19f)
                    }
                    else if (item.stackable) {
                        val amountString = qs!!.qty.toItemCountText()
                        batch.color = Color(0xfff066_ff.toInt())
                        val textLen = amountString.length * App.fontSmallNumbers.W
                        val y = slotY + 25 - App.fontSmallNumbers.H
                        val x = slotX - 19 + (38 - textLen) / 2
                        App.fontSmallNumbers.draw(batch, amountString, x.toFloat(), y.toFloat())
                    }
                }
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

    override fun dispose() {
    }

}