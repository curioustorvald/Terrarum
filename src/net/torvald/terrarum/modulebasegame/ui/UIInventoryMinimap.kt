package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_UI_HEIGHT
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

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
    private var minimapPanX = -MinimapComposer.totalWidth / 2f
    private var minimapPanY = -MinimapComposer.totalHeight / 2f
    private val minimapFBO = FrameBuffer(Pixmap.Format.RGBA8888, MINIMAP_WIDTH.toInt(), MINIMAP_HEIGHT.toInt(), false)
    private val minimapCamera = OrthographicCamera(MINIMAP_WIDTH, MINIMAP_HEIGHT)

    private var minimapRerenderTimer = 0f
    private val minimapRerenderInterval = .5f

    override fun updateUI(delta: Float) {
        MinimapComposer.setWorld(INGAME.world)
        MinimapComposer.update()
        minimapRerenderTimer += Gdx.graphics.deltaTime
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        blendNormal(batch)
        val cellOffY = INVENTORY_CELLS_OFFSET_Y()


        // update map panning
        // if left click is down and cursor is in the map area
        if (Terrarum.mouseDown &&
            Terrarum.mouseScreenY in cellOffY..cellOffY + INVENTORY_CELLS_UI_HEIGHT) {
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

        minimapFBO.inActionF(minimapCamera, batch) {
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
                Toolkit.fillArea(batch, 0f, 0f, MINIMAP_WIDTH, MINIMAP_HEIGHT)
                // the actual image
                batch.color = Color.WHITE
                batch.draw(MinimapComposer.tempTex, tx, ty, MinimapComposer.totalWidth * minimapZoom, MinimapComposer.totalHeight * minimapZoom)

            }
        }
        batch.begin()

        if (debugvals) {
            App.fontSmallNumbers.draw(batch, "$minimapPanX, $minimapPanY; x$minimapZoom", (width - MINIMAP_WIDTH) / 2, -10f + cellOffY)
        }

        val minimapDrawX = (width - MINIMAP_WIDTH) / 2
        val minimapDrawY = (height - cellOffY - App.scr.tvSafeGraphicsHeight - MINIMAP_HEIGHT - 72) / 2 + cellOffY * 1f

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

    override fun endOpening(delta: Float) {}

    override fun endClosing(delta: Float) {}

    override fun dispose() {
        minimapFBO.dispose()
    }
}