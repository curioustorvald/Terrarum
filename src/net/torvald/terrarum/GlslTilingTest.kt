package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarumsansbitmap.gdx.GameFontBase

/**
 * Created by minjaesong on 2017-08-25.
 */
fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    //config.useGL30 = true
    config.vSyncEnabled = false
    config.resizable = false
    config.width = 1072
    config.height = 742
    config.foregroundFPS = 9999
    LwjglApplication(GlslTilingTest, config)
}

object GlslTilingTest : ApplicationAdapter() {

    lateinit var shader: ShaderProgram
    lateinit var font: GameFontBase

    lateinit var tilesQuad: Mesh

    lateinit var camera: OrthographicCamera

    lateinit var batch: SpriteBatch

    lateinit var fucktex: Texture

    val TILE_SIZE = 16


    lateinit var tilesBuffer: FloatArray


    override fun create() {
        ShaderProgram.pedantic = false

        shader = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/loadingCircle.frag"))


        font = GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", flipY = false)


        if (!shader.isCompiled) {
            Gdx.app.log("Shader", shader.log)
            System.exit(1)
        }



        val tilesInHorizontal = Gdx.graphics.width.toFloat() / TILE_SIZE
        val tilesInVertical = Gdx.graphics.height.toFloat() / TILE_SIZE

        tilesQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        tilesQuad.setVertices(floatArrayOf(
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, tilesInVertical,
                Gdx.graphics.width.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, tilesInHorizontal, tilesInVertical,
                Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, tilesInHorizontal, 0f,
                0f, Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        tilesQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        tilesBuffer = FloatArray(tilesInHorizontal.ceilInt() * tilesInVertical.ceilInt())


        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()


        batch = SpriteBatch()

        fucktex = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))
    }


    override fun render() {
        Gdx.graphics.setTitle("ShitOnGlsl — F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)



        batch.inUse {

            batch.shader = shader

            shader.setUniformMatrix("u_projTrans", camera.combined)
            shader.setUniformi("u_texture", 0)
            shader.setUniform1fv("tilesBuffer", tilesBuffer, 0, tilesBuffer.size)
            //tilesQuad.render(shader, GL20.GL_TRIANGLES)

            batch.draw(fucktex, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        }


        /*shader.begin()
        shader.setUniformMatrix("u_projTrans", batch.projectionMatrix)
        shader.setUniformi("u_texture", 0)
        shader.setUniformf("circleCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
        shader.setUniformf("colorCentrePoint", Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
        shader.setUniformf("circleSize", 200f)
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()*/
    }

    override fun dispose() {
        shader.dispose()
    }
}