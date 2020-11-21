package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_WHITE
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_BACKGROUNDCOL
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2019-02-14.
 */
class UIBuildingMakerBlockChooser(val parent: BuildingMaker): UICanvas() {

    companion object {
        const val TILES_X = 16
        const val TILES_Y = 14

        const val TILESREGION_SIZE = 24
        const val MENUBAR_SIZE = 72
        const val SCROLLBAR_SIZE = 24

        const val WIDTH = TILES_X * TILESREGION_SIZE + SCROLLBAR_SIZE + MENUBAR_SIZE
        const val HEIGHT = TILES_Y * TILESREGION_SIZE
    }

    override var width = WIDTH
    override var height = HEIGHT
    override var openCloseTime = 0f

    private val palette = Array<UIItemImageButton>(TILES_X * TILES_Y) {
        // initialise with terrain blocks
        UIItemImageButton(
                this, ItemCodex.getItemImage(it),
                initialX = MENUBAR_SIZE + (it % 16) * TILESREGION_SIZE,
                initialY = (it / 16) * TILESREGION_SIZE,
                highlightable = false,
                width = TILESREGION_SIZE,
                height = TILESREGION_SIZE,
                highlightCol = Color.WHITE,
                activeCol = Color.WHITE
        )
    }
    private val tabs = UIItemTextButtonList(
            this, arrayOf("Terrain", "Wall", "Wire"),
            0, 0, textAreaWidth = MENUBAR_SIZE, width = MENUBAR_SIZE,
            defaultSelection = 0
    )
    private val closeButton = UIItemTextButtonList(
            this, arrayOf("Close"),
            0, this.height - UIItemTextButtonList.DEFAULT_LINE_HEIGHT,
            width = MENUBAR_SIZE, textAreaWidth = MENUBAR_SIZE
    )

    init {
        palette.forEachIndexed { index, it ->
            uiItems.add(it)

            it.clickOnceListener = { _, _, _ ->
                parent.setPencilColour(paletteScroll * 16 + index)
            }
        }
    }

    override fun updateUI(delta: Float) {
        parent.tappedOnUI = true
        if (!mouseOnScroll) palette.forEach { it.update(delta) }
        tabs.update(delta)
        closeButton.update(delta)
        if (closeButton.mousePushed) {
            closeButton.deselect()
            closeGracefully()
        }

        // respond to click
        if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mouseprimary"))) {
            // scroll bar
            if (relativeMouseX in width - SCROLLBAR_SIZE until width && relativeMouseY in 0 until height) {
                mouseOnScroll = true
            }

            if (mouseOnScroll) {
                scrollBarPos = relativeMouseY - (scrollBarHeight / 2).roundToInt()
                scrollBarPos = scrollBarPos.coerceIn(0, scrollableArea - 1)
            }
        }
        else {
            mouseOnScroll = false
        }

        // rebuild if necessary
        val newPaletteScroll = ((scrollBarPos.toFloat() / scrollableArea) * paletteScrollMax).roundToInt()
        if (paletteScroll != newPaletteScroll) {
            paletteScroll = newPaletteScroll
            rebuildPalette()
        }
    }

    private val scrollbarBackCol = Color(0x000000_70)
    private var scrollBarPos = 0
    private var paletteScroll = 0
    private val paletteScrollMax = 256f - 14f
    private val scrollBarHeight = (HEIGHT - 16 * 14).toFloat()
    private val scrollableArea = HEIGHT - scrollBarHeight.roundToInt()
    private var mouseOnScroll = false

    private fun closeGracefully() {
        this.isVisible = false
        parent.tappedOnUI = true
    }

    private fun rebuildPalette() {
        palette.forEachIndexed { index, it ->
            it.image = ItemCodex.getItemImage(paletteScroll * 16 + index)
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        palette.forEach { it.render(batch, camera) }
        blendNormal(batch)

        // gaps between tabs and close button
        batch.color = DEFAULT_BACKGROUNDCOL
        batch.fillRect(0f, tabs.height.toFloat(), MENUBAR_SIZE.toFloat(), height.toFloat() - (tabs.height + closeButton.height))
        // scrollbar back
        batch.color = DEFAULT_BACKGROUNDCOL
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), 0f, SCROLLBAR_SIZE.toFloat(), height.toFloat())
        batch.color = scrollbarBackCol
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), 0f, SCROLLBAR_SIZE.toFloat(), height.toFloat())
        // scrollbar
        batch.color = CELLCOLOUR_WHITE
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), scrollBarPos.toFloat(), SCROLLBAR_SIZE.toFloat(), scrollBarHeight)

        // the actual buttons
        tabs.render(batch, camera)
        closeButton.render(batch, camera)
    }

    private var dragOriginX = 0 // relative mousepos
    private var dragOriginY = 0 // relative mousepos
    private var dragForReal = false

    private fun mouseOnDragHandle() = relativeMouseX in 0 until MENUBAR_SIZE && relativeMouseY in tabs.height until height - closeButton.height

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (mouseOnDragHandle()) {
            if (dragForReal) {
                handler.setPosition(screenX - dragOriginX, screenY - dragOriginY)
            }
        }

        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseOnDragHandle()) {
            dragOriginX = relativeMouseX
            dragOriginY = relativeMouseY
            dragForReal = true
            parent.tappedOnUI = true
        }
        else {
            dragForReal = false
        }

        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
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
        // nothing to dispose; you can't dispose the palette as its image is dynamically assigned
    }
}