package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.BlendMode
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
        const val SCROLLBAR_SIZE = 16

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
    private val buttonGapClickDummy = UIItemImageButton(
            this, TextureRegion(AppLoader.textureWhiteSquare),
            width = MENUBAR_SIZE, height = height - (tabs.height + closeButton.height),
            highlightable = false,
            posX = 0, posY = tabs.height,
            activeCol = Color(0),
            inactiveCol = Color(0),
            activeBackCol = DEFAULT_BACKGROUNDCOL,
            activeBackBlendMode = BlendMode.NORMAL,
            backgroundCol = DEFAULT_BACKGROUNDCOL,
            backgroundBlendMode = BlendMode.NORMAL
    )

    override fun updateUI(delta: Float) {
        palette.forEach { it.update(delta) }
        tabs.update(delta)
        closeButton.update(delta)
        buttonGapClickDummy.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        palette.forEach { it.render(batch, camera) }

        buttonGapClickDummy.render(batch, camera)
        tabs.render(batch, camera)
        closeButton.render(batch, camera)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
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