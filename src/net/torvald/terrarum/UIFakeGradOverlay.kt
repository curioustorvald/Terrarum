package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2021-09-09.
 */
class UIFakeGradOverlay : UICanvas() {

    override var width: Int
        get() = App.scr.width
        set(value) {}

    override var height: Int
        get() = App.scr.height
        set(value) {}

    override var openCloseTime: Second = 0f

    private val tex = CommonResourcePool.getAsTexture("title_halfgrad")

    init {
        setAsAlwaysVisible()
    }

    override fun updateUI(delta: Float) {}
    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        gdxSetBlendMul()

        batch.draw(tex, 0f, 0f, App.scr.wf, App.scr.hf)

        gdxSetBlendNormal()
    }

    override fun doOpening(delta: Float) {}
    override fun doClosing(delta: Float) {}
    override fun endOpening(delta: Float) {}
    override fun endClosing(delta: Float) {}
    override fun dispose() {}
}