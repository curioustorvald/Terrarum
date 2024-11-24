package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.inAction

/**
 * Created by minjaesong on 2024-11-23.
 */
object BlurMgr {

    private class FrameBufferSet(val width: Int, val height: Int) {
        private val internalWidth = (width.toFloat() / 4f).ceilToInt() * 4
        private val internalHeight = (height.toFloat() / 4f).ceilToInt() * 4

        val full = Float16FrameBuffer(width, height, false)
        val half = Float16FrameBuffer(internalWidth / 2, internalHeight / 2, false)
        val quarter = Float16FrameBuffer(internalWidth / 4, internalHeight / 4, false)
        val camera = OrthographicCamera(width.toFloat(), height.toFloat())

        val quadFull = Mesh(
            true, 4, 4,
            VertexAttribute.Position(),
            VertexAttribute.ColorUnpacked(),
            VertexAttribute.TexCoords(0)
        )
        val quadHalf = Mesh(
            true, 4, 4,
            VertexAttribute.Position(),
            VertexAttribute.ColorUnpacked(),
            VertexAttribute.TexCoords(0)
        )
        val quadQuarter = Mesh(
            true, 4, 4,
            VertexAttribute.Position(),
            VertexAttribute.ColorUnpacked(),
            VertexAttribute.TexCoords(0)
        )

        init {
            camera.setToOrtho(true)

            quadFull.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                width.toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                width.toFloat(), height.toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, height.toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
            quadFull.setIndices(shortArrayOf(0, 1, 2, 3))

            quadHalf.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                width.div(2).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                width.div(2).toFloat(), height.div(2).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, height.div(2).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
            quadHalf.setIndices(shortArrayOf(0, 1, 2, 3))

            quadQuarter.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                width.div(4).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                width.div(4).toFloat(), height.div(4).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, height.div(4).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
            quadQuarter.setIndices(shortArrayOf(0, 1, 2, 3))
        }
        
        fun dispose() {
            full.dispose()
            half.dispose()
            quarter.dispose()
        }
    }

    private val fboDict = HashMap<Long, FrameBufferSet>()

    private lateinit var blurtex0: Texture
    private lateinit var blurtex1: Texture
    private lateinit var blurtex2: Texture
    private lateinit var blurtex3: Texture

    private val shaderKawaseDown = App.loadShaderFromClasspath("shaders/default.vert", "shaders/kawasedown.frag")
    private val shaderKawaseUp = App.loadShaderFromClasspath("shaders/default.vert", "shaders/kawaseup.frag")

    fun makeBlur(`in`: FrameBuffer, out: FrameBuffer, strength: Float) {
        assert(`in`.width == out.width && `in`.height == out.height) {
            "Input and Output dimension mismatch: In(${`in`.width}x${`in`.height}), Out(${out.width}x${out.height})"
        }

        val fbos = fboDict.getOrPut(`in`.width.toLong().shl(32) or `in`.height.toLong()) {
            FrameBufferSet(`in`.width, `in`.height)
        }

        val batch: SpriteBatch? = null // placeholder

        val radius3 = FastMath.pow(strength / 2, 0.5f)//(blurRadius - 3f) / 8f
        fbos.half.inAction(fbos.camera, batch) {
            gdxClearAndEnableBlend(0f,0f,0f,0f)

            blurtex0 = `in`.colorBufferTexture
            blurtex0.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex0.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", fbos.camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", radius3 / fbos.half.width, radius3 / fbos.half.height)
            fbos.quadHalf.render(shaderKawaseDown, GL20.GL_TRIANGLE_FAN)
        }

        fbos.quarter.inAction(fbos.camera, batch) {
            gdxClearAndEnableBlend(0f,0f,0f,0f)

            blurtex1 = fbos.half.colorBufferTexture
            blurtex1.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex1.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", fbos.camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", radius3 / fbos.quarter.width, radius3 / fbos.quarter.height)
            fbos.quadQuarter.render(shaderKawaseDown, GL20.GL_TRIANGLE_FAN)
        }

        fbos.half.inAction(fbos.camera, batch) {
            gdxClearAndEnableBlend(0f,0f,0f,0f)

            blurtex2 = fbos.quarter.colorBufferTexture
            blurtex2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex2.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", fbos.camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", radius3 / fbos.quarter.width, radius3 / fbos.quarter.height)
            fbos.quadHalf.render(shaderKawaseUp, GL20.GL_TRIANGLE_FAN)
        }

        out.inAction(fbos.camera, batch) {
            gdxClearAndEnableBlend(0f,0f,0f,0f)

            blurtex3 = fbos.half.colorBufferTexture
            blurtex3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex3.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", fbos.camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", radius3 / fbos.half.width, radius3 / fbos.half.height)
            fbos.quadFull.render(shaderKawaseUp, GL20.GL_TRIANGLE_FAN)
        }
    }

    fun dispose() {
        fboDict.values.forEach { it.dispose() }
        shaderKawaseUp.dispose()
        shaderKawaseDown.dispose()
    }

}