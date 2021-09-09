package net.torvald.terrarum

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
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

    private val shaderBlur = App.loadShaderFromFile("assets/blur.vert", "assets/blur2.frag")

    private val blurRadius = 2f
    private val darken = Color(0.5f, 0.5f, 0.5f, 1f)
    private var fbo = FrameBuffer(Pixmap.Format.RGBA8888, width / 4, height / 4, true)


    override fun updateUI(delta: Float) {}
    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        for (i in 0 until 6) {
            val scalar = blurRadius * (1 shl i.ushr(1))

            batch.shader = shaderBlur
            shaderBlur.setUniformMatrix("u_projTrans", camera.combined)
            shaderBlur.setUniformi("u_texture", 0)
            shaderBlur.setUniformf("iResolution", width.toFloat(), height.toFloat())
            IngameRenderer.shaderBlur.setUniformf("flip", 1f)
            if (i % 2 == 0)
                IngameRenderer.shaderBlur.setUniformf("direction", scalar, 0f)
            else
                IngameRenderer.shaderBlur.setUniformf("direction", 0f, scalar)

            val p = Pixmap.createFromFrameBuffer(0, 0, width, height)
            val t = Texture(p); t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

            batch.draw(t, 0f, 0f)
            batch.flush() // so I can safely dispose of the texture

            t.dispose(); p.dispose()
        }

        // sample blurred but blocky texture, scale it down, and re-scale up to the main screen
        batch.end()
        val p = Pixmap.createFromFrameBuffer(0, 0, width, height)
        val t = Texture(p); t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        fbo.inAction(camera as OrthographicCamera, batch) {
            batch.inUse {
                batch.shader = null
                batch.draw(t, 0f, 0f, fbo.width.toFloat(), fbo.height.toFloat())
            }
        }
        t.dispose(); p.dispose()

        batch.begin()

        val t2 = fbo.colorBufferTexture
        t2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        blendNormal(batch)
        batch.draw(t2, 0f, 0f, width.toFloat(), height.toFloat())


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
        fbo.dispose()
    }
}