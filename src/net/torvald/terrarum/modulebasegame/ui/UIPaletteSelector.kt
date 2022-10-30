package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_BLACK
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINSMenu

/**
 * Created by minjaesong on 2019-02-03.
 */
class UIPaletteSelector(val parent: BuildingMaker) : UICanvas() {

    override var width = 36
    override var height = 72
    override var openCloseTime = 0f

    val LINE_HEIGHT = 24
    val TEXT_OFFSETX = 3f
    val TEXT_OFFSETY = (LINE_HEIGHT - App.fontGame.lineHeight) / 2f

    fun mouseOnTitleBar() =
            relativeMouseX in 0 until width && relativeMouseY in 0 until LINE_HEIGHT

    var fore: ItemID = Block.STONE_BRICKS
    var back: ItemID = Block.GLASS_CRUDE

    private val titleText = "Pal."

    private val swapIcon: Texture

    init {
        // make swap icon, because I can't be bothered to make yet another tga
        val clut = intArrayOf(0, 0xaaaaaaff.toInt(), -1, -1)

        val swapIconPixmap = Pixmap(13, 13, Pixmap.Format.RGBA8888)
        arrayOf(
                0b00_00_11_01_00_00_00_00_00_00_00_00_00,
                0b00_11_11_01_00_00_00_00_00_00_00_00_00,
                0b11_11_11_11_11_11_11_11_11_11_01_00_00,
                0b01_11_11_01_01_01_01_01_01_11_01_00_00,
                0b00_01_11_01_00_00_00_00_00_11_01_00_00,
                0b00_00_01_01_00_00_00_00_00_11_01_00_00,
                0b00_00_00_00_00_00_00_00_00_11_01_00_00,
                0b00_00_00_00_00_00_00_00_00_11_01_00_00,
                0b00_00_00_00_00_00_00_00_00_11_01_00_00,
                0b00_00_00_00_00_00_00_11_11_11_11_11_01,
                0b00_00_00_00_00_00_00_01_11_11_11_01_01,
                0b00_00_00_00_00_00_00_00_01_11_01_01_00,
                0b00_00_00_00_00_00_00_00_00_01_01_00_00
        ).reversed().forEachIndexed { index, bits ->
            for (shiftmask in 12 downTo 0) {
                val bit = bits.ushr(shiftmask * 2).and(3)

                swapIconPixmap.drawPixel(12 - shiftmask, index, clut[bit])
            }
        }

        swapIcon = Texture(swapIconPixmap)
        swapIconPixmap.dispose()

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // draw title bar
        batch.color = UINSMenu.DEFAULT_TITLEBACKCOL
        blendNormalStraightAlpha(batch)
        Toolkit.fillArea(batch, 0, 0, width, LINE_HEIGHT)

        // draw "Pal."
        batch.color = UINSMenu.DEFAULT_TITLETEXTCOL
        App.fontGame.draw(batch, titleText, TEXT_OFFSETX, TEXT_OFFSETY)

        // draw background
        batch.color = CELLCOLOUR_BLACK
        Toolkit.fillArea(batch, 0, LINE_HEIGHT, 36, 48)

        // draw back and fore selection
        batch.color = Color.WHITE
        // TODO carve the overlap
        batch.draw(ItemCodex.getItemImage(back), 14f, 41f)
        batch.draw(ItemCodex.getItemImage(fore), 6f, 33f)
        App.fontSmallNumbers.draw(batch, fore.toString(), 3f, 61f)

        // draw swap icon
        batch.color = Color.WHITE
        batch.draw(swapIcon, 22f, 26f)

    }

    override fun updateUI(delta: Float) {

    }

    fun swapForeAndBack() {
        val t = fore
        fore = back
        back = t
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

        // if either of the block is down, open palette window of the parent
        if ((relativeMouseX in 14..30 && relativeMouseY in 41..57) || (relativeMouseX in 6..22 && relativeMouseY in 33..49)) {
            parent.uiPalette.isVisible = true
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        swapDown = false
        return true
    }
}