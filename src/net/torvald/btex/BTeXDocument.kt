package net.torvald.terrarum.btex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import net.torvald.btex.BTeXDocViewer
import net.torvald.terrarum.btex.BTeXDocument.Companion.HREF_UNDERLINE
import net.torvald.terrarum.btex.BTeXDocument.Companion.HREF_UNDERLINE_SHADOW
import net.torvald.terrarum.btex.BTeXDocument.Companion.UNDERLINE_Y
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClustfileOutputStream
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.toInt
import net.torvald.terrarum.tryDispose
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarumsansbitmap.MovableType
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.Deflater

/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXDocument : Disposable {
    var context = "tome" // tome (cover=hardcover), sheets (cover=typewriter or cover=printout), examination (def=examination)
    var font = "default" // default or typewriter
    var inner = "standard"
    var papersize = "standard"

    var theTitle = ""
    var theSubtitle = ""
    var theAuthor = ""
    var theEdition = ""

    var textWidth = 508
    val lineHeightInPx = 24
    var pageLines = 24
    val textHeight: Int
        get() = pageLines * lineHeightInPx

    val pageMarginH = 20
    val pageMarginV = 12

    val pageDimensionWidth: Int
        get() = 2 * pageMarginH + textWidth
    val pageDimensionHeight: Int
        get() = 2 * pageMarginV + textHeight + pageLines // add a pagenum row

    var endOfPageStart = 2147483647
    var tocPageStart = 2

    val indexTable = HashMap<String, Int>()

    internal var inputXML: String? = null

    companion object {
        val DEFAULT_PAGE_BACK = Color(0xe0dfdb_ff.toInt())
//        val DEFAULT_PAGE_FORE = Color(0x0a0706_ff)
        val DEFAULT_ORNAMENTS_COL = Color(0x3f3c3b_ff)
        val HREF_UNDERLINE = Color(0x0033BBff)
        val HREF_UNDERLINE_SHADOW = Color(0x7F99ddff) // blend=none makes transparency unusable
        val ccPagenum = TerrarumSansBitmap.toColorCode(0xf333)

        const val UNDERLINE_Y = 22

        private fun String.escape() = this.replace("\"", "\\\"")

        private fun newTempFile(name: String) = FileHandle.tempFile(name)

        fun fromFile(fileHandle: FileHandle) = fromFile(fileHandle.file())

        fun fromFile(file: File): BTeXDocument {
            val doc = BTeXDocument()

            val ra = RandomAccessFile(file, "r")
            val DOM = ClusteredFormatDOM(ra)

            val xml = Clustfile(DOM, "/src.xml")
            doc.inputXML = xml.readBytes().toString(Common.CHARSET)

            // get meta file
            val meta = Clustfile(DOM, "/bibliography.json")
            if (!meta.exists()) throw IllegalStateException("No bibliography.json found on the archive")
            val metaReader = meta.readBytes().toString(Common.CHARSET).reader()
            val metaJson = JsonFetcher.readFromJsonString(metaReader)

            doc.theTitle = metaJson["title"].asString()
            doc.theSubtitle = metaJson["subtitle"].asString()
            doc.theAuthor = metaJson["author"].asString()
            doc.theEdition = metaJson["edition"].asString()
            val pageCount = metaJson["pages"].asInt()
            doc.context = metaJson["context"].asString()
            doc.font = metaJson["font"].asString()
            doc.inner = metaJson["inner"].asString()
            doc.papersize = metaJson["papersize"].asString()
            doc.fromArchive = true
            doc.pageTextures = Array(pageCount) { null }

            println("Title: ${doc.theTitle}")
            println("Pages: $pageCount")

            for (page in 0 until pageCount) {
                Clustfile(DOM, "/${page}.png").also {
                    if (!it.exists()) throw IllegalStateException("No file '${page}.png' on the archive")

//                    val tempFile = newTempFile("btex-import") // must create new file descriptor for every page, or else every page will share a single file descriptor which cause problems
                    val tempFile = Gdx.files.external("./btex-import.tmp") // tmpfs not working???
                    it.exportFileTo(tempFile.file())
                    val texture = TextureRegion(Texture(tempFile))
                    doc.pageTextures[page] = texture

                    if (page == 0) {
                        doc.textWidth = texture.regionWidth - 2 * doc.pageMarginH
                        doc.pageLines = (texture.regionHeight - 2 * doc.pageMarginH) / doc.lineHeightInPx

                        println("Page dimension: (${texture.regionWidth}x${texture.regionHeight}) (${doc.pageDimensionWidth}x${doc.pageDimensionHeight})")
                    }
                    tempFile.delete() // deleting also affects file descriptor juggling
                }
            }

            doc.pages.let {
                it.clear()
                repeat(pageCount) { _ ->
                    it.add(BTeXPage(doc, Color.WHITE, doc.pageTextures[0]!!.regionWidth, doc.pageTextures[0]!!.regionHeight))
                }
            }

            // read hrefs.json
            val hrefs = Clustfile(DOM, "/hrefs.json")
            if (hrefs.exists()) {
                val hrefReader = hrefs.readBytes().toString(Common.CHARSET).reader()
                val hrefJson = JsonFetcher.readFromJsonString(hrefReader)

                JsonFetcher.forEachSiblings(hrefJson) { pageNum, value ->
                    val pageNum = pageNum.toInt()
                    JsonFetcher.forEachSiblings(value) { _, hrefObj ->
                        doc.pages[pageNum].appendClickable(
                            BTeXClickable(
                                hrefObj.get("x").asInt(),
                                hrefObj.get("y").asInt(),
                                hrefObj.get("w").asInt(),
                                hrefObj.get("h").asInt(),
                                false
                            ) {
                                hrefObj.get("a").asInt()
                            }
                        )
                    }
                }
            }

            ra.close()

            return doc
        }
    }

    internal val pages = ArrayList<BTeXPage>()

    private lateinit var pagePixmaps: Array<Pixmap?>
    private lateinit var pageTextures: Array<TextureRegion?>

    val currentPage: Int
        get() = pages.size - 1

    val currentPageObj: BTeXPage
        get() = pages[currentPage]

    val pageIndices: IntRange
        get() = if (fromArchive) pageTextures.indices else pages.indices

    internal val linesPrintedOnPage = ArrayList<Int>()

    @Transient private val fontNum = TinyAlphNum

    fun addNewPage(progressIndicator: AtomicInteger, back: Color = DEFAULT_PAGE_BACK) {
        pages.add(BTeXPage(this, back, pageDimensionWidth, pageDimensionHeight))
        linesPrintedOnPage.add(0)
        progressIndicator.getAndAdd(1)
    }

    fun addNewPageAt(progressIndicator: AtomicInteger, index: Int, back: Color = DEFAULT_PAGE_BACK) {
        pages.add(index, BTeXPage(this, back, pageDimensionWidth, pageDimensionHeight))
        linesPrintedOnPage.add(index, 0)
        progressIndicator.getAndAdd(1)
    }

    private val lock = Any()
    private val texturefiedPages = HashSet<Int>()

    /**
     * Must be called on a thread with GL context!
     */
    fun finalise(progressIndicator: AtomicInteger, multithread: Boolean = false) {
        synchronized(lock) {
            if (fromArchive) throw IllegalStateException("Document is loaded from the archive and thus cannot be finalised")
            if (isFinalised) throw IllegalStateException("Page is already been finalised")

            // serialise and finalise via CPU (store every page as Pixmap)

            pageTextures = Array(pages.size) { null }
            pagePixmaps = Array(pages.size) { null }

            if (!multithread) {
                pages.forEachIndexed { pageNum, page ->
                    val pixmap = Pixmap(pageDimensionWidth, pageDimensionHeight, Pixmap.Format.RGBA8888).also {
                        it.blending = Pixmap.Blending.SourceOver
                        it.filter = Pixmap.Filter.NearestNeighbour
                    }
                    page.renderToPixmap(pixmap, 0, 0, pageMarginH, pageMarginV)
                    printPageNumber(pixmap, pageNum, 0, 0)
                    pagePixmaps[pageNum] = pixmap
                    progressIndicator.getAndAdd(1)
                }
            }
            else {
                // my experiment tells 4, 8, 16, 32 threads all perform the same
                val THREAD_COUNT = Runtime.getRuntime().availableProcessors().div(2).coerceIn(1..4)
                val jobs = pages.indices.sliceEvenly(THREAD_COUNT).map { Callable { it.forEach { pageNum ->
                    val page = pages[pageNum]
                    val pixmap = Pixmap(pageDimensionWidth, pageDimensionHeight, Pixmap.Format.RGBA8888).also {
                        it.blending = Pixmap.Blending.SourceOver
                        it.filter = Pixmap.Filter.NearestNeighbour
                    }
                    page.renderToPixmap(pixmap, 0, 0, pageMarginH, pageMarginV)
                    printPageNumber(pixmap, pageNum, 0, 0)
                    pagePixmaps[pageNum] = pixmap
                    progressIndicator.getAndAdd(1)
                    Unit
                } } }

                ThreadExecutor(THREAD_COUNT).also {
                    it.renew()
                    it.submitAll(jobs)
                    it.join()
                }
            }

            isFinalised = true
        }
    }

    override fun dispose() {
        if (isFinalised) {
            pageTextures.forEach { it?.texture?.dispose() }
            pagePixmaps.forEach { it?.tryDispose() }
        }
        else if (fromArchive) {
            pageTextures.forEach { it?.texture?.dispose() }
            pagePixmaps.forEach { it?.tryDispose() }
        }
    }

    fun serialise(viewer: BTeXDocViewer, archiveFile: File) {
        if (!isFinalised) throw IllegalStateException("Document must be finalised before being serialised")

        val diskFile = ClusteredFormatDOM.createNewArchive(archiveFile, Common.CHARSET, "", 0x7FFFF)
        val DOM = ClusteredFormatDOM(diskFile)

        inputXML?.let { xmlstr ->
            Clustfile(DOM, "src.xml").also {
                it.createNewFile()
                it.writeBytes(xmlstr.toByteArray(Common.CHARSET))
            }
        }

        val json = """
            {
                "title":"${theTitle.escape()}",
                "subtitle":"${theSubtitle.escape()}",
                "author":"${theAuthor.escape()}",
                "edition":"${theEdition.escape()}",
                "pages":"${pageTextures.size}",
                "context":"${context.escape()}",
                "font":"${font.escape()}",
                "inner":"${inner.escape()}",
                "papersize":"${papersize.escape()}"
            }
        """.trimIndent()

        Clustfile(DOM, "bibliography.json").also {
            it.createNewFile()
            it.writeBytes(json.encodeToByteArray())
        }

        val json2 = StringBuilder(); json2.append("{\n")
        pages.forEachIndexed { index, page ->
            if (page.clickableElements.isNotEmpty()) {
                json2.append("\"$index\":[")
                page.clickableElements.forEach {
                    val objStr = "{\"x\":${it.posX},\"y\":${it.posY},\"w\":${it.width},\"h\":${it.height},\"a\":${it.getTargetPage(viewer)}},"
                    json2.append(objStr)
                }
                json2.deleteCharAt(json2.length - 1)
                json2.append("],\n")
            }
        }
        if (json2.length > 5) json2.deleteCharAt(json2.length - 2) // delete , but not \n
        json2.append("}")

        Clustfile(DOM, "hrefs.json").also {
            it.createNewFile()
            it.writeBytes(json2.toString().encodeToByteArray())
        }

        pagePixmaps.forEachIndexed { index, pixmap ->
            Clustfile(DOM, "$index.png").also { file ->
                file.createNewFile()
                val tempFile = newTempFile("btex-export.png")
                PixmapIO.writePNG(tempFile, pixmap, Deflater.BEST_COMPRESSION, false)
                val outstream = ClustfileOutputStream(file)
                outstream.write(tempFile.readBytes())
                outstream.flush(); outstream.close()
                tempFile.delete()
            }
        }

        DOM.changeDiskCapacity(diskFile.length().div(4096f).ceilToInt())
    }

    var isFinalised = false; private set
    var fromArchive = false; private set

    /**
     * Appends draw call to the list. The draw call must be prepared manually so that they would not overflow.
     * Use `addNewPage` to append the overflowing text to the next page.
     *
     * `currentLine` *will* be updated automatically.
     */
    fun appendDrawCall(page: BTeXPage, drawCall: DrawCallWrapper) {
        page.appendDrawCall(drawCall)

        val pagenum = pages.indexOf(page)
        linesPrintedOnPage[pagenum] += drawCall.lineCount
    }

    fun appendClickable(page: BTeXPage, clickable: BTeXClickable) {
        page.appendClickable(clickable)
    }

    fun render(frameDelta: Float, batch: SpriteBatch, page: Int, x: Int, y: Int) {
        batch.color = Color.WHITE

        if (fromArchive || isFinalised && texturefiedPages.contains(page))
            batch.draw(pageTextures[page], x.toFloat(), y.toFloat())
        else if (isFinalised && !texturefiedPages.contains(page)) {
            pageTextures[page] = TextureRegion(Texture(pagePixmaps[page]))
            texturefiedPages.add(page)
            batch.draw(pageTextures[page], x.toFloat(), y.toFloat())
        }
        else {
            pages[page].render(frameDelta, batch, x, y, pageMarginH, pageMarginV)
            printPageNumber(batch, page, x, y)
        }
    }

    private fun printPageNumber(batch: SpriteBatch, page: Int, x: Int, y: Int) {
        val num = "${page + 1}"
        val numW = TinyAlphNum.getWidth(num)
        val numX = if (context == "tome") {
            if (page % 2 == 1)
                x + pageMarginH
            else
                x + pageDimensionWidth - pageMarginH - numW
        }
        else {
            x + (pageDimensionWidth - numW) / 2
        }
        val numY = y + pageDimensionHeight - 2 * pageMarginV - 4

        if (page == 0 && context != "tome" || page in tocPageStart until endOfPageStart) {
            batch.color = DEFAULT_ORNAMENTS_COL
            TinyAlphNum.draw(batch, num, numX.toFloat(), numY.toFloat())
        }
    }

    private fun printPageNumber(pixmap: Pixmap, page: Int, x: Int, y: Int) {
        val num = "${page + 1}"
        val numW = TinyAlphNum.getWidth(num)
        val numX = if (context == "tome") {
            if (page % 2 == 1)
                x + pageMarginH
            else
                x + pageDimensionWidth - pageMarginH - numW
        }
        else {
            x + (pageDimensionWidth - numW) / 2
        }
        val numY = y + pageDimensionHeight - 2 * pageMarginV - 4

        if (page == 0 && context != "tome" || page in tocPageStart until endOfPageStart) {
            pixmap.setColor(DEFAULT_ORNAMENTS_COL)
            TinyAlphNum.drawToPixmap(pixmap, "$ccPagenum$num", numX, numY)
        }
    }
}

