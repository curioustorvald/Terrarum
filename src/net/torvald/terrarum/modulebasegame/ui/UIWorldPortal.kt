package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.YPOS_CORRECTION
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.drawBackground
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC

/**
 * Structure:
 *
 * UIWorldPortal (the container)
 * + UIWorldPortalSearch (left panel)
 * + UIWorldPortalListing (centre panel)
 * + UIWorldPortalCargo (right panel)
 *
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortal : UICanvas(
    toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
    toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
) {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height



    val controlHelpHeight = App.fontGame.lineHeight

    private var panelTransitionLocked = false

    fun lockTransition() {
        panelTransitionLocked = true
    }
    fun unlockTransition() {
        panelTransitionLocked = false
    }
    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)


    val catBar = UIItemWorldPortalTopBar(
        this,
        0,
        42 - YPOS_CORRECTION + (App.scr.height - internalHeight) / 2,
    ) { i -> if (!panelTransitionLocked) requestTransition(i) }


    private val SP = "\u3000 "
    val portalListingControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" +
                    "$SP${App.gamepadLabelLT} ${Lang["GAME_WORLD_SEARCH"]}" +
                    "$SP${App.gamepadLabelRT} ${Lang["GAME_INVENTORY"]}"


    private val transitionalSearch = UIWorldPortalSearch(this)
    private val transitionalListing = UIWorldPortalListing(this)
    private val transitionalCargo = UIWorldPortalCargo(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - internalWidth) / 2,
        INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        1f,
        transitionalSearch, transitionalListing, transitionalCargo
    )

    init {
        addUIitem(catBar)
        addUIitem(transitionPanel)



    }

    internal var xEnd = (width + internalWidth).div(2).toFloat()
        private set
    internal var yEnd = -YPOS_CORRECTION + (App.scr.height + internalHeight).div(2).toFloat()
        private set




    override fun updateUI(delta: Float) {

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        drawBackground(batch, handler.opacity)

        // UI items
        catBar.render(batch, camera)
        transitionPanel.render(batch, camera)
    }

    override fun dispose() {
        catBar.dispose()
    }

    fun resetUI() {

    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        resetUI()
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        resetUI()
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

}

class UIItemWorldPortalTopBar(
    parentUI: UIWorldPortal,
    initialX: Int,
    initialY: Int,
    val panelTransitionReqFun: (Int) -> Unit = {} // for side buttons; for the selection change, override selectionChangeListener
) : UIItem(parentUI, initialX, initialY) {

    override val width = 580
    override val height = 25

    init {
        CommonResourcePool.addToLoadingList("terrarum-basegame-worldportalicons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/worldportal_catbar.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val genericIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_category")
    private val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")
    private val catIconImages = listOf(
        icons.get(0, 0),
        genericIcons.get(16,0),
        icons.get(1, 0),
        genericIcons.get(17,0),
        icons.get(2, 0),
    )
    private val catIconLabels = listOf(
        "CONTEXT_WORLD_SEARCH",
        "",
        "CONTEXT_WORLD_LIST",
        "GAME_INVENTORY",
        "",
    )
    private val buttonGapSize = 120
    private val highlighterYPos = icons.tileH + 4

    var selection = 2

    private val buttons = Array<UIItemImageButton>(5) {
        val xoff = if (it == 1) -32 else if (it == 3) 32 else 0
        UIItemImageButton(
            parentUI,
            catIconImages[it],
            activeBackCol = Color(0),
            backgroundCol = Color(0),
            highlightBackCol = Color(0),
            activeBackBlendMode = BlendMode.NORMAL,
            initialX = (Toolkit.drawWidth - width) / 2 + it * (buttonGapSize + 20) + xoff,
            initialY = posY,
            inactiveCol = if (it % 2 == 0) Color.WHITE else Color(0xffffff7f.toInt()),
            activeCol = if (it % 2 == 0) Toolkit.Theme.COL_MOUSE_UP else Color(0xffffff7f.toInt()),
            highlightable = (it % 2 == 0)
        )
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        // button
        buttons.forEach { it.render(batch, camera) }

        // label
        batch.color = Color.WHITE
        val text = Lang[catIconLabels[selection]]
        App.fontGame.draw(batch, text, buttons[selection].posX + 10 - (App.fontGame.getWidth(text) / 2), posY + highlighterYPos + 4)


        blendNormalStraightAlpha(batch)



    }

    override fun dispose() {
    }


}