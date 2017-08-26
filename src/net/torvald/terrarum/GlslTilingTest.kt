package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarum.worlddrawer.BlocksDrawer
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


    lateinit var tilesBuffer: Pixmap

    lateinit var tileAtlas: Texture


    override fun create() {
        ShaderProgram.pedantic = false

        shader = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/tiling.frag"))


        font = GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", flipY = false)


        if (!shader.isCompiled) {
            Gdx.app.log("Shader", shader.log)

            //ErrorDisp.title = "Error in shader ${shader.vertexShaderSource}"
            //ErrorDisp.text = shader.log.split('\n')
            //TerrarumAppLoader.getINSTANCE().setScreen(ErrorDisp)
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
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                Gdx.graphics.width.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        tilesQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        tilesBuffer = Pixmap(tilesInHorizontal.ceilInt(), tilesInVertical.ceilInt(), Pixmap.Format.RGBA8888)


        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()


        batch = SpriteBatch()

        fucktex = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))

        tileAtlas = Texture(Gdx.files.internal("assets/terrain.tga"))//BlocksDrawer.tilesTerrain.texture



        println(tilesBuffer.format)
        // 0brrrrrrrr_gggggggg_bbbbbbbb_aaaaaaaa
        for (x in 0 until tilesBuffer.width * tilesBuffer.height) {
            val color = Color(0f, 1f/16f, 0f, 1f)
            tilesBuffer.drawPixel(x / tilesBuffer.width, x % tilesBuffer.width, 0x00ff00ff)
        }
    }


    override fun render() {
        Gdx.graphics.setTitle("GlslTilingTest — F: ${Gdx.graphics.framesPerSecond}")

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val tilesInHorizontal = Gdx.graphics.width.toFloat() / TILE_SIZE
        val tilesInVertical = Gdx.graphics.height.toFloat() / TILE_SIZE



        val tilesBufferAsTex = Texture(tilesBuffer)
        tilesBufferAsTex.bind(2)
        tileAtlas.bind(1) // for some fuck reason, it must be bound as last

        shader.begin()
        shader.setUniformMatrix("u_projTrans", batch.projectionMatrix)//camera.combined)
        shader.setUniformi("tilesAtlas", 1)
        shader.setUniformi("tilemap", 2)
        shader.setUniformf("tilemapSize", tilesBuffer.width.toFloat(), tilesBuffer.height.toFloat())
        shader.setUniformf("tileInDim", tilesInHorizontal, tilesInVertical)
        shader.setUniformf("cameraTranslation", 4f, 1f)
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()
        tilesBufferAsTex.dispose()

    }

    override fun dispose() {
        shader.dispose()
    }
}