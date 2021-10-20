package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2019-02-16.
 */
class UIBuildingMakerPenMenu(val parent: BuildingMaker): UICanvas() {

    companion object {
        const val SIZE = 330
        const val RADIUS = SIZE / 2.0
        const val RADIUSF = RADIUS.toFloat()

        const val BLOCKS_ROW_RADIUS = 120.0
        const val TOOLS_ROW_RADIUS = 56.0

        const val BLOCK_BACK_SIZE = 72
        const val BLOCK_BACK_RADIUS = BLOCK_BACK_SIZE / 2f

        const val ICON_SIZE = 38
        const val ICON_SIZEH = ICON_SIZE / 2f
        const val CLOSE_BUTTON_SIZE = 48
        const val CLOSE_BUTTON_RADIUS = CLOSE_BUTTON_SIZE / 2f

        const val PALETTE_SIZE = 10
        const val TOOLS_SIZE = 5
    }

    private val backCol = ItemSlotImageFactory.CELLCOLOUR_BLACK
    private val blockCellCol = ItemSlotImageFactory.CELLCOLOUR_WHITE
    /** Centre pos. */
    private val blockCellPos = Array<Vector2>(PALETTE_SIZE) {
        Vector2(0.0, 0.0 - BLOCKS_ROW_RADIUS)
                .rotate(Math.PI / 5.0 * it)
                .plus(Vector2(RADIUS, RADIUS))
    }
    private val toolIcons = TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/buildingmaker/penmenu_icons.tga"), 38, 38)
    private val toolButtons = Array<UIItemImageButton>(TOOLS_SIZE) {
        val newvec = Vector2(TOOLS_ROW_RADIUS, 0.0)
                .rotate(Math.PI / 2.5 * it)
                .plus(Vector2(RADIUS - ICON_SIZEH, RADIUS - ICON_SIZEH))

        UIItemImageButton(
                this, toolIcons.get(it, 0),
                backgroundCol = Color(0),
                highlightBackCol = Color(0),
                activeBackCol = Color(0),
                initialX = newvec.x.roundToInt(),
                initialY = newvec.y.roundToInt(),
                width = ICON_SIZE, height = ICON_SIZE,
                highlightable = false
        )
    }
    private val toolButtonsJob = arrayOf(
            { parent.currentPenMode = BuildingMaker.PENMODE_MARQUEE },
            { parent.currentPenMode = BuildingMaker.PENMODE_MARQUEE_ERASE },
            {
                parent.uiPalette.isVisible = true
                parent.uiPalette.setPosition(Gdx.input.x - parent.uiPalette.width / 2, Gdx.input.y - parent.uiPalette.height / 2)
                parent.uiPalette.posX = parent.uiPalette.posX.coerceIn(0, App.scr.width - parent.uiPalette.width)
                parent.uiPalette.posY = parent.uiPalette.posY.coerceIn(0, App.scr.height - parent.uiPalette.height)
            },
            {
                parent.currentPenMode = BuildingMaker.PENMODE_PENCIL_ERASE
                parent.currentPenTarget = BuildingMaker.PENTARGET_TERRAIN
            },
            {
                parent.currentPenMode = BuildingMaker.PENMODE_PENCIL_ERASE
                parent.currentPenTarget = BuildingMaker.PENTARGET_WALL
            }

    )

    override var width = SIZE
    override var height = SIZE
    override var openCloseTime = 0f//UIQuickslotBar.COMMON_OPEN_CLOSE

    private var mouseVec = Vector2(0.0, 0.0)

    private var mouseOnCloseButton = false
    private var mouseOnBlocksSlot: Int? = null

    init {
        // make toolbox work
        toolButtons.forEachIndexed { index, button ->
            uiItems.add(button)

            button.clickOnceListener = { _, _, b ->
                if (b == App.getConfigInt("config_mouseprimary")) {
                    toolButtonsJob[index].invoke()
                    closeGracefully()
                }
            }
        }
    }

    override fun updateUI(delta: Float) {
        mouseVec.x = relativeMouseX.toDouble()
        mouseVec.y = relativeMouseY.toDouble()

        toolButtons.forEach { it.update(delta) }

        // determine if cursor is above shits
        mouseOnCloseButton = (mouseVec.distanceSquared(RADIUS, RADIUS) <= CLOSE_BUTTON_RADIUS.sqr())
        // --> blocks slot
        for (i in 0 until PALETTE_SIZE) {
            val posVec = blockCellPos[i]
            if (mouseVec.distanceSquared(posVec) <= BLOCK_BACK_RADIUS.sqr()) {
                mouseOnBlocksSlot = i
                break
            }
            mouseOnBlocksSlot = null
            // actually selecting the slot is handled by renderUI()
        }


        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            this.isVisible = false
            parent.tappedOnUI = false
        }

        // primary click
        if (Terrarum.mouseDown) {
            // close by clicking close button or out-of-boud
            if (mouseVec.distanceSquared(RADIUS, RADIUS) !in CLOSE_BUTTON_RADIUS.sqr()..RADIUSF.sqr()) {
                closeGracefully()
            }
        }
    }

    private fun closeGracefully() {
        this.isVisible = false
        parent.tappedOnUI = true
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // draw back
        batch.color = backCol
        batch.fillCircle(0f, 0f, SIZE.toFloat(), SIZE.toFloat())

        // draw blocks slot
        batch.color = blockCellCol
        val slotConfig = arrayOf(
                Block.GLASS_CRUDE,
                Block.PLANK_NORMAL,
                Block.PLANK_BIRCH,
                Block.STONE_QUARRIED,
                Block.STONE_BRICKS,

                Block.STONE_TILE_WHITE,
                Block.TORCH,
                "wall@" + Block.PLANK_NORMAL,
                "wall@" + Block.PLANK_BIRCH,
                "wall@" + Block.GLASS_CRUDE
        )//AppLoader.getConfigStringArray("buildingmakerfavs")
        for (i in 0 until PALETTE_SIZE) {
            val x = blockCellPos[i].x.roundToInt().toFloat()
            val y = blockCellPos[i].y.roundToInt().toFloat()
            batch.color = blockCellCol
            repeat((i == mouseOnBlocksSlot).toInt() + 1) { batch.fillCircle(x - BLOCK_BACK_RADIUS, y - BLOCK_BACK_RADIUS, BLOCK_BACK_SIZE.toFloat(), BLOCK_BACK_SIZE.toFloat()) }
            batch.color = Color.WHITE
            batch.draw(ItemCodex.getItemImage(slotConfig[i]), x - 16, y - 16, 32f, 32f)

            // update as well while looping
            if (i == mouseOnBlocksSlot && Terrarum.mouseDown) {
                parent.setPencilColour(slotConfig[i])
                closeGracefully()
            }
        }

        // draw close button
        batch.color = blockCellCol
        repeat(mouseOnCloseButton.toInt() + 1) { batch.fillCircle(RADIUSF - CLOSE_BUTTON_RADIUS, RADIUSF - CLOSE_BUTTON_RADIUS, CLOSE_BUTTON_SIZE.toFloat(), CLOSE_BUTTON_SIZE.toFloat()) }

        batch.color = if (mouseOnCloseButton) toolButtons[0].activeCol else toolButtons[0].inactiveCol
        batch.draw(toolIcons.get(5, 0), RADIUSF - ICON_SIZEH, RADIUSF - ICON_SIZEH)

        // draw icons
        toolButtons.forEach {
            it.render(batch, camera)
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
        toolIcons.dispose()
    }

}