package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class NullUI : UICanvas() {
    override var width: Int = 0
    override var height: Int = 0
    override var handler: UIHandler? = null
    override var openCloseTime = 0f

    override fun update(delta: Float) {
    }

    override fun render(batch: SpriteBatch) {
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