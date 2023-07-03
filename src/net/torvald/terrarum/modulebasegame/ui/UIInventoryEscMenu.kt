package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum.getPlayerSaveFiledesc
import net.torvald.terrarum.Terrarum.getWorldSaveFiledesc
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.TitleScreen
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_UI_HEIGHT
import net.torvald.terrarum.modulebasegame.serialise.WriteSavegame
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT

class UIInventoryEscMenu(val full: UIInventoryFull) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val gameMenu = arrayOf(
            "MENU_IO_SAVE_GAME",
            "MENU_OPTIONS_CONTROLS",
            "MENU_LABEL_IME",
            "MENU_LABEL_LANGUAGE",
            "MENU_LABEL_QUIT",
    )
    private val gameMenuListHeight = DEFAULT_LINE_HEIGHT * gameMenu.size
    private val gameMenuListWidth = 400
    private val gameMenuButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, gameMenu,
            (width - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y() + (INVENTORY_CELLS_UI_HEIGHT - gameMenuListHeight) / 2,
            gameMenuListWidth, gameMenuListHeight,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )
    private val areYouSureMainMenuButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, arrayOf("MENU_LABEL_QUIT_CONFIRM", "MENU_LABEL_UNSAVED_PROGRESSES_WILL_BE_LOST", "MENU_LABEL_QUIT", "MENU_LABEL_CANCEL"),
            (width - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y() + (INVENTORY_CELLS_UI_HEIGHT - (DEFAULT_LINE_HEIGHT * 3)) / 2,
            gameMenuListWidth, DEFAULT_LINE_HEIGHT * 3,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    ).also {
        listOf(it.buttons[0], it.buttons[1]).forEach {
            it.skipUpdate = true
            it.isActive = false
        }
    }
    /*private val areYouSureQuitButtons = UIItemTextButtonList(
            this, DEFAULT_LINE_HEIGHT, arrayOf("MENU_LABEL_DESKTOP_QUESTION", "MENU_LABEL_DESKTOP", "MENU_LABEL_CANCEL"),
            (width - gameMenuListWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y() + (INVENTORY_CELLS_UI_HEIGHT - (DEFAULT_LINE_HEIGHT * 3)) / 2,
            gameMenuListWidth, DEFAULT_LINE_HEIGHT * 3,
            readFromLang = true,
            textAreaWidth = gameMenuListWidth,
            activeBackCol = Color(0),
            highlightBackCol = Color(0),
            inactiveCol = Color.WHITE,
            defaultSelection = null
    )*/
    private val savingUI = UIItemSaving(this, (width - UIItemSaving.WIDTH) / 2, (height - UIItemSaving.HEIGHT) / 2)
    private val keyConfigUI = UIKeyboardControlPanel(null)
    private val languageUI = UITitleLanguage(null)
    private val keyboardSetupUI = UIIMEConfig(null)

    private var oldScreen = 0
    private var screen = 0

    fun toInitScreen() {
        screen = 0
    }

    init {
        uiItems.add(gameMenuButtons)

        // `gameMenu` order
        gameMenuButtons.selectionChangeListener = { _, new ->
            when (new) {
                0 -> {
                    screen = 3; gameMenuButtons.deselect()
                    full.handler.lockToggle()
                    full.lockTransition()


                    // save the game
                    val onError = { _: Throwable ->
                        // TODO: show some error indicator
                        screen = 0
                        full.handler.unlockToggle()
                        full.unlockTransition()
                    }

                    val onSuccessful = {
                        // return to normal state
                        System.gc()
                        screen = 0
                        full.handler.unlockToggle()
                        full.unlockTransition()
                        (INGAME as TerrarumIngame).autosaveTimer = 0f
                    }


                    /*val saveTime_t = App.getTIME_T()
                    val playerSavefile = getPlayerSaveFiledesc(INGAME.playerSavefileName)
                    val worldSavefile = getWorldSaveFiledesc(INGAME.worldSavefileName)
                    INGAME.makeSavegameBackupCopy(playerSavefile)
                    WriteSavegame(saveTime_t, WriteSavegame.SaveMode.PLAYER, INGAME.playerDisk, playerSavefile, INGAME as TerrarumIngame, false, onError) {

                        INGAME.makeSavegameBackupCopy(worldSavefile)
                        WriteSavegame(saveTime_t, WriteSavegame.SaveMode.WORLD, INGAME.worldDisk, worldSavefile, INGAME as TerrarumIngame, false, onError) {
                            // callback:
                            // rebuild the disk skimmers
                            INGAME.actorContainerActive.filterIsInstance<IngamePlayer>().forEach {
                                printdbg(this, "Game Save callback -- rebuilding the disk skimmer for IngamePlayer ${it.actorValue.getAsString(AVKey.NAME)}")
//                                it.rebuildingDiskSkimmer?.rebuild()
                            }

                            // return to normal state
                            onSuccessful()
                        }
                    }*/

                    INGAME.saveTheGame(onSuccessful, onError)


                }
                1 -> {
                    screen = 4; gameMenuButtons.deselect()
                }
                2 -> {
                    screen = 1; gameMenuButtons.deselect()
                }
                3 -> {
                    screen = 5; gameMenuButtons.deselect()
                }
                4 -> {
                    screen = 2; gameMenuButtons.deselect()
                }
            }
        }
        areYouSureMainMenuButtons.selectionChangeListener = { _, new ->
            when (new) {
                2 -> {
                    areYouSureMainMenuButtons.deselect()
                    App.setScreen(TitleScreen(App.batch))
                }
                3 -> {
                    screen = 0; areYouSureMainMenuButtons.deselect()
                }
            }
        }
    }

    // Completely unrelated to the gameMenuButtons order
    private val screens = arrayOf(
            gameMenuButtons, keyboardSetupUI, areYouSureMainMenuButtons, savingUI, keyConfigUI, languageUI
    )

    // `screens` order
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
                keyboardSetupUI.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)
                areYouSureMainMenuButtons.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                savingUI.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)
                keyConfigUI.render(batch, camera)
            },
            { batch: SpriteBatch, camera: Camera ->
                // control hints
                App.fontGame.draw(batch, full.gameMenuControlHelp, full.offsetX, full.yEnd - 20)
                languageUI.render(batch, camera)
            },
    )

    // `screens` order
    private val screenTouchDowns = arrayOf(
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->
                keyboardSetupUI.touchDown(screenX, screenY, pointer, button)
            },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->
                keyConfigUI.touchDown(screenX, screenY, pointer, button)
            },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
        )

    // `screens` order
    private val screenTouchUps = arrayOf(
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->
                keyboardSetupUI.touchUp(screenX, screenY, pointer, button)
            },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->
                keyConfigUI.touchUp(screenX, screenY, pointer, button)
            },
            { screenX: Int, screenY: Int, pointer: Int, button: Int ->  },
        )

    // `screens` order
    private val screenScrolls = arrayOf(
            { amountX: Float, amountY: Float ->  },
            { amountX: Float, amountY: Float ->
                keyboardSetupUI.scrolled(amountX, amountY)
            },
            { amountX: Float, amountY: Float ->  },
            { amountX: Float, amountY: Float ->  },
            { amountX: Float, amountY: Float ->  },
            { amountX: Float, amountY: Float ->  },
    )

    override fun show() {
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }


    override fun updateUI(delta: Float) {
        val yeet = screens[screen]
        if (oldScreen != screen) {
            val yeOlde = screens[oldScreen]

            if (yeOlde is UIItem)
                yeOlde.hide()
            else if (yeOlde is UICanvas) {
                yeOlde.setAsClose()
            }

            if (yeet is UIItem)
                yeet.show()
            else if (yeet is UICanvas) {
                yeet.show()
                yeet.setPosition(0,0)
                yeet.setAsOpen()
            }
            oldScreen = screen
        }
        if (yeet is UIItem)
            yeet.update(delta)
        else if (yeet is UICanvas)
            yeet.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormalStraightAlpha(batch)
        batch.color = Color.WHITE
        screenRenders[screen](batch, camera)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)
        screenTouchDowns[screen](screenX, screenY, pointer, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchUp(screenX, screenY, pointer, button)
        screenTouchUps[screen](screenX, screenY, pointer, button)
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        super.scrolled(amountX, amountY)
        screenScrolls[screen](amountX, amountY)
        return true
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        if (screens[screen] == keyboardSetupUI) {
            keyboardSetupUI.inputStrobed(e)
        }
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        screen = 0
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        INGAME.setTooltipMessage(null)
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        screen = 0
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun dispose() {
    }
}