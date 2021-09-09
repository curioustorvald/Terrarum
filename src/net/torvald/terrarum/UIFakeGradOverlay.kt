package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulebasegame.IngameRenderer
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
        blendMul(batch)
        batch.draw(tex, 0f, 0f, App.scr.halfwf * 1.5f, App.scr.hf)

        blendNormal(batch)
    }

    override fun doOpening(delta: Float) {}
    override fun doClosing(delta: Float) {}
    override fun endOpening(delta: Float) {}
    override fun endClosing(delta: Float) {}
    override fun dispose() {}
}

class UIFakeBlurOverlay : UICanvas() {

    override var width: Int
        get() = App.scr.width
        set(value) {}

    override var height: Int
        get() = App.scr.height
        set(value) {}

    override var openCloseTime: Second = 0f

    private val shaderBlur = App.loadShaderFromFile("assets/blur.vert", "assets/blur.frag")

    init { }

    private val blurRadius = 32f
    private val darken = Color(0.5f, 0.5f, 0.5f, 1f)

    override fun updateUI(delta: Float) {}
    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        /*for (i in 0 until 5) {
            batch.shader = shaderBlur
            shaderBlur.setUniformMatrix("u_projTrans", camera.combined)
            shaderBlur.setUniformi("u_texture", 0)
            shaderBlur.setUniformf("iResolution", width.toFloat(), height.toFloat())
            IngameRenderer.shaderBlur.setUniformf("flip", 1f)
            if (i % 2 == 0)
                IngameRenderer.shaderBlur.setUniformf("direction", blurRadius, 0f)
            else
                IngameRenderer.shaderBlur.setUniformf("direction", 0f, blurRadius)

            batch.fillRect(0f, 0f, width.toFloat(), height.toFloat())
        }*/
        blendMul(batch)
        batch.color = darken
        batch.fillRect(0f, 0f, width.toFloat(), height.toFloat())

        blendNormal(batch)
    }

    override fun doOpening(delta: Float) {}
    override fun doClosing(delta: Float) {}
    override fun endOpening(delta: Float) {}
    override fun endClosing(delta: Float) {}
    override fun dispose() {
        shaderBlur.dispose()
    }
}