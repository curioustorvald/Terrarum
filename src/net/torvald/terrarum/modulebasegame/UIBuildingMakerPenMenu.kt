package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2019-02-16.
 */
class UIBuildingMakerPenMenu(val parent: BuildingMaker): UICanvas() {

    companion object {
        const val SIZE = 400
        const val RADIUS = SIZE / 2.0
        const val RADIUSF = RADIUS.toFloat()

        const val BLOCKS_ROW_RADIUS = 150.0
        const val TOOLS_ROW_RADIUS = 72.0

        const val BLOCK_BACK_SIZE = 72
        const val BLOCK_BACK_RADIUS = BLOCK_BACK_SIZE / 2f

        const val ICON_SIZE = 38
        const val ICON_SIZEH = ICON_SIZE / 2f
        const val ICON_HITBOX_SIZE = 52
        const val ICON_HITBOX_RADIUS = ICON_HITBOX_SIZE / 2f

        const val PALETTE_SIZE = 10
        const val TOOLS_SIZE = 5
    }

    private val backCol = ItemSlotImageFactory.CELLCOLOUR_BLACK
    private val blockCellCol = ItemSlotImageFactory.CELLCOLOUR_WHITE
    /** Centre pos. */
    private val blockCellPos = Array<Vector2>(PALETTE_SIZE) {
        val newvec = Vector2(0.0, 0.0 - BLOCKS_ROW_RADIUS)
        newvec.rotate(Math.PI / 5.0 * it).plus(Vector2(RADIUS, RADIUS))
    }
    /** Centre pos. */
    private val toolsPos = Array<Vector2>(TOOLS_SIZE) {
        val newvec = Vector2(TOOLS_ROW_RADIUS, 0.0)
        newvec.rotate(Math.PI / 2.5 * it).plus(Vector2(RADIUS, RADIUS))
    }

    private val menuIcons = TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/buildingmaker/penmenu_icons.tga"), 38, 38)

    override var width = SIZE
    override var height = SIZE
    override var openCloseTime = 0f//UIQuickslotBar.COMMON_OPEN_CLOSE

    private var mouseVec = Vector2(0.0, 0.0)

    override fun updateUI(delta: Float) {
        mouseVec.x = relativeMouseX.toDouble()
        mouseVec.y = relativeMouseY.toDouble()

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            this.isVisible = false
            parent.tappedOnUI = false
        }

        // primary click
        if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary"))) {
            // close by clicking close button or out-of-boud
            if (mouseVec.distanceSquared(RADIUS, RADIUS) !in ICON_HITBOX_RADIUS.sqr()..RADIUSF.sqr()) {
                this.isVisible = false
                parent.tappedOnUI = true
            }
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // draw back
        batch.color = backCol
        batch.fillCircle(0f, 0f, SIZE.toFloat(), SIZE.toFloat())

        // draw fore
        batch.color = blockCellCol
        for (i in 0 until PALETTE_SIZE) {
            val x = blockCellPos[i].x.roundInt().toFloat() - BLOCK_BACK_RADIUS
            val y = blockCellPos[i].y.roundInt().toFloat() - BLOCK_BACK_RADIUS
            batch.fillCircle(x, y, BLOCK_BACK_SIZE.toFloat(), BLOCK_BACK_SIZE.toFloat())
        }

        // draw close button
        batch.fillCircle(RADIUSF - ICON_HITBOX_RADIUS, RADIUSF - ICON_HITBOX_RADIUS, ICON_HITBOX_SIZE.toFloat(), ICON_HITBOX_SIZE.toFloat())

        // draw icons
        batch.color = Color.WHITE
        batch.draw(menuIcons.get(5, 0), RADIUSF - ICON_SIZEH, RADIUSF - ICON_SIZEH)
        for (i in 0 until TOOLS_SIZE) {
            val x = toolsPos[i].x.roundInt().toFloat() - ICON_SIZEH
            val y = toolsPos[i].y.roundInt().toFloat() - ICON_SIZEH
            batch.draw(menuIcons.get(i, 0), x, y)
        }
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        menuIcons.dispose()
    }
}