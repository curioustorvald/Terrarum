package net.torvald.terrarum.btex

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.ui.Toolkit

/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXDocument {
    var context = "tome" // tome (cover=hardcover), sheets (cover=typewriter or cover=printout), examination (def=examination)
    var font = "default" // default or typewriter
    var inner = "standard"
    var papersize = "standard"

    var pageWidth = 420
    var pageHeight = 25 * 24

    companion object {
        val DEFAULT_PAGE_BACK = Color(0xe1e1d7ff.toInt())
        val DEFAULT_PAGE_FORE = Color(0x131311ff)
    }

    private val pages = ArrayList<BTeXPage>()



    fun addNewPage(back: Color = DEFAULT_PAGE_BACK) {
        pages.add(BTeXPage(back, pageWidth, pageHeight))
    }

    fun appendDrawCall(drawCall: BTeXDrawCall) {
        pages.last().appendDrawCall(drawCall)
    }

    fun render(batch: SpriteBatch, page: Int, x: Int, y: Int) {
        pages[page].render(batch, x, y)
    }
}

class BTeXPage(
    val back: Color,
    val width: Int,
    val height: Int,
) {
    private val drawCalls = ArrayList<BTeXDrawCall>()

    fun appendDrawCall(drawCall: BTeXDrawCall) {
        drawCalls.add(drawCall)
    }

    fun render(batch: SpriteBatch, x: Int, y: Int) {
        batch.color = back
        Toolkit.fillArea(batch, x, y, width, height)
        drawCalls.forEach {
            it.draw(batch, x, y)
        }
    }
}

class BTeXDrawCall(
    val posX: Int,
    val posY: Int,
    val theme: String,
    val colour: Color,
    val font: BitmapFont,
    val text: String? = null,
    val texture: TextureRegion? = null,
) {

    fun draw(batch: SpriteBatch, x: Int, y: Int) {
        val px = (posX + x).toFloat()
        val py = (posY + y).toFloat()

        if (theme == "code") {
            // todo draw code background
        }

        batch.color = colour

        if (text != null && texture == null) {
            font.draw(batch, text, px, py)
        }
        else if (text == null && texture != null) {
            batch.draw(texture, px, py)
        }
        else throw Error("Text and Texture are both non-null")
    }

}