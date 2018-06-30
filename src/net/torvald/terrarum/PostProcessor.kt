package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import kotlin.system.measureNanoTime

/**
 * Must be called by the App Loader
 */
object PostProcessor {

    private lateinit var batch: SpriteBatch // not nulling to save some lines of code
    private var textureRegion: TextureRegion? = null


    private lateinit var lutTex: Texture

    fun reloadLUT(filename: String) {
        lutTex = Texture(Gdx.files.internal("assets/clut/$filename"))
    }

    fun draw(fbo: FrameBuffer) {

        if (textureRegion == null) {
            textureRegion = TextureRegion(fbo.colorBufferTexture)
            batch = SpriteBatch()
        }


        // FIXME something's really fucked between sky_gradient and the actual_world_render,
        //       maybe overlaying world over grad
        //       OR    mixing lightmap (less likely?)
        // known symptom: when localising the spritebatch, greyscale lightmap and the UI are the
        //                only thing gets drawn
        



        Terrarum.debugTimers["GFX.PostProcessor"] = measureNanoTime {

            //Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
            //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            //Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
            //Gdx.gl.glEnable(GL20.GL_BLEND)
            //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val shader = AppLoader.shader18Bit

            // no-screen screen renders but the game don't? wtf?

            shader.begin()
            shader.setUniformf("resolution", AppLoader.appConfig.width.toFloat(), AppLoader.appConfig.height.toFloat())
            AppLoader.fullscreenQuad.render(shader, GL20.GL_TRIANGLES)
            shader.end()


            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it


        }
    }

}