data class BTeXClickable(
    var posX: Int, var posY: Int, val width: Int, val height: Int, val drawUnderline: Boolean = true,
    val getTargetPage: (BTeXDocViewer) -> Int,
//    val onHover: () -> Unit = {}
) {
    var deltaX = 0
    var deltaY = 0

    fun debugDrawHitboxToPixmap(pixmap: Pixmap, doc: BTeXDocument) {
        pixmap.drawRectangle(
            posX - HBPADH + doc.pageMarginH,
            posY - HBPADV + doc.pageMarginV,
            width + HBPADH,
            height + 2 * HBPADV
        )
    }

    fun drawUnderline(pixmap: Pixmap, doc: BTeXDocument) {
        if (drawUnderline) {
            val x = posX - HBPADH + doc.pageMarginH
            val y = posY - HBPADV + doc.pageMarginV + UNDERLINE_Y
            val width = width + HBPADH

            pixmap.setColor(HREF_UNDERLINE_SHADOW)
            pixmap.drawRectangle(x, y, width + 1, 2)
            pixmap.setColor(HREF_UNDERLINE)
            pixmap.drawRectangle(x, y, width, 1)
        }
    }

    fun pointInHitbox(doc: BTeXDocument, x: Int, y: Int) =
        (x in posX - HBPADH + doc.pageMarginH until posX - HBPADH + doc.pageMarginH + width + HBPADH &&
        y in posY - HBPADV + doc.pageMarginV until posY - HBPADV + doc.pageMarginV + height + 2 * HBPADV)

    companion object {
        private const val HBPADH = 1
        private const val HBPADV = 1
    }
}

