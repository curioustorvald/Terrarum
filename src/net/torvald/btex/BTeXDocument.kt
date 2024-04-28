package net.torvald.terrarum.btex

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.blendNormalPremultAlpha
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.MovableType
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence

/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXDocument {
    var context = "tome" // tome (cover=hardcover), sheets (cover=typewriter or cover=printout), examination (def=examination)
    var font = "default" // default or typewriter
    var inner = "standard"
    var papersize = "standard"

    var textWidth = 480
    var lineHeightInPx = 24
    var pageLines = 25
    val textHeight: Int
        get() = pageLines * lineHeightInPx

    val pageMarginH = 15
    val pageMarginV = 12

    val pageDimensionWidth: Int
        get() = 2 * pageMarginH + textWidth
    val pageDimensionHeight: Int
        get() = 2 * pageMarginV + textHeight

    companion object {
        val DEFAULT_PAGE_BACK = Color(0xe0dfdb_ff.toInt())
        val DEFAULT_PAGE_FORE = Color(0x0a0706_ff)
    }

    internal val pages = ArrayList<BTeXPage>()

    val currentPage: Int
        get() = pages.size - 1

    val currentPageObj: BTeXPage
        get() = pages[currentPage]

    val pageIndices: IntRange
        get() = pages.indices

    internal val linesPrintedOnPage = ArrayList<Int>()

    fun addNewPage(back: Color = DEFAULT_PAGE_BACK) {
        pages.add(BTeXPage(back, pageDimensionWidth, pageDimensionHeight))
        linesPrintedOnPage.add(0)
    }

    fun addNewPageAt(index: Int, back: Color = DEFAULT_PAGE_BACK) {
        pages.add(index, BTeXPage(back, pageDimensionWidth, pageDimensionHeight))
        linesPrintedOnPage.add(index, 0)
    }

    /**
     * Appends draw call to the list. The draw call must be prepared manually so that they would not overflow.
     * Use `addNewPage` to append the overflowing text to the next page.
     *
     * `currentLine` *will* be updated automatically.
     */
    fun appendDrawCall(drawCall: BTeXDrawCall) {
        pages.last().appendDrawCall(drawCall)

        linesPrintedOnPage[linesPrintedOnPage.lastIndex] += drawCall.lineCount
    }

    fun appendDrawCall(page: BTeXPage, drawCall: BTeXDrawCall) {
        page.appendDrawCall(drawCall)

        val pagenum = pages.indexOf(page)
        linesPrintedOnPage[pagenum] += drawCall.lineCount
    }

    fun render(frameDelta: Float, batch: SpriteBatch, page: Int, x: Int, y: Int) {
        pages[page].render(frameDelta, batch, x, y, pageMarginH, pageMarginV)
    }
}

class BTeXPage(
    val back: Color,
    val width: Int,
    val height: Int,
) {
    internal val drawCalls = ArrayList<BTeXDrawCall>()

    fun appendDrawCall(drawCall: BTeXDrawCall) {
        if (drawCall.isNotBlank()) drawCalls.add(drawCall)
    }

    fun render(frameDelta: Float, batch: SpriteBatch, x: Int, y: Int, marginH: Int, marginV: Int) {
        batch.color = back.cpy().also { it.a = 0.93f }
        Toolkit.fillArea(batch, x, y, width, height)
        batch.color = Color.WHITE
        drawCalls.forEach {
            it.draw(batch, x + marginH, y + marginV)
        }
    }

    fun isEmpty() = drawCalls.isEmpty()
    fun isNotEmpty() = drawCalls.isNotEmpty()
}

interface BTeXTextDrawCall {
    val rowStart: Int
    val rows: Int
    fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float)
}

data class MovableTypeDrawCall(val movableType: MovableType, override val rowStart: Int, override val rows: Int): BTeXTextDrawCall {
    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float) {
        movableType.draw(batch, x, y, rowStart, minOf(rows, doc.pageLines))
    }
}

/*data class RaggedLeftDrawCall(val raggedType: RaggedType, override val rowStart: Int, override val rowEnd: Int): BTeXTextDrawCall {
    override fun draw(batch: SpriteBatch, x: Float, y: Float) {
        raggedType.draw(batch, x, y, rowStart, rowEnd)
    }
}*/

class BTeXDrawCall(
    val doc: BTeXDocument,
    var posX: Int, // position relative to the page start (excluding page margin)
    var posY: Int, // position relative to the page start (excluding page margin)
    val theme: String,
    var colour: Color,
    val text: BTeXTextDrawCall? = null,
    val texture: TextureRegion? = null,
) {

    init {
        if (text != null && texture != null) throw IllegalArgumentException("Text and Texture are both non-null")
    }

    fun draw(batch: SpriteBatch, x: Int, y: Int) {
        val px = (posX + x).toFloat()
        val py = (posY + y).toFloat()

        if (theme == "code") {
            // todo draw code background
            println("code themed")
        }

        batch.color = colour

        if (text != null && texture == null) {
            text.draw(doc, batch, px, py)
        }
        else if (text == null && texture != null) {
            batch.draw(texture, px, py)
        }
        else throw Error("Text and Texture are both non-null")

        extraDrawFun(batch, px, py)
    }

    fun isNotBlank(): Boolean {
        if (text == null && texture == null) return false
        if (text is MovableTypeDrawCall && text.movableType.inputText.isBlank()) return false
//        if (text is RaggedLeftDrawCall && text.raggedType.inputText.isBlank()) return false
        return true
    }

    internal val width: Int
        get() = if (text != null)
            if (text is MovableTypeDrawCall)
                text.movableType.width
            else
                TODO()
        else
            texture!!.regionWidth

    internal var extraDrawFun: (SpriteBatch, Float, Float) -> Unit = { _, _, _ ->}
    internal val lineCount = if (text != null)
        text.rows
    else
        TODO()

    companion object {
        private fun CodepointSequence.isBlank() = this.all { whitespaces.contains(it) }
        private val whitespaces = (listOf(0x00, 0x20, 0x3000, 0xA0, 0xAD) + (0x2000..0x200F) + (0x202A..0x202F) + (0x205F..0x206F) + (0xFFFE0..0xFFFFF)).toHashSet()
    }
}