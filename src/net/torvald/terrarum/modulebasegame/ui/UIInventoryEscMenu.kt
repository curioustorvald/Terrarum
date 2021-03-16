package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.TitleScreen
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_UI_HEIGHT
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT

class UIInventoryEscMenu(val full: UIInventoryFull) : UICanvas() {

    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH
    override var openCloseTime = 0.0f

    private val gameMenu = arrayOf("MENU_LABEL_MAINMENU", "MENU_LABEL_DESKTOP", "MENU_OPTIONS_CONTROLS", "MENU_OPTIONS_SOUND", "MENU_LABEL_GRAPHICS")
    private val gameMenuListHeight = DEFAULT_LINE_HEIGHT * gameMenu.size
    private val gameMenuListWidth = 400
    private val gameMenuButtons = UIItemTextButtonList(
            this, gameMenu,
            (AppLoader.screenW - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y + (INVENTORY_CELLS_UI_HEIGHT - gameMenuListHeight) / 2,
            gameMenuListWidth, gameMenuListHeight,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )

    init {
        uiItems.add(gameMenuButtons)

        gameMenuButtons.selectionChangeListener = { _, new ->
            when (new) {
                0 -> AppLoader.setScreen(TitleScreen(AppLoader.batch))
                1 -> Gdx.app.exit()
            }
        }
    }

    override fun updateUI(delta: Float) {
        gameMenuButtons.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // control hints
        blendNormal(batch)
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)

        // text buttons
        gameMenuButtons.render(batch, camera)
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
}