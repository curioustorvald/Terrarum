package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.savegame.ByteArray64InputStream
import net.torvald.terrarum.savegame.EntryFile
import net.torvald.terrarum.savegame.VDFileID
import net.torvald.terrarum.ui.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadAutosave(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height


    private val altSelDrawW = 640
    private val altSelHdrawW = altSelDrawW / 2
    private val altSelDrawH = 480
    private val imageButtonW = 300
    private val imageButtonH = 240
    private val altSelDrawY = ((App.scr.height - altSelDrawH)/2)
    private val altSelQdrawW = altSelDrawW / 4
    private val altSelQQQdrawW = altSelDrawW * 3 / 4

    private lateinit var loadManualThumbButton: UIItemImageButton
    private lateinit var loadAutoThumbButton: UIItemImageButton

    private val buttonXcentre = Toolkit.hdrawWidth - (full.buttonWidth / 2)
    private val buttonRowY = full.drawY + 480 - full.buttonHeight

    private var mode = 0
    private val MODE_INIT = 0
    private val MODE_LOAD = 256 // is needed to make the static loading screen

    private lateinit var selectedGame: DiskPair

    private val mainBackButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonXcentre, buttonRowY, full.buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            full.changePanelTo(1)
        }
    }

    init {
        addUIitem(mainBackButton)
    }

    override fun show() {
        super.show()

        val loadables = full.loadables
        val autoThumb = loadables.getAutoSave()!!.getThumbnail()
        val manualThumb = loadables.getManualSave()!!.getThumbnail()

        loadManualThumbButton = UIItemImageButton(this, manualThumb,
            initialX = (Toolkit.drawWidth - altSelDrawW)/2 + altSelQdrawW - imageButtonW/2,
            initialY = altSelDrawY + 120,
            width = imageButtonW,
            height = imageButtonH,
            imageDrawWidth = imageButtonW,
            imageDrawHeight = imageButtonH,
            highlightable = false,
            useBorder = true,
        ).also {
            it.extraDrawOp = getDrawTextualInfoFun(loadables.getManualSave()!!)
            it.clickOnceListener = { _,_ ->
                selectedGame = loadables.getManualSave()!!
                mode = MODE_LOAD
            }
        }
        loadAutoThumbButton = UIItemImageButton(this, autoThumb,
            initialX = (Toolkit.drawWidth - altSelDrawW)/2 + altSelQQQdrawW - imageButtonW/2,
            initialY = altSelDrawY + 120,
            width = imageButtonW,
            height = imageButtonH,
            imageDrawWidth = imageButtonW,
            imageDrawHeight = imageButtonH,
            highlightable = false,
            useBorder = true,
        ).also {
            it.extraDrawOp = getDrawTextualInfoFun(loadables.getAutoSave()!!)
            it.clickOnceListener = { _,_ ->
                selectedGame = loadables.getAutoSave()!!
                mode = MODE_LOAD
            }
        }

    }

    override fun updateUI(delta: Float) {
        if (::loadAutoThumbButton.isInitialized) loadAutoThumbButton.update(delta)
        if (::loadManualThumbButton.isInitialized) loadManualThumbButton.update(delta)
        mainBackButton.update(delta)
    }

    private var loadFiredFrameCounter = 0

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        if (mode == MODE_INIT) {
            // "The Autosave is more recent than the manual save"
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["GAME_MORE_RECENT_AUTOSAVE1"], Toolkit.drawWidth, 0, altSelDrawY)
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["GAME_MORE_RECENT_AUTOSAVE2"], Toolkit.drawWidth, 0, altSelDrawY + 24)
            // Manual Save             Autosave
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_IO_MANUAL_SAVE"], altSelHdrawW, (Toolkit.drawWidth - altSelDrawW)/2, altSelDrawY + 80)
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_IO_AUTOSAVE"], altSelHdrawW, Toolkit.drawWidth/2, altSelDrawY + 80)

            if (::loadAutoThumbButton.isInitialized) loadAutoThumbButton.render(batch, camera)
            if (::loadManualThumbButton.isInitialized) loadManualThumbButton.render(batch, camera)

            mainBackButton.render(batch, camera)
        }
        else if (mode == MODE_LOAD) {
            loadFiredFrameCounter += 1
            StaticLoadScreenSubstitute(batch)
            if (loadFiredFrameCounter == 2) LoadSavegame(selectedGame)
        }
    }

    override fun dispose() {
        if (::loadAutoThumbButton.isInitialized) loadAutoThumbButton.dispose()
        if (::loadManualThumbButton.isInitialized) loadManualThumbButton.dispose()
    }


    private fun DiskPair.getThumbnail(): TextureRegion {
        return this.player.requestFile(VDFileID.PLAYER_SCREENSHOT).let { file ->
            if (file != null) {
                val zippedTga = (file.contents as EntryFile).bytes
                val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
                val tgaFileContents = gzin.readAllBytes(); gzin.close()
                val pixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
                TextureRegion(Texture(pixmap)).also {
                    App.disposables.add(it.texture)
                    // do cropping and resizing
                    it.setRegion(
                        (pixmap.width - imageButtonW*2) / 2,
                        (pixmap.height - imageButtonH*2) / 2,
                        imageButtonW * 2,
                        imageButtonH * 2
                    )
                }
            }
            else {
                CommonResourcePool.getAsTextureRegion("terrarum-defaultsavegamethumb")
            }
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)
        if (::loadAutoThumbButton.isInitialized) loadAutoThumbButton.touchDown(screenX, screenY, pointer, button)
        if (::loadManualThumbButton.isInitialized) loadManualThumbButton.touchDown(screenX, screenY, pointer, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchUp(screenX, screenY, pointer, button)
        if (::loadAutoThumbButton.isInitialized) loadAutoThumbButton.touchUp(screenX, screenY, pointer, button)
        if (::loadManualThumbButton.isInitialized) loadManualThumbButton.touchUp(screenX, screenY, pointer, button)
        return true
    }

    private fun getDrawTextualInfoFun(disks: DiskPair): (UIItem, SpriteBatch) -> Unit {
        val lastPlayedStamp = Instant.ofEpochSecond(disks.player.getLastModifiedTime())
            .atZone(TimeZone.getDefault().toZoneId())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return { item: UIItem, batch: SpriteBatch ->
            App.fontSmallNumbers.draw(batch, lastPlayedStamp, item.posX + 5f, item.posY + 3f)
        }
    }
}