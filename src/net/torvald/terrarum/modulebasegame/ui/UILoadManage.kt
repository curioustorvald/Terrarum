package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.savegame.VDFileID
import net.torvald.terrarum.savegame.VDFileID.PLAYER_SCREENSHOT
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.tryDispose
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextLineInput
import net.torvald.unicode.EMDASH

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadManage(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val buttonHeight = full.buttonHeight
    private val buttonGap = full.buttonGap
    private val buttonWidth = full.buttonWidth
    private val drawX = full.drawX
    private val hx = Toolkit.hdrawWidth

    private val buttonX1third = hx - (buttonWidth * 1.5).toInt() - buttonGap
    private val buttonXcentre = hx - (buttonWidth / 2)
    private val buttonX3third = hx + (buttonWidth / 2) + buttonGap

    private val buttonXleft = drawX + (240 - buttonWidth) / 2
    private val buttonXright = drawX + 240 + (240 - buttonWidth) / 2

    private val buttonRowY = full.buttonRowY - buttonHeight
    private val buttonRowY2 = buttonRowY - buttonHeight - buttonGap

    private var altDown = false

    private val mainGoButton = UIItemTextButton(this,
        { if (altDown) Lang["MENU_LABEL_PREV_SAVES"] else Lang["MENU_IO_LOAD_GAME"] }, buttonX1third, buttonRowY2, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            App.printdbg(this, "Load playerUUID: ${UILoadGovernor.playerUUID}, worldUUID: ${UILoadGovernor.worldUUID}")

            /*if (full.loadables.moreRecentAutosaveAvailable()) {
                full.bringAutosaveSelectorUp()
                full.changePanelTo(2)
            }
            else */

            if (!altDown) {
                if (full.loadables.saveAvaliable()) {
                    if (full.loadables.newerSaveIsDamaged) {
                        UILoadGovernor.previousSaveWasLoaded = true
                    }

//                full.takeAutosaveSelectorDown()
                    full.loadManageSelectedGame = full.loadables.getLoadableSave()!!

                    mode = MODE_LOAD
                }
            }
            else {
                mode = MODE_PREV_SAVES
            }
        }
    }
    private val mainNoGoButton = UIItemTextButton(this,
        { Lang["ERROR_SAVE_CORRUPTED"].replace(".","") }, buttonX1third, buttonRowY2, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.isEnabled = false
    }
    private val mainImportedPlayerCreateNewWorldButton = UIItemTextButton(this,
        { Lang["CONTEXT_WORLD_NEW"].replace(".","") }, buttonX1third, buttonRowY2, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            val playerDisk = full.loadables.getImportedPlayer()!!
            full.remoCon.openUI(UINewWorld(full.remoCon, playerDisk))
        }
    }
    private val mainRenameButton = UIItemTextButton(this,
        { if (altDown) Lang["MENU_MODULES"] else Lang["MENU_LABEL_RENAME"] }, buttonX1third, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            if (!altDown) {
                mode = MODE_RENAME
            }
            else {
                mode = MODE_SHOW_LOAD_ORDER
            }
        }
    }
    private val mainBackButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonXcentre, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            full.resetScroll()
            full.changePanelTo(0)
        }
    }
    private val mainDeleteButton = UIItemTextButton(this,
        { Lang["CONTEXT_CHARACTER_DELETE"] }, buttonX3third, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_DELETE
        }
    }

    private val confirmDeleteButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_DELETE"] }, buttonXleft, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
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
    private val confirmCancelButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CANCEL"] }, buttonXright, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_INIT
        }
    }

    private val renameInput = UIItemTextLineInput(this, buttonXleft, App.scr.halfh, 240 + buttonWidth)
    private val renameCancelButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CANCEL"] }, buttonXleft, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_INIT
        }
    }
    private val renameRenameButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_RENAME"] }, buttonXright, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            val newName = renameInput.getText().trim()
            if (newName.isNotBlank()) {
                full.playerButtonSelected!!.playerUUID.let { uuid ->
                    App.savegamePlayersName[uuid] = newName
                    App.savegamePlayers[uuid]!!.renamePlayer(newName)
                    full.playerButtonSelected!!.playerName = newName
                }
            }

            mode = MODE_INIT
        }
    }

    private val modulesBackButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonXcentre, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_INIT
        }
    }

    private var mode = 0

    private var mainButtons0 = listOf(mainGoButton, mainBackButton, mainRenameButton, mainDeleteButton)
    private var mainButtons1 = listOf(mainNoGoButton, mainBackButton, mainRenameButton, mainDeleteButton)
    private var mainButtons2 = listOf(mainImportedPlayerCreateNewWorldButton, mainBackButton, mainRenameButton, mainDeleteButton)
    private var delButtons = listOf(confirmCancelButton, confirmDeleteButton)
    private var renameButtons = listOf(renameRenameButton, renameCancelButton)

    private val mainButtons: List<UIItemTextButton>
        get() = if (full.loadables.saveAvaliable()) mainButtons0 else if (full.loadables.isImported) mainButtons2 else mainButtons1

    private val MODE_INIT = 0
    private val MODE_DELETE = 16 // are you sure?
    private val MODE_RENAME = 32 // show rename dialogue
    private val MODE_PREV_SAVES = 48
    private val MODE_SHOW_LOAD_ORDER = 64
    private val MODE_LOAD = 256 // is needed to make the static loading screen

    init {

    }

    private var screencap: TextureRegion? = null
    private val screencapW = SAVE_THUMBNAIL_MAIN_WIDTH
    private val screencapH = SAVE_THUMBNAIL_MAIN_HEIGHT

    override fun doOpening(delta: Float) {
        full.playerButtonSelected?.forceUnhighlight = true
        full.playerButtonSelected?.let { button ->
            screencap?.texture?.tryDispose()
            button.savegameThumbnailPixmap?.let {
                Texture(it).also {
                    it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    screencap = TextureRegion(it)
                }
            }
        }
    }

    override fun doClosing(delta: Float) {
        full.playerButtonSelected?.forceUnhighlight = false
    }

    override fun show() {
        super.show()

    }

    override fun updateUI(delta: Float) {
        altDown = Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT)

        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.update(delta) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.update(delta) }
            }
            MODE_RENAME -> {
                renameButtons.forEach { it.update(delta) }
                renameInput.update(delta)
            }
            MODE_SHOW_LOAD_ORDER -> {
                modulesBackButton.update(delta)
            }
        }
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        when (mode) {
            MODE_RENAME -> {
                renameInput.inputStrobed(e)
            }
        }
    }

    private var loadFiredFrameCounter = 0

    private var selectedRevision = 0 // 0: most recent, 1: second most recent, etc.

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        if (mode != MODE_SHOW_LOAD_ORDER) {
            val buttonYdelta = (full.titleTopGradEnd) - full.playerButtonSelected!!.posY
            full.playerButtonSelected!!.render(batch, camera, 0, buttonYdelta)
        }

        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.render(batch, camera) }

                // draw thumbnails of the most recent game
