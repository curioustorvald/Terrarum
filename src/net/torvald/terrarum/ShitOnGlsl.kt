package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.unicode.EMDASH
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap

/**
 * Created by minjaesong on 2017-08-11.
 */
fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = Lwjgl3ApplicationConfiguration()
    config.useVsync(false)
    config.setResizable(false)
    config.setWindowedMode(1072, 742)
    Lwjgl3Application(ShitOnGlsl, config)
}

object ShitOnGlsl : ApplicationAdapter() {

    lateinit var shader: ShaderProgram
    lateinit var font: TerrarumSansBitmap

    lateinit var fullscreenQuad: Mesh

    lateinit var camera: OrthographicCamera

    lateinit var batch: SpriteBatch

    lateinit var fucktex: Texture

    lateinit var testTex: Texture

    override fun create() {
        ShaderProgram.pedantic = false

        shader = ShaderProgram(Gdx.files.internal("assets/shaders/default.vert"), Gdx.files.internal("assets/shaders/crt.frag"))


        font = TerrarumSansBitmap("assets/graphics/fonts/terrarum-sans-bitmap")


        if (!shader.isCompiled) {
            Gdx.app.log("Shader", shader.log)
            System.exit(1)
        }



        fullscreenQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        fullscreenQuad.setVertices(floatArrayOf(
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                Gdx.graphics.width.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        fullscreenQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()


        batch = SpriteBatch()

        fucktex = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))
        testTex = Texture(Gdx.files.internal("assets/test_texture.tga"))
    }


    override fun render() {
        Gdx.graphics.setTitle("ShitOnGlsl $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)



        batch.inUse {

            batch.shader = shader

            //shader.setUniformMatrix("u_projTrans", camera.combined)
            //shader.setUniformi("u_texture", 0)
            shader.setUniformf("resolution", 1072f, 742f)
            shader.setUniformf("circleCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
            shader.setUniformf("colorCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
            shader.setUniformf("circleSize", 200f)

            //batch.draw(fucktex, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            batch.draw(testTex, 0f, 0f)

        }


        /*shader.begin()
        shader.setUniformMatrix("u_projTrans", batch.projectionMatrix)
        shader.setUniformi("u_texture", 0)
        shader.setUniformf("circleCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
        shader.setUniformf("colorCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
        shader.setUniformf("circleSize", 200f)
        fullscreenQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()*/
    }

    override fun dispose() {
        shader.dispose()
    }
}