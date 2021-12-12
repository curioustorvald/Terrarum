package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FloatFrameBuffer
import com.badlogic.gdx.utils.Disposable
import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.FrameBufferManager
import net.torvald.terrarum.inAction
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack


/**
 * Created by minjaesong on 2016-08-04.
 */
object Toolkit : Disposable {

    object Theme {
        val COL_INVENTORY_CELL_BORDER = Color(1f, 1f, 1f, 0.25f)
        val COL_CELL_FILL = Color(0x282828C8)

        val COL_LIST_DEFAULT = Color.WHITE
        val COL_INACTIVE = Color.LIGHT_GRAY
        val COL_ACTIVE = Color(0xfff066_ff.toInt()) // yellow
        val COL_HIGHLIGHT = Color(0x00f8ff_ff) // cyan
        val COL_DISABLED = Color(0xaaaaaaff.toInt())
    }


    private val shaderKawaseDown = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/kawasedown.frag")
    private val shaderKawaseUp = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/kawaseup.frag")
    private val shaderBoxDown = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/boxdown.frag")
    private val shaderBoxUp = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/boxup.frag")

    private lateinit var fboBlur: FloatFrameBuffer
    private lateinit var fboBlurHalf: FloatFrameBuffer
    private lateinit var fboBlurQuarter: FloatFrameBuffer
    private lateinit var blurWriteQuad: Mesh
    private lateinit var blurWriteQuad2: Mesh
    private lateinit var blurWriteQuad4: Mesh

    val baloonTile = TextureRegionPack("assets/graphics/gui/message_black_tileable.tga", 36, 36, flipY = true)

