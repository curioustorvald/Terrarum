package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.ui.UICanvas

class ModOptionsHost : UICanvas() {

    override var openCloseTime: Second = 0f

    private val moduleAreaHMargin = 48
    private val moduleAreaBorder = 8

    override var width = AppLoader.screenW - UIRemoCon.remoConWidth - moduleAreaHMargin
    override var height = AppLoader.screenH - moduleAreaHMargin * 2

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
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
    }
}