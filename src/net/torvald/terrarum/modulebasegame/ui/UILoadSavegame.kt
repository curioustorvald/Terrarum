package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.unicode.getKeycapConsole
import net.torvald.unicode.getKeycapPC
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*
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
//    internal val titleTextPosY: Int = App.scr.tvSafeGraphicsHeight + 10
    internal val titleTopGradStart: Int = App.scr.tvSafeGraphicsHeight
    internal val titleTopGradEnd: Int = titleTopGradStart + gradAreaHeight
    internal val titleBottomGradStart: Int = height - App.scr.tvSafeGraphicsHeight - gradAreaHeight
    internal val titleBottomGradEnd: Int = titleBottomGradStart + gradAreaHeight


    private var scrollAreaHeight = height - 2 * App.scr.tvSafeGraphicsHeight - 64
    private var listScroll = 0 // only update when animation is finished
    private var savesVisible = (scrollAreaHeight + cellGap) / cellInterval
    private var uiScroll = 0f
    private var scrollFrom = 0
    private var scrollTarget = 0
    private var scrollAnimCounter = 0f
    private val scrollAnimLen = 0.1f
    private var sliderFBO = FrameBuffer(Pixmap.Format.RGBA8888, uiWidth + 10, height, false)

    internal var buttonSelectedForDeletion: UIItemPlayerCells? = null

    private val goButtonWidth = 180
    private val drawX = (Toolkit.drawWidth - 480) / 2
    private val drawY = (App.scr.height - 480) / 2
    private val buttonRowY = drawY + 480 - 24

    internal lateinit var loadables: SavegameCollectionPair // will be used and modified by subUIs

    /*private val altSelDrawW = 640
    private val altSelHdrawW = altSelDrawW / 2
    private val altSelDrawH = 480
    private val imageButtonW = 300
    private val imageButtonH = 240
    private val altSelDrawY = ((App.scr.height - altSelDrawH)/2)
    private val altSelQdrawW = altSelDrawW / 4
    private val altSelQQQdrawW = altSelDrawW * 3 / 4*/

    internal var hasNewerAutosave = false

    private val transitionalListing = UILoadList(this)
    private val transitionalAutosave = UILoadAutosave(this)
    private val transitionalManage = UILoadManage(this)
    private val transitionalNewCharacter = UINewCharacter(remoCon)
    private val transitionalSaveDamaged = UILoadSaveDamaged(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - internalWidth) / 2,
        INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        0f,
        listOf(transitionalListing),
        listOf(transitionalManage, transitionalNewCharacter, transitionalSaveDamaged),
        listOf(NullUI, transitionalAutosave)
    )

    internal fun queueUpManageScr() { transitionPanel.setCentreUIto(0) }
    internal fun queueUpNewCharScr() { transitionPanel.setCentreUIto(1) }
    internal fun queueUpDamagedSaveScr() { transitionPanel.setCentreUIto(2) }

    internal fun bringAutosaveSelectorUp() { transitionPanel.setRightUIto(1) }
    internal fun takeAutosaveSelectorDown() { transitionPanel.setRightUIto(0) }

    internal fun changePanelTo(index: Int) {
        transitionPanel.requestTransition(index)
    }

    override fun advanceMode(button: UIItem) {
        printdbg(this, "advanceMode ${button.javaClass.canonicalName}")

        if (button.javaClass.simpleName == "UIItemPlayerCells") {
            transitionalListing.advanceMode()
        }

    }

    override fun show() {
        takeAutosaveSelectorDown()
        transitionPanel.show()
        hasNewerAutosave = false
        /*try {
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
                        System.err.println("[UILoadSavegame] Error while loading Player '${skimmer.diskFile.absolutePath}'")
                        e.printStackTrace()
                    }
                }


                remoCon.handler.unlockToggle()
                showSpinner = false
            }.start()

        }
        catch (e: UninitializedPropertyAccessException) {}*/
    }

    override fun hide() {
        transitionPanel.hide()
//        playerCells.forEach { it.dispose() }
//        playerCells.clear()
    }

    private var touchLatched = false

//    private fun getCells() = playerCells
    private var loadFired = 0
    private var oldMode = -1

//    private val mode1Node = Yaml(UITitleRemoConYaml.injectedMenuSingleCharSel).parse()
//    private val mode2Node = Yaml(UITitleRemoConYaml.injectedMenuSingleWorldSel).parse()

//    private val menus = listOf(mode1Node, mode2Node)

    /*private val deleteCharacterButton = UIItemTextButton(
        this, "CONTEXT_CHARACTER_DELETE",
        UIRemoCon.menubarOffX - UIRemoCon.UIRemoConElement.paddingLeft + 72,
        UIRemoCon.menubarOffY - UIRemoCon.UIRemoConElement.lineHeight * 3 + 16,
        remoCon.width + UIRemoCon.UIRemoConElement.paddingLeft,
        true,
        inactiveCol = Toolkit.Theme.COL_RED,
        activeCol = Toolkit.Theme.COL_REDD,
        hitboxSize = UIRemoCon.UIRemoConElement.lineHeight - 2,
        alignment = UIItemTextButton.Companion.Alignment.LEFT
    ).also {
        it.clickOnceListener = { _,_ ->
            mode = MODE_SAVE_DELETE
            it.highlighted = true
        }
    }*/

    init {
        // this UI will NOT persist; the parent of the mode1Node must be set using an absolute value (e.g. treeRoot, not remoCon.currentRemoConContents)

        //printdbg(this, "UILoadSavegame called, from:")
        //printStackTrace(this)

//        mode1Node.parent = remoCon.treeRoot
//        mode2Node.parent = mode1Node

//        mode1Node.data = "MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadSavegame"
//        mode2Node.data = "MENU_MODE_SINGLEPLAYER : net.torvald.terrarum.modulebasegame.ui.UILoadSavegame"

//        printdbg(this, "mode1Node parent: ${mode1Node.parent?.data}") // will be 'null' because the parent is the root node
//        printdbg(this, "mode1Node data: ${mode1Node.data}")
//        printdbg(this, "mode2Node data: ${mode2Node.data}")

    }

    private fun modeChangedHandler(mode: Int) {
        printdbg(this, "Change mode: $oldMode -> $mode")
//        remoCon.setNewRemoConContents(menus[mode])
//        remoCon.setNewRemoConContents(mode1Node)
    }

    override fun updateUI(delta: Float) {
        transitionPanel.update(delta)
        /*if (mode == MODE_SELECT || mode == MODE_SAVE_DELETE) {

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
        }*/

        /*if (mode == MODE_SAVE_DELETE_CONFIRM && deleteCellAnimCounter <= scrollAnimLen) {
            // do transitional moving stuff
            buttonSelectedForDeletion?.posY = Movement.fastPullOut(deleteCellAnimCounter / scrollAnimLen, deleteCellPosYstart, (titleTopGradEnd + cellInterval).toFloat()).roundToInt()

            deleteCellAnimCounter += delta
            if (deleteCellAnimCounter > scrollAnimLen) deleteCellAnimCounter = scrollAnimLen
        }*/
    }

    private var deleteCellAnimCounter = 0f
    private var deleteCellPosYstart = 0f

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        transitionPanel.render(batch, camera)

        /*if (mode == MODE_LOAD_DA_SHIT_ALREADY) {
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

        if (mode == MODE_SAVE_DELETE_CONFIRM) {
            buttonSelectedForDeletion?.render(batch, camera)
            confirmCancelButton.render(batch, camera)
            confirmDeleteButton.render(batch, camera)

            batch.color = Color.WHITE
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, titleTopGradEnd + cellInterval - 46)
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, titleTopGradEnd + cellInterval + SAVE_CELL_HEIGHT + 36)
        }*/


    }

    override fun keyDown(keycode: Int): Boolean {
        transitionPanel.keyDown(keycode)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        transitionPanel.touchDown(screenX, screenY, pointer, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        transitionPanel.touchUp(screenX, screenY, pointer, button)
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        /*if (this.isVisible && mode == MODE_SELECT || mode == MODE_SAVE_DELETE) {
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
        }*/
        transitionPanel.scrolled(amountX, amountY)
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
        try { transitionPanel.dispose() } catch (e: IllegalArgumentException) {}
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

    internal fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).round(), (-newY + App.scr.halfh).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

}

