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
    fun gotoIndex(id: String) {
        gotoPage(doc.indexTable[id]!!)
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


    /**
     * @param x top-centre
     * @param y top-centre
     */
    fun render(batch: SpriteBatch, x: Float, y: Float) {
        val x1 = if (isTome)
            x.toInt() - pageGap/2 - doc.pageDimensionWidth
        else
            x.toInt() - doc.pageDimensionWidth / 2

        val x2 = if (isTome)
            x.toInt() + pageGap/2
        else
            0

        val y = y.toInt()

        if (doc.isFinalised || doc.fromArchive) {
            if (isTome) {
                batch.color = Color.WHITE

                if (currentPage - 1 in doc.pageIndices)
                    doc.render(0f, batch, currentPage - 1, x1, y)
                if (currentPage in doc.pageIndices)
                    doc.render(0f, batch, currentPage, x2, y)
            }
            else {
                batch.color = Color.WHITE

                if (currentPage in doc.pageIndices)
                    doc.render(0f, batch, currentPage, x1, y)
            }
        }
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