package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.unicode.EMDASH
import net.torvald.unicode.getKeycapConsole
import net.torvald.unicode.getKeycapPC
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.CELL_COL
import net.torvald.terrarum.savegame.ByteArray64InputStream
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.EntryFile
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.SaveLoadError
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.savegame.VDFileID.BODYPART_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF
import net.torvald.terrarum.spriteassembler.ADProperties
import net.torvald.terrarum.spriteassembler.ADProperties.Companion.EXTRA_HEADROOM_X
import net.torvald.terrarum.spriteassembler.ADProperties.Companion.EXTRA_HEADROOM_Y
import net.torvald.terrarum.spriteassembler.AssembleFrameBase
import net.torvald.terrarum.spriteassembler.AssembleSheetPixmap
import net.torvald.terrarum.ui.Movement
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

val SAVE_CELL_WIDTH = 560
val SAVE_CELL_HEIGHT = 120
val SAVE_THUMBNAIL_MAIN_WIDTH = SAVE_CELL_WIDTH
val SAVE_THUMBNAIL_MAIN_HEIGHT = 384

/**
 * The pinnacle of the dirty coding! This object exists only because I couldn't make
 * UILoadDemoSavefiles persistent.
 *
 * This objects holds which Player and World has been chosen.
 *
 * WARNING: the values are not guaranteed to reset when the selector UI is closed!
 */
object UILoadGovernor {
    // used by the default save loader
    var playerUUID: UUID? = null
    var worldUUID: UUID? = null
    var previousSaveWasLoaded = false
    // used by the debug save loader
    /*var playerDisk: DiskSkimmer? = null
        set(value) {
            printdbg(this, "Player selected: ${value?.diskFile?.name}")
            field = value
        }

    var worldDisk: DiskSkimmer? = null
        set(value) {
            printdbg(this, "World selected: ${value?.diskFile?.name}")
            field = value
        }*/

    var interruptSavegameListGenerator = false

    fun reset() {
        printdbg(this, "Resetting player and world selection")
//        playerDisk = null
//        worldDisk = null

        playerUUID = null
        worldUUID = null
        previousSaveWasLoaded = false
    }
}

abstract class Advanceable : UICanvas() {
    abstract fun advanceMode(button: UIItem)
}

/**
 * Only works if current screen set by the App is [TitleScreen]
 *
 * Created by minjaesong on 2021-09-09.
 */
class UILoadDemoSavefiles(val remoCon: UIRemoCon) : Advanceable() {

//    private val hash = RandomWordsName(3)

    init {
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
    override var openCloseTime: Second = OPENCLOSE_GENERIC


    private val shapeRenderer = App.makeShapeRenderer()


    internal val uiWidth = SAVE_CELL_WIDTH
    internal val uiX: Int
        get() = (Toolkit.drawWidth - uiWidth) / 2
    internal val uiXdiffChatOverlay = App.scr.chatWidth / 2

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
            "${getKeycapPC(ControlPresets.getKey("control_key_up"))}${getKeycapPC(ControlPresets.getKey("control_key_down"))}" +
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

    private var sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)

    private var showSpinner = false

    private val worldCells = ArrayList<UIItemWorldCells>()
    private val playerCells = ArrayList<UIItemPlayerCells>()

    var mode = 0; private set// 0: show players, 1: show worlds

    constructor(remoCon: UIRemoCon, mode: Int) : this(remoCon) {
        this.mode = mode
    }

    override fun advanceMode(button: UIItem) {
        mode += 1
        uiScroll = 0f
        scrollFrom = 0
        scrollTarget = 0
        scrollAnimCounter = 0f
        loadFired = 0

        printdbg(this, "savelist mode: $mode")
    }