//                val tex = screencap ?: CommonResourcePool.getAsTextureRegion("terrarum-defaultsavegamethumb")


                if (screencap != null) {
                    val tx = (Toolkit.drawWidth - screencapW) / 2
                    val tys = full.titleTopGradEnd + SAVE_CELL_HEIGHT + buttonGap
                    val tye = buttonRowY2 - buttonGap
                    val ty = tys + (tye - tys - SAVE_THUMBNAIL_MAIN_HEIGHT) / 2

                    batch.color = Toolkit.Theme.COL_INACTIVE
                    Toolkit.drawBoxBorder(batch, tx - 1, ty - 1, screencapW + 2, screencapH + 2)
                    batch.color = UIInventoryFull.CELL_COL
                    Toolkit.fillArea(batch, tx, ty, screencapW, screencapH)

                    batch.color = Color.WHITE
                    batch.draw(screencap, tx.toFloat(), ty.toFloat(), screencapW.toFloat(), screencapH.toFloat())
                }

            }
            MODE_DELETE -> {
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval + SAVE_CELL_HEIGHT + 36)
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, full.titleTopGradEnd + full.cellInterval + SAVE_CELL_HEIGHT + 36 + 24)

                delButtons.forEach { it.render(batch, camera) }
            }
            MODE_RENAME -> {
                renameInput.render(batch, camera)
                renameButtons.forEach { it.render(batch, camera) }
            }
            MODE_LOAD -> {
                loadFiredFrameCounter += 1
                StaticLoadScreenSubstitute(batch)
                if (loadFiredFrameCounter == 2) LoadSavegame(full.loadManageSelectedGame)
            }
            MODE_SHOW_LOAD_ORDER -> {
                Toolkit.drawTextCentered(batch, App.fontUITitle, Lang["MENU_MODULES"], Toolkit.drawWidth, 0, full.titleTopGradEnd)

                val playerName = App.savegamePlayersName[full.playerButtonSelected!!.playerUUID] ?: "Player"

                val loadOrderPlayer =
                    App.savegamePlayers[full.playerButtonSelected!!.playerUUID]!!.files[selectedRevision].getFile(
                        VDFileID.LOADORDER
                    )?.getContent()?.toByteArray()?.toString(Common.CHARSET)?.split('\n')?.let {
                        it.mapIndexed { index, s -> "${(index+1).toString().padStart(it.size.fastLen())}. $s" }
                    } ?: listOf("$EMDASH")
                val loadOrderWorld =
                    App.savegameWorlds[full.playerButtonSelected!!.worldUUID]!!.files[selectedRevision].getFile(
                        VDFileID.LOADORDER
                    )?.getContent()?.toByteArray()?.toString(Common.CHARSET)?.split('\n')?.let {
                        it.mapIndexed { index, s -> "${(index+1).toString().padStart(it.size.fastLen())}. $s" }
                    } ?: listOf("$EMDASH")

                Toolkit.drawTextCentered(batch, App.fontGame, playerName, modulesTextboxW, playerModTextboxX, full.titleTopGradEnd + 32)
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_WORLD"], modulesTextboxW, worldModTextboxX, full.titleTopGradEnd + 32)

                val playerTBW = loadOrderPlayer.maxOfOrNull { App.fontGame.getWidth(it) } ?: 0
                val worldTBW = loadOrderWorld.maxOfOrNull { App.fontGame.getWidth(it) } ?: 0

                val px = playerModTextboxX + (modulesTextboxW - playerTBW) / 2
                val wx = worldModTextboxX + (modulesTextboxW - worldTBW) / 2

                // TODO box background

                loadOrderPlayer.forEachIndexed { index, s ->
                    App.fontGame.draw(batch, s, px, full.titleTopGradEnd + 64 + App.fontGame.lineHeight.toInt() * index)
                }
                loadOrderWorld.forEachIndexed { index, s ->
                    App.fontGame.draw(batch, s, wx, full.titleTopGradEnd + 64 + App.fontGame.lineHeight.toInt() * index)
                }

                modulesBackButton.render(batch, camera)
            }
        }
    }

    private val modulesTextboxW = 280
    private val boxTextMargin = 3
    private val listGap = 10
    private val playerModTextboxX = Toolkit.drawWidth / 2 - (2 * boxTextMargin + listGap) - modulesTextboxW
    private val worldModTextboxX = Toolkit.drawWidth / 2 + (2 * boxTextMargin + listGap)



    override fun dispose() {
        screencap?.texture?.tryDispose()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
            MODE_RENAME -> {
                renameInput.touchDown(screenX, screenY, pointer, button)
                renameButtons.forEach { it.touchDown(screenX, screenY, pointer, button) }
            }
            MODE_SHOW_LOAD_ORDER -> {
                modulesBackButton.touchDown(screenX, screenY, pointer, button)
            }
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (mode) {
            MODE_INIT -> {
                mainButtons.forEach { it.touchUp(screenX, screenY, pointer, button) }
            }
            MODE_DELETE -> {
                delButtons.forEach { it.touchUp(screenX, screenY, pointer, button) }
            }
            MODE_RENAME -> {
                renameInput.touchUp(screenX, screenY, pointer, button)
                renameButtons.forEach { it.touchUp(screenX, screenY, pointer, button) }
            }
            MODE_SHOW_LOAD_ORDER -> {
                modulesBackButton.touchUp(screenX, screenY, pointer, button)
            }
        }

        return true
    }

    private fun Int.fastLen(): Int {
        return if (this < 0) 1 + this.unaryMinus().fastLen()
        else if (this < 10) 1
        else if (this < 100) 2
        else if (this < 1000) 3
        else if (this < 10000) 4
        else if (this < 100000) 5
        else if (this < 1000000) 6
        else if (this < 10000000) 7
        else if (this < 100000000) 8
        else if (this < 1000000000) 9
        else 10
    }

}