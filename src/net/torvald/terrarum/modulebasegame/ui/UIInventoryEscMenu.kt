package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TitleScreen
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_UI_HEIGHT
import net.torvald.terrarum.serialise.WriteSavegame
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT
import java.io.File

class UIInventoryEscMenu(val full: UIInventoryFull) : UICanvas() {

    override var width: Int = App.scr.width
    override var height: Int = App.scr.height
    override var openCloseTime = 0.0f

    private val gameMenu = arrayOf(
            "MENU_IO_SAVE_GAME",
            "MENU_LABEL_GRAPHICS",
            "MENU_OPTIONS_CONTROLS",
            "MENU_OPTIONS_SOUND",
            "MENU_LABEL_RETURN_MAIN",
            "MENU_LABEL_DESKTOP",
    )
    private val gameMenuListHeight = DEFAULT_LINE_HEIGHT * gameMenu.size
    private val gameMenuListWidth = 400
    private val gameMenuButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, gameMenu,
            (App.scr.width - gameMenuListWidth) / 2,
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
    private val areYouSureMainMenuButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, arrayOf("MENU_LABEL_RETURN_MAIN_QUESTION", "MENU_LABEL_RETURN_MAIN", "MENU_LABEL_CANCEL"),
            (App.scr.width - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y + (INVENTORY_CELLS_UI_HEIGHT - (DEFAULT_LINE_HEIGHT * 3)) / 2,
            gameMenuListWidth, DEFAULT_LINE_HEIGHT * 3,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )
    private val areYouSureQuitButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, arrayOf("MENU_LABEL_DESKTOP_QUESTION", "MENU_LABEL_DESKTOP", "MENU_LABEL_CANCEL"),
            (App.scr.width - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y + (INVENTORY_CELLS_UI_HEIGHT - (DEFAULT_LINE_HEIGHT * 3)) / 2,
            gameMenuListWidth, DEFAULT_LINE_HEIGHT * 3,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            backgroundCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )
    private val savingUI = UIItemSaving(this, (width - UIItemSaving.WIDTH) / 2, (height - UIItemSaving.HEIGHT) / 2)

    private var screen = 0

    init {
        uiItems.add(gameMenuButtons)

        gameMenuButtons.selectionChangeListener = { _, new ->
            when (new) {
                0 -> {
                    screen = 3; gameMenuButtons.deselect()
                    full.handler.lockToggle()
                    // save the game
                    WriteSavegame(Terrarum.ingame!!.savegameArchive, File(App.defaultSaveDir, "${App.getTIME_T()}"), Terrarum.ingame!! as TerrarumIngame) {
                        // callback:
                        screen = 0
                        full.handler.unlockToggle()
                    }
                }
                4 -> {
                    screen = 2; gameMenuButtons.deselect()
                }
                5 -> {
                    screen = 1; gameMenuButtons.deselect()
                }
            }
        }
        areYouSureMainMenuButtons.selectionChangeListener = { _, new ->
            when (new) {
                1 -> {
                    areYouSureMainMenuButtons.deselect()
                    App.setScreen(TitleScreen(App.batch))
                }
                2 -> {
                    screen = 0; areYouSureMainMenuButtons.deselect()
                }
            }
        }
        areYouSureQuitButtons.selectionChangeListener = { _, new ->
            when (new) {
                1 -> Gdx.app.exit()
                2 -> {
                    screen = 0; areYouSureQuitButtons.deselect()
                }
            }
        }
    }

    private val screenUpdates = arrayOf(
            { delta: Float ->
                gameMenuButtons.update(delta)
            },
            { delta: Float ->
                areYouSureQuitButtons.update(delta)
            },
            { delta: Float ->
                areYouSureMainMenuButtons.update(delta)
            },
            { delta: Float ->
                savingUI.update(delta)
            }
    )
    private val screenRenders = arrayOf(
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)

                // text buttons
                gameMenuButtons.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)

                areYouSureQuitButtons.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)

                areYouSureMainMenuButtons.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                savingUI.render(batch, camera)
            }
    )


    override fun updateUI(delta: Float) {
        screenUpdates[screen](delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)
        batch.color = Color.WHITE
        screenRenders[screen](batch, camera)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
        screen = 0
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        screen = 0
    }

    override fun dispose() {
    }
}