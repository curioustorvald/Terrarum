package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*


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

    internal var playerButtonSelected: UIItemPlayerCells? = null

    internal lateinit var loadables: SavegameCollectionPair // will be used and modified by subUIs

    internal lateinit var loadManageSelectedGame: DiskPair

    internal val buttonHeight = 24
    internal val buttonGap = 10
    internal val buttonWidth = 180
    internal val drawX = (Toolkit.drawWidth - 480) / 2
    internal val buttonRowY = App.scr.height - App.scr.tvSafeGraphicsHeight - 24
//    internal val drawY = (App.scr.height - 480) / 2

    private val transitionalListing = UILoadList(this)
//    private val transitionalAutosave = UILoadAutosave(this)
    private val transitionalManage = UILoadManage(this)
    private val transitionalNewCharacter = UINewCharacter(remoCon)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - internalWidth) / 2,
        INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        0f,
        listOf(transitionalListing),
        listOf(transitionalManage, transitionalNewCharacter),
        listOf(NullUI/*, transitionalAutosave*/)
    )

    private val nodesForListing = Yaml(UITitleRemoConYaml.injectedMenuSingleCharSel).parse()
    private val nodesForManage = Yaml(UITitleRemoConYaml.injectedMenuSingleSaveManage).parse()

    internal fun queueUpManageScr() {
        transitionPanel.setCentreUIto(0)
        remoCon.setNewRemoConContents(nodesForManage)
    }
    internal fun queueUpNewCharScr() {
        transitionPanel.setCentreUIto(1)
        remoCon.setNewRemoConContents(nodesForListing)
    }

//    internal fun bringAutosaveSelectorUp() { transitionPanel.setRightUIto(1) }
//    internal fun takeAutosaveSelectorDown() { transitionPanel.setRightUIto(0) }

    internal fun resetScroll() {
        transitionalListing.resetScroll()
    }

    internal fun changePanelTo(index: Int) {
        transitionPanel.requestTransition(index)
        if (index == 1)
            remoCon.setNewRemoConContents(nodesForManage)
        else
            remoCon.setNewRemoConContents(nodesForListing)
    }

    override fun advanceMode(button: UIItem) {
        printdbg(this, "advanceMode ${button.javaClass.canonicalName}")

        if (button.javaClass.simpleName == "UIItemPlayerCells") {
            transitionalListing.advanceMode()
            playerButtonSelected = button as UIItemPlayerCells
        }

    }

    override fun show() {
//        takeAutosaveSelectorDown()
        transitionPanel.show()

        nodesForListing.parent = remoCon.treeRoot
        nodesForManage.parent = remoCon.treeRoot

    }

    override fun hide() {
        transitionPanel.hide()
    }

    override fun updateUI(delta: Float) {
        transitionPanel.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        transitionPanel.render(batch, camera)
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        transitionPanel.inputStrobed(e)
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
        transitionPanel.scrolled(amountX, amountY)
        return true
    }

    override fun dispose() {
        shapeRenderer.tryDispose()
        transitionPanel.tryDispose()
    }

    override fun resize(width: Int, height: Int) {
        transitionPanel.uis.forEach { it.resize(width, height) }
        super.resize(width, height)
    }

    internal fun setCameraPosition(batch: SpriteBatch, camera: OrthographicCamera, newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

}

