package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-01-13.
 */
class UIJukebox : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start",
) {

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
    )

    private val transitionalSonglistPanel = UIJukeboxSonglistPanel(this)
    private val transitionalDiscInventory = UIJukeboxInventory(this)

    private val transitionPanel = UIItemHorizontalFadeSlide(
        this,
        0, 0, width, height, 0f,
        listOf(transitionalSonglistPanel),
        listOf(transitionalDiscInventory)
    )

    init {
        addUIitem(catbar)
        addUIitem(transitionPanel)
    }

    private var openingClickLatched = false

    override fun show() {
        openingClickLatched = Terrarum.mouseDown
        transitionPanel.show()
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    override fun hide() {
        transitionPanel.hide()
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        UIInventoryFull.drawBackground(batch, 1f)
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!openingClickLatched) {
            return super.touchDown(screenX, screenY, pointer, button)
        }
        return false
    }

    override fun dispose() {
    }

}