    val textureWhiteSquare = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))
    val textureWhiteCircle = Texture(Gdx.files.internal("assets/graphics/circle_512.tga"))

    init {
        App.disposables.add(this)

        textureWhiteSquare.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        textureWhiteCircle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        CommonResourcePool.addToLoadingList("toolkit_box_border") {
            TextureRegionPack(Gdx.files.internal("./assets/graphics/gui/box_border_flat_tileable.tga"), 1, 1)
        }
        CommonResourcePool.loadAll()
    }

    private val rng = HQRNG()

    override fun dispose() {
        baloonTile.dispose()
        textureWhiteSquare.dispose()
        textureWhiteCircle.dispose()

        fboBlur.dispose()
        fboBlurHalf.dispose()
        fboBlurQuarter.dispose()

        blurWriteQuad.dispose()
        blurWriteQuad2.dispose()
        blurWriteQuad4.dispose()

        shaderKawaseUp.dispose()
        shaderKawaseDown.dispose()
        shaderBoxDown.dispose()
        shaderBoxUp.dispose()
    }

    val drawWidth: Int
        get() = App.scr.width - if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth else 0
    val drawWidthf: Float
        get() = drawWidth.toFloat()

    fun drawCentered(batch: SpriteBatch, image: Texture, screenPosY: Int, ui: UICanvas? = null) {
        val imageW = image.width
        val targetW = ui?.width ?: drawWidth
        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat(), screenPosY.toFloat())
    }
    fun drawCentered(batch: SpriteBatch, image: TextureRegion, screenPosY: Int, ui: UICanvas? = null) {
        val imageW = image.regionWidth
        val targetW = ui?.width ?: drawWidth
        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat(), screenPosY.toFloat())
    }

    fun drawCentered(batch: SpriteBatch, image: Texture, screenPosY: Int, targetW: Int, offsetX: Int = 0, offsetY: Int = 0) {
        val imageW = image.width
        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat() + offsetX, screenPosY.toFloat() + offsetY)
    }
    fun drawCentered(batch: SpriteBatch, image: TextureRegion, screenPosY: Int, targetW: Int, offsetX: Int = 0, offsetY: Int = 0) {
        val imageW = image.regionWidth
        batch.draw(image, targetW.minus(imageW).ushr(1).toFloat() + offsetX, screenPosY.toFloat() + offsetY)
    }

    fun fillArea(batch: SpriteBatch, x: Int, y: Int, w: Int, h: Int) {
        batch.draw(textureWhiteSquare, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }
    fun fillArea(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) {
        batch.draw(textureWhiteSquare, x, y, w, h)
    }
    fun fillCircle(batch: SpriteBatch, x: Int, y: Int, w: Int, h: Int) {
        batch.draw(textureWhiteCircle, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
    }
    fun drawStraightLine(batch: SpriteBatch, x: Int, y: Int, otherEnd: Int, thickness: Int, isVertical: Boolean) {
        if (!isVertical)
            fillArea(batch, x, y, otherEnd - x, thickness)
        else
            fillArea(batch, x, y, thickness, otherEnd - y)
    }

    /**
     * Parameters are THAT OF THE BOX, the border will be drawn OUTSIDE of the params you specified!
     */
    fun drawBoxBorder(batch: SpriteBatch, x: Int, y: Int, w: Int, h: Int) {
        val pack = CommonResourcePool.getAsTextureRegionPack("toolkit_box_border")
        val tx = pack.tileW.toFloat()
        val ty = pack.tileH.toFloat()

        // top edge
        batch.draw(pack.get(1, 0), x.toFloat(), y - ty, w.toFloat(), ty)
        // bottom edge
        batch.draw(pack.get(1, 2), x.toFloat(), y.toFloat() + h, w.toFloat(), ty)
        // left edge
        batch.draw(pack.get(0, 1), x.toFloat() - tx, y.toFloat(), tx, h.toFloat())
        // right edge
        batch.draw(pack.get(2, 1), x.toFloat() + w, y.toFloat(), tx, h.toFloat())

        // top left point
        /*batch.draw(pack.get(0, 0), x - tx, y - ty)
        // top right point
        batch.draw(pack.get(2, 0), x + tx, y - ty)
        // bottom left point
        batch.draw(pack.get(0, 2), x - tx, y + ty)
        // bottom right point
        batch.draw(pack.get(2, 2), x + tx, y + ty)*/

    }

    private lateinit var blurtex0: Texture
    private lateinit var blurtex1: Texture
    private lateinit var blurtex2: Texture
    private lateinit var blurtex3: Texture

    fun blurEntireScreen(batch: SpriteBatch, camera: OrthographicCamera, blurRadius0: Float, x: Int, y: Int, w: Int, h: Int) {
        batch.end()

        val blurRadius = blurRadius0
        val renderTarget = FrameBufferManager.peek()

        /*fboBlurHalf.inAction(camera, batch) {
            blurtex0 = renderTarget.colorBufferTexture
            blurtex0.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex0.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", blurRadius / fboBlurHalf.width, blurRadius / fboBlurHalf.height)
            blurWriteQuad2.render(shaderKawaseDown, GL20.GL_TRIANGLES)
        }

        fboBlurQuarter.inAction(camera, batch) {
            blurtex1 = fboBlurHalf.colorBufferTexture
            blurtex1.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex1.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", blurRadius / fboBlurQuarter.width, blurRadius / fboBlurQuarter.height)
            blurWriteQuad4.render(shaderKawaseDown, GL20.GL_TRIANGLES)
        }

        fboBlurHalf.inAction(camera, batch) {
            blurtex2 = fboBlurQuarter.colorBufferTexture
            blurtex2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex2.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", blurRadius / fboBlurQuarter.width, blurRadius / fboBlurQuarter.height)
            blurWriteQuad2.render(shaderKawaseUp, GL20.GL_TRIANGLES)
        }

        fboBlur.inAction(camera,  batch) {
            blurtex3 = fboBlurHalf.colorBufferTexture
            blurtex3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex3.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", blurRadius / fboBlurHalf.width, blurRadius / fboBlurHalf.height)
            blurWriteQuad.render(shaderKawaseUp, GL20.GL_TRIANGLES)
        }*/

        ////////////////////////////////////////////////////////////////////////

        fboBlurHalf.inAction(camera, batch) {
            blurtex2 = renderTarget.colorBufferTexture
            blurtex2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex2.bind(0)
            shaderBoxDown.bind()
            shaderBoxDown.setUniformMatrix("u_projTrans", camera.combined)
            shaderBoxDown.setUniformi("u_texture", 0)
            shaderBoxDown.setUniformf("halfpixel", blurRadius / fboBlurHalf.width, blurRadius / fboBlurHalf.height)
            blurWriteQuad2.render(shaderBoxDown, GL20.GL_TRIANGLES)
        }

        fboBlur.inAction(camera,  batch) {
            blurtex3 = fboBlurHalf.colorBufferTexture
            blurtex3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex3.bind(0)
            shaderBoxUp.bind()
            shaderBoxUp.setUniformMatrix("u_projTrans", camera.combined)
            shaderBoxUp.setUniformi("u_texture", 0)
            shaderBoxUp.setUniformf("halfpixel", blurRadius / fboBlurHalf.width, blurRadius / fboBlurHalf.height)
            blurWriteQuad.render(shaderBoxUp, GL20.GL_TRIANGLES)
        }



        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

        batch.begin()
        batch.shader = null
        batch.draw(fboBlur.colorBufferTexture, x.toFloat(), y.toFloat())
    }

    fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) {
        // centre area
        batch.draw(baloonTile.get(1, 1), x, y, w, h)

        // edges
        batch.draw(baloonTile.get(1, 0), x, y - baloonTile.tileH, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(1, 2), x, y + h, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(0, 1), x - baloonTile.tileW, y, baloonTile.tileW.toFloat(), h)
        batch.draw(baloonTile.get(2, 1), x + w, y, baloonTile.tileW.toFloat(), h)

        // corners
        batch.draw(baloonTile.get(0, 0), x - baloonTile.tileW, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 0), x + w, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 2), x + w, y + h)
        batch.draw(baloonTile.get(0, 2), x - baloonTile.tileW, y + h)
    }

    private var init = false

    /**
     * Make sure App.resize is called first!
     */
    fun resize() {
        if (!init) {
            init = true
        }
        else {
            blurWriteQuad.dispose()
            blurWriteQuad2.dispose()
            blurWriteQuad4.dispose()
            fboBlur.dispose()
            fboBlurHalf.dispose()
            fboBlurQuarter.dispose()
        }

        blurWriteQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )
        blurWriteQuad2 = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )
        blurWriteQuad4 = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        val fw = App.scr.width//MathUtils.nextPowerOfTwo(App.scr.width)
        val fh = App.scr.height//MathUtils.nextPowerOfTwo(App.scr.height)

        fboBlur = FloatFrameBuffer(
                fw,
                fh,
                false
        )
        fboBlurHalf = FloatFrameBuffer(
                fw / 2,
                fh / 2,
                false
        )
        fboBlurQuarter = FloatFrameBuffer(
                fw / 4,
                fh / 4,
                false
        )

        blurWriteQuad.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                fw.toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                fw.toFloat(), fh.toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, fh.toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))

        blurWriteQuad2.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                fw.div(2).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                fw.div(2).toFloat(), fh.div(2).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, fh.div(2).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad2.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))

        blurWriteQuad4.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                fw.div(4).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                fw.div(4).toFloat(), fh.div(4).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f, fh.div(4).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad4.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))
    }
}