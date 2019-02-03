package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_BLACK
import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINSMenu

/**
 * Created by minjaesong on 2019-02-03.
 */
class UIEditorPalette : UICanvas() {

    override var width = 36
    override var height = 72
    override var openCloseTime = 0f

    val LINE_HEIGHT = 24
    val TEXT_OFFSETX = 3f
    val TEXT_OFFSETY = (LINE_HEIGHT - Terrarum.fontGame.lineHeight) / 2f

    fun mouseOnTitleBar() =
            relativeMouseX in 0 until width && relativeMouseY in 0 until LINE_HEIGHT

    var fore = Block.STONE_BRICKS
    var back = Block.DIRT

    private val titleText = "Pal."

    private val swapIcon: Texture

    init {
        // make swap icon, because I can't be bothered to make yet another tga
        val swapIconPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888)
        swapIconPixmap.pixels.rewind()
        arrayOf(
                0b001000000000,
                0b011000000000,
                0b111111111100,
                0b011000000100,
                0b001000000100,
                0b000000000100,
                0b000000000100,
                0b000000000100,
                0b000000000100,
                0b000000011111,
                0b000000001110,
                0b000000000100
        ).reversed().forEachIndexed { index, bits ->
            for (shiftmask in 11 downTo 0) {
                val bit = bits.ushr(shiftmask).and(1) == 1

                swapIconPixmap.pixels.put((if (bit) -1 else 0).toLittle())
            }
        }
        swapIconPixmap.pixels.rewind()

        swapIcon = Texture(swapIconPixmap)
        swapIconPixmap.dispose()

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // draw title bar
        batch.color = UINSMenu.DEFAULT_TITLEBACKCOL
        blendNormal(batch)
        batch.fillRect(0f, 0f, width.toFloat(), LINE_HEIGHT.toFloat())

        // draw "Pal."
        batch.color = UINSMenu.DEFAULT_TITLETEXTCOL
        Terrarum.fontGame.draw(batch, titleText, TEXT_OFFSETX, TEXT_OFFSETY)

        // draw background
        batch.color = CELLCOLOUR_BLACK
        batch.fillRect(0f, LINE_HEIGHT.toFloat(), 36f, 48f)

        // draw back and fore selection
        batch.color = Color.WHITE
        // TODO carve the overlap
        batch.draw(ItemCodex.getItemImage(back), 14f, 41f)
        batch.draw(ItemCodex.getItemImage(fore), 6f, 33f)
        Terrarum.fontSmallNumbers.draw(batch, fore.toString(), 3f, 61f)

        // draw swap icon
        batch.color = Color.WHITE
        batch.draw(swapIcon, 18f, 26f)

    }

    override fun updateUI(delta: Float) {

    }

    fun swapForeAndBack() {
        // xor used, because why not?
        fore = fore xor back
        back = back xor fore
        fore = fore xor back
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }

    private var dragOriginX = 0 // relative mousepos
    private var dragOriginY = 0 // relative mousepos
    private var dragForReal = false
    private var swapDown = false

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (mouseInScreen(screenX, screenY)) {
            if (dragForReal) {
                handler.setPosition(screenX - dragOriginX, screenY - dragOriginY)
                //println("drag $screenX, $screenY")
            }
        }

        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseOnTitleBar()) {
            dragOriginX = relativeMouseX
            dragOriginY = relativeMouseY
            dragForReal = true
        }
        else {
            dragForReal = false
        }

        // make swap button work
        if (!swapDown && (relativeMouseX in 14..35 && relativeMouseY in 24..32 || relativeMouseX in 22..35 && relativeMouseY in 33..40)) {
            swapDown = true
            swapForeAndBack()
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        swapDown = false
        return true
    }
}