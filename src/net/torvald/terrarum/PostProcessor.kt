package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer

/**
 * Must be called by the App Loader
 */
object PostProcessor {

    private val batch = SpriteBatch()

    private lateinit var lutTex: Texture

    fun reloadLUT(filename: String) {
        lutTex = Texture(Gdx.files.internal("assets/clut/$filename"))
    }

    fun draw(screenTexHolder: FrameBuffer) {
        //Gdx.gl.glClearColor(.094f, .094f, .094f, 1f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        batch.shader = Terrarum.shader18Bit // essential design decision; 262 144 colours VGA; NOT related with LCD monitor's internals
        batch.inUse {
            val texture = screenTexHolder.colorBufferTexture
            batch.shader.setUniformMatrix("u_projTrans", batch.projectionMatrix)
            batch.draw(texture, 0f, 0f, texture.width.toFloat(), texture.height.toFloat())
        }


        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // don't know why it is needed; it really depresses me

    }

}