class BTeXPage(
    val doc: BTeXDocument,
    val back: Color,
    val width: Int,
    val height: Int,
) {
    internal val drawCalls = ArrayList<DrawCallWrapper>()
    internal val clickableElements = ArrayList<BTeXClickable>()

    fun appendDrawCall(drawCall: DrawCallWrapper) {
        if (drawCall.isNotBlank()) drawCalls.add(drawCall)
    }
    fun appendClickable(clickable: BTeXClickable) {
        if (clickable.width > 1)
            clickableElements.add(clickable)
    }

    private var prerender = false

    fun render(frameDelta: Float, batch: SpriteBatch, x: Int, y: Int, marginH: Int, marginV: Int) {
        if (!prerender) {
            prerender = true
            drawCalls.sortBy { if (it.text != null) 16 else 0 }
        }

        // paint background
        batch.color = back.cpy().also { it.a = 0.93f }
        Toolkit.fillArea(batch, x, y, width, height)

        // print texts
        batch.color = Color.WHITE
        drawCalls.forEach {
            it.draw(batch, x + marginH, y + marginV)
        }
    }

    fun touchDown(viewer: BTeXDocViewer, pageRelX: Int, pageRelY: Int, pointer: Int, button: Int) {
        // filter clickable elements that are under the cursor
        clickableElements.filter {
            it.pointInHitbox(doc, pageRelX, pageRelY)
        }.lastOrNull()?.let {
            val target = it.getTargetPage(viewer)
            viewer.gotoPage(target)
        }
    }

    fun isEmpty() = drawCalls.isEmpty()
    fun isNotEmpty() = drawCalls.isNotEmpty()

    fun renderToPixmap(pixmap: Pixmap, x: Int, y: Int, marginH: Int, marginV: Int) {
        drawCalls.sortedBy { if (it.text != null) 16 else 0 }.let { drawCalls ->
            // paint background
            val backCol = back.cpy().also { it.a = 0.93f }
            pixmap.blending = Pixmap.Blending.None
            pixmap.setColor(backCol)
            pixmap.fill()

            // debug underlines on clickableElements
            clickableElements.forEach {
                pixmap.setColor(HREF_UNDERLINE)
                it.drawUnderline(pixmap, doc)
            }

            // print texts
            pixmap.setColor(Color.WHITE)
            pixmap.blending = Pixmap.Blending.SourceOver
            drawCalls.forEach {
                it.drawToPixmap(pixmap, x + marginH, y + marginV)
            }
        }
    }
}


