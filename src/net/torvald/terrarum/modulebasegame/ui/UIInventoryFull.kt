package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.ENDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.*
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryDynamicList.Companion.CAT_ALL
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        var actor: Pocketed,

        toggleKeyLiteral: Int? = null, toggleButtonLiteral: Int? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH
    override var openCloseTime: Second = 0.0f

    val REQUIRED_MARGIN: Int = 138 // hard-coded value. Don't know the details. Range: [91-146]. I chose MAX-8 because cell gap is 8

    val CELLS_HOR = 10
    val CELLS_VRT: Int; get() = (AppLoader.screenH - REQUIRED_MARGIN - 134 + UIItemInventoryDynamicList.listGap) / // 134 is another magic number
                            (UIItemInventoryElemSimple.height + UIItemInventoryDynamicList.listGap)

    private val itemListToEquipViewGap = UIItemInventoryDynamicList.listGap // used to be 24; figured out that the extra gap does nothig

    val internalWidth: Int = UIItemInventoryDynamicList.getEstimatedW(CELLS_HOR) + UIItemInventoryEquippedView.WIDTH + itemListToEquipViewGap
    val internalHeight: Int = REQUIRED_MARGIN + UIItemInventoryDynamicList.getEstimatedH(CELLS_VRT) // grad_begin..grad_end..contents..grad_begin..grad_end

    val itemListHeight: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * net.torvald.terrarum.modulebasegame.ui.UIItemInventoryDynamicList.Companion.listGap

    val INVENTORY_CELLS_UI_HEIGHT: Int = CELLS_VRT * UIItemInventoryElemSimple.height + (CELLS_VRT - 1) * UIItemInventoryDynamicList.listGap
    val INVENTORY_CELLS_OFFSET_X = 0 + (AppLoader.screenW - internalWidth) / 2
    val INVENTORY_CELLS_OFFSET_Y: Int = 107 + (AppLoader.screenH - internalHeight) / 2

    init {
        handler.allowESCtoClose = true
        CommonResourcePool.addToLoadingList("inventory_caticons") {
            TextureRegionPack("./assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    internal val catIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_caticons")
    internal val catArrangement: IntArray = intArrayOf(9,6,7,1,0,2,3,4,5,8)
    internal val catIconsMeaning = listOf( // sortedBy: catArrangement
            arrayOf(GameItem.Category.WEAPON),
            arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
            arrayOf(GameItem.Category.ARMOUR),
            arrayOf(GameItem.Category.GENERIC),
            arrayOf(GameItem.Category.POTION),
            arrayOf(GameItem.Category.MAGIC),
            arrayOf(GameItem.Category.BLOCK),
            arrayOf(GameItem.Category.WALL),
            arrayOf(GameItem.Category.MISC),
            arrayOf(CAT_ALL)
    )


    private val SP = "${0x3000.toChar()} "
    val listControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}$ENDASH${0x2009.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["CONTEXT_ITEM_MAP"]}$SP" +
            "$gamepadLabelRT ${Lang["MENU_LABEL_MENU"]}$SP" +
            "$gamepadLabelWest ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "$gamepadLabelNorth$gamepadLabelLStick ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$gamepadLabelEast ${Lang["GAME_INVENTORY_DROP"]}"
    val minimapControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_ACTION_MOVE_VERB"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelRStick ${Lang["GAME_ACTION_MOVE_VERB"]}$SP" +
            "$gamepadLabelRT ${Lang["GAME_INVENTORY"]}"
    val gameMenuControlHelp: String
        get() = if (AppLoader.environment == RunningEnvironment.PC)
            "${0xe031.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "$gamepadLabelLT ${Lang["GAME_INVENTORY"]}"
    val controlHelpHeight = AppLoader.fontGame.lineHeight

    val catBarWidth = 330
    val categoryBar = UIItemInventoryCatBar(
            this,
            (AppLoader.screenW - catBarWidth) / 2,
            42 + (AppLoader.screenH - internalHeight) / 2,
            catBarWidth
    )


    private val transitionalMinimap = UIInventoryMinimap(this) // PLACEHOLDER
    private val transitionalItemCells = UIInventoryCells(this)
    private val transitionalEscMenu = UIInventoryEscMenu(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
            this,
            (AppLoader.screenW - internalWidth) / 2,
            INVENTORY_CELLS_OFFSET_Y,
            AppLoader.screenW,
            AppLoader.screenH,
            1f,
            transitionalMinimap, transitionalItemCells, transitionalEscMenu
    )


    init {
        addUIitem(categoryBar)
        addUIitem(transitionPanel)

        categoryBar.selectionChangeListener = { old, new  ->
            rebuildList()
            transitionalItemCells.resetStatusAsCatChanges(old, new)
        }


        rebuildList()


    }

    internal var offsetX = ((AppLoader.screenW - internalWidth)  / 2).toFloat()
        private set
    internal var offsetY = ((AppLoader.screenH - internalHeight) / 2).toFloat()
        private set

    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)

    override fun updateUI(delta: Float) {
        if (handler.openFired) {
            rebuildList()
        }

        categoryBar.update(delta)
        transitionPanel.update(delta)
    }

    private val gradStartCol = Color(0x404040_60)
    private val gradEndCol   = Color(0x000000_70)
    private val shapeRenderer = ShapeRenderer()
    private val gradHeight = 48f

    internal var xEnd = (AppLoader.screenW + internalWidth).div(2).toFloat()
        private set
    internal var yEnd = (AppLoader.screenH + internalHeight).div(2).toFloat()
        private set

    override fun renderUI(batch: SpriteBatch, camera: Camera) {


        // background fill
        batch.end()
        gdxSetBlendNormal()


        val gradTopStart = (AppLoader.screenH - internalHeight).div(2).toFloat()
        val gradBottomEnd = AppLoader.screenH - gradTopStart

        shapeRenderer.inUse {
            shapeRenderer.rect(0f, gradTopStart, AppLoader.screenWf, gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)
            shapeRenderer.rect(0f, gradBottomEnd, AppLoader.screenWf, -gradHeight, gradStartCol, gradStartCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, gradTopStart + gradHeight, AppLoader.screenWf, internalHeight - (2 * gradHeight), gradEndCol, gradEndCol, gradEndCol, gradEndCol)

            shapeRenderer.rect(0f, 0f, AppLoader.screenWf, gradTopStart, gradStartCol, gradStartCol, gradStartCol, gradStartCol)
            shapeRenderer.rect(0f, AppLoader.screenHf, AppLoader.screenWf, -(AppLoader.screenHf - gradBottomEnd), gradStartCol, gradStartCol, gradStartCol, gradStartCol)
        }


        batch.begin()

        // UI items
        categoryBar.render(batch, camera)
        transitionPanel.render(batch, camera)
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
        categoryBar.dispose()
        transitionPanel.dispose()

    }



    override fun doOpening(delta: Float) {
        Terrarum.ingame?.paused = true
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        Terrarum.ingame?.paused = false
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null) // required!

        MinimapComposer.revalidateAll()
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        offsetX = ((AppLoader.screenW - internalWidth)   / 2).toFloat()
        offsetY = ((AppLoader.screenH - internalHeight) / 2).toFloat()

        xEnd = (AppLoader.screenW + internalWidth).div(2).toFloat()
        yEnd = (AppLoader.screenH + internalHeight).div(2).toFloat()
    }

    companion object {
        const val INVEN_DEBUG_MODE = false
    }
}

