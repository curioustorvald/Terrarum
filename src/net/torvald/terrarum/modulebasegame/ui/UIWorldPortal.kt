package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureWorldPortal
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.YPOS_CORRECTION
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.drawBackground
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalHeight
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.internalWidth
import net.torvald.terrarum.serialise.toAscii85
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC
import java.util.UUID

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

    internal lateinit var host: FixtureWorldPortal


    val controlHelpHeight = App.fontGame.lineHeight


    private val SP = "\u3000 "
    val portalListingControlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
                    "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
                    "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]}" +
                    "$SP${App.gamepadLabelLT} ${Lang["GAME_WORLD_SEARCH"]}" +
                    "$SP${App.gamepadLabelRT} ${Lang["GAME_INVENTORY"]}"


    val transitionalSearch = UIWorldPortalSearch(this)
    val transitionalListing = UIWorldPortalListing(this)
//    val transitionalCargo = UIWorldPortalCargo(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        (width - internalWidth) / 2,
        INVENTORY_CELLS_OFFSET_Y(),
        width,
        App.scr.height,
        0f,
         transitionalListing, transitionalSearch
    )

    /**
     * Called by:
     * - "Search" button on UIWorldPortalListing
     * - "Cancel" button on UIWorldPortalSearch
     */
    fun requestTransition(target: Int) = transitionPanel.requestTransition(target)

    init {
        addUIitem(transitionPanel)

    }

    internal var xEnd = (width + internalWidth).div(2).toFloat()
        private set
    internal var yEnd = -YPOS_CORRECTION + (App.scr.height + internalHeight).div(2).toFloat()
        private set




    override fun updateUI(delta: Float) {
        transitionPanel.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        drawBackground(batch, handler.opacity)

        // UI items
        transitionPanel.render(batch, camera)
    }

    private fun addWorldToPlayersDict(uuid: UUID) {
        val uuidstr = uuid.toAscii85()
        INGAME.actorNowPlaying?.let {
            val avList = (it.actorValue.getAsString(AVKey.WORLD_PORTAL_DICT) ?: "").split(',').filter { it.isNotBlank() }.toMutableList()
            if (!avList.contains(uuidstr)) {
                avList.add(uuidstr)
                it.actorValue[AVKey.WORLD_PORTAL_DICT] = avList.joinToString(",")
            }
        }
    }

    override fun show() {
        super.show()
        transitionPanel.forcePosition(0)
        transitionPanel.show()
        INGAME.setTooltipMessage(null)

        // add current world to the player's worldportaldict
        addWorldToPlayersDict(INGAME.world.worldIndex)
    }

    override fun hide() {
        transitionPanel.hide()
    }

    override fun dispose() {
        transitionPanel.dispose()
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        transitionPanel.uis.forEach { it.opacity = FastMath.pow(opacity, 0.5f) }
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        transitionPanel.uis.forEach { it.opacity = FastMath.pow(opacity, 0.5f) }
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        transitionPanel.uis.forEach { it.opacity = FastMath.pow(opacity, 0.5f)  }
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        transitionPanel.uis.forEach { it.opacity = FastMath.pow(opacity, 0.5f) }
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        super.inputStrobed(e)
        transitionPanel.uis.forEach { it.inputStrobed(e) }
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        super.touchDragged(screenX, screenY, pointer)
        transitionPanel.uis.forEach { it.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)
        transitionPanel.uis.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchUp(screenX, screenY, pointer, button)
        transitionPanel.uis.forEach { it.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        super.scrolled(amountX, amountY)
        transitionPanel.uis.forEach { it.scrolled(amountX, amountY) }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        super.keyDown(keycode)
        transitionPanel.uis.forEach { it.keyDown(keycode) }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)
        transitionPanel.uis.forEach { it.keyUp(keycode) }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        super.keyTyped(character)
        transitionPanel.uis.forEach { it.keyTyped(character) }
        return true
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        transitionPanel.uis.forEach { it.resize(width, height) }
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
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/worldportal_catbar.tga"), 30, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val genericIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_category")
    private val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")
    /*private val catIconImages = listOf(
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
        "",
        "GAME_INVENTORY",
    )*/
    private val buttonGapSize = 120
    private val highlighterYPos = icons.tileH + 4

    var selectedPanel = 2; private set

    /** (oldIndex: Int?, newIndex: Int) -> Unit
     * Indices are raw index. That is, not re-arranged. */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    private var transitionFired = false

    /*private val buttons = Array<UIItemImageButton>(5) {
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

    private val workingButtons = arrayOf(0,2,4)*/

    override fun update(delta: Float) {
        super.update(delta)


        /*workingButtons.forEach { buttons[it].update(delta) }

        // transition stuffs
        workingButtons.filter { buttons[it].mousePushed }.firstOrNull()?.let { pushedButton ->
            if (selectedPanel != pushedButton) transitionFired = true
            selectedPanel = pushedButton

            workingButtons.forEach {  i ->
                buttons[i].highlighted = i == pushedButton
            }
        }*/

        if (transitionFired) {
            transitionFired = false
            panelTransitionReqFun(selectedPanel)
        }
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        // button
        /*buttons.forEach { it.render(batch, camera) }

        // label
        batch.color = Color.WHITE
        val text = Lang[catIconLabels[selectedPanel]]
        App.fontGame.draw(batch, text, buttons[selectedPanel].posX + 10 - (App.fontGame.getWidth(text) / 2), posY + highlighterYPos + 4)
        */

        blendNormalStraightAlpha(batch)



    }

    override fun dispose() {
    }


}