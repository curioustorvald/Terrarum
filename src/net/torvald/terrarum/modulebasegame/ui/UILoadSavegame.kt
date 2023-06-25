package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.JsonReader
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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt


/**
 * Only works if current screen set by the App is [TitleScreen]
 *
 * Created by minjaesong on 2023-06-24.
 */
class UILoadSavegame(val remoCon: UIRemoCon) : Advanceable() {

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

    private var sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)

    private var showSpinner = false

    private val playerCells = ArrayList<UIItemPlayerCells>()

    var mode = 0; private set// 0: show players, 1: show worlds

    override fun advanceMode() {
        mode += 1
        uiScroll = 0f
        scrollFrom = 0
        scrollTarget = 0
        scrollAnimCounter = 0f
        loadFired = 0

        printdbg(this, "savelist mode: $mode")

        // look for recently played world
        if (mode == 1) {

            // select the most recent loadable save by comparing manual and autosaves, NOT JUST going with loadable()
            printdbg(this, "Load playerUUID: ${UILoadGovernor.playerUUID}, worldUUID: ${UILoadGovernor.worldUUID}")
            val loadables = SavegameCollectionPair(App.savegamePlayers[UILoadGovernor.playerUUID], App.savegameWorlds[UILoadGovernor.worldUUID])

            var loadAuto = false
            if (loadables.moreRecentAutosaveAvailable()) {
                // TODO make choice for load manual or auto, if available

            }
            else if (!loadables.saveAvaliable()) {
                // TODO show save is damaged and cannot be loaded
                return
            }

            val (p, w) = if (loadAuto) loadables.getAutoSave()!! else loadables.getManualSave()!!
            UILoadGovernor.playerDisk = p; UILoadGovernor.worldDisk = w

            if (loadables.newerSaveIsDamaged) {
                // TODO queue message: GAME_PREV_SAVE_WAS_LOADED
            }

            mode += 1

        }
    }

    override fun show() {
        try {
            remoCon.handler.lockToggle()
            showSpinner = true

            Thread {
                // read savegames
                var savegamesCount = 0
                App.sortedPlayers.forEach { uuid ->
                    val skimmer = App.savegamePlayers[uuid]!!.loadable()
                    val x = uiX
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
        playerCells.forEach { it.dispose() }
        playerCells.clear()
    }

    private fun getCells() = playerCells
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

    override fun updateUI(delta: Float) {

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

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        if (mode == 2) {
            loadFired += 1
            // to hide the "flipped skybox" artefact
            batch.end()

            gdxClearAndEnableBlend(.094f, .094f, .094f, 0f)

            batch.begin()

            batch.color = Color.WHITE
            val txt = Lang["MENU_IO_LOADING"]
            App.fontGame.draw(batch, txt, (App.scr.width - App.fontGame.getWidth(txt)) / 2f, (App.scr.height - App.fontGame.lineHeight) / 2f)

            if (loadFired == 2) {
                LoadSavegame(UILoadGovernor.playerDisk!!, UILoadGovernor.worldDisk)
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
                            it.render(batch, camera)

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

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
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
        sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)
    }

    private fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).round(), (-newY + App.scr.halfh).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

}

