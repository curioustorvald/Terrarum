package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.round
import net.torvald.terrarum.ui.Movement
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import kotlin.math.roundToInt

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

    private val mainGoButton = UIItemTextButton(this, "MENU_IO_LOAD_GAME", buttonX1third, buttonRowY2, buttonWidth * 3 + buttonGap * 2, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

    }
    private val mainReturnButton = UIItemTextButton(this, "MENU_LABEL_RETURN", buttonX1third, buttonRowY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            full.changePanelTo(0)
        }
    }
    private val mainRenameButton = UIItemTextButton(this, "MENU_LABEL_RENAME", buttonXcentre, buttonRowY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

    }
    private val mainDeleteButton = UIItemTextButton(this, "CONTEXT_CHARACTER_DELETE", buttonX3third, buttonRowY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {

    }

    private val confirmCancelButton = UIItemTextButton(this, "MENU_LABEL_CANCEL", buttonXleft, buttonRowY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ -> full.remoCon.openUI(UILoadSavegame(full.remoCon)) }
    }
    private val confirmDeleteButton = UIItemTextButton(this, "MENU_LABEL_DELETE", buttonXright, buttonRowY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
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

    private var mainButtons = listOf(mainGoButton, mainReturnButton, mainRenameButton, mainDeleteButton)
    private var delButtons = listOf(confirmCancelButton, confirmDeleteButton)

    private val MODE_INIT = 0
    private val MODE_DELETE = 16 // are you sure?
    private val MODE_RENAME = 32 // show rename dialogue

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

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val buttonYdelta = (full.titleTopGradEnd + full.cellInterval) - full.playerButtonSelected!!.posY
        full.playerButtonSelected!!.render(batch, camera, 0, buttonYdelta)

        if (mode == MODE_DELETE) {
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval - 46)
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval + SAVE_CELL_HEIGHT + 36)

            delButtons.forEach { it.render(batch, camera) }
        }
        else if (mode == MODE_INIT) {
            mainButtons.forEach { it.render(batch, camera) }
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