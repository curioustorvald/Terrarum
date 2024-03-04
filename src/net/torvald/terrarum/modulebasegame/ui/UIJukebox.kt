package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureJukebox
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.getKeycapPC

/**
 * Created by minjaesong on 2024-01-13.
 */
class UIJukebox : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start",
) {

    lateinit var parent: FixtureJukebox

    companion object {
        const val SLOT_SIZE = 8
    }

    init {
        CommonResourcePool.addToLoadingList("basegame-gui-jukebox_caticons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/jukebox_caticons.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val catbar = UITemplateCatBar(
        this, false,
        CommonResourcePool.getAsTextureRegionPack("basegame-gui-jukebox_caticons"),
        intArrayOf(0, 1),
        emptyList(),
        listOf({ "" }, { "" })
    ).also {
        it.catBar.selectionChangeListener = { old, new ->
            transitionPanel.requestTransition(new)
        }
    }


    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "${App.gamepadLabelStart} ${Lang["GAME_ACTION_CLOSE"]} "


    private val transitionalSonglistPanel = UIJukeboxSonglistPanel(this)
    private val transitionalDiscInventory = UIJukeboxInventory(this)
    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        0, 0, width, height, 0f,
        listOf(transitionalSonglistPanel),
        listOf(transitionalDiscInventory)
    )

    internal val discInventory: ArrayList<ItemID>
        get() = parent.discInventory

    init {
        addUIitem(catbar)
        addUIitem(transitionPanel)
    }

    override fun show() {
        super.show()
        transitionPanel.show()
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun hide() {
        super.hide()
        transitionPanel.hide()
    }

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, 1f)
        uiItems.forEach { it.render(frameDelta, batch, camera) }

        val controlHintXPos = thisOffsetX + 2f
        App.fontGame.draw(batch, controlHelp, controlHintXPos, yEnd - 20)
    }


    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val yEnd = -UIInventoryFull.YPOS_CORRECTION + (App.scr.height + UIInventoryFull.internalHeight).div(2).toFloat()


    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.disablePlayerControl()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resumePlayerControl()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun dispose() {
    }

}