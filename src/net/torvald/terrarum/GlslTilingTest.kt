package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarum.gameactors.floor
import net.torvald.terrarum.gameactors.floorInt
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
    val TILE_SIZEF = 16f


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



        val tilesInHorizontal = (Gdx.graphics.width.toFloat() / TILE_SIZE).ceil() + 1f
        val tilesInVertical = (Gdx.graphics.height.toFloat() / TILE_SIZE).ceil() + 1f

        tilesQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        tilesQuad.setVertices(floatArrayOf( // WARNING! not ususal quads; TexCoords of Y is flipped
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f,
                Gdx.graphics.width.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                0f, Gdx.graphics.height.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 1f
        ))
        tilesQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        tilesBuffer = Pixmap(tilesInHorizontal.toInt(), tilesInVertical.toInt(), Pixmap.Format.RGBA8888)


        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()


        batch = SpriteBatch()

        fucktex = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))

        tileAtlas = Texture(Gdx.files.internal("assets/terrain.tga"))//BlocksDrawer.tilesTerrain.texture


        println(tilesBuffer.format)
    }


    var cameraX = 0f
    var cameraY = 0f

    override fun render() {
        Gdx.graphics.setTitle("GlslTilingTest — F: ${Gdx.graphics.framesPerSecond}")


        // 0brrrrrrrr_gggggggg_bbbbbbbb_aaaaaaaa
        for (y in 0 until tilesBuffer.height) {
            for (x in 0 until tilesBuffer.width) {
                val i = (y * 256) + x
                val color = Color(i.shl(8).or(255))
                tilesBuffer.setColor(color)
                tilesBuffer.drawPixel(x, y)
            }
        }




        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(1f, 0f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val tilesInHorizontal = (Gdx.graphics.width.toFloat() / TILE_SIZE).ceil() + 1f
        val tilesInVertical = (Gdx.graphics.height.toFloat() / TILE_SIZE).ceil() + 1f



        val tilesBufferAsTex = Texture(tilesBuffer)
        tilesBufferAsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        tilesBufferAsTex.bind(2)
        tileAtlas.bind(1) // for some fuck reason, it must be bound as last

        shader.begin()
        shader.setUniformMatrix("u_projTrans", batch.projectionMatrix)//camera.combined)
        shader.setUniformf("screenDimension", Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        shader.setUniformi("tilesAtlas", 1)
        shader.setUniformi("tilemap", 2)
        shader.setUniformi("tilemapDimension", tilesBuffer.width, tilesBuffer.height)
        shader.setUniformf("tilesInAxes", tilesInHorizontal, tilesInVertical)
        shader.setUniformf("cameraTranslation", cameraX, cameraY)
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()
        tilesBufferAsTex.dispose()


        cameraX += 160 * Gdx.graphics.deltaTime
        cameraY += 160 * Gdx.graphics.deltaTime
    }

    override fun dispose() {
        shader.dispose()
    }
}

private fun Float.ceil(): Float = FastMath.ceil(this).toFloat()
