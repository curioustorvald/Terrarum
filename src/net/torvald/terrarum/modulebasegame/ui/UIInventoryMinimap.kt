package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.blockstats.MinimapComposer.MINIMAP_TILE_HEIGHT
import net.torvald.terrarum.blockstats.MinimapComposer.MINIMAP_TILE_WIDTH
import net.torvald.terrarum.gameworld.fmod
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

        const val MINIMAP_HALFW = MINIMAP_WIDTH / 2f
        const val MINIMAP_HALFH = MINIMAP_HEIGHT / 2f

        val MINIMAP_SKYCOL = Color(0x88bbddff.toInt())
    }

//    private val debugvals = true

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height
    override var openCloseTime = 0.0f

    private var minimapZoom = 1f
    private var minimapPanX: Float = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.toFloat() ?: (INGAME.world.width / 2f)
    private var minimapPanY: Float = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.toFloat() ?: (INGAME.world.height / 2f)

    private var minimapDrawOffX: Float = 0f
    private var minimapDrawOffY: Float = 0f

    private val minimapFBO = FrameBuffer(Pixmap.Format.RGBA8888, MINIMAP_WIDTH.toInt(), MINIMAP_HEIGHT.toInt(), false)
    private val minimapCamera = OrthographicCamera(MINIMAP_WIDTH, MINIMAP_HEIGHT)

    private var minimapRerenderTimer = 0f
    private var minimapRerenderInterval = 5f // seconds

    private var dragStatus = 0

    private var renderTextures = Array(MinimapComposer.pixmaps.size) { Texture(16, 16, Pixmap.Format.RGBA8888) }

    private var oldPanX = minimapPanX
    private var oldPanY = minimapPanY

    override fun updateUI(delta: Float) {
        MinimapComposer.setWorld(INGAME.world)
//        MinimapComposer.update()
        minimapRerenderTimer += Gdx.graphics.deltaTime
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormalStraightAlpha(batch)
        val cellOffY = INVENTORY_CELLS_OFFSET_Y()
        val worldWidth = INGAME.world.width

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
            minimapDrawOffX += mdx
            minimapDrawOffY += mdy

            minimapPanX = minimapPanX fmod worldWidth.toFloat()

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
            minimapPanX = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.roundToInt()?.toFloat() ?: (INGAME.world.width / 2f)
            minimapPanY = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.roundToInt()?.toFloat() ?: (INGAME.world.height / 2f)
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

            minimapDrawOffX = 0f
            minimapDrawOffY = 0f
            oldPanX = minimapPanX
            oldPanY = minimapPanY
        }


        minimapFBO.inActionF(minimapCamera, batch) {
            gdxClearAndEnableBlend(MINIMAP_SKYCOL)

            batch.inUse {

                // [  1  0 0 ]   [  s   0  0 ]   [  s  0 0 ]
                // [  0  1 0 ] x [  0   s  0 ] = [  0  s 0 ]
                // [ px py 1 ]   [ w/2 h/2 1 ]   [ tx ty 1 ]
                // https://www.wolframalpha.com/input/?i=%7B%7B1,0,0%7D,%7B0,1,0%7D,%7Bp_x,p_y,1%7D%7D+*+%7B%7Bs,0,0%7D,%7B0,s,0%7D,%7Bw%2F2,h%2F2,1%7D%7D
                //
                // therefore,
                //  tx = px * s + w/2
                //  ty = py * s + h/w

                // sky background
                batch.color = Color.WHITE

                MinimapComposer.pixmaps.forEachIndexed { index, pixmap ->
                    renderTextures[MinimapComposer.pixmaps.lastIndex - index].dispose()
                    renderTextures[MinimapComposer.pixmaps.lastIndex - index] = Texture(pixmap)

                    val ix = index % MinimapComposer.SQUARE_SIZE; val iy = index / MinimapComposer.SQUARE_SIZE
                    val ox = (ix - (MinimapComposer.SQUARE_SIZE / 2) + 0.5f) * MINIMAP_TILE_WIDTH
                    val oy = (iy - (MinimapComposer.SQUARE_SIZE / 2) + 0.5f) * MINIMAP_TILE_HEIGHT
                    val tx = (minimapDrawOffX - ox) * minimapZoom + MINIMAP_HALFW
                    val ty = (minimapDrawOffY - oy) * minimapZoom + MINIMAP_HALFH

                    batch.draw(renderTextures[index], tx, ty, MINIMAP_TILE_WIDTH * minimapZoom, MINIMAP_TILE_HEIGHT * minimapZoom)
                }


                ((INGAME.actorContainerInactive + INGAME.actorContainerActive + listOf(INGAME.actorNowPlaying)).filterIsInstance<IngamePlayer>()).forEach {
                    it.getSpriteHead()?.let { t ->
                        val sf = it.scale.toFloat()
                        val headHeight = 6
                        val ox = MINIMAP_TILE_WIDTH / 2f
                        val oy = MINIMAP_TILE_HEIGHT / 2f
                        val tx = (minimapDrawOffX - ox)
                        val ty = (minimapDrawOffY - oy)
                        val worldPos = it.hitbox
                        val cw = t.regionWidth * sf
                        val ch = t.regionHeight * sf
                        val cx = worldPos.centeredX.div(TILE_SIZEF).roundToInt().toFloat()
                        val cy = worldPos.startY.plus(headHeight * sf).div(TILE_SIZEF).roundToInt().toFloat()
                        val dx = cx - oldPanX
                        val dy = cy - oldPanY

                        // drawing "crosshair"
                        val x1 = (tx + dx + MINIMAP_TILE_WIDTH / 2f) * minimapZoom + MINIMAP_HALFW
                        val x2 = (tx + dx + worldWidth + MINIMAP_TILE_WIDTH / 2f) * minimapZoom + MINIMAP_HALFW
                        val x3 = (tx + dx - worldWidth + MINIMAP_TILE_WIDTH / 2f) * minimapZoom + MINIMAP_HALFW
                        val y = (ty + dy + MINIMAP_TILE_HEIGHT / 2f) * minimapZoom + MINIMAP_HALFH

                        // how do I actually centre the head?
                        val doffx = -cw / 2f
                        val doffy = -ch / 2f

                        // throwing three static images instead of one won't hurt anything
                        if (it.sprite?.flipHorizontal == false) {
                            batch.draw(t, x1 + doffx, y + doffy, cw, ch)
                            batch.draw(t, x2 + doffx, y + doffy, cw, ch)
                            batch.draw(t, x3 + doffx, y + doffy, cw, ch)
                        }
                        else {
                            batch.draw(t, x1 + doffx + cw, y + doffy, -cw, ch)
                            batch.draw(t, x2 + doffx + cw, y + doffy, -cw, ch)
                            batch.draw(t, x3 + doffx + cw, y + doffy, -cw, ch)
                        }

                        /*
                        val xi = x.roundToInt()
                        val yi = y.roundToInt()
                        batch.color = Color.LIME
                        Toolkit.drawBoxBorder(batch, x + doffx, y + doffy, cw, ch)
                        batch.color = Color.CORAL
                        Toolkit.drawStraightLine(batch, xi-10, yi, xi+10, 1, false)
                        Toolkit.drawStraightLine(batch, xi, yi-10, yi+10, 1, true)
                        */
                    }
                }
            }
        }
        batch.begin()

        batch.color = Color.WHITE

        val minimapDrawX = (width - MINIMAP_WIDTH) / 2
        val minimapDrawY = (height - cellOffY - App.scr.tvSafeGraphicsHeight - MINIMAP_HEIGHT - 72) / 2 + cellOffY * 1f

        if (App.IS_DEVELOPMENT_BUILD) {
            App.fontSmallNumbers.draw(batch, "Pan: ($minimapPanX,$minimapPanY) old ($oldPanX,$oldPanY); Trans: ($minimapDrawOffX,$minimapDrawOffY); x$minimapZoom", minimapDrawX, minimapDrawY - 16f)
        }

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
        minimapPanX = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredX?.roundToInt()?.toFloat() ?: (INGAME.world.width / 2f)
        minimapPanY = INGAME.actorNowPlaying?.intTilewiseHitbox?.centeredY?.roundToInt()?.toFloat() ?: (INGAME.world.height / 2f)

        minimapRerenderInterval = 0.25f
    }

    override fun endClosing(delta: Float) {}

    override fun dispose() {
        minimapFBO.dispose()
        renderTextures.forEach { it.tryDispose() }
    }
}