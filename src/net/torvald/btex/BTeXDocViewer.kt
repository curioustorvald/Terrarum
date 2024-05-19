package net.torvald.btex

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.inUse

/**
 * Created by minjaesong on 2024-05-17.
 */
class BTeXDocViewer(val doc: BTeXDocument) {

    private val pageGap = 6
    private var currentPage = 0
    private val isTome = (doc.context == "tome")

    val pageCount = doc.pageIndices.endInclusive + 1

    fun gotoPage(page: Int) {
        val page = page.coerceIn(doc.pageIndices)
        if (isTome)
            currentPage = (page / 2) * 2
        else
            currentPage = page
    }
    fun getPageOfIndex(id: String): Int {
        return doc.indexTable[id]!!
    }

    fun currentPageStr(): String {
        // TODO non-tome
        if (isTome) {
            return if (currentPage == 0)
                "1"
            else
                "${currentPage}-${currentPage+1}"
        }
        else {
            return (currentPage + 1).toString()
        }
    }

    private var x1: Int = 0
    private var x2: Int = 0
    private var y: Int = 0

    /**
     * @param x top-centre
     * @param y top-centre
     */
    fun render(batch: SpriteBatch, x: Float, y: Float) {
        x1 = if (isTome)
            x.toInt() - pageGap/2 - doc.pageDimensionWidth
        else
            x.toInt() - doc.pageDimensionWidth / 2

        x2 = if (isTome)
            x.toInt() + pageGap/2
        else
            0

        this.y = y.toInt()

        if (doc.isFinalised || doc.fromArchive) {
            if (isTome) {
                batch.color = Color.WHITE

                if (currentPage - 1 in doc.pageIndices)
                    doc.render(0f, batch, currentPage - 1, x1, this.y)
                if (currentPage in doc.pageIndices)
                    doc.render(0f, batch, currentPage, x2, this.y)
            }
            else {
                batch.color = Color.WHITE

                if (currentPage in doc.pageIndices)
                    doc.render(0f, batch, currentPage, x1, this.y)
            }
        }
    }

    private var clickLatched = false//true

    fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) {
        if (!clickLatched) {
            clickLatched = true

            if (isTome) {
                if (currentPage - 1 in doc.pageIndices)
                    doc.pages[currentPage - 1].touchDown(this, screenX - x1, screenY - y, pointer, button)
                if (currentPage in doc.pageIndices)
                    doc.pages[currentPage].touchDown(this, screenX - x2, screenY - y, pointer, button)
            }
            else {
                if (currentPage in doc.pageIndices)
                    doc.pages[currentPage].touchDown(this, screenX - x1, screenY - y, pointer, button)
            }
        }
    }

    fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) {
        clickLatched = false
    }

    fun prevPage() {
        if (isTome) {
            currentPage = (currentPage - 2).coerceAtLeast(0)
        }
        else {
            currentPage = (currentPage - 1).coerceAtLeast(0)
        }
    }

    fun nextPage() {
        if (isTome) {
            currentPage = (currentPage + 2).coerceAtMost(doc.pageIndices.endInclusive.toFloat().div(2f).ceilToInt().times(2))
        }
        else {
            currentPage = (currentPage + 1).coerceAtLeast(doc.pageIndices.endInclusive)
        }
    }

    fun gotoFirstPage() {
        gotoPage(0)
    }

    fun gotoLastPage() {
        if (isTome) {
            currentPage = doc.pageIndices.endInclusive.toFloat().div(2f).ceilToInt().times(2)
        }
        else {
            gotoPage(doc.pageIndices.endInclusive)
        }
    }
}