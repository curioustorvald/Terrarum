package net.torvald.btex

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.App
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.btex.BTeXDrawCall
import net.torvald.terrarum.gameitems.ItemID
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.*
import java.util.*
import javax.xml.parsers.SAXParserFactory

/**
 * Created by minjaesong on 2023-10-28.
 */
object BTeXParser {

    operator fun invoke(file: FileHandle) = invoke(file.file())

    operator fun invoke(file: File): BTeXDocument {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newDefaultInstance().newSAXParser()
        val stream = FileInputStream(file)
        parser.parse(stream, BTeXHandler(doc))
        return doc
    }

    private class BTeXHandler(val doc: BTeXDocument) : DefaultHandler() {
        private val DEFAULT_FONTCOL = Color(0x222222ff)
        private val LINE_HEIGHT = 24

        private var cover = ""
        private var inner = ""
        private var papersize = ""
        private var def = ""

        private var pageWidth = 420
        private var pageLines = 25
        private var pageHeight = 25 * LINE_HEIGHT

        private val blockLut = HashMap<String, ItemID>()

        private val tagStack = ArrayList<String>() // index zero should be "btex"


        private var spanColour: String? = null


        private var typeX = 0
        private var typeY = 0

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            val tag = qName; if (tagStack.isEmpty() && tag != "btex") throw BTeXParsingException("Document is not btex")
            tagStack.add(tag)

            val attribs = HashMap<String, String>().also {
                it.putAll((0 until attributes.length).map { attributes.getQName(it) to attributes.getValue(it) })
            }

            val mode = tagStack.getOrNull(1)

            when (tag) {
                "btex" -> {
                    if (attribs.containsKey("def"))
                        def = attribs["def"]!!
                    else {
                        cover = attribs["cover"] ?: "printout"
                        inner = attribs["inner"] ?: "standard"
                        papersize = attribs["papersize"] ?: "standard"

                        pageWidth = pageWidthMap[papersize]!!
                        pageLines = pageHeightMap[papersize]!!
                        pageHeight = pageLines * LINE_HEIGHT

                        doc.pageWidth = pageWidth
                        doc.pageHeight = pageHeight
                    }
                }
                "pair" -> {
                    if (tagStack.size == 3 && mode == "blocklut") {
                        blockLut[attribs["key"]!!] = attribs["value"]!!
                    }
                    else {
                        throw BTeXParsingException("<pair> used outside of <blocklut>")
                    }
                }
                "span" -> {
                    attribs["span"]?.let {
                        spanColour = it
                    }
                }

            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            tagStack.removeLast()

            when (qName) {
                "span" -> {
                    spanColour = null
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            val str = String(ch.sliceArray(start until start+length)).replace('\n',' ').replace(Regex(" +"), " ").trim()
            val font = getFont()
            advanceCursorPre(font.getWidth(str), 0)
            doc.appendDrawCall(BTeXDrawCall(typeX, typeY, inner, getSpanColour(), font, str))
            advanceCursorPost(font.getWidth(str), 0)
        }

        private fun advanceCursorPre(w: Int, h: Int) {
            if (typeX + w > pageWidth) {
                typeY += LINE_HEIGHT
                typeX = 0
            }

            if (typeY + h > pageHeight) {
                typeX = 0
                typeY = 0
                doc.addNewPage()
            }
        }

        private fun advanceCursorPost(w: Int, h: Int) {
            typeX += w
            typeY += h
        }


        private fun getFont() = when (cover) {
            "typewriter" -> TODO()
            else -> App.fontGame
        }

        private fun getSpanColour(): Color = spanColourMap.getOrDefault(spanColour, DEFAULT_FONTCOL)

        private val spanColourMap = hashMapOf(
            "grey" to Color.LIGHT_GRAY
        )

        private val pageWidthMap = hashMapOf(
            "standard" to 420
        )
        private val pageHeightMap = hashMapOf(
            "standard" to 25
        )
    }

}


class BTeXParsingException(s: String) : RuntimeException(s) {

}