    override fun show() {
        try {
            remoCon.handler.lockToggle()
            showSpinner = true

            Thread {
                // read savegames
                var savegamesCount = 0
                App.sortedSavegameWorlds.forEach { uuid ->
                    val skimmer = App.savegameWorlds[uuid]!!.loadable()
                    val x = uiX
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
                App.sortedPlayers.forEach { uuid ->
                    val x = uiX
                    val y = titleTopGradEnd + cellInterval * savegamesCount
                    try {
                        playerCells.add(UIItemPlayerCells(this, x, y, uuid))
                        savegamesCount += 1
                    }
                    catch (e: Throwable) {
                        System.err.println("[UILoadDemoSavefiles] Error while loading Player with UUID $uuid")
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
    private var oldMode = -1

    private val mode1Node = Yaml(UITitleRemoConYaml.injectedMenuSingleCharSel).parse()
    private val mode2Node = Yaml(UITitleRemoConYaml.injectedMenuSingleWorldSel).parse()

    private val menus = listOf(mode1Node, mode2Node)
    private val titles = listOf("CONTEXT_CHARACTER", "MENU_LABEL_WORLD")

    init {
        // this UI will NOT persist; the parent of the mode1Node must be set using an absolute value (e.g. treeRoot, not remoCon.currentRemoConContents)

        //printdbg(this, "UILoadDemoSaveFiles called, from:")
        //printStackTrace(this)

        mode1Node.parent = remoCon.treeRoot
        mode2Node.parent = mode1Node

        mode1Node.data = "MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadDemoSavefiles"
        mode2Node.data = "MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadDemoSavefiles"

//        printdbg(this, "mode1Node parent: ${mode1Node.parent?.data}") // will be 'null' because the parent is the root node
//        printdbg(this, "mode1Node data: ${mode1Node.data}")
//        printdbg(this, "mode2Node data: ${mode2Node.data}")
    }

    private fun modeChangedHandler(mode: Int) {
        remoCon.setNewRemoConContents(menus[mode])
    }

    override fun updateImpl(delta: Float) {

        if (mode < 2) {

            if (oldMode != mode) {
                modeChangedHandler(mode)
                oldMode = mode
            }

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

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {

        if (mode == 2) {
            loadFired += 1
            // to hide the "flipped skybox" artefact
            batch.end()

            gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

            batch.begin()

            batch.color = Color.WHITE
            val txt = Lang["MENU_IO_LOADING"]
            App.fontGame.draw(batch, txt, (App.scr.width - App.fontGame.getWidth(txt)) / 2f, (App.scr.height - App.fontGame.lineHeight) / 2f)

            if (loadFired == 2) {
                LoadSavegame(
                    App.savegamePlayers[UILoadGovernor.playerUUID]!!.loadable(),
                    App.savegameWorlds[UILoadGovernor.worldUUID]?.loadable()
                )
            }
        }
        else {
            batch.end()

            val cells = getCells()

            lateinit var savePixmap: Pixmap
            sliderFBO.inAction(camera as OrthographicCamera, batch) {
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

                setCameraPosition(batch, camera, 0f, 0f)
                batch.color = Color.WHITE
                batch.inUse {
                    for (index in 0 until cells.size) {
                        val it = cells[index]

                        if (App.getConfigBoolean("fx_streamerslayout"))
                            it.posX += uiXdiffChatOverlay

                        if (index in listScroll - 2 until listScroll + savesVisible + 2)
                            it.render(frameDelta, batch, camera)

                        if (App.getConfigBoolean("fx_streamerslayout"))
                            it.posX -= uiXdiffChatOverlay
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
            val saveTex = TextureRegion(Texture(savePixmap)); saveTex.flip(false, true)
            batch.inUse {
                batch.draw(saveTex, (width - uiWidth - 10) / 2f, 0f)

                // draw texts
                val loadGameTitleStr = Lang[titles[mode]]// + "$EMDASH$hash"
                // "Game Load"
                App.fontUITitle.draw(batch, loadGameTitleStr, (width - App.fontUITitle.getWidth(loadGameTitleStr)).div(2).toFloat(), titleTextPosY.toFloat())
                // Control help
                App.fontGame.draw(batch, controlHelp, uiX.toFloat(), controlHelperY.toFloat())
            }

            saveTex.texture.dispose()
            savePixmap.dispose()

            batch.begin()
        }
    }


    override fun keyDown(keycode: Int): Boolean {
        if (this.isVisible) {
            val cells = getCells()

            if ((keycode == Input.Keys.UP || keycode == ControlPresets.getKey("control_key_up")) && scrollTarget > 0) {
                scrollFrom = listScroll
                scrollTarget -= 1
                scrollAnimCounter = 0f
            }
            else if ((keycode == Input.Keys.DOWN || keycode == ControlPresets.getKey("control_key_down")) && scrollTarget < cells.size - savesVisible) {
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

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        listScroll = 0
        scrollTarget = 0
        uiScroll = 0f
    }

    override fun dispose() {
        shapeRenderer.tryDispose()
        sliderFBO.tryDispose()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
        savesVisible = (scrollAreaHeight + cellInterval) / (cellInterval + SAVE_CELL_HEIGHT)

        listScroll = 0
        scrollTarget = 0
        uiScroll = 0f

        sliderFBO.dispose()
        sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)
    }

    private fun setCameraPosition(batch: SpriteBatch, camera: OrthographicCamera, newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

}




class UIItemPlayerCells(
    parent: Advanceable,
    initialX: Int,
    initialY: Int,
    val playerUUID: UUID,
) : UIItem(parent, initialX, initialY) {

    override val width = SAVE_CELL_WIDTH
    override val height = SAVE_CELL_HEIGHT

    override var clickOnceListener: ((Int, Int) -> Unit) = { _: Int, _: Int ->
//        UILoadGovernor.playerDisk = App.
        UILoadGovernor.playerUUID = playerUUID
        UILoadGovernor.worldUUID = worldUUID
        parent.advanceMode(this)
    }

    internal var playerName: String = "$EMDASH"
    private var worldName: String = "$EMDASH"
    private var lastPlayTime: String = "????-??-?? --:--:--"
    private var totalPlayTime: String = "--h--m--s"
    private var versionString: String = "0.0.0"
    var isNewerVersion = false; private set

//    lateinit var playerUUID: UUID; private set
    lateinit var worldUUID: UUID; private set

    private val savegameStatus: Int
//    internal val pixmapManual: Pixmap?
//    internal val pixmapAuto: Pixmap?
    internal val savegameThumbnailPixmap: Pixmap?

    private val isImported: Boolean

    init {
        val loadable = App.savegamePlayers[playerUUID]!!.loadable()
        printdbg(this, "UUID: ${playerUUID}")
        printdbg(this, "File: ${loadable.diskFile.absolutePath}")
        loadable.rebuild()
        loadable.getFile(SAVEGAMEINFO)?.bytes?.let {
            var lastPlayTime0 = 0L
            var genverLong = 0L
            var genver = ""

            JsonFetcher.readFromJsonString(ByteArray64Reader(it, Common.CHARSET)).forEachSiblings { name, value ->
                if (name == "worldCurrentlyPlaying") worldUUID = UUID.fromString(value.asString())
                if (name == "totalPlayTime") totalPlayTime = parseDuration(value.asLong())
                if (name == "lastPlayTime") lastPlayTime0 = value.asLong()
                if (name == "genver") {
                    genverLong = value.asLong()
                    genver = genverLong.let { "${it.ushr(48)}.${it.ushr(24).and(0xFFFFFF)}.${it.and(0xFFFFFF)}" }
                }
            }

            val snap = loadable.getSaveSnapshotVersion()
            versionString = genver + (if (snap != null) "-$snap" else "")

            val savegameVersionNum = // 0x GGGG GGGGGG GGGGGG YY WW RR
                BigInteger(genverLong.toString()) * BigInteger("16777216") + BigInteger(snap?.hashCode()?.toString() ?: "16777215")
            val thisVersionNum =
                BigInteger(TerrarumAppConfiguration.VERSION_RAW.toString()) * BigInteger("16777216") + BigInteger(TerrarumAppConfiguration.VERSION_SNAPSHOT?.hashCode()?.toString() ?: "16777215")

            isNewerVersion = (savegameVersionNum > thisVersionNum)

            App.savegamePlayersName[playerUUID]?.let { if (it.isNotBlank()) playerName = it else "(name)" }
            App.savegameWorldsName[worldUUID]?.let { if (it.isNotBlank()) worldName = it }
            lastPlayTime = Instant.ofEpochSecond(lastPlayTime0)
                    .atZone(TimeZone.getDefault().toZoneId())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        } ?: throw Error("SAVEGAMEINFO not found on Skimmer:${loadable.diskFile}")

        val savegamePair = SavegameCollectionPair(App.savegamePlayers[playerUUID], App.savegameWorlds[worldUUID])
        savegameStatus = savegamePair.status
//        pixmapManual = savegamePair.getManualSave()?.player?.getThumbnailPixmap(SAVE_THUMBNAIL_MAIN_WIDTH, SAVE_THUMBNAIL_MAIN_HEIGHT, 2.0)
//        pixmapAuto = savegamePair.getAutoSave()?.player?.getThumbnailPixmap(SAVE_THUMBNAIL_MAIN_WIDTH, SAVE_THUMBNAIL_MAIN_HEIGHT, 2.0)
        savegameThumbnailPixmap = savegamePair.getPlayerThumbnailPixmap(SAVE_THUMBNAIL_MAIN_WIDTH, SAVE_THUMBNAIL_MAIN_HEIGHT, 2.0)

        isImported = savegamePair.isImported
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

    private var sprite: TextureRegion? = null

    internal var hasTexture = false
        private set

    private val litCol = Toolkit.Theme.COL_MOUSE_UP
    private val cellCol = CELL_COL
    private val defaultCol = Toolkit.Theme.COL_INACTIVE
    private val hruleCol = Color(1f,1f,1f,0.35f)
    private val hruleColLit = litCol.cpy().sub(0f,0f,0f,0.65f)

    private val icons = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    private var highlightCol: Color = defaultCol
    private var highlightTextCol: Color = defaultCol

    var forceUnhighlight = false

    override fun update(delta: Float) {
        super.update(delta)
        highlightCol = if (mouseUp && !forceUnhighlight) litCol else defaultCol
        highlightTextCol = if (mouseUp && !forceUnhighlight) litCol else Toolkit.Theme.COL_LIST_DEFAULT
    }

    private val avatarViewWidth = 120

    fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera, offX: Int, offY: Int) {
        // try to generate a texture
        if (!hasTexture) { acquirePlayerAvatar(); hasTexture = true }

        val x = posX + offX
        val y = posY + offY

        val line1 = y + 3f
        val line2 = line1 + 30
        val line3 = line2 + 30
        val line4 = line3 + 30

        // draw box backgrounds
        batch.color = cellCol
        Toolkit.fillArea(batch, x, y, avatarViewWidth, height)
        Toolkit.fillArea(batch, x + avatarViewWidth + 10, y, width - avatarViewWidth - 10, height)

        // draw borders
        batch.color = highlightCol
        // avatar border
        Toolkit.drawBoxBorder(batch, x - 1, y - 1, avatarViewWidth + 2, height + 2)
        // infocell border
        Toolkit.drawBoxBorder(batch, x + avatarViewWidth + 9, y - 1, width - avatarViewWidth - 8, height + 2)

        // texts
        batch.color = if (isNewerVersion) Toolkit.Theme.COL_RED else highlightTextCol
        App.fontGame.draw(batch, versionString, x + avatarViewWidth + 40f, line4)

        batch.color = highlightTextCol
        val playTimeTextLen = App.fontGame.getWidth(totalPlayTime)
        App.fontGame.draw(batch, playerName, x + avatarViewWidth + 40f, line1)
        App.fontGame.draw(batch, worldName, x + avatarViewWidth + 40f, line2)
        App.fontGame.draw(batch, lastPlayTime, x + avatarViewWidth + 40f, line3)
        App.fontGame.draw(batch, totalPlayTime, x + width - 5f - playTimeTextLen, line4)
        // icons
        batch.draw(icons.get(24,0), x + avatarViewWidth + 14f - 0, line1 + 2f) // player name
        batch.draw(icons.get(12,0), x + avatarViewWidth + 14f - 1, line2 + 2f) // world map
        batch.draw(icons.get(13,0), x + avatarViewWidth + 14f - 0, line3 + 2f) // journal
        batch.draw(icons.get(22,1), x + avatarViewWidth + 14f - 1, line4 + 2f) // version(?)
        batch.draw(icons.get(23,0), x + width - 4f - playTimeTextLen - 24f, line4 + 2f) // stopwatch

        // save status marker
        (if (isImported)
            icons.get(21,1)
        else if (savegameStatus == 2) // newer autosave
            icons.get(24,1)
        else if (savegameStatus == 0) // no world data found
            icons.get(23,1)
        else null)?.let {
            batch.draw(it, x + width - 25f, line1 + 2f)
        }



        // infocell divider
        batch.color = if (mouseUp) hruleColLit else hruleCol
        Toolkit.fillArea(batch, x + avatarViewWidth + 12, y + 29, width - avatarViewWidth - 14, 1)
        Toolkit.fillArea(batch, x + avatarViewWidth + 12, y + 59, width - avatarViewWidth - 14, 1)
        Toolkit.fillArea(batch, x + avatarViewWidth + 12, y + 89, width - avatarViewWidth - 14, 1)

        // player avatar
        batch.color = Color.WHITE
        this.sprite?.let {
            batch.draw(it,
                    x.toFloat() + FastMath.ceil((avatarViewWidth - it.regionWidth) / 2f) + EXTRA_HEADROOM_X / 2,
                    y.toFloat() + FastMath.ceil((height - it.regionHeight) / 2f) - EXTRA_HEADROOM_Y / 2
            )
        }
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        render(frameDelta, batch, camera, 0, 0)
    }

    private fun acquirePlayerAvatar() {
        val skimmer = App.savegamePlayers[playerUUID]!!.loadable()
        skimmer.getFile(SAVEGAMEINFO)?.bytes?.let {
            try {
                printdbg(this, "Generating portrait for $playerName")
                val frameName = "ANIM_IDLE_1"
                val animFile = skimmer.getFile(SPRITEDEF)!!
                val props = ADProperties(ByteArray64Reader(animFile.bytes, Common.CHARSET))

                val imagesSelfContained = skimmer.hasEntry(BODYPART_TO_ENTRY_MAP)
                val bodypartMapping = Properties().also { if (imagesSelfContained) it.load(ByteArray64Reader(skimmer.getFile(BODYPART_TO_ENTRY_MAP)!!.bytes, Common.CHARSET)) }

                val fileGetter = if (imagesSelfContained)
                    AssembleSheetPixmap.getVirtualDiskFileGetter(bodypartMapping, skimmer)
                else
                    AssembleSheetPixmap.getAssetsDirFileGetter(props)

//                    println(properties.transforms.keys)

                val canvas = Pixmap(props.frameWidth, props.frameHeight, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.SourceOver }
                val theAnim = props.getAnimByFrameName(frameName)
                val skeleton = theAnim.skeleton.joints.reversed()
                val transforms = props.getTransform(frameName)
                val bodypartOrigins = props.bodypartJoints
                val bodypartImages = props.bodypartJoints.keys.associate { partname ->
                    fileGetter(partname).let { file ->
                        if (file == null) {
//                                printdbg(this, "   Partname $partname points to a null file!")
                            partname to null
                        }
                        else {
                            try {
//                                    printdbg(this, "   Partname $partname successfully retrieved")
                                val bytes = file.readAllBytes()
                                partname to Pixmap(bytes, 0, bytes.size)
                            }
                            catch (e: GdxRuntimeException) {
//                                    printdbg(this, "   Partname $partname failed to load: ${e.message}")
                                partname to null
                            }
                        }
                    }
                }
                val transformList = AssembleFrameBase.makeTransformList(skeleton, transforms)

                // manually draw 0th frame of ANIM_IDLE
                transformList.forEach { (name, bodypartPos) ->
                    bodypartImages[name]?.let { image ->
                        val imgCentre = bodypartOrigins[name]!!.invertX()
                        val drawPos = props.origin + bodypartPos + imgCentre

                        canvas.drawPixmap(image, drawPos.x, props.frameHeight - drawPos.y - 1)
                    }
                }

                // dispose of temporary resources
                bodypartImages.values.forEach { it?.dispose() }

//                    PixmapIO.writePNG(Gdx.files.absolute("${App.defaultDir}/Exports/Portrait-$playerName.tga"), canvas)

                this.sprite = TextureRegion(Texture(canvas))
            }
            catch (e: Throwable) {
                throw SaveLoadError(skimmer.diskFile, e)
            }
        }
    }

    override fun dispose() {
        sprite?.texture?.dispose()
//        pixmapManual?.dispose()
//        pixmapAuto?.dispose()
        savegameThumbnailPixmap?.dispose()
    }


}


class UIItemWorldCells(
        parent: Advanceable,
        initialX: Int,
        initialY: Int,
        val skimmer: DiskSkimmer) : UIItem(parent, initialX, initialY) {


    private val metaFile: EntryFile?
    private val saveName: String
    private val saveMode: Int
    private val snapshot: String
    private val isQuick: Boolean
    private val isAuto: Boolean
    private var saveDamaged: Boolean = false
    private val lastPlayedTimestamp: String

    init {
        metaFile = skimmer.getFile(-1)
        if (metaFile == null) saveDamaged = true

        saveName = skimmer.getDiskName(Common.CHARSET)
        saveMode = skimmer.getSaveMode()
        snapshot = skimmer.getSaveSnapshotVersion()?.toString() ?: ""
        isQuick = (saveMode % 2 == 1)
        isAuto = (saveMode.ushr(1) != 0)

        saveDamaged = saveDamaged or checkForSavegameDamage(skimmer)

        if (metaFile != null) {
//            val lastplay_t = skimmer.getLastModifiedTime()//worldJson["lastPlayTime"].asLong()
            var playtime_t = ""
            var lastplay_t = 0L
            JsonFetcher.readFromJsonString(ByteArray64Reader(metaFile.bytes, Common.CHARSET)).forEachSiblings { name, value ->
                if (name == "lastPlayTime") lastplay_t = value.asLong()
                if (name == "totalPlayTime") playtime_t = parseDuration(value.asLong())
            }
            lastPlayedTimestamp =
                    Instant.ofEpochSecond(lastplay_t)
                            .atZone(TimeZone.getDefault().toZoneId())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    "/$playtime_t"
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

    private var highlightCol: Color = Toolkit.Theme.COL_LIST_DEFAULT

    override var clickOnceListener: ((Int, Int) -> Unit) = { _: Int, _: Int ->
//        UILoadGovernor.worldDisk = skimmer
        parent.advanceMode(this)
    }

    internal var hasTexture = false
        private set

    override fun update(delta: Float) {
        super.update(delta)
        highlightCol = if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_LIST_DEFAULT
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
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
        blendNormalStraightAlpha(batch)
        batch.draw(thumb ?: CommonResourcePool.getAsTextureRegion("terrarum-defaultsavegamethumb"), x, y, width.toFloat(), height.toFloat())
        // draw gradient
        blendMul(batch)
        batch.draw(grad, x + width.toFloat(), y, -width.toFloat(), height.toFloat())

        // draw texts
        batch.color = highlightCol
        blendNormalStraightAlpha(batch)

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
        App.fontGame.draw(batch, saveName, x + 3f, y + -1f)

        super.render(frameDelta, batch, camera)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        thumb?.texture?.dispose()
        thumbPixmap?.dispose()
    }

}