package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.blockstats.MinimapComposer.MINIMAP_TILE_HEIGHT
import net.torvald.terrarum.blockstats.MinimapComposer.MINIMAP_TILE_WIDTH
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_UI_HEIGHT
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import kotlin.math.roundToInt

class UIInventoryMinimap(val full: UIInventoryFull) : UICanvas() {

    companion object {
        const val MINIMAP_WIDTH = 720f
        const val MINIMAP_HEIGHT = 480f
        const val MINIMAP_ZOOM_MIN = 1f
        const val MINIMAP_ZOOM_MAX = 8f

        val MINIMAP_SKYCOL = Color(0x88bbddff.toInt())
    }

    private val debugvals = true

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height
    override var openCloseTime = 0.0f

    private var minimapZoom = 1f
    private var minimapPanX: Float = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.toFloat() ?: (INGAME.world.width / 2f)
    private var minimapPanY: Float = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.toFloat() ?: (INGAME.world.height / 2f)

    private var minimapTranslateX: Float = 0f
    private var minimapTranslateY: Float = 0f

    private val minimapFBO = FrameBuffer(Pixmap.Format.RGBA8888, MINIMAP_WIDTH.toInt(), MINIMAP_HEIGHT.toInt(), false)
    private val minimapCamera = OrthographicCamera(MINIMAP_WIDTH, MINIMAP_HEIGHT)

    private var minimapRerenderTimer = 0f
    private var minimapRerenderInterval = 5f // seconds

    private var dragStatus = 0

    private var renderTextures = Array(MinimapComposer.pixmaps.size) { Texture(16, 16, Pixmap.Format.RGBA8888) }

    override fun updateUI(delta: Float) {
        MinimapComposer.setWorld(INGAME.world)
//        MinimapComposer.update()
        minimapRerenderTimer += Gdx.graphics.deltaTime
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)
        val cellOffY = INVENTORY_CELLS_OFFSET_Y()

        var mdx = 0f
        var mdy = 0f

        // update map panning
        // if left click is down and cursor is in the map area
        if (Terrarum.mouseDown &&
            Terrarum.mouseScreenY in cellOffY..cellOffY + INVENTORY_CELLS_UI_HEIGHT) {
            mdx = Terrarum.mouseDeltaX * 3f / minimapZoom
            mdy = Terrarum.mouseDeltaY * 3f / minimapZoom

            val ymin = 0//-MINIMAP_HEIGHT / 2
            val ymax = INGAME.world.height// - MINIMAP_HEIGHT
            val my = minimapPanY + mdy

            if (my < ymin) mdy += my - ymin
            else if (my > ymax) mdy += my - ymax

            minimapPanX -= mdx
            minimapPanY -= mdy
            minimapTranslateX += mdx
            minimapTranslateY += mdy

            dragStatus = 1
        }



        if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
            minimapZoom *= (1f / 1.02f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
            minimapZoom *= 1.02f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) {
            minimapZoom = 1f
            minimapPanX = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.toFloat() ?: (INGAME.world.width / 2f)
            minimapPanY = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.toFloat() ?: (INGAME.world.height / 2f)
            dragStatus = 1
        }


        try {
            //minimapPanX = minimapPanX.coerceIn(-(MinimapComposer.totalWidth * minimapZoom) + MINIMAP_WIDTH, 0f) // un-comment this line to constain the panning over x-axis
        } catch (e: IllegalArgumentException) { }
        try {
            //minimapPanY = minimapPanY.coerceIn(-(MinimapComposer.totalHeight * minimapZoom) + MINIMAP_HEIGHT, 0f)
        } catch (e: IllegalArgumentException) { }
        minimapZoom = minimapZoom.coerceIn(MINIMAP_ZOOM_MIN, MINIMAP_ZOOM_MAX)


        // make image to roll over for x-axis. This is for the ROUNDWORLD implementation, feel free to remove below.


        // render minimap
        batch.end()

        if (!Terrarum.mouseDown && (dragStatus == 1 || minimapRerenderTimer >= minimapRerenderInterval)) {
            dragStatus = 0
            minimapRerenderTimer = 0f

            MinimapComposer.queueRender(minimapPanX.roundToInt(), minimapPanY.roundToInt())

            minimapTranslateX = 0f
            minimapTranslateY = 0f
        }


        minimapFBO.inActionF(minimapCamera, batch) {
            gdxClearAndSetBlend(MINIMAP_SKYCOL)

            batch.inUse {

                // [  1  0 0 ]   [  s   0  0 ]   [  s  0 0 ]
                // [  0  1 0 ] x [  0   s  0 ] = [  0  s 0 ]
                // [ px py 1 ]   [ w/2 h/2 1 ]   [ tx ty 1 ]
                //
                // https://www.wolframalpha.com/input/?i=%7B%7B1,0,0%7D,%7B0,1,0%7D,%7Bp_x,p_y,1%7D%7D+*+%7B%7Bs,0,0%7D,%7B0,s,0%7D,%7Bw%2F2,h%2F2,1%7D%7D

                // sky background
                batch.color = Color.WHITE

                MinimapComposer.pixmaps.forEachIndexed { index, pixmap ->
                    renderTextures[MinimapComposer.pixmaps.lastIndex - index].dispose()
                    renderTextures[MinimapComposer.pixmaps.lastIndex - index] = Texture(pixmap)

                    val ix = index % MinimapComposer.SQUARE_SIZE; val iy = index / MinimapComposer.SQUARE_SIZE
                    val ox = (ix - (MinimapComposer.SQUARE_SIZE / 2) + 0.5f) * MINIMAP_TILE_WIDTH
                    val oy = (iy - (MinimapComposer.SQUARE_SIZE / 2) + 0.5f) * MINIMAP_TILE_HEIGHT
                    val tx = (minimapTranslateX - ox) * minimapZoom + 0.5f * MINIMAP_WIDTH
                    val ty = (minimapTranslateY - oy) * minimapZoom + 0.5f * MINIMAP_HEIGHT

                    batch.draw(renderTextures[index], tx, ty, MINIMAP_TILE_WIDTH * minimapZoom, MINIMAP_TILE_HEIGHT * minimapZoom)
                }


                ((INGAME.actorContainerInactive + INGAME.actorContainerActive).filter { it is IngamePlayer } as List<IngamePlayer>).forEach {
                    it.getSpriteHead()?.let { t ->
                        val ox = 0.5f * MINIMAP_TILE_WIDTH
                        val oy = 0.5f * MINIMAP_TILE_HEIGHT
                        val tx = (minimapTranslateX - ox) * minimapZoom + 0.5f * MINIMAP_WIDTH
                        val ty = (minimapTranslateY - oy) * minimapZoom + 0.5f * MINIMAP_HEIGHT
                        val worldPos = it.intTilewiseHitbox
                        val cw = it.scale.toFloat() * t.regionWidth
                        val ch = it.scale.toFloat() * t.regionHeight
                        val cx = worldPos.centeredX.toFloat() + cw / 2
                        val cy = worldPos.startY.toFloat() + (((it.baseHitboxH - 6) * it.scale) / TILE_SIZE).toFloat()// - (ch / 2)
                        val dx = cx - minimapPanX
                        val dy = cy - minimapPanY

                        val x = tx + dx
                        val y = ty + dy

                        printdbg(this, "map pan: ($minimapPanX,$minimapPanY);draw pos: ($x,$y)")

                        if (it.sprite?.flipHorizontal == false)
                            batch.draw(t, x, y, cw, ch)
                        else
                            batch.draw(t, x + cw, y, -cw, ch)
                    }
                }
            }
        }
        batch.begin()

        val minimapDrawX = (width - MINIMAP_WIDTH) / 2
        val minimapDrawY = (height - cellOffY - App.scr.tvSafeGraphicsHeight - MINIMAP_HEIGHT - 72) / 2 + cellOffY * 1f

        if (debugvals) {
            App.fontSmallNumbers.draw(batch, "$minimapPanX, $minimapPanY; $minimapTranslateX, $minimapTranslateY; x$minimapZoom", minimapDrawX, minimapDrawY - 16f)
        }

        batch.color = Color.WHITE
        batch.projectionMatrix = camera.combined

        // border
        Toolkit.drawBoxBorder(batch, minimapDrawX.toInt() - 1, minimapDrawY.toInt() - 1, MINIMAP_WIDTH.toInt() + 2, MINIMAP_HEIGHT.toInt() + 2)
        // control hints
        App.fontGame.draw(batch, full.minimapControlHelp, minimapDrawX, minimapDrawY + MINIMAP_HEIGHT + 12)
        // the minimap
        batch.draw(minimapFBO.colorBufferTexture, minimapDrawX, minimapDrawY)
    }

    override fun show() {
        INGAME.setTooltipMessage(null)
    }

    override fun doOpening(delta: Float) {}

    override fun doClosing(delta: Float) {}

    override fun endOpening(delta: Float) {
        minimapPanX = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.toFloat() ?: (INGAME.world.width / 2f)
        minimapPanY = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.toFloat() ?: (INGAME.world.height / 2f)

        minimapRerenderInterval = 0.25f
    }

    override fun endClosing(delta: Float) {}

    override fun dispose() {
        minimapFBO.dispose()
        renderTextures.forEach { try { it.dispose() } catch (e: GdxRuntimeException) {} }
    }
}