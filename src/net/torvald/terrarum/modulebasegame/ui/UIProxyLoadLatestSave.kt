package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2021-09-13.
 */
class UIProxyLoadLatestSave(val remoCon: UIRemoCon) : UICanvas() {

    override var width: Int = 0
    override var height: Int = 0
    override var openCloseTime: Second = 0f

    override fun updateImpl(delta: Float) {
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {


        // do something!


    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}