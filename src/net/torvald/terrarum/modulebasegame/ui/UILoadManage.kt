package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadManage(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val buttonHeight = 24
    private val buttonGap = 10
    private val buttonWidth = 180
    private val drawX = (Toolkit.drawWidth - 480) / 2
    private val drawY = (App.scr.height - 480) / 2
    private val hx = Toolkit.hdrawWidth

    private val buttonX1third = hx - (buttonWidth * 1.5).toInt() - buttonGap
    private val buttonXcentre = hx - (buttonWidth / 2)
    private val buttonX3third = hx + (buttonWidth / 2) + buttonGap

    private val buttonXleft = drawX + (240 - buttonWidth) / 2
    private val buttonXright = drawX + 240 + (240 - buttonWidth) / 2

    private val buttonRowY = drawY + 480 - buttonHeight
    private val buttonRowY2 = buttonRowY - buttonHeight - buttonGap

    private val mainGoButton = UIItemTextButton(this,
        { Lang["MENU_IO_LOAD_GAME"] }, buttonX1third, buttonRowY, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            App.printdbg(this, "Load playerUUID: ${UILoadGovernor.playerUUID}, worldUUID: ${UILoadGovernor.worldUUID}")

            if (full.loadables.moreRecentAutosaveAvailable()) {
                TODO()
            }
            else if (full.loadables.saveAvaliable()) {
                if (full.loadables.newerSaveIsDamaged) {
                    UILoadGovernor.previousSaveWasLoaded = true
                }

                full.loadManageSelectedGame = full.loadables.getLoadableSave()!!

                mode = MODE_LOAD
            }
        }
    }
    private val mainNoGoButton = UIItemTextButton(this,
        { Lang["ERROR_SAVE_CORRUPTED"].replace(".","") }, buttonX1third, buttonRowY, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.isEnabled = false
    }
    private val mainReturnButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonX1third, buttonRowY2, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            full.changePanelTo(0)
        }
    }
    private val mainRenameButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_RENAME"] }, buttonXcentre, buttonRowY2, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_RENAME
        }
    }
    private val mainDeleteButton = UIItemTextButton(this,
        { Lang["CONTEXT_CHARACTER_DELETE"] }, buttonX3third, buttonRowY2, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_DELETE
        }
    }

    private val confirmCancelButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CANCEL"] }, buttonXleft, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_INIT
        }
    }
    private val confirmDeleteButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_DELETE"] }, buttonXright, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
        it.clickOnceListener = { _,_ ->
            val pu = full.playerButtonSelected!!.playerUUID
            val wu = full.playerButtonSelected!!.worldUUID
            App.savegamePlayers[pu]?.moveToRecycle(App.recycledPlayersDir)?.let {
                App.sortedPlayers.remove(pu)
                App.savegamePlayers.remove(pu)
                App.savegamePlayersName.remove(pu)
            }
            // don't delete the world please
            full.remoCon.openUI(UILoadSavegame(full.remoCon))
        }
    }

    private var mode = 0

    private var mainButtons0 = listOf(mainGoButton, mainReturnButton, mainRenameButton, mainDeleteButton)
    private var mainButtons1 = listOf(mainNoGoButton, mainReturnButton, mainRenameButton, mainDeleteButton)
    private var delButtons = listOf(confirmCancelButton, confirmDeleteButton)

    private val mainButtons: List<UIItemTextButton>
        get() = if (full.loadables.saveAvaliable()) mainButtons0 else mainButtons1

    private val MODE_INIT = 0
    private val MODE_DELETE = 16 // are you sure?
    private val MODE_RENAME = 32 // show rename dialogue
    private val MODE_LOAD = 256 // is needed to make the static loading screen

    init {

    }

    override fun doOpening(delta: Float) {
        full.playerButtonSelected?.forceMouseDown = true
    }

    override fun doClosing(delta: Float) {
        full.playerButtonSelected?.forceMouseDown = false
    }

    override fun updateUI(delta: Float) {
        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.update(delta) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.update(delta) }
            }
        }
    }

    private var loadFiredFrameCounter = 0

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val buttonYdelta = (full.titleTopGradEnd + full.cellInterval) - full.playerButtonSelected!!.posY
        full.playerButtonSelected!!.render(batch, camera, 0, buttonYdelta)

        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.render(batch, camera) }
            }
            MODE_DELETE -> {
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval - 46)
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval + SAVE_CELL_HEIGHT + 36)

                delButtons.forEach { it.render(batch, camera) }
            }
            MODE_LOAD -> {
                loadFiredFrameCounter += 1
                // to hide the "flipped skybox" artefact
                batch.end()

                gdxClearAndEnableBlend(.094f, .094f, .094f, 0f)

                batch.begin()

                batch.color = Color.WHITE
                val txt = Lang["MENU_IO_LOADING"]
                App.fontGame.draw(batch, txt, (App.scr.width - App.fontGame.getWidth(txt)) / 2f, (App.scr.height - App.fontGame.lineHeight) / 2f)

                if (loadFiredFrameCounter == 2) {
                    LoadSavegame(full.loadManageSelectedGame)
                }
            }
        }
    }

    override fun dispose() {
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
        }

        return true
    }

}