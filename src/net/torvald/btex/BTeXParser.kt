package net.torvald.btex

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.App
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.gameitems.ItemID
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.*
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

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

    operator fun invoke(string: String): BTeXDocument {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newDefaultInstance().newSAXParser()
        parser.parse(InputSource(StringReader(string)), BTeXHandler(doc))
        return doc
    }

    internal class BTeXHandler(val doc: BTeXDocument) : DefaultHandler() {
        private val DEFAULT_FONTCOL = Color(0x222222ff)
        private val LINE_HEIGHT = doc.lineHeight

        private var cover = ""
        private var inner = ""
        private var papersize = ""
        private var def = ""

        private var btexOpened = false

        private var pageWidth = doc.pageWidth
        private var pageLines = doc.pageLines
        private var pageHeight = doc.pageHeight

        private val blockLut = HashMap<String, ItemID>()

        private val tagStack = ArrayList<String>() // index zero should be "btex"


        private var spanColour: String? = null


        private var typeX = 0
        private var typeY = 0

        private val elemOpeners: HashMap<String, KFunction<*>> = HashMap()
        private val elemClosers: HashMap<String, KFunction<*>> = HashMap()

        init {
            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<OpenTag>() != null }.forEach {
                println("Tag opener: ${it.name}")
                elemOpeners[it.name] = it
            }

            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<CloseTag>() != null }.forEach {
                println("Tag closer: ${it.name}")
                elemClosers[it.name] = it
            }
        }

        private fun printdbg(message: String?) {
            val CSI = "\u001B[32m"
            val timeNow = System.currentTimeMillis()
            val ss = (timeNow / 1000) % 60
            val mm = (timeNow / 60000) % 60
            val hh = (timeNow / 3600000) % 24
            val ms = timeNow % 1000
            val out = this.javaClass.getSimpleName()
            val prompt = CSI + String.format("%02d:%02d:%02d.%03d [%s]%s ", hh, mm, ss, ms, out, App.csi0)
            if (message == null) {
                println(prompt + "null")
            }
            else {
                val indentation = " ".repeat(out.length + 16)
                val msgLines: Array<String> = message.toString().split("\\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (i in msgLines.indices) {
                    println((if (i == 0) prompt else indentation) + msgLines[i])
                }
            }
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            val tag = qName; if (tagStack.isEmpty() && tag.lowercase() != "btexdoc") throw BTeXParsingException("Document is not BTeX")
            tagStack.add(tag)

            val attribs = HashMap<String, String>().also {
                it.putAll((0 until attributes.length).map { attributes.getQName(it) to attributes.getValue(it) })
            }

            val theTag = tag.uppercase()

            elemOpeners["processElem$theTag"].let {
                if (it == null)
                    System.err.println("Unknown tag: $theTag")
                else {
                    try {
                        it.call(this, this, doc, theTag, uri, attribs)
                    }
                    catch (e: Throwable) {
                        throw BTeXParsingException(e.stackTraceToString())
                    }
                }
            }

//            printdbg("Start element \t($tag)")
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            val popped = tagStack.removeLast()

            val theTag = qName.uppercase()

            elemClosers["closeElem$theTag"].let {
                try {
                    it?.call(this, this, doc, theTag, uri)
                }
                catch (e: Throwable) {
                    throw BTeXParsingException(e.stackTraceToString())
                }
            }

//            printdbg("  End element \t($popped)")
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            val str =
                String(ch.sliceArray(start until start + length)).replace('\n', ' ').replace(Regex(" +"), " ")//.trim()

            if (str.isNotBlank()) {
                printdbg("Characters \t\"$str\"")
            }
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


        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {
            if (handler.btexOpened) {
                throw BTeXParsingException("BTEXDOC tag has already opened")
            }

            if (attribs.containsKey("def"))
                handler.def = attribs["def"]!!
            else {
                handler.cover = attribs["cover"] ?: "printout"
                handler.inner = attribs["inner"] ?: "standard"
                handler.papersize = attribs["papersize"] ?: "standard"

                //change the "default values" of the document
                handler.pageWidth = pageWidthMap[papersize]!!
                handler.pageLines = pageHeightMap[papersize]!!
                handler.pageHeight = pageLines * LINE_HEIGHT

                doc.pageWidth = pageWidth
                doc.pageHeight = pageHeight
            }

            handler.btexOpened = true

            printdbg("BTeX document: def=${handler.def}, cover=${handler.cover}, inner=${handler.inner}, papersize=${handler.papersize}, dim=${handler.pageWidth}x${handler.pageHeight} (${handler.pageLines} lines)")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemPAIR(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {
            if (tagStack.size == 3 && tagStack.getOrNull(1) == "blocklut") {
                blockLut[attribs["key"]!!] = attribs["value"]!!
            }
            else {
                throw BTeXParsingException("<pair> used outside of <blocklut>")
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemSPAN(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {
            attribs["span"]?.let {
                spanColour = it
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFCONTENTS(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {
            // TODO add post-parsing hook to the handler
        }






        @OpenTag // reflective access is impossible with 'private'
        fun processElemBR(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {

        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemNEWPAGE(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {

        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemP(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {

        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemP(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String) {

        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemARST(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>) {

        }



        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSPAN(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String) {
            spanColour = null
        }
    }


    private annotation class OpenTag
    private annotation class CloseTag

}


class BTeXParsingException(s: String) : RuntimeException(s) {

}
