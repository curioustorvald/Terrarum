package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.gamecontroller.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.Clipboard
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.OpenFile
import java.awt.Desktop
import java.io.File
import java.util.UUID

/**
 * Created by minjaesong on 2023-08-24.
 */
class UIImportAvatar(val remoCon: UIRemoCon) : Advanceable() {
    init {
        handler.allowESCtoClose = false
    }

    override var width = 480 // SAVE_CELL_WIDTH
    override var height = 480
    override var openCloseTime: Second = OPENCLOSE_GENERIC

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2
    private val goButtonWidth = 180

    private val descStartY = 24 * 4
    private val lh = App.fontGame.lineHeight.toInt()

    private val inputWidth = 340
    private val filenameInput = UIItemTextLineInput(this,
        (Toolkit.drawWidth - inputWidth) / 2, (App.scr.height - height) / 2 + descStartY + (5) * lh, inputWidth,
        maxLen = InputLenCap(256, InputLenCap.CharLenUnit.UTF8_BYTES)
    ).also {
        // reset importReturnCode if the text input has changed
        it.onKeyDown = { _ ->
            importReturnCode = 0
        }
    }

    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, drawX + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _,_ ->
            remoCon.openUI(UILoadSavegame(remoCon))
        }
    }
    private val goButton = UIItemTextButton(this,
        { Lang["MENU_IO_IMPORT"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _,_ ->
            if (filenameInput.getText().isNotBlank()) {
                importReturnCode = doImport()
                if (importReturnCode == 0) remoCon.openUI(UILoadSavegame(remoCon))
            }
        }
    }

    private var importReturnCode = 0

    init {
        addUIitem(filenameInput)
        addUIitem(backButton)
        addUIitem(goButton)

    }

//    private var textX = 0
    private var textY = 0
    private var mouseOnLink = false
    private var pathW = 0

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }

        pathW = App.fontGame.getWidth(App.importDir)
        val textX = (Toolkit.drawWidth - pathW) / 2
        textY = (App.scr.height - height) / 2 + descStartY + (1) * lh
        mouseOnLink = (Terrarum.mouseScreenX in textX - 48..textX + 48 + pathW &&
                Terrarum.mouseScreenY in textY - 12..textY + lh + 12)

        if (mouseOnLink && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            OpenFile(File(App.importDir))
        }

    }

    private val errorMessages = listOf(
        Lang["ERROR_GENERIC_TEXT"], // -1
        "", // 0
        Lang["ERROR_FILE_NOT_FOUND"], // 1
        Lang["ERROR_AVATAR_ALREADY_EXISTS"], // 2
    )

    override fun show() {
        super.show()
        wotKeys = (1..3).map { Lang["CONTEXT_IMPORT_AVATAR_INSTRUCTION_$it", false] }
    }
    private lateinit var wotKeys: List<String>

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE
        val textboxWidth = wotKeys.maxOf { App.fontGame.getWidth(it) }
        val textX = (Toolkit.drawWidth - textboxWidth) / 2
        // draw texts
        wotKeys.forEachIndexed { i, s ->
            App.fontGame.draw(batch, s, textX, (App.scr.height - height) / 2 + descStartY + i * lh)
        }
        // draw path
        batch.color = if (mouseOnLink) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_MOUSE_UP
        App.fontGame.draw(batch, App.importDir, (Toolkit.drawWidth - pathW) / 2, textY)


        uiItems.forEach { it.render(frameDelta, batch, camera) }


        if (importReturnCode != 0) {
            batch.color = Toolkit.Theme.COL_RED
            val tby = filenameInput.posY
            val btny = backButton.posY
            Toolkit.drawTextCentered(batch, App.fontGame, errorMessages[importReturnCode + 1], Toolkit.drawWidth, 0, (tby + btny) / 2)
        }
    }

    override fun dispose() {
    }

    override fun advanceMode(button: UIItem) {
    }

    private fun doImport(): Int {
        val file = File("${App.importDir}/${filenameInput.getText().trim()}")

        // check file's existence
        if (!file.exists()) {
            return 1
        }

        // try to mount the TEVd
        try {
            val dom = VDUtil.readDiskArchive(file)
            val timeNow = App.getTIME_T()

            // get the uuid
            val oldPlayerInfoFile = dom.getEntry(SAVEGAMEINFO)!!
            val playerInfo = JsonFetcher.readFromJsonString(ByteArray64Reader(VDUtil.getAsNormalFile(dom, SAVEGAMEINFO).bytes, Common.CHARSET))
            val uuid = playerInfo.getString("uuid")
            val newFile = File("${App.playersDir}/$uuid")

            printdbg(this, "Avatar uuid: $uuid")

            if (newFile.exists()) return 2

            // update playerinfo so that:
            // totalPlayTime to zero
            // lastPlayedTime to now
            // playerinfofile's lastModifiedTime to now
            // root's lastModifiedTime to now
            printdbg(this, "avatar old lastPlayTime: ${playerInfo.getLong("lastPlayTime")}")
            printdbg(this, "avatar old totalPlayTime: ${playerInfo.getLong("totalPlayTime")}")
            playerInfo.get("lastPlayTime").set(timeNow, null)
            playerInfo.get("totalPlayTime").set(0, null)
            printdbg(this, "avatar new lastPlayTime: ${playerInfo.getLong("lastPlayTime")}")
            printdbg(this, "avatar new totalPlayTime: ${playerInfo.getLong("totalPlayTime")}")

            val newJsonBytes = ByteArray64Writer(Common.CHARSET).let {
//                println(playerInfo.toString())
                it.write(playerInfo.toString())
                it.close()
                it.toByteArray64()
            }
            val newPlayerInfo = DiskEntry(SAVEGAMEINFO, ROOT, oldPlayerInfoFile.creationDate, timeNow, EntryFile(newJsonBytes))
            VDUtil.addFile(dom, newPlayerInfo)

            dom.getEntry(ROOT)!!.modificationDate = timeNow

            // mark the file as Imported
            dom.saveOrigin = VDSaveOrigin.IMPORTED

            // write modified file to the Players dir
            VDUtil.dumpToRealMachine(dom, newFile)


            // add imported character to the  list of savegames
//            AppUpdateListOfSavegames()
            try {
                val it = DiskSkimmer(newFile, true)

                val collection = SavegameCollection.collectFromBaseFilename(File(App.playersDir), it.diskFile.name)
                val playerUUID = UUID.fromString(uuid)

                printdbg(this, "${filenameInput} existed before: ${App.savegamePlayers.contains(playerUUID)}")

                // if multiple valid savegames with same UUID exist, only the most recent one is retained
                if (!App.savegamePlayers.contains(playerUUID)) {
                    App.savegamePlayers[playerUUID] = collection
                    App.sortedPlayers.add(0, playerUUID)
                    App.savegamePlayersName[playerUUID] = it.getDiskName(Common.CHARSET)
                }
            }
            catch (e: Throwable) {
                printdbgerr(this, e.stackTraceToString())
            }
        }
        catch (e: Throwable) {
            // format error
            e.printStackTrace()
            return -1
        }

        return 0
    }
}


