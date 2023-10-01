package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
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
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.VDFileID
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.tryDispose
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextLineInput
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.unicode.EMDASH
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadManage(val full: UILoadSavegame) : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

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
        { if (altDown && savegameIsNotNew) Lang["MENU_LABEL_PREV_SAVES"] else Lang["MENU_IO_LOAD_GAME"] }, buttonX1third, buttonRowY2, buttonWidth * 3 + buttonGap * 2, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            App.printdbg(this, "Load playerUUID: ${UILoadGovernor.playerUUID}, worldUUID: ${UILoadGovernor.worldUUID}")

            /*if (full.loadables.moreRecentAutosaveAvailable()) {
                full.bringAutosaveSelectorUp()
                full.changePanelTo(2)
            }
            else */

            if (altDown && savegameIsNotNew) {
                mode = MODE_PREV_SAVES
                loadPrevGameInfo()
            }
            else {
                if (full.loadables.saveAvaliable()) {
                    if (full.loadables.newerSaveIsDamaged) {
                        UILoadGovernor.previousSaveWasLoaded = true
                    }

//                full.takeAutosaveSelectorDown()
                    full.loadManageSelectedGame = full.loadables.getLoadableSave()!!

                    mode = MODE_LOAD
                }
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
        { if (altDown && savegameIsNotNew) Lang["MENU_MODULES"] else Lang["MENU_LABEL_RENAME"] }, buttonX1third, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            if (altDown && savegameIsNotNew) {
                mode = MODE_SHOW_LOAD_ORDER
            }
            else {
                mode = MODE_RENAME
            }
        }
    }
    private val mainBackButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonXcentre, buttonRowY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            full.resetScroll()
            full.changePanelTo(0)
            full.playerButtonSelected?.forceUnhighlight = false
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

    private val savegameHasError: Boolean
        get() = (!full.loadables.saveAvaliable() && !full.loadables.isImported)
    private val savegameIsNotNew: Boolean
        get() = full.loadables.saveAvaliable()

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


    private val modulesBoxBaseY = full.titleTopGradEnd + 48
    private val modulesTextboxW = 280
    private val boxTextMargin = 3
    private val listGap = 10
    private val playerModTextboxX = Toolkit.drawWidth / 2 - (2 * boxTextMargin + listGap) - modulesTextboxW
    private val worldModTextboxX = Toolkit.drawWidth / 2 + (2 * boxTextMargin + listGap)


    private lateinit var sortedPlayerWorldList: List<Pair<SavegameMeta?, SavegameMeta?>>
    private lateinit var loadOrderPlayer: List<String>
    private lateinit var loadOrderWorld: List<String>
    private var playerName = ""
    private var px64 = 0
    private var wx64 = 0
    private var px48 = 0
    private var wx48 = 0
    private var totalTBH48 = 0
    private var totalTBH64 = 0

    override fun show() {
        super.show()
    }

    private fun loadPrevGameInfo() {
        val players = App.savegamePlayers[full.playerButtonSelected!!.playerUUID]!!.files
        val worlds = App.savegameWorlds[full.playerButtonSelected!!.worldUUID]!!.files
        val playerSavesInfo = players.map { it.getSavegameMeta() }.sortedByDescending { it.lastPlayTime }
        val worldSavesInfo = worlds.map { it.getSavegameMeta() }.sortedByDescending { it.lastPlayTime }

        sortedPlayerWorldList = getChronologicalPair(playerSavesInfo, worldSavesInfo)

        px48 = playerModTextboxX + (modulesTextboxW - tbw48) / 2
        wx48 = worldModTextboxX + (modulesTextboxW - tbw48) / 2
        totalTBH48 = sortedPlayerWorldList.size * 32
    }

    internal fun loadSavegameInfo() {
        playerName = App.savegamePlayersName[full.playerButtonSelected!!.playerUUID] ?: "Player"

        loadOrderPlayer =
            App.savegamePlayers[full.playerButtonSelected!!.playerUUID]!!.files[selectedRevision].getFile(
                VDFileID.LOADORDER
            )?.getContent()?.toByteArray()?.toString(Common.CHARSET)?.split('\n')?.let {
                it.mapIndexed { index, s -> "${(index+1).toString().padStart(it.size.fastLen())}. $s" }
            } ?: listOf("$EMDASH")
        loadOrderWorld =
            App.savegameWorlds[full.playerButtonSelected!!.worldUUID]!!.files[selectedRevision].getFile(
                VDFileID.LOADORDER
            )?.getContent()?.toByteArray()?.toString(Common.CHARSET)?.split('\n')?.let {
                it.mapIndexed { index, s -> "${(index+1).toString().padStart(it.size.fastLen())}. $s" }
            } ?: listOf("$EMDASH")

        val playerTBW64 = loadOrderPlayer.maxOfOrNull { App.fontGame.getWidth(it) } ?: 0
        val worldTBW64 = loadOrderWorld.maxOfOrNull { App.fontGame.getWidth(it) } ?: 0
        px64 = playerModTextboxX + (modulesTextboxW - playerTBW64) / 2
        wx64 = worldModTextboxX + (modulesTextboxW - worldTBW64) / 2
        totalTBH64 = maxOf(loadOrderPlayer.size, loadOrderWorld.size) * App.fontGame.lineHeight.toInt()
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
            MODE_SHOW_LOAD_ORDER, MODE_PREV_SAVES -> {
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

    private val icons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

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

                batch.color = Toolkit.Theme.COL_CELL_FILL
                Toolkit.fillArea(batch, playerModTextboxX, modulesBoxBaseY - 4, modulesTextboxW, 36 + totalTBH64)
                Toolkit.fillArea(batch, worldModTextboxX, modulesBoxBaseY - 4, modulesTextboxW, 36 + totalTBH64)
                batch.color = Toolkit.Theme.COL_INACTIVE
                Toolkit.drawBoxBorder(batch, playerModTextboxX - 1, modulesBoxBaseY - 4 - 1, modulesTextboxW + 2, 36 + totalTBH64 + 2)
                Toolkit.drawBoxBorder(batch, worldModTextboxX - 1, modulesBoxBaseY - 4 - 1, modulesTextboxW + 2, 36 + totalTBH64 + 2)

                batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
                for (i in 0 until maxOf(loadOrderPlayer.size, loadOrderWorld.size)) {
                    Toolkit.drawStraightLine(batch, playerModTextboxX + boxTextMargin, modulesBoxBaseY + 32 + App.fontGame.lineHeight.toInt() * i, playerModTextboxX + modulesTextboxW - boxTextMargin, 1, false)
                    Toolkit.drawStraightLine(batch, worldModTextboxX + boxTextMargin, modulesBoxBaseY + 32 + App.fontGame.lineHeight.toInt() * i, worldModTextboxX + modulesTextboxW - boxTextMargin, 1, false)
                }


                batch.color = Color.WHITE
                Toolkit.drawTextCentered(batch, App.fontGame, playerName, modulesTextboxW, playerModTextboxX, modulesBoxBaseY + 1)
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_WORLD"], modulesTextboxW, worldModTextboxX, modulesBoxBaseY + 1)

                loadOrderPlayer.forEachIndexed { index, s ->
                    App.fontGame.draw(batch, s, px64, modulesBoxBaseY + 32 + App.fontGame.lineHeight.toInt() * index)
                }
                loadOrderWorld.forEachIndexed { index, s ->
                    App.fontGame.draw(batch, s, wx64, modulesBoxBaseY + 32 + App.fontGame.lineHeight.toInt() * index)
                }

                modulesBackButton.render(batch, camera)
            }
            MODE_PREV_SAVES -> {
                val modulesBoxBaseY2 = full.titleTopGradEnd + SAVE_CELL_HEIGHT + listGap + 4

                batch.color = Toolkit.Theme.COL_CELL_FILL
                Toolkit.fillArea(batch, playerModTextboxX, modulesBoxBaseY2 - 4, modulesTextboxW, 36 + totalTBH48)
                Toolkit.fillArea(batch, worldModTextboxX, modulesBoxBaseY2 - 4, modulesTextboxW, 36 + totalTBH48)
                batch.color = Toolkit.Theme.COL_INACTIVE
                Toolkit.drawBoxBorder(batch, playerModTextboxX - 1, modulesBoxBaseY2 - 4 - 1, modulesTextboxW + 2, 36 + totalTBH48 + 2)
                Toolkit.drawBoxBorder(batch, worldModTextboxX - 1, modulesBoxBaseY2 - 4 - 1, modulesTextboxW + 2, 36 + totalTBH48 + 2)

                batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
                for (i in 0 until sortedPlayerWorldList.size) {
                    Toolkit.drawStraightLine(batch, playerModTextboxX + boxTextMargin, modulesBoxBaseY2 + 32 + 32 * i, playerModTextboxX + modulesTextboxW - boxTextMargin, 1, false)
                    Toolkit.drawStraightLine(batch, worldModTextboxX + boxTextMargin, modulesBoxBaseY2 + 32 + 32 * i, worldModTextboxX + modulesTextboxW - boxTextMargin, 1, false)
                }

                batch.color = Color.WHITE
                Toolkit.drawTextCentered(batch, App.fontGame, playerName, modulesTextboxW, playerModTextboxX, modulesBoxBaseY2 + 1)
                Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_WORLD"], modulesTextboxW, worldModTextboxX, modulesBoxBaseY2 + 1)
                sortedPlayerWorldList.forEachIndexed { index, (pmeta, wmeta) ->
                    if (pmeta != null) {
                        if (pmeta.isAuto) batch.draw(icons.get(24,1), playerModTextboxX + 4f, modulesBoxBaseY2 + 38f + 32 * index)
                        App.fontGame.draw(batch, "$pmeta", px48, modulesBoxBaseY2 + 36 + 32 * index)
                    }
                    if (wmeta != null) {
                        if (wmeta.isAuto) batch.draw(icons.get(24,1), worldModTextboxX + 4f, modulesBoxBaseY2 + 38f + 32 * index)
                        App.fontGame.draw(batch, "$wmeta", wx48, modulesBoxBaseY2 + 36 + 32 * index)
                    }
                }

                modulesBackButton.render(batch, camera)
            }
        }
    }

    val tbw48 = App.fontGame.getWidth("8888-88-88 88:88:88 (8.8.88)")


    private data class SavegameMeta(
        val lastPlayTime: Long,
        val genver: String,
        val isAuto: Boolean,
    ) {
        private val lastPlayTimeS = Instant.ofEpochSecond(lastPlayTime)
            .atZone(TimeZone.getDefault().toZoneId())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        override fun toString() = "$lastPlayTimeS\u3000($genver)"
    }

    private fun DiskSkimmer.getSavegameMeta(): SavegameMeta {
        if (!this.initialised) this.rebuild()
        this.getFile(SAVEGAMEINFO)!!.bytes.let {
            var lastPlayTime = 0L
            var versionString = ""
            val isAuto = (this.getSaveMode() and 0b10 != 0)
            JsonFetcher.readFromJsonString(ByteArray64Reader(it, Common.CHARSET)).forEachSiblings { name, value ->
                if (name == "lastPlayTime") lastPlayTime = value.asLong()
                if (name == "genver") versionString = value.asLong().let { "${it.ushr(48)}.${it.ushr(24).and(0xFFFFFF)}.${it.and(0xFFFFFF)}" }
            }

            return SavegameMeta(
                lastPlayTime,
                versionString,
                isAuto
            )
        }
    }

    private fun getChronologicalPair(ps: List<SavegameMeta>, ws: List<SavegameMeta>): List<Pair<SavegameMeta?, SavegameMeta?>> {
        val li = ArrayList<Pair<SavegameMeta?, SavegameMeta?>>()
        var pc = 0
        var wc = 0
        var breakStatus = -1 // 0: ps ran out, 1: ws ran out
        while (true) {
            if (ps.size == pc) {
                breakStatus = 0
                break
            }
            else if (ws.size == wc) {
                breakStatus = 1
                break
            }

            if (ps[pc].lastPlayTime == ws[wc].lastPlayTime) {
                li.add(ps[pc] to ws[wc])
                pc++; wc++
            }
            else if (ps[pc].lastPlayTime > ws[wc].lastPlayTime) {
                li.add(ps[pc] to null)
                pc++
            }
            else {
                li.add(null to ws[wc])
                wc++
            }
        }

        val remainder = if (breakStatus == 0) ws else ps
        var rc = if (breakStatus == 0) wc else pc
        while (rc < remainder.size) {
            if (breakStatus == 0)
                li.add(null to ws[rc])
            else
                li.add(ps[pc] to null)

            rc++
        }

        return li
    }

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
            MODE_SHOW_LOAD_ORDER, MODE_PREV_SAVES -> {
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
            MODE_SHOW_LOAD_ORDER, MODE_PREV_SAVES -> {
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