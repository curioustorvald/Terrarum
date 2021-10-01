package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.getKeycapConsole
import net.torvald.getKeycapPC
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.LoadSavegame
import net.torvald.terrarum.serialise.ReadMeta
import net.torvald.terrarum.tvda.ByteArray64InputStream
import net.torvald.terrarum.tvda.DiskSkimmer
import net.torvald.terrarum.tvda.EntryFile
import net.torvald.terrarum.tvda.VDUtil
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2021-09-09.
 */
class UILoadDemoSavefiles : UICanvas() {

    init {
        CommonResourcePool.addToLoadingList("terrarum-defaultsavegamethumb") {
            TextureRegion(Texture(Gdx.files.internal("assets/graphics/gui/savegame_thumb_placeholder.png")))
        }
        CommonResourcePool.loadAll()
    }

    override var width: Int
        get() = App.scr.width
        set(value) {}
    override var height: Int
        get() = App.scr.height
        set(value) {}
    override var openCloseTime: Second = 0f


    private val shapeRenderer = ShapeRenderer()


    private val uiWidth = UIItemDemoSaveCells.WIDTH // 480
    private val uiX = (width - uiWidth) / 2

    private val textH = App.fontGame.lineHeight.toInt()

    private val cellGap = 20
    private val cellInterval = cellGap + UIItemDemoSaveCells.HEIGHT
    private val gradAreaHeight = 32

    private val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10
    private val titleTopGradStart: Int = titleTextPosY + textH
    private val titleTopGradEnd: Int = titleTopGradStart + gradAreaHeight
    private val titleBottomGradStart: Int = height - App.scr.tvSafeGraphicsHeight - gradAreaHeight
    private val titleBottomGradEnd: Int = titleBottomGradStart + gradAreaHeight
    private val controlHelperY: Int = titleBottomGradStart + gradAreaHeight - textH


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

    override fun show() {
        printdbg(this, "savefiles show()")

        // read savegames
        var savegamesCount = 0
        App.savegames.forEach { skimmer ->
            val x = uiX
            val y = titleTopGradEnd + cellInterval * savegamesCount
            try {
                addUIitem(UIItemDemoSaveCells(this, x, y, skimmer))
                savegamesCount += 1
            }
            catch (e: Throwable) {
                System.err.println("[UILoadDemoSavefiles] Savefile '${skimmer.diskFile.absolutePath}' cannot be loaded")
                e.printStackTrace()
            }

        }
    }

    override fun hide() {
        uiItems.forEach { it.dispose() }
        uiItems.clear()
    }

    override fun updateUI(delta: Float) {

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


        uiItems.forEachIndexed { index, it ->
            if (index in listScroll - 2 until listScroll + savesVisible + 2) {
                // re-position
                it.posY = (it.initialY - uiScroll).roundToInt()
                it.update(delta)
            }
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {



        batch.end()

        lateinit var savePixmap: Pixmap
        sliderFBO.inAction(camera as OrthographicCamera, batch) {
            gdxClearAndSetBlend(0f,0f,0f,0f)

            setCameraPosition(batch, camera, 0f, 0f)
            batch.color = Color.WHITE
            batch.inUse {
                uiItems.forEachIndexed { index, it ->
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
        savePixmap.setColor(0f,0f,0f,0f)
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
        savePixmap.setColor(0f,0f,0f,0f)
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


    override fun keyDown(keycode: Int): Boolean {
        if (this.isVisible) {
            if ((keycode == Input.Keys.UP || keycode == App.getConfigInt("control_key_up")) && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if ((keycode == Input.Keys.DOWN || keycode == App.getConfigInt("control_key_down")) && scrollTarget < uiItems.size - savesVisible) {
                scrollFrom = listScroll
                scrollTarget += 1
                scrollAnimCounter = 0f
            }
        }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (this.isVisible) {
            if (amountY <= -1f && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if (amountY >= 1f && scrollTarget < uiItems.size - savesVisible) {
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
        shapeRenderer.dispose()
        sliderFBO.dispose()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
        savesVisible = (scrollAreaHeight + cellInterval) / (cellInterval + UIItemDemoSaveCells.HEIGHT)

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







class UIItemDemoSaveCells(
        parent: UILoadDemoSavefiles,
        initialX: Int,
        initialY: Int,
        val skimmer: DiskSkimmer) : UIItem(parent, initialX, initialY) {

    companion object {
        const val WIDTH = 480
        const val HEIGHT = 120
    }

    init {
        CommonResourcePool.addToLoadingList("savegame_status_icon") {
            TextureRegionPack("assets/graphics/gui/savegame_status_icon.tga", 24, 24)
        }
        CommonResourcePool.loadAll()

        printdbg(this, "Rebuilding skimmer for savefile ${skimmer.diskFile.absolutePath}")
        skimmer.rebuild()
    }

    private var saveDamaged = checkForSavegameDamage(skimmer)

    override val width: Int = WIDTH
    override val height: Int = HEIGHT

    private var thumbPixmap: Pixmap? = null
    private var thumb: TextureRegion? = null
    private val grad = CommonResourcePool.getAsTexture("title_halfgrad")

    private val icons = CommonResourcePool.getAsTextureRegionPack("savegame_status_icon")

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

    private val metaFile = skimmer.requestFile(-1)

    private val saveName = skimmer.getDiskName(Common.CHARSET)
    private val saveMode = skimmer.getSaveMode()
    private val isQuick = (saveMode % 2 == 1)
    private val isAuto = (saveMode.ushr(1) != 0)
    private val meta = if (metaFile != null) ReadMeta.fromDiskEntry(metaFile) else null

    private val colourBad = Color(0xFF8888FF.toInt())

    private val lastPlayedTimestamp = if (meta != null)
        Instant.ofEpochSecond(meta.lastplay_t)
            .atZone(TimeZone.getDefault().toZoneId())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                                      "/${parseDuration(meta.playtime_t)}"
        else "--:--:--/--h--m--s"

    init {
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

    }

    override var clickOnceListener: ((Int, Int, Int) -> Unit)? = { _: Int, _: Int, _: Int ->
        LoadSavegame(VDUtil.readDiskArchive(skimmer.diskFile, Level.INFO))
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        val highlightCol = if (mouseUp) UIItemTextButton.defaultActiveCol else Color.WHITE
        val x = posX.toFloat()
        val y = posY.toFloat()


        // TODO draw border
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