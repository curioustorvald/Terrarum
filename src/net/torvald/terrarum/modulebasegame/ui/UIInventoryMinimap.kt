package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.ui.UICanvas

class UIInventoryMinimap(val full: UIInventoryFull) : UICanvas() {

    private val debugvals = true

    override var width: Int = AppLoader.screenW
    override var height: Int = AppLoader.screenH
    override var openCloseTime = 0.0f

    private val MINIMAP_WIDTH = 800f
    private val MINIMAP_HEIGHT = full.INVENTORY_CELLS_UI_HEIGHT.toFloat()
    private val MINIMAP_SKYCOL = Color(0x88bbddff.toInt())
    private var minimapZoom = 1f
    private var minimapPanX = -MinimapComposer.totalWidth / 2f
    private var minimapPanY = -MinimapComposer.totalHeight / 2f
    private val MINIMAP_ZOOM_MIN = 0.5f
    private val MINIMAP_ZOOM_MAX = 8f
    private val minimapFBO = FrameBuffer(Pixmap.Format.RGBA8888, MINIMAP_WIDTH.toInt(), MINIMAP_HEIGHT.toInt(), false)
    private val minimapCamera = OrthographicCamera(MINIMAP_WIDTH, MINIMAP_HEIGHT)

    private var minimapRerenderTimer = 0f
    private val minimapRerenderInterval = .5f

    override fun updateUI(delta: Float) {
        MinimapComposer.setWorld(Terrarum.ingame!!.world)
        MinimapComposer.update()
        minimapRerenderTimer += Gdx.graphics.rawDeltaTime
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)

        // update map panning
        // if left click is down and cursor is in the map area
        if (Gdx.input.isButtonPressed(AppLoader.getConfigInt("config_mouseprimary")) &&
            Terrarum.mouseScreenY in full.INVENTORY_CELLS_OFFSET_Y..full.INVENTORY_CELLS_OFFSET_Y + full.INVENTORY_CELLS_UI_HEIGHT) {
            minimapPanX += Terrarum.mouseDeltaX * 2f / minimapZoom
            minimapPanY += Terrarum.mouseDeltaY * 2f / minimapZoom
        }


        if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
            minimapZoom *= (1f / 1.02f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
            minimapZoom *= 1.02f
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) {
            minimapZoom = 1f
            minimapPanX = -MinimapComposer.totalWidth / 2f
            minimapPanY = -MinimapComposer.totalHeight / 2f
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

        if (minimapRerenderTimer >= minimapRerenderInterval) {
            minimapRerenderTimer = 0f
            MinimapComposer.requestRender()
        }

        MinimapComposer.renderToBackground()

        minimapFBO.inAction(minimapCamera, batch) {
            // whatever.
            MinimapComposer.tempTex.dispose()
            MinimapComposer.tempTex = Texture(MinimapComposer.minimap)
            MinimapComposer.tempTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Nearest)

            batch.inUse {

                // [  1  0 0 ]   [  s   0  0 ]   [  s  0 0 ]
                // [  0  1 0 ] x [  0   s  0 ] = [  0  s 0 ]
                // [ px py 1 ]   [ w/2 h/2 1 ]   [ tx ty 1 ]
                //
                // https://www.wolframalpha.com/input/?i=%7B%7B1,0,0%7D,%7B0,1,0%7D,%7Bp_x,p_y,1%7D%7D+*+%7B%7Bs,0,0%7D,%7B0,s,0%7D,%7Bw%2F2,h%2F2,1%7D%7D

                val tx = minimapPanX * minimapZoom + 0.5f * MINIMAP_WIDTH
                val ty = minimapPanY * minimapZoom + 0.5f * MINIMAP_HEIGHT

                // sky background
                batch.color = MINIMAP_SKYCOL
                batch.fillRect(0f, 0f, MINIMAP_WIDTH, MINIMAP_HEIGHT)
                // the actual image
                batch.color = Color.WHITE
                batch.draw(MinimapComposer.tempTex, tx, ty + MinimapComposer.totalHeight * minimapZoom, MinimapComposer.totalWidth * minimapZoom, -MinimapComposer.totalHeight * minimapZoom)

            }
        }
        batch.begin()

        if (debugvals) {
            AppLoader.fontSmallNumbers.draw(batch, "$minimapPanX, $minimapPanY; x$minimapZoom", (AppLoader.screenW - MINIMAP_WIDTH) / 2, -10f + full.INVENTORY_CELLS_OFFSET_Y)
        }

        batch.projectionMatrix = camera.combined
        // 1px stroke
        batch.color = Color.WHITE
        batch.fillRect((AppLoader.screenW - MINIMAP_WIDTH) / 2, -1 + full.INVENTORY_CELLS_OFFSET_Y.toFloat(), MINIMAP_WIDTH, 1f)
        batch.fillRect((AppLoader.screenW - MINIMAP_WIDTH) / 2, full.INVENTORY_CELLS_OFFSET_Y + MINIMAP_HEIGHT, MINIMAP_WIDTH, 1f)
        batch.fillRect(-1 + (AppLoader.screenW - MINIMAP_WIDTH) / 2, full.INVENTORY_CELLS_OFFSET_Y.toFloat(), 1f, MINIMAP_HEIGHT)
        batch.fillRect((AppLoader.screenW - MINIMAP_WIDTH) / 2 + MINIMAP_WIDTH, full.INVENTORY_CELLS_OFFSET_Y.toFloat(), 1f, MINIMAP_HEIGHT)

        // control hints
        batch.color = Color.WHITE
        AppLoader.fontGame.draw(batch, full.minimapControlHelp, full.offsetX, full.yEnd - 20)

        // the minimap
        batch.draw(minimapFBO.colorBufferTexture, (AppLoader.screenW - MINIMAP_WIDTH) / 2, full.INVENTORY_CELLS_OFFSET_Y.toFloat())
    }

    override fun doOpening(delta: Float) {}

    override fun doClosing(delta: Float) {}

    override fun endOpening(delta: Float) {}

    override fun endClosing(delta: Float) {}

    override fun dispose() {
        minimapFBO.dispose()
    }
}