package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.ui.UICanvas

class ModOptionsHost(val remoCon: UIRemoCon) : UICanvas() {
    init {
        handler.allowESCtoClose = false
    }

    private val moduleAreaHMargin = 48
    private val moduleAreaBorder = 8

    override var width = App.scr.width - UIRemoCon.remoConWidth - moduleAreaHMargin
    override var height = App.scr.height - moduleAreaHMargin * 2

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
    }

    override fun dispose() {
    }
}