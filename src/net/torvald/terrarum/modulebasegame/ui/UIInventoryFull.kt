package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.audio.dsp.Lowpass
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.*

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        toggleKeyLiteral: String? = "control_key_inventory", toggleButtonLiteral: String? = "control_gamepad_start",
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    val actor: ActorHumanoid
        get() = INGAME.actorNowPlaying!!

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    companion object {
//        private var shapeRenderer: ShapeRenderer? = null

        private val backDropsLoaded = Array<Boolean>(16) { false }
        private val backdrop01: TextureRegionPack
            get() {
                if (!backDropsLoaded[0]) {
                    CommonResourcePool.addToLoadingList("basegame.uibackdrop01") {
                        TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/backdrop01.tga"), 2, 140)
                    }
                    CommonResourcePool.loadAll()
                    backDropsLoaded[0] = true
                }
                return CommonResourcePool.getAsTextureRegionPack("basegame.uibackdrop01")
            }

        val CELL_COL = Toolkit.Theme.COL_CELL_FILL

        const val YPOS_CORRECTION = 16 // another hard-coding. X_X

        const val REQUIRED_MARGIN: Int = 138 // hard-coded value. Don't know the details. Range: [91-146]. I chose MAX-8 because cell gap is 8
        const val CELLS_HOR = 12

        fun getCellCountVertically(cellHeight: Int, gapHeight: Int = UIItemInventoryItemGrid.listGap): Int {
            return (App.scr.height - REQUIRED_MARGIN - 134 + gapHeight) / // 134 is another magic number
                    (cellHeight + gapHeight)
        }
        val CELLS_VRT: Int; get() = getCellCountVertically(UIItemInventoryElemSimple.height, UIItemInventoryItemGrid.listGap)

        const val itemListToEquipViewGap = UIItemInventoryItemGrid.listGap // used to be 24; figured out that the extra gap does nothig

        val internalWidth: Int = UIItemInventoryItemGrid.getEstimatedW(CELLS_HOR) + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
        val internalHeight: Int = REQUIRED_MARGIN + UIItemInventoryItemGrid.getEstimatedH(CELLS_VRT) // grad_begin..grad_end..contents..grad_begin..grad_end

        val itemListHeight: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap

        val INVENTORY_CELLS_UI_HEIGHT: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * UIItemInventoryItemGrid.listGap
        val INVENTORY_CELLS_OFFSET_X = { 0 + (Toolkit.drawWidth - internalWidth) / 2 }
        val INVENTORY_CELLS_OFFSET_Y = { -YPOS_CORRECTION + 107 + (App.scr.height - internalHeight) / 2 }

        fun getWidthOfCells(count: Int, cellWidth: Int = UIItemInventoryElemWide.height, gapWidth: Int = UIItemInventoryItemGrid.listGap) =
            (cellWidth * count) + (gapWidth * (count - 1))

        val catBarWidth = 330

        val gradStartCol = Color(0x404040_60)
        val gradEndCol   = Color(0x000000_70)
        val gradHeight = 48f

        val controlHelpHeight = App.fontGame.lineHeight

        private val gsta = Color(gradStartCol)
        private val gend = Color(gradEndCol)


        private val drawBackgroundColourBuffer = Color(1f,1f,1f,1f)
        fun drawBackground(batch: SpriteBatch, opacity: Float) {
            gdxBlendNormalStraightAlpha()
            /*batch.end()

            if (shapeRenderer == null) {
                shapeRenderer = App.makeShapeRenderer()
                App.disposables.add(shapeRenderer)
            }

            val magn = App.scr.magn

            val gradTopStart0 = -YPOS_CORRECTION + (App.scr.height - internalHeight).div(2).toFloat()
            val gradTopStart = gradTopStart0 * magn
            val gradBottomEnd = (App.scr.height - gradTopStart0) * magn
            val gradHeightFlat = (2*YPOS_CORRECTION + internalHeight - (2 * gradHeight)) * magn
            val gradHeight = gradHeight * magn
            val w = App.scr.wf * magn
            val h = App.scr.hf * magn

            gsta.a = 0.375f * opacity
            gend.a = 0.4375f * opacity
            shapeRenderer!!.inUse {
                // shaperender starts at bottom-left!
                it.rect(0f, gradTopStart, w, gradHeight, gsta, gsta, gend, gend)
                it.rect(0f, gradBottomEnd, w, -gradHeight, gsta, gsta, gend, gend)

                it.rect(0f, gradTopStart + gradHeight, w, gradHeightFlat, gend, gend, gend, gend)

                it.rect(0f, 0f, w, gradTopStart, gsta, gsta, gsta, gsta)
                it.rect(0f, h, w, -(h - gradBottomEnd), gsta, gsta, gsta, gsta)
            }

            batch.begin()*/
//            drawBackgroundColourBuffer.a = opacity
            batch.color = drawBackgroundColourBuffer
            val w = App.scr.wf
            val h = App.scr.hf
            val gradTopStart = (-YPOS_CORRECTION + (App.scr.height - internalHeight).div(2).toFloat()) * App.scr.magn

            val hTop = gradTopStart
            val hTopRem = hTop - 64f

            val hMid = h - 2 * (hTopRem + 140f)
            batch.draw(backdrop01.get(0, 0), 0f, 0f, w, hTopRem)
            batch.draw(backdrop01.get(0, 1), 0f, hTopRem, w, 140f)
            batch.draw(backdrop01.get(0, 2), 0f, hTopRem + 140f, w, hMid)
            batch.draw(backdrop01.get(0, 3), 0f, hTopRem + 140f + hMid, w, 140f)
            batch.draw(backdrop01.get(0, 4), 0f, hTopRem + 280f + hMid, w, hTopRem)

        }

        internal val xEnd: Float
            get() = (Toolkit.drawWidth + internalWidth).div(2).toFloat()
        internal val yEnd: Float
            get() = -YPOS_CORRECTION + (App.scr.height + internalHeight).div(2).toFloat()
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
    }

    private val SP = "\u3000 "
    val listControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$KEYCAP_LEFT_MOUSE ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "$KEYCAP_1$ENDASH\u2009$KEYCAP_0 ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${getKeycapPC(ControlPresets.getKey("control_key_discard"))} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["CONTEXT_ITEM_MAP"]}$SP" +
            "$gamepadLabelRT ${Lang["MENU_LABEL_MENU"]}$SP" +
            "$gamepadLabelWest ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "$gamepadLabelNorth$gamepadLabelLStick ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$gamepadLabelEast ${Lang["GAME_INVENTORY_DROP"]}"
    val minimapControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$KEYCAP_LEFT_MOUSE$KEYCAP_MOVE ${Lang["GAME_ACTION_MOVE_VERB"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelRStick ${Lang["GAME_ACTION_MOVE_VERB"]}$SP" +
            "$gamepadLabelRT ${Lang["GAME_INVENTORY"]}"
    val gameMenuControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
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
    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)

    val catBar = UIItemCatBar(
        this,
        (width - catBarWidth) / 2,
        42 - YPOS_CORRECTION + (scr.height - internalHeight) / 2,
        internalWidth,
        catBarWidth,
        true,

        catIcons = CommonResourcePool.getAsTextureRegionPack("inventory_category"),
        catArrangement = intArrayOf(9,6,7,1,0,2,1_011,3,4,5,8), // icon order
        catIconsMeaning = listOf( // sortedBy: catArrangement
            arrayOf(UIItemCatBar.CAT_ALL),
            arrayOf(GameItem.Category.BLOCK),
            arrayOf(GameItem.Category.WALL),
            arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
            arrayOf(GameItem.Category.WEAPON),
            arrayOf(GameItem.Category.ARMOUR),
            arrayOf(GameItem.Category.FIXTURE),
            arrayOf(GameItem.Category.GENERIC),
            arrayOf(GameItem.Category.POTION),
            arrayOf(GameItem.Category.MAGIC),
            arrayOf(GameItem.Category.MISC),
        ),
        catIconsLabels = listOf(
            { Lang["MENU_LABEL_ALL"] },
            { Lang["GAME_INVENTORY_BLOCKS"] },
            { Lang["GAME_INVENTORY_WALLS"] },
            { Lang["CONTEXT_ITEM_TOOL_PLURAL"] },
            { Lang["GAME_INVENTORY_WEAPONS"] },
            { Lang["CONTEXT_ITEM_ARMOR"] },
            { Lang["CONTEXT_ITEM_FIXTURES"] },
            { Lang["GAME_INVENTORY_INGREDIENTS"] },
            { Lang["GAME_INVENTORY_POTIONS"] },
            { Lang["CONTEXT_ITEM_MAGIC"] },
            { Lang["GAME_GENRE_MISC"] },
        ),

    ) { i -> if (!panelTransitionLocked) requestTransition(i) }


    //    private val transitionalMinimap = UIInventoryMinimap(this)
    internal val transitionalCraftingUI = UICrafting(this)
    internal val transitionalTechTreeViewUI = UITechView(this)
    internal val transitionalItemCells = UIInventoryCells(this)
    internal val transitionalEscMenu = UIInventoryEscMenu(this)
    val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - internalWidth) / 2,
        INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        1f,
        listOf(transitionalCraftingUI, transitionalTechTreeViewUI),
        listOf(transitionalItemCells),
        listOf(transitionalEscMenu)
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

        // allow "control_key_gamemenu" to open this UI and bring system menu immediately
        this.handler.toggleKeyExtra.add("control_key_gamemenu" )
        this.handler.toggleKeyExtraAction.add {
            if (it.isClosed)
                openGamemenu(it)
            else if (it.isOpened)
                setAsClose()
        }

        // allow "control_key_crafting" to open this UI and bring system menu immediately
        this.handler.toggleKeyExtra.add("control_key_crafting")
        this.handler.toggleKeyExtraAction.add {
            if (it.isClosed)
                openCrafting(it)
            else if (it.isOpened)
                setAsClose()
        }


        rebuildList()
    }

    fun openGamemenu(it: UIHandler) {
        INGAME.setTooltipMessage(null)
        transitionPanel.forcePosition(2)
        catBar.setSelectedPanel(2)
        it.setAsOpen()
    }

    fun openCrafting(it: UIHandler) {
        INGAME.setTooltipMessage(null)
        transitionPanel.forcePosition(0)
        catBar.setSelectedPanel(0)
        transitionalCraftingUI.resetUI()
        transitionalTechTreeViewUI.resetUI()
        it.setAsOpen()
    }

    override fun show() {
        transitionPanel.show()
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun hide() {
        transitionPanel.hide()
    }

    internal var offsetX = ((width - internalWidth) / 2).toFloat()
        private set
    internal var offsetY = ((App.scr.height - internalHeight) / 2).toFloat()
        private set

    override fun updateImpl(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }

        if (!panelTransitionLocked) catBar.update(delta)
        transitionPanel.update(delta)
    }

    //private val gradStartCol = Color(0x404040_60)
    //private val gradEndCol   = Color(0x000000_70)
    //private val gradHeight = 48f


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {

        drawBackground(batch, 1f)

        // UI items
        catBar.render(frameDelta, batch, camera)
        transitionPanel.render(frameDelta, batch, camera)

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
        super.doOpening(delta)
        transitionPanel.allUIs.forEach { it.opacity = FastMath.pow(opacity, 0.5f) }
        INGAME.pause()
        INGAME.setTooltipMessage(null)

        App.audioMixer.requestLowpassIn(0.25)
        App.audioMixer.requestFadeOut(App.audioMixer.fadeBus, 0.25, decibelsToFullscale(-3.0))
    }

    private var shouldIFadeIn: Boolean? = null

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        transitionPanel.allUIs.forEach { it.opacity = FastMath.pow(opacity, 0.5f) }
        INGAME.resume()
        INGAME.setTooltipMessage(null)

        if (shouldIFadeIn == null) {
            shouldIFadeIn = (App.audioMixer.fadeBus.getFilter<Lowpass>().cutoff < SAMPLING_RATE / 2)
        }

        if (shouldIFadeIn == true) {
            App.audioMixer.requestLowpassOut(0.25)
            App.audioMixer.requestFadeIn(App.audioMixer.fadeBus, 0.25, 1.0)
        }
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        transitionPanel.allUIs.forEach { it.opacity = 1f  }

        shouldIFadeIn = null
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        transitionPanel.allUIs.forEach { it.opacity = 0f }
        INGAME.setTooltipMessage(null) // required!
//        MinimapComposer.revalidateAll()

        tooltipShowing.clear()

//        printdbg(this, "Clearing out tooltipShowing")

        shouldIFadeIn = null
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((width - internalWidth) / 2).toFloat()
        offsetY = ((App.scr.height - internalHeight) / 2).toFloat()
    }
}