class UIItemCodeBox(parent: UIImportAvatar, initialX: Int, initialY: Int, val cols: Int = 80, val rows: Int = 24) : UIItem(parent, initialX, initialY) {

    override val width = App.fontSmallNumbers.W * cols
    override val height = App.fontSmallNumbers.H * rows
    override fun dispose() {
    }

    private var highlightCol: Color = Toolkit.Theme.COL_INACTIVE

    var selected = false

    private var textCursorPosition = 0
    internal var textBuffer = StringBuilder()

    fun pasteFromClipboard() {
        textBuffer.clear()
        Clipboard.fetch().forEach {
            if (it.code in 33..122) textBuffer.append(it)
        }
        textCursorPosition += textBuffer.length
    }

    override fun update(delta: Float) {
        super.update(delta)
//        if (!selected && mousePushed)
//            selected = true
//        else if (selected && mouseDown && !mouseUp)
//            selected = false

        if (textBuffer.isEmpty() && (Gdx.input.isKeyJustPressed(Input.Keys.V) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)))) {
            pasteFromClipboard()
        }
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // draw box backgrounds
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, posX, posY, width, height)

        // draw borders
        batch.color = if (selected) Toolkit.Theme.COL_SELECTED else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)

        // draw texts
        if (textBuffer.isEmpty()) {
            batch.color = Toolkit.Theme.COL_INACTIVE
            App.fontGame.draw(batch, Lang["CONTEXT_IMPORT_AVATAR_INSTRUCTION_1"], posX + 5, posY)
            App.fontGame.draw(batch, Lang["CONTEXT_IMPORT_AVATAR_INSTRUCTION_2"], posX + 5f, posY + App.fontGame.lineHeight)
        }
        else {
            batch.color = Color.WHITE
            val scroll = ((textBuffer.length.toDouble() / cols).ceilToInt() - rows).coerceAtLeast(0)
            for (i in scroll * cols until textBuffer.length) {
                val c = textBuffer[i]
                val x = ((i - scroll * cols) % cols) * App.fontSmallNumbers.W.toFloat()
                val y = ((i - scroll * cols) / cols) * App.fontSmallNumbers.H.toFloat()

                App.fontSmallNumbers.draw(batch, "$c", posX + x, posY + y)
            }
        }
    }

    fun clearTextBuffer() {
        textBuffer.clear()
        textCursorPosition = 0
    }
}