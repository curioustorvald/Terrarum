package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.imagefont.BigAlphNum
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt


/**
 * Created by minjaesong on 2016-08-04.
 */
object Toolkit : Disposable {

    object Theme {
        val COL_INVENTORY_CELL_BORDER = Color(1f, 1f, 1f, 0.25f)
        val COL_CELL_FILL = Color(0x232528C8)
        val COL_CELL_FILL_ALT = Color(0x3c3835C8)
        val COL_CELL_FILL_OPAQUE = Color(0x232528FF)

        val COL_LIST_DEFAULT = Color.WHITE // white
        val COL_INACTIVE = Color.LIGHT_GRAY
        val COL_SELECTED = Color(0x00f8ff_ff) // cyan, HIGHLY SATURATED
        val COL_MOUSE_UP = Color(0xfff066_ff.toInt()) // yellow (all yellows are of low saturation according to the colour science)
        val COL_DISABLED = Color(0xaaaaaaff.toInt())
        val COL_RED = Color(0xff8888ff.toInt())
        val COL_REDD = Color(0xff4448ff.toInt())

        /*
        Try this for alt colour set:

        COL_SELECTED: FFE800 oklch(92% 0.19255 102) saturated yellow
        COL_MOUSE_UP: 55ECFE oklch(87% 0.127 207) whitened and slightly desaturated cyan

         */
    }
    private lateinit var fboBlur: Float16FrameBuffer

//    val baloonTile = TextureRegionPack("assets/graphics/gui/message_black_tileable.tga", 36, 36)
    val shadowTile = TextureRegionPack("assets/graphics/gui/blur_shadow.tga", 32, 32)

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
//        baloonTile.dispose()
        textureWhiteSquare.dispose()
        textureWhiteCircle.dispose()
    }

    val drawWidth: Int
        get() = App.scr.width - if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth else 0
    val drawWidthf: Float
        get() = drawWidth.toFloat()
    val hdrawWidth: Int
        get() = drawWidth / 2
    val hdrawWidthf: Float
        get() = hdrawWidth.toFloat()

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

    fun drawTextCentered(batch: SpriteBatch, font: TerrarumSansBitmap, text: String, tbw: Int, tbx: Int, tby: Int) {
        val tw = font.getWidth(text)
        font.draw(batch, text, tbx + (tbw - tw) / 2, tby)
    }
    fun drawTextCentered(batch: SpriteBatch, font: TinyAlphNum, text: String, tbw: Int, tbx: Int, tby: Int) {
        val tw = font.getWidth(text)
        font.draw(batch, text, (tbx + (tbw - tw) / 2).toFloat(), tby.toFloat())
    }
    fun drawTextCentered(batch: SpriteBatch, font: BigAlphNum, text: String, tbw: Int, tbx: Int, tby: Int) {
        val tw = font.getWidth(text)
        font.draw(batch, text, (tbx + (tbw - tw) / 2).toFloat(), tby.toFloat())
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
    fun drawStraightLine(batch: SpriteBatch, x: Float, y: Float, otherEnd: Float, thickness: Float, isVertical: Boolean) {
        if (!isVertical)
            fillArea(batch, x, y, otherEnd - x, thickness)
        else
            fillArea(batch, x, y, thickness, otherEnd - y)
    }
    fun drawStraightLine(batch: SpriteBatch, x: Int, y: Int, otherEnd: Int, thickness: Int, isVertical: Boolean) {
        drawStraightLine(batch, x.toFloat(), y.toFloat(), otherEnd.toFloat(), thickness.toFloat(), isVertical)
    }

    fun drawBoxBorder(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) =
            drawBoxBorder(batch, x.roundToInt(), y.roundToInt(), w.roundToInt(), h.roundToInt())

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

    // draws highly simplified box border
    fun drawBoxBorderToPixmap(pixmap: Pixmap, x: Int, y: Int, w: Int, h: Int) {
        // top edge
        pixmap.fillRectangle(x, y - 1, w, 1)
        // bottom edge
        pixmap.fillRectangle(x, y + h, w, 1)
        // left edge
        pixmap.fillRectangle(x - 1, y, 1, h)
        // right edge
        pixmap.fillRectangle(x + w, y, 1, h)
    }

    fun blurEntireScreen(batch: SpriteBatch, blurRadius0: Float, x: Int, y: Int, w: Int, h: Int) {
        batch.end()

        val renderTarget = FrameBufferManager.peek()
        BlurMgr.makeBlur(renderTarget, fboBlur, blurRadius0)

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

        batch.shader = null
        batch.begin()
        (batch as FlippingSpriteBatch).drawFlipped(fboBlur.colorBufferTexture, x.toFloat(), y.toFloat())
    }

    fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float, opacity: Float = 1f) {
        // centre area
        /*batch.draw(baloonTile.get(1, 1), x, y, w, h)

        // edges
        batch.draw(baloonTile.get(1, 0), x, y - baloonTile.tileH, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(1, 2), x, y + h, w, baloonTile.tileH.toFloat())
        batch.draw(baloonTile.get(0, 1), x - baloonTile.tileW, y, baloonTile.tileW.toFloat(), h)
        batch.draw(baloonTile.get(2, 1), x + w, y, baloonTile.tileW.toFloat(), h)

        // corners
        batch.draw(baloonTile.get(0, 0), x - baloonTile.tileW, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 0), x + w, y - baloonTile.tileH)
        batch.draw(baloonTile.get(2, 2), x + w, y + h)
        batch.draw(baloonTile.get(0, 2), x - baloonTile.tileW, y + h)*/


        batch.color = Theme.COL_CELL_FILL_OPAQUE.cpy().mul(1f,1f,1f,opacity)
        fillArea(batch, x - 4, y - 4, w + 8, h + 8)
        batch.color = Theme.COL_INACTIVE.cpy().mul(1f,1f,1f,opacity)
        drawBoxBorder(batch, x - 4, y - 4, w + 8, h + 8)
    }

    fun drawBlurShadowBack(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float) {
        val x = x - 2
        val y = y + 4
        val w = w + 4

        // centre area
        batch.draw(shadowTile.get(1, 1), x, y, w, h)

        // edges
        batch.draw(shadowTile.get(1, 0), x, y - shadowTile.tileH, w, shadowTile.tileH.toFloat())
        batch.draw(shadowTile.get(1, 2), x, y + h, w, shadowTile.tileH.toFloat())
        batch.draw(shadowTile.get(0, 1), x - shadowTile.tileW, y, shadowTile.tileW.toFloat(), h)
        batch.draw(shadowTile.get(2, 1), x + w, y, shadowTile.tileW.toFloat(), h)

        // corners
        batch.draw(shadowTile.get(0, 0), x - shadowTile.tileW, y - shadowTile.tileH)
        batch.draw(shadowTile.get(2, 0), x + w, y - shadowTile.tileH)
        batch.draw(shadowTile.get(2, 2), x + w, y + h)
        batch.draw(shadowTile.get(0, 2), x - shadowTile.tileW, y + h)
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
            fboBlur.dispose()
        }

        val fw = App.scr.width//MathUtils.nextPowerOfTwo(App.scr.width)
        val fh = App.scr.height//MathUtils.nextPowerOfTwo(App.scr.height)

        fboBlur = Float16FrameBuffer(
                fw,
                fh,
                false
        )
    }
}