data class TypesetDrawCall(val movableType: MovableType, val rowStart: Int, val rows: Int) {
    fun getText(): List<CodepointSequence> = movableType.typesettedSlugs.subList(rowStart, minOf(movableType.typesettedSlugs.size, rowStart + rows))
    fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float) {
        movableType.draw(batch, x, y, rowStart, minOf(rows, doc.pageLines))
    }

    fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int) {
        movableType.drawToPixmap(pixmap, x, y, rowStart, minOf(rows, doc.pageLines))
    }
}

abstract class BatchDrawCall(
    val width: Int,
    val lineHeight: Int,
    val parentText: DrawCallWrapper?// = null
) {
    abstract fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap? = null)
    abstract fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int, font: TerrarumSansBitmap? = null)
}

class DrawCallWrapper(
    val doc: BTeXDocument,
    val pageObject: BTeXPage,
    var posX: Int, // position relative to the page start (excluding page margin)
    var posY: Int, // position relative to the page start (excluding page margin)
    val text: TypesetDrawCall? = null,
    val cmd: BatchDrawCall? = null,
    val font: TerrarumSansBitmap? = null
) {

    internal var deltaX = 0 // used by the BTexParser.typeset*()
    internal var deltaY = 0 // used by the BTexParser.typeset*()

    init {
        if (text != null && cmd != null) throw IllegalArgumentException("Text and Texture are both non-null")
    }

    fun draw(batch: SpriteBatch, x: Int, y: Int) {
        val px = (posX + x).toFloat()
        val py = (posY + y).toFloat()

        extraDrawFun(batch, px, py)

        batch.color = Color.WHITE

        if (text != null && cmd == null) {
            text.draw(doc, batch, px, py)
        }
        else if (text == null && cmd != null) {
            cmd.draw(doc, batch, px, py, font)
        }
        else throw Error("Text and Texture are both non-null")
    }

    fun isNotBlank(): Boolean {
        if (text == null && cmd == null) return false
        if (text is TypesetDrawCall && text.movableType.inputText.isBlank()) return false
//        if (text is RaggedLeftDrawCall && text.raggedType.inputText.isBlank()) return false
        return true
    }

    fun drawToPixmap(pixmap: Pixmap, x: Int, y: Int) {
        val px = posX + x
        val py = posY + y

        extraPixmapDrawFun(pixmap, px, py)

        pixmap.setColor(Color.WHITE)

        if (text != null && cmd == null) {
            text.drawToPixmap(doc, pixmap, px, py)
        }
        else if (text == null && cmd != null) {
            cmd.drawToPixmap(doc, pixmap, px, py, font)
        }
        else throw Error("Text and Texture are both non-null")
    }

    internal val width: Int
        get() = if (text != null)
            text.movableType.width * text.movableType.font.scale
        else
            cmd!!.width

    internal var extraDrawFun: (SpriteBatch, Float, Float) -> Unit = { _, _, _ ->}
    internal var extraPixmapDrawFun: (Pixmap, Int, Int) -> Unit = { _, _, _ ->}
    internal val lineCount = if (text != null)
        text.rows
    else
        cmd!!.lineHeight

    companion object {
        private fun CodepointSequence.isBlank() = this.all { whitespaces.contains(it) }
        private val whitespaces = (listOf(0x00, 0x20, 0x3000, 0xA0, 0xAD) + (0x2000..0x200F) + (0x202A..0x202F) + (0x205F..0x206F) + (0xFFFE0..0xFFFFF)).toHashSet()
    }
}