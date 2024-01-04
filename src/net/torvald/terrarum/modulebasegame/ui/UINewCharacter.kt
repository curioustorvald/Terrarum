package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.playersDir
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilderTestSubject1
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilderWerebeastTest
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.VDUtil
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.modulebasegame.serialise.WritePlayer
import net.torvald.terrarum.savegame.VDSaveOrigin
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.RandomWordsName
import java.io.File

/**
 * Created by minjaesong on 2021-12-09.
 */
class UINewCharacter(val remoCon: UIRemoCon) : UICanvas() {

    init {
        handler.allowESCtoClose = false
    }

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val row1 = 186 + 40


    private val inputWidth = 340

    private val nameInput = UIItemTextLineInput(this,
            drawX + width - inputWidth, drawY + row1, inputWidth,
            { RandomWordsName(4) }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES))

    private val goButtonWidth = 180
    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, drawX + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val goButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CONFIRM_BUTTON"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, drawY + height - 24, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

    private var returnedFromChargen = false

    private var uiLocked = false

    init {
        goButton.clickOnceListener = { _,_ ->
            uiLocked = true


            val player = PlayerBuilderTestSubject1()
//            val player = PlayerBuilderWerebeastTest()
            player.actorValue[AVKey.NAME] = nameInput.getTextOrPlaceholder()

            val disk = VDUtil.createNewDisk(
                    1L shl 60,
                    player.actorValue.getAsString(AVKey.NAME) ?: "",
                    Common.CHARSET
            )
            val outFile = Terrarum.getPlayerSaveFiledesc(LoadSavegame.getPlayerSavefileName(player))
            val time_t = App.getTIME_T()


            val savingThread = Thread({
                printdbg(this, "Player saving thread fired")

                disk.saveMode = 2 // auto, no quick
                disk.capacity = 0L
                disk.saveOrigin = VDSaveOrigin.INGAME
                disk.snapshot = TerrarumAppConfiguration.VERSION_SNAPSHOT

                WritePlayer(player, disk, null, time_t)
                VDUtil.dumpToRealMachine(disk, outFile)

                App.savegamePlayers[player.uuid] = SavegameCollection.collectFromBaseFilename(File(playersDir), outFile.name)
                App.savegamePlayersName[player.uuid] = player.actorValue.getAsString(AVKey.NAME)
                UILoadGovernor.playerUUID = player.uuid


                uiLocked = false
                returnedFromChargen = true

                // comment below if chargen must send gamers back to the charcters list
//                UILoadGovernor.playerDisk = DiskSkimmer(outFile)
                // comment above if chargen must send gamers back to the charcters list

//                printdbg(this, "playerdisk: ${UILoadGovernor.playerDisk?.diskFile?.path}")

            }, "TerrarumBasegameNewCharcterSaveThread")

//            savingThread.start()
//            savingThread.join()

            remoCon.openUI(UINewWorld(remoCon, savingThread)) // let UINewWorld handle the character file generation
        }
        backButton.clickOnceListener = { _,_ ->
            remoCon.openUI(UILoadSavegame(remoCon))
        }

        addUIitem(nameInput)
        addUIitem(goButton)
        addUIitem(backButton)
    }

    override fun updateUI(delta: Float) {
        if (!uiLocked) {
            uiItems.forEach { it.update(delta) }
        }

        if (returnedFromChargen) {
            returnedFromChargen = false
            remoCon.openUI(UILoadSavegame(remoCon))
        }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE
        // ui title
//        val titlestr = Lang["CONTEXT_WORLD_NEW"]
//        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), titleTextPosY.toFloat())


        // name/seed input labels
        App.fontGame.draw(batch, Lang["MENU_NAME"], drawX, drawY + row1)

        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {

    }

}