package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.JsonReader
import com.jme3.math.FastMath
import net.torvald.EMDASH
import net.torvald.getKeycapConsole
import net.torvald.getKeycapPC
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.spriteassembler.ADProperties
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELL_COL
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.LoadSavegame
import net.torvald.terrarum.serialise.ReadPlayer
import net.torvald.terrarum.tvda.ByteArray64InputStream
import net.torvald.terrarum.tvda.ByteArray64Reader
import net.torvald.terrarum.tvda.DiskSkimmer
import net.torvald.terrarum.tvda.EntryFile
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

val SAVE_CELL_WIDTH = 480
val SAVE_CELL_HEIGHT = 120

/**
 * Only works if current screen set by the App is [TitleScreen]
 *
 * Created by minjaesong on 2021-09-09.
 */
class UILoadDemoSavefiles : UICanvas() {

    init {
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.addToLoadingList("terrarum-defaultsavegamethumb") {
            TextureRegion(Texture(Gdx.files.internal("assets/graphics/gui/savegame_thumb_placeholder.png")))
        }
        CommonResourcePool.addToLoadingList("savegame_status_icon") {
            TextureRegionPack("assets/graphics/gui/savegame_status_icon.tga", 24, 24)
        }
        CommonResourcePool.loadAll()
    }

    override var width: Int
        get() = Toolkit.drawWidth
        set(value) {}
    override var height: Int
        get() = App.scr.height
        set(value) {}
    override var openCloseTime: Second = 0f


    private val shapeRenderer = ShapeRenderer()


    internal val uiWidth = SAVE_CELL_WIDTH
    internal val uiX = (width - uiWidth) / 2

    internal val textH = App.fontGame.lineHeight.toInt()

    internal val cellGap = 20
    internal val cellInterval = cellGap + SAVE_CELL_HEIGHT
    internal val gradAreaHeight = 32

    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10
    internal val titleTopGradStart: Int = titleTextPosY + textH
    internal val titleTopGradEnd: Int = titleTopGradStart + gradAreaHeight
    internal val titleBottomGradStart: Int = height - App.scr.tvSafeGraphicsHeight - gradAreaHeight
    internal val titleBottomGradEnd: Int = titleBottomGradStart + gradAreaHeight
    internal val controlHelperY: Int = titleBottomGradStart + gradAreaHeight - textH


    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_up"))}${getKeycapPC(App.getConfigInt("control_key_down"))}" +
            " ${Lang["MENU_CONTROLS_SCROLL"]}"
        else
            "${getKeycapConsole('R')} ${Lang["MENU_CONTROLS_SCROLL"]}"


    private var scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
    private var listScroll = 0 // only update when animation is finished
    private var savesVisible = (scrollAreaHeight + cellGap) / cellInterval

    private var uiScroll = 0f
    private var scrollFrom = 0
    private var scrollTarget = 0
    private var scrollAnimCounter = 0f
    private val scrollAnimLen = 0.1f

    private var sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, true)

    private var showSpinner = false

    // used by UIItem*Cells
    internal var playerDisk: DiskSkimmer? = null
    internal var worldDisk: DiskSkimmer? = null

    private val worldCells = ArrayList<UIItemWorldCells>()
    private val playerCells = ArrayList<UIItemPlayerCells>()

    var mode = 0; private set// 0: show players, 1: show worlds

    fun advanceMode() {
        mode += 1
        uiScroll = 0f
        scrollFrom = 0
        scrollTarget = 0
        scrollAnimCounter = 0f
        loadFired = 0

        println("savelist mode: $mode")
    }

    override fun show() {
        try {
            val remoCon = (App.getCurrentScreen() as TitleScreen).uiRemoCon

            remoCon.handler.lockToggle()
            showSpinner = true

            Thread {
                // read savegames
                var savegamesCount = 0
                App.savegameWorlds.forEach { (_, skimmer) ->
                    val x = uiX + if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth / 2 else 0
                    val y = titleTopGradEnd + cellInterval * savegamesCount
                    try {
                        worldCells.add(UIItemWorldCells(this, x, y, skimmer))
                        savegamesCount += 1
                    }
                    catch (e: Throwable) {
                        System.err.println("[UILoadDemoSavefiles] Error while loading World '${skimmer.diskFile.absolutePath}'")
                        e.printStackTrace()
                    }
                }

                savegamesCount = 0
                App.savegamePlayers.forEach { (_, skimmer) ->
                    val x = uiX + if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth / 2 else 0
                    val y = titleTopGradEnd + cellInterval * savegamesCount
                    try {
                        playerCells.add(UIItemPlayerCells(this, x, y, skimmer))
                        savegamesCount += 1
                    }
                    catch (e: Throwable) {
                        System.err.println("[UILoadDemoSavefiles] Error while loading Player '${skimmer.diskFile.absolutePath}'")
                        e.printStackTrace()
                    }
                }


                remoCon.handler.unlockToggle()
                showSpinner = false
            }.start()

        }
        catch (e: UninitializedPropertyAccessException) {}
    }

    override fun hide() {
        worldCells.forEach { it.dispose() }
        worldCells.clear()
        playerCells.forEach { it.dispose() }
        playerCells.clear()
    }

    private fun getCells() = if (mode == 0) playerCells else worldCells
    private var loadFired = 0

    override fun updateUI(delta: Float) {

        if (mode < 2) {
            if (scrollTarget != listScroll) {
                if (scrollAnimCounter < scrollAnimLen) {
                    scrollAnimCounter += delta
                    uiScroll = Movement.fastPullOut(
                            scrollAnimCounter / scrollAnimLen,
                            listScroll * cellInterval.toFloat(),
                            scrollTarget * cellInterval.toFloat()
                    )
                }
                else {
                    scrollAnimCounter = 0f
                    listScroll = scrollTarget
                    uiScroll = cellInterval.toFloat() * scrollTarget
                }
            }

            val cells = getCells()

            for (index in 0 until cells.size) {


                val it = cells[index]
                if (index in listScroll - 2 until listScroll + savesVisible + 2) {
                    // re-position
                    it.posY = (it.initialY - uiScroll).roundToInt()
                    it.update(delta)
                }
            }
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        if (mode == 2) {
            loadFired += 1
            // to hide the "flipped skybox" artefact
            gdxClearAndSetBlend(.094f, .094f, .094f, 0f)

            if (loadFired == 2) {
                LoadSavegame(playerDisk!!, worldDisk)
            }
        }
        else {
            batch.end()

            val cells = getCells()

            lateinit var savePixmap: Pixmap
            sliderFBO.inAction(camera as OrthographicCamera, batch) {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                setCameraPosition(batch, camera, 0f, 0f)
                batch.color = Color.WHITE
                batch.inUse {
                    for (index in 0 until cells.size) {
                        val it = cells[index]
                        if (index in listScroll - 2 until listScroll + savesVisible + 2) {
                            it.render(batch, camera)
                        }
                    }
                }
                savePixmap = Pixmap.createFromFrameBuffer(0, 0, sliderFBO.width, sliderFBO.height)
                savePixmap.blending = Pixmap.Blending.None
            }


            // implement "wipe-out" by CPU-rendering (*deep exhale*)
            //savePixmap.setColor(1f,1f,1f,0f)
            savePixmap.setColor(0f, 0f, 0f, 0f)
            savePixmap.fillRectangle(0, savePixmap.height - titleTopGradStart, savePixmap.width, titleTopGradStart)
            // top grad
            for (y in titleTopGradStart until titleTopGradEnd) {
                val alpha = (y - titleTopGradStart).toFloat() / gradAreaHeight
                for (x in 0 until savePixmap.width) {
                    val col = savePixmap.getPixel(x, savePixmap.height - y)
                    val blendAlpha = (col.and(0xFF) * alpha).roundToInt()
                    savePixmap.drawPixel(x, savePixmap.height - y, col.and(0xFFFFFF00.toInt()) or blendAlpha)
                }
            }
            // bottom grad
            for (y in titleBottomGradStart until titleBottomGradEnd) {
                val alpha = 1f - ((y - titleBottomGradStart).toFloat() / gradAreaHeight)
                for (x in 0 until savePixmap.width) {
                    val col = savePixmap.getPixel(x, savePixmap.height - y)
                    val blendAlpha = (col.and(0xFF) * alpha).roundToInt()
                    savePixmap.drawPixel(x, savePixmap.height - y, col.and(0xFFFFFF00.toInt()) or blendAlpha)
                }
            }
            savePixmap.setColor(0f, 0f, 0f, 0f)
            savePixmap.fillRectangle(0, 0, savePixmap.width, height - titleBottomGradEnd + 1)



            setCameraPosition(batch, camera, 0f, 0f)
            val saveTex = Texture(savePixmap)
            batch.inUse {
                batch.draw(saveTex, (width - uiWidth - 10) / 2f, 0f)

                // draw texts
                val loadGameTitleStr = Lang["MENU_IO_LOAD_GAME"]
                // "Game Load"
                App.fontGame.draw(batch, loadGameTitleStr, (width - App.fontGame.getWidth(loadGameTitleStr)).div(2).toFloat(), titleTextPosY.toFloat())
                // Control help
                App.fontGame.draw(batch, controlHelp, uiX.toFloat(), controlHelperY.toFloat())
            }

            saveTex.dispose()
            savePixmap.dispose()

            batch.begin()
        }
    }


    override fun keyDown(keycode: Int): Boolean {
        if (this.isVisible) {
            val cells = getCells()

            if ((keycode == Input.Keys.UP || keycode == App.getConfigInt("control_key_up")) && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if ((keycode == Input.Keys.DOWN || keycode == App.getConfigInt("control_key_down")) && scrollTarget < cells.size - savesVisible) {
                scrollFrom = listScroll
                scrollTarget += 1
                scrollAnimCounter = 0f
            }
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        getCells().forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        getCells().forEach { it.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (this.isVisible) {
            val cells = getCells()

            if (amountY <= -1f && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if (amountY >= 1f && scrollTarget < cells.size - savesVisible) {
                scrollFrom = listScroll
                scrollTarget += 1
                scrollAnimCounter = 0f
            }
        }
        return true
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        listScroll = 0
        scrollTarget = 0
        uiScroll = 0f
    }

    override fun dispose() {
        try { shapeRenderer.dispose() } catch (e: IllegalArgumentException) {}
        try { sliderFBO.dispose() } catch (e: IllegalArgumentException) {}
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
        savesVisible = (scrollAreaHeight + cellInterval) / (cellInterval + SAVE_CELL_HEIGHT)

        listScroll = 0
        scrollTarget = 0
        uiScroll = 0f

        sliderFBO.dispose()
        sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, true)
    }

    private fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).round(), (-newY + App.scr.halfh).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

}




class UIItemPlayerCells(
        parent: UILoadDemoSavefiles,
        initialX: Int,
        initialY: Int,
        val skimmer: DiskSkimmer) : UIItem(parent, initialX, initialY) {

    override val width = SAVE_CELL_WIDTH
    override val height = SAVE_CELL_HEIGHT

    private var thumbPixmap: Pixmap? = null
    private var thumb: TextureRegion? = null

    override var clickOnceListener: ((Int, Int, Int) -> Unit)? = { _: Int, _: Int, _: Int ->
        parent.playerDisk = skimmer
        parent.advanceMode()
    }

    private var playerName: String = "$EMDASH"
    private var worldName: String = "$EMDASH"
    private var lastPlayTime: String = "????-??-?? --:--:--"
    private var totalPlayTime: String = "--h--m--s"

    init {
        skimmer.getFile(-1L)?.bytes?.let {
            val json = JsonReader().parse(ByteArray64Reader(it, Common.CHARSET))

            val playerUUID = UUID.fromString(json["uuid"]?.asString())
            val worldUUID = UUID.fromString(json["worldCurrentlyPlaying"]?.asString())

            App.savegamePlayersName[playerUUID]?.let { if (it.isNotBlank()) playerName = it else "(name)" }
            App.savegameWorldsName[worldUUID]?.let { if (it.isNotBlank()) worldName = it }
            json["lastPlayTime"]?.asString()?.let {
                lastPlayTime = Instant.ofEpochSecond(it.toLong())
                        .atZone(TimeZone.getDefault().toZoneId())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
            json["totalPlayTime"]?.asString()?.let {
                totalPlayTime = parseDuration(it.toLong())
            }

        }
    }

    private fun parseDuration(seconds: Long): String {
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = (seconds / 3600) % 24
        val d = seconds / 86400
        return if (d == 0L)
            "${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
        else
            "${d}d${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
    }

    private var sprite: SpriteAnimation? = null

    internal var hasTexture = false
        private set

    private val cellCol = CELL_COL

    private val icons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override fun render(batch: SpriteBatch, camera: Camera) {
        // try to generate a texture
        if (skimmer.initialised && !hasTexture) {
            skimmer.getFile(-1L)?.bytes?.let {
                val animFile = skimmer.getFile(-2L)!!
                val p = ReadPlayer(skimmer, ByteArray64Reader(it, Common.CHARSET))
                p.sprite = SpriteAnimation(p)
                p.animDesc = ADProperties(ByteArray64Reader(animFile.bytes, Common.CHARSET))
                p.reassembleSprite(skimmer, p.sprite)
                p.sprite!!.textureRegion.get(0,0).let {
                    thumb = it
                }
                this.sprite = p.sprite
            }

            hasTexture = true
        }

        val highlightCol = if (mouseUp) UIItemTextButton.defaultActiveCol else Color.WHITE
        val x = posX.toFloat()
        val y = posY.toFloat()

        // draw box backgrounds
        batch.color = cellCol
        Toolkit.fillArea(batch, posX, posY, 106, height)
        Toolkit.fillArea(batch, posX + 116, posY + 34, width - 116, 86)

        // draw borders
        batch.color = highlightCol
        // avatar border
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, 106 + 2, height + 2)
        // infocell border
        Toolkit.drawBoxBorder(batch, posX + 115, posY + 33, width - 114, 88)
        // infocell divider
        Toolkit.fillArea(batch, posX + 118, posY + 62, width - 120, 1)
        Toolkit.fillArea(batch, posX + 118, posY + 91, width - 120, 1)

        // texts
        val playTimeTextLen = App.fontGame.getWidth(totalPlayTime)
        App.fontGame.draw(batch, playerName, x + 146f, y + height - 82f)
        App.fontGame.draw(batch, worldName, x + 146f, y + height - 53f)
        App.fontGame.draw(batch, lastPlayTime, x + 146f, y + height - 24f)
        App.fontGame.draw(batch, totalPlayTime, x + width - 5f - playTimeTextLen, y + height - 24f)
        // icons
        batch.draw(icons.get(24,0), x + 120f, y + height - 82f) // player name
        batch.draw(icons.get(12,0), x + 119f, y + height - 53f) // world map
        batch.draw(icons.get(13,0), x + 120f, y + height - 24f) // journal
        batch.draw(icons.get(23,0), x + width - 5f - playTimeTextLen - 24f, y + height - 24f) // stopwatch

        // player avatar
        batch.color = Color.WHITE
        thumb?.let {
            batch.draw(it, x + FastMath.ceil((106f - it.regionWidth) / 2f), y + FastMath.ceil((height - it.regionHeight) / 2f))
        }
    }

    override fun dispose() {
        thumb?.texture?.dispose()
        thumbPixmap?.dispose()
        sprite?.dispose()
    }


}


class UIItemWorldCells(
        parent: UILoadDemoSavefiles,
        initialX: Int,
        initialY: Int,
        val skimmer: DiskSkimmer) : UIItem(parent, initialX, initialY) {


    private val metaFile: EntryFile?
    private val saveName: String
    private val saveMode: Int
    private val isQuick: Boolean
    private val isAuto: Boolean
    private var saveDamaged: Boolean = false
    private val lastPlayedTimestamp: String

    init {
        printdbg(this, "Rebuilding skimmer for savefile ${skimmer.diskFile.absolutePath}")
        skimmer.rebuild()

        metaFile = skimmer.getFile(-1)
        if (metaFile == null) saveDamaged = true

        saveName = skimmer.getDiskName(Common.CHARSET)
        saveMode = skimmer.getSaveMode()
        isQuick = (saveMode % 2 == 1)
        isAuto = (saveMode.ushr(1) != 0)

        saveDamaged = saveDamaged or checkForSavegameDamage(skimmer)

        if (metaFile != null) {
            val worldJson = JsonReader().parse(ByteArray64Reader(metaFile.bytes, Common.CHARSET))
            val lastplay_t = worldJson["lastPlayTime"].asLong()
            val playtime_t = worldJson["totalPlayTime"].asLong()
            lastPlayedTimestamp =
                    Instant.ofEpochSecond(lastplay_t)
                            .atZone(TimeZone.getDefault().toZoneId())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    "/${parseDuration(playtime_t)}"
        }
        else {
            lastPlayedTimestamp = "--:--:--/--h--m--s"
        }
    }

    private fun parseDuration(seconds: Long): String {
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = (seconds / 3600) % 24
        val d = seconds / 86400
        return if (d == 0L)
            "${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
        else
            "${d}d${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
    }

    override val width: Int = SAVE_CELL_WIDTH
    override val height: Int = SAVE_CELL_HEIGHT

    private var thumbPixmap: Pixmap? = null
    private var thumb: TextureRegion? = null
    private val grad = CommonResourcePool.getAsTexture("title_halfgrad")

    private val icons = CommonResourcePool.getAsTextureRegionPack("savegame_status_icon")


    private val colourBad = Color(0xFF0011FF.toInt())
    private val cellCol = CELL_COL


    init {

    }

    override var clickOnceListener: ((Int, Int, Int) -> Unit)? = { _: Int, _: Int, _: Int ->
        parent.worldDisk = skimmer
        parent.advanceMode()
    }

    internal var hasTexture = false
        private set

    override fun render(batch: SpriteBatch, camera: Camera) {
        // try to generate a texture
        if (skimmer.initialised && !hasTexture) {
            // load thumbnail or use stock if the file is not there
            skimmer.requestFile(-2)?.let {
                val zippedTga = (it.contents as EntryFile).bytes
                val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
                val tgaFileContents = gzin.readAllBytes(); gzin.close()

                thumbPixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
                val thumbTex = Texture(thumbPixmap)
                thumbTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                thumb = TextureRegion(thumbTex)
                thumb!!.setRegion(0, (thumbTex.height - 2 * height) / 2, width * 2, height * 2)
            }


            hasTexture = true
        }

        val highlightCol = if (mouseUp) UIItemTextButton.defaultActiveCol else Color.WHITE
        val x = posX.toFloat()
        val y = posY.toFloat()

        // draw background
        batch.color = cellCol
        Toolkit.fillArea(batch, posX, posY, width, height)
        // draw border
        batch.color = highlightCol
        Toolkit.drawBoxBorder(batch, posX-1, posY-1, width+2, height+2)

        // draw thumbnail
        batch.color = Color.WHITE
        blendNormal(batch)
        batch.draw(thumb ?: CommonResourcePool.getAsTextureRegion("terrarum-defaultsavegamethumb"), x, y + height, width.toFloat(), -height.toFloat())
        // draw gradient
        blendMul(batch)
        batch.draw(grad, x + width.toFloat(), y, -width.toFloat(), height.toFloat())

        // draw texts
        batch.color = highlightCol
        blendNormal(batch)

        // save status icon
        (if (saveDamaged) icons.get(1,0)
         else if (isAuto) icons.get(0,0)
         else if (isQuick) icons.get(2,0)
         else null)?.let {
            batch.draw(it, x + width - icons.tileW - 2f, y + 2f)
        }
        // timestamp
        val tlen = App.fontSmallNumbers.getWidth(lastPlayedTimestamp)
        App.fontSmallNumbers.draw(batch, lastPlayedTimestamp, x + (width - tlen) - 3f, y + height - 16f)
        // file size
        App.fontSmallNumbers.draw(batch, "${skimmer.diskFile.length().ushr(10)} KiB", x + 3f, y + height - 16f)
        // savegame name
        if (saveDamaged) batch.color = colourBad
        App.fontGame.draw(batch, saveName, x + 3f, y + 1f)

        super.render(batch, camera)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        thumb?.texture?.dispose()
        thumbPixmap?.dispose()
    }

}