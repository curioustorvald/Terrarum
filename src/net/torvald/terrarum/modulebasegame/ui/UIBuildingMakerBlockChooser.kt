package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_WHITE
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_BACKGROUNDCOL
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2019-02-14.
 */
class UIBuildingMakerBlockChooser(val parent: BuildingMaker): UICanvas() {

    companion object {
        const val TILES_X = 8
        const val TILES_Y = 8

        const val TILESREGION_SIZE = 24
        const val MENUBAR_SIZE = 72
        const val SCROLLBAR_SIZE = 24

        const val WIDTH = TILES_X * TILESREGION_SIZE + SCROLLBAR_SIZE + MENUBAR_SIZE
        const val HEIGHT = TILES_Y * TILESREGION_SIZE
    }

    override var width = WIDTH
    override var height = HEIGHT
    override var openCloseTime = 0f

    val palette = ArrayList<UIItemImageButton>()

    // TODO scrolling of the palette, as the old method flat out won't work with The Flattening

    private val tabs = UIItemTextButtonList(
        this, 36, arrayOf("Terrain", "Wall", "Wire"),
        0, 0, textAreaWidth = MENUBAR_SIZE, width = MENUBAR_SIZE,
        defaultSelection = 0,
        backgroundCol = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL
    )
    private val closeButton = UIItemTextButtonList(
        this, 36, arrayOf("Close"),
        0, this.height - UIItemTextButtonList.DEFAULT_LINE_HEIGHT,
        width = MENUBAR_SIZE, textAreaWidth = MENUBAR_SIZE,
        backgroundCol = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL
    )

    init {

        BlockCodex.getAll().filter {
            (try {
                ItemCodex.getItemImage(it.id)
            }
            catch (e: NullPointerException) {
                null
            }) != null
        }.filter { !it.hasTag("INTERNAL") }.sortedBy { it.id }.forEachIndexed { index, prop ->
            val paletteItem = UIItemImageButton(
                this, ItemCodex.getItemImage(prop.id)!!,
                initialX = MENUBAR_SIZE + (index % TILES_X) * TILESREGION_SIZE,
                initialY = (index / TILES_X) * TILESREGION_SIZE,
                highlightable = false,
                width = TILESREGION_SIZE,
                height = TILESREGION_SIZE,
                highlightCol = Color.WHITE,
                activeCol = Color.WHITE
            )

            paletteItem.clickOnceListener = { _, _ ->
                parent.setPencilColour(prop.id)
            }

            uiItems.add(paletteItem)
            palette.add(paletteItem)
        }
    }

    private val scrollOverflowSize = ((palette.size - TILES_X * TILES_Y).toDouble() / TILES_X).ceilToDouble().coerceAtLeast(0.0)

    private val scrollBar = UIItemVertSlider(
        this, WIDTH - UIItemVertSlider.WIDTH, 0, 0.0, 0.0, scrollOverflowSize, height, SCROLLBAR_SIZE
    ).also {
        println("scrollOverflowSize = $scrollOverflowSize")

        addUIitem(it)
        it.scrolledListener = { scrollX, scrollY ->

        }
    }


    override fun updateUI(delta: Float) {
        if (!scrollBar.mouseUp) palette.forEach { it.update(delta) }
        tabs.update(delta)
        closeButton.update(delta)
        if (closeButton.mousePushed) {
            closeButton.deselect()
            closeGracefully()
        }

        uiItems.forEach { it.update(delta) }
    }

    private fun closeGracefully() {
        this.isVisible = false
        parent.tappedOnUI = true
    }

    private fun rebuildPalette() {

    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)

        // background
        batch.color = DEFAULT_BACKGROUNDCOL
        Toolkit.fillArea(batch, 0, 0, width, height)

        // gaps between tabs and close button
        batch.color = DEFAULT_BACKGROUNDCOL
        Toolkit.fillArea(batch, 0f, tabs.height.toFloat(), MENUBAR_SIZE.toFloat(), height.toFloat() - (tabs.height + closeButton.height))

        // the actual buttons
        tabs.render(batch, camera)
        closeButton.render(batch, camera)

        uiItems.forEach { it.render(batch, camera) }
    }

    private var dragOriginX = 0 // relative mousepos
    private var dragOriginY = 0 // relative mousepos
    private var dragForReal = false

    private fun mouseOnDragHandle() = relativeMouseX in 0 until MENUBAR_SIZE && relativeMouseY in tabs.height until height - closeButton.height

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (mouseOnDragHandle()) {
            if (dragForReal) {
                handler.setPosition(
                    (screenX / App.scr.magn - dragOriginX).roundToInt(),
                    (screenY / App.scr.magn - dragOriginY).roundToInt()
                )
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