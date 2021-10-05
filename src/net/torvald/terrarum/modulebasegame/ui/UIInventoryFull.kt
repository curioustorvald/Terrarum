package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.ENDASH
import net.torvald.getKeycapPC
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemHorizontalFadeSlide
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        toggleKeyLiteral: Int? = App.getConfigInt("control_key_inventory"), toggleButtonLiteral: Int? = App.getConfigInt("control_gamepad_start"),
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    val actor: ActorHumanoid
        get() = INGAME.actorNowPlaying!!

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height
    override var openCloseTime: Second = 0.0f

    companion object {
        const val INVEN_DEBUG_MODE = false

        const val REQUIRED_MARGIN: Int = 138 // hard-coded value. Don't know the details. Range: [91-146]. I chose MAX-8 because cell gap is 8
        const val CELLS_HOR = 10
        val CELLS_VRT: Int; get() = (App.scr.height - REQUIRED_MARGIN - 134 + UIItemInventoryItemGrid.listGap) / // 134 is another magic number
                                (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap)

        const val itemListToEquipViewGap = UIItemInventoryItemGrid.listGap // used to be 24; figured out that the extra gap does nothig

        val internalWidth: Int = UIItemInventoryItemGrid.getEstimatedW(CELLS_HOR) + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
        val internalHeight: Int = REQUIRED_MARGIN + UIItemInventoryItemGrid.getEstimatedH(CELLS_VRT) // grad_begin..grad_end..contents..grad_begin..grad_end

        val itemListHeight: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap

        val INVENTORY_CELLS_UI_HEIGHT: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * UIItemInventoryItemGrid.listGap
        val INVENTORY_CELLS_OFFSET_X = 0 + (Toolkit.drawWidth - internalWidth) / 2
        val INVENTORY_CELLS_OFFSET_Y: Int = 107 + (App.scr.height - internalHeight) / 2

        val catBarWidth = 330

        val gradStartCol = Color(0x404040_60)
        val gradEndCol   = Color(0x000000_70)
        val gradHeight = 48f

        val controlHelpHeight = App.fontGame.lineHeight

        fun drawBackground(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
            batch.end()
            gdxSetBlendNormal()


            val gradTopStart = (App.scr.height - internalHeight).div(2).toFloat()
            val gradBottomEnd = App.scr.height - gradTopStart

            shapeRenderer.inUse {
                // shaperender starts at bottom-left!

                shapeRenderer.rect(0f, gradTopStart, App.scr.wf, gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)
                shapeRenderer.rect(0f, gradBottomEnd, App.scr.wf, -gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)

                shapeRenderer.rect(0f, gradTopStart + gradHeight, App.scr.wf, internalHeight - (2 * gradHeight), gradEndCol, gradEndCol, gradEndCol, gradEndCol)

                shapeRenderer.rect(0f, 0f, App.scr.wf, gradTopStart, gradStartCol, gradStartCol, gradStartCol, gradStartCol)
                shapeRenderer.rect(0f, App.scr.hf, App.scr.wf, -(App.scr.hf - gradBottomEnd), gradStartCol, gradStartCol, gradStartCol, gradStartCol)
            }

            batch.begin()
        }
    }

    //val REQUIRED_MARGIN: Int = 138 // hard-coded value. Don't know the details. Range: [91-146]. I chose MAX-8 because cell gap is 8

    //val CELLS_HOR = 10
    //val CELLS_VRT: Int; get() = (AppLoader.terrarumAppConfig.screenH - REQUIRED_MARGIN - 134 + UIItemInventoryItemGrid.listGap) / // 134 is another magic number
    //                            (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap)

    //private val itemListToEquipViewGap = UIItemInventoryItemGrid.listGap // used to be 24; figured out that the extra gap does nothig

    //val internalWidth: Int = UIItemInventoryItemGrid.getEstimatedW(CELLS_HOR) + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
    //val internalHeight: Int = REQUIRED_MARGIN + UIItemInventoryItemGrid.getEstimatedH(CELLS_VRT) // grad_begin..grad_end..contents..grad_begin..grad_end

    //val itemListHeight: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap

    //val INVENTORY_CELLS_UI_HEIGHT: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * UIItemInventoryItemGrid.listGap
    //val INVENTORY_CELLS_OFFSET_X = 0 + (AppLoader.terrarumAppConfig.screenW - internalWidth) / 2
    //val INVENTORY_CELLS_OFFSET_Y: Int = 107 + (AppLoader.terrarumAppConfig.screenH - internalHeight) / 2

    init {
        handler.allowESCtoClose = true
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val SP = "\u3000 "
    val listControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}$ENDASH${0x2009.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${getKeycapPC(App.getConfigInt("control_key_discard"))} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["CONTEXT_ITEM_MAP"]}$SP" +
            "$gamepadLabelRT ${Lang["MENU_LABEL_MENU"]}$SP" +
            "$gamepadLabelWest ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "$gamepadLabelNorth$gamepadLabelLStick ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$gamepadLabelEast ${Lang["GAME_INVENTORY_DROP"]}"
    val minimapControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_ACTION_MOVE_VERB"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelRStick ${Lang["GAME_ACTION_MOVE_VERB"]}$SP" +
            "$gamepadLabelRT ${Lang["GAME_INVENTORY"]}"
    val gameMenuControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["GAME_INVENTORY"]}"

    private var panelTransitionLocked = false

    fun lockTransition() {
        panelTransitionLocked = true
    }
    fun unlockTransition() {
        panelTransitionLocked = false
    }

    val catBar = UIItemInventoryCatBar(
            this,
            (width - catBarWidth) / 2,
            42 + (App.scr.height - internalHeight) / 2,
            internalWidth,
            catBarWidth,
            true,
            { i -> if (!panelTransitionLocked) requestTransition(i) }
    )


    private val transitionalMinimap = UIInventoryMinimap(this) // PLACEHOLDER
    private val transitionalItemCells = UIInventoryCells(this)
    private val transitionalEscMenu = UIInventoryEscMenu(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
            this,
            (width - internalWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y,
            width,
            App.scr.height,
            1f,
            transitionalMinimap, transitionalItemCells, transitionalEscMenu
    )


    init {
        addUIitem(catBar)
        addUIitem(transitionPanel)

        catBar.selectionChangeListener = { old, new ->
            if (!panelTransitionLocked) {
                rebuildList()
                transitionalItemCells.resetStatusAsCatChanges(old, new)
            }
        }


        // force position of things to UI when opened with "control_key_inventory"
        this.handler.uiTogglerFunctionDefault = {
            if (it.isClosed) {
                transitionPanel.forcePosition(1)
                catBar.setSelectedPanel(1)
                it.setAsOpen()
            }
            else if (it.isOpened)
                setAsClose()
        }

        // allow "control_key_gamemenu" to open this UI
        this.handler.toggleKeyExtra.add { App.getConfigInt("control_key_gamemenu") }
        this.handler.toggleKeyExtraAction.add {
            if (it.isClosed) {
                INGAME.setTooltipMessage(null)
                transitionPanel.forcePosition(2)
                catBar.setSelectedPanel(2)
                it.setAsOpen()
            }
            else if (it.isOpened)
                setAsClose()
        }


        rebuildList()


    }

    internal var offsetX = ((width - internalWidth) / 2).toFloat()
        private set
    internal var offsetY = ((App.scr.height - internalHeight) / 2).toFloat()
        private set

    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)

    override fun updateUI(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }

        if (!panelTransitionLocked) catBar.update(delta)
        transitionPanel.update(delta)
    }

    //private val gradStartCol = Color(0x404040_60)
    //private val gradEndCol   = Color(0x000000_70)
    //private val gradHeight = 48f
    private val shapeRenderer = ShapeRenderer()

    internal var xEnd = (width + internalWidth).div(2).toFloat()
        private set
    internal var yEnd = (App.scr.height + internalHeight).div(2).toFloat()
        private set

    override fun renderUI(batch: SpriteBatch, camera: Camera) {

        drawBackground(batch, shapeRenderer)

        // UI items
        catBar.render(batch, camera)
        transitionPanel.render(batch, camera)

//        if (transitionPanel.currentPosition != 1f) INGAME.setTooltipMessage(null)
    }

    fun rebuildList() {
        transitionalItemCells.rebuildList()
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

    override fun dispose() {
        catBar.dispose()
        transitionPanel.dispose()

    }



    override fun doOpening(delta: Float) {
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        INGAME.setTooltipMessage(null) // required!
        MinimapComposer.revalidateAll()
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((width - internalWidth) / 2).toFloat()
        offsetY = ((App.scr.height - internalHeight) / 2).toFloat()

        xEnd = (width + internalWidth).div(2).toFloat()
        yEnd = (App.scr.height + internalHeight).div(2).toFloat()
    }
}

