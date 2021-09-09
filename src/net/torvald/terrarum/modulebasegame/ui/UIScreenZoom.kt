package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumScreenSize
import net.torvald.terrarum.keyToIcon
import net.torvald.terrarum.ui.Movement
import net.torvald.terrarum.ui.UICanvas

/**
 * Screen zooms in when the UI is opened; zooms out when being closed.
 *
 * Created by minjaesong on 2019-08-11.
 */
class UIScreenZoom : UICanvas(
        App.getConfigInt("config_keyzoom")
) {

    val zoomText = "${keyToIcon(handler.toggleKeyLiteral!!)} $EMDASH Zoom Out"

    override var width = App.fontGame.getWidth(zoomText)
    override var height = App.fontGame.lineHeight.toInt()

    override var openCloseTime = 0.15f

    override val mouseUp = false

    private val zoomMin = 1f
    private val zoomMax = 2f

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE

        App.fontGame.draw(
                batch, zoomText,
                (App.scr.width * TerrarumScreenSize.TV_SAFE_GRAPHICS + 1).toInt().toFloat(),
                (App.scr.height - height - App.scr.tvSafeGraphicsHeight).toFloat()
        )
    }

    override fun dispose() {
    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.screenZoom = Movement.fastPullOut(handler.openCloseCounter / openCloseTime, zoomMin, zoomMax)
        handler.opacity = Terrarum.ingame?.screenZoom!! - zoomMin
    }

    override fun doClosing(delta: Float) {
        Terrarum.ingame?.screenZoom = Movement.fastPullOut(handler.openCloseCounter / openCloseTime, zoomMax, zoomMin)
        handler.opacity = Terrarum.ingame?.screenZoom!! - zoomMin
    }

    override fun endOpening(delta: Float) {
        Terrarum.ingame?.screenZoom = zoomMax
        handler.opacity = 1f
    }

    override fun endClosing(delta: Float) {
        Terrarum.ingame?.screenZoom = zoomMin
        handler.opacity = 0f
    }
}