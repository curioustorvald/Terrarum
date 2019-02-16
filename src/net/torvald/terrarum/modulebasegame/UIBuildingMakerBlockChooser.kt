package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blendScreen
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_BACKGROUNDCOL

/**
 * Created by minjaesong on 2019-02-14.
 */
class UIBuildingMakerBlockChooser(val parent: BuildingMaker): UICanvas() {

    companion object {
        const val TILES_X = 16
        const val TILES_Y = 14

        const val TILESREGION_SIZE = 24
        const val MENUBAR_SIZE = 80
        const val SCROLLBAR_SIZE = 24

        const val WIDTH = TILES_X*TILESREGION_SIZE + SCROLLBAR_SIZE + MENUBAR_SIZE
        const val HEIGHT = TILES_Y*TILESREGION_SIZE
    }

    override var width = WIDTH
    override var height = HEIGHT
    override var openCloseTime = 0f

    private val palette = Array<UIItemImageButton>(TILES_X * TILES_Y) {
        // initialise with terrain blocks
        UIItemImageButton(
                this, ItemCodex.getItemImage(it),
                posX = MENUBAR_SIZE + (it % 16) * TILESREGION_SIZE,
                posY = (it / 16) * TILESREGION_SIZE,
                highlightable = true,
                width = TILESREGION_SIZE,
                height = TILESREGION_SIZE,
                highlightCol = Color.WHITE,
                activeCol = Color.WHITE

        )
    }
    private val tabs = UIItemTextButtonList(
            this, arrayOf("Terrain", "Wall", "Wire", "Fixtures"),
            0, 0, textAreaWidth = MENUBAR_SIZE, width = MENUBAR_SIZE,
            defaultSelection = 0
    )
    private val closeButton = UIItemTextButtonList(
            this, arrayOf("Close"),
            0, this.height - UIItemTextButtonList.DEFAULT_LINE_HEIGHT,
            width = MENUBAR_SIZE, textAreaWidth = MENUBAR_SIZE
    )

    override fun updateUI(delta: Float) {
        palette.forEach { it.update(delta) }
        tabs.update(delta)
        closeButton.update(delta)
        if (closeButton.mousePushed) {
            parent.tappedOnUI = true
            isVisible = false
        }
    }

    private val addCol = Color(0x242424ff)
    private var scrollBarPos = 0
    private val scrollBarHeight = (HEIGHT - 16*14).toFloat()
    private val scrollableArea = HEIGHT - scrollBarHeight

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        palette.forEach { it.render(batch, camera) }

        // gaps between tabs and close button
        batch.color = DEFAULT_BACKGROUNDCOL
        batch.fillRect(0f, tabs.height.toFloat(), MENUBAR_SIZE.toFloat(), height.toFloat() - (tabs.height + closeButton.height))
        // scrollbar back
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), 0f, SCROLLBAR_SIZE.toFloat(), height.toFloat())
        blendScreen(batch)
        batch.color = addCol
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), 0f, SCROLLBAR_SIZE.toFloat(), height.toFloat())
        // scrollbar
        batch.fillRect(width - SCROLLBAR_SIZE.toFloat(), scrollBarPos.toFloat(), SCROLLBAR_SIZE.toFloat(), scrollBarHeight)
        blendNormal(batch)

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

        return true
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

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return true
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