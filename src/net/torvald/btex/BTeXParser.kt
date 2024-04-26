package net.torvald.btex

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.jme3.math.FastMath.DEG_TO_RAD
import net.torvald.colourutil.OKLch
import net.torvald.colourutil.tosRGB
import net.torvald.terrarum.App
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.btex.BTeXDocument.Companion.DEFAULT_PAGE_FORE
import net.torvald.terrarum.btex.BTeXDrawCall
import net.torvald.terrarum.btex.BTeXPage
import net.torvald.terrarum.btex.MovableTypeDrawCall
import net.torvald.terrarum.ceilToFloat
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.MovableType
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.unicode.toUTF8Bytes
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import kotlin.math.absoluteValue
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

/**
 * Created by minjaesong on 2023-10-28.
 */
object BTeXParser {

    internal val textTags = hashSetOf("P", "TITLE", "AUTHOR", "EDITION", "CHAPTER", "SECTION")
    internal val textDecorTags = hashSetOf("SPAN", "CODE")

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
        private val DEFAULT_FONTCOL = DEFAULT_PAGE_FORE
        private val LINE_HEIGHT = doc.lineHeightInPx

        private var cover = ""
        private var inner = ""
        private var papersize = ""
        private var def = ""

        private var btexOpened = false

        private var pageWidth = doc.textWidth
        private var pageLines = doc.pageLines
        private var pageHeight = doc.textHeight

        private val blockLut = HashMap<String, ItemID>()

        private val tagStack = ArrayList<String>() // index zero should be "btex"

        private var currentTheme = ""
        private var spanColour: String? = null


        private var typeX = 0
        private var typeY = 0

        private val elemOpeners: HashMap<String, KFunction<*>> = HashMap()
        private val elemClosers: HashMap<String, KFunction<*>> = HashMap()

        private val paragraphBuffer = StringBuilder()

        private var lastTagAtDepth = Array(24) { "" }
        private var pTagCntAtDepth = IntArray(24)

        private val indexMap = HashMap<String, Int>() // id to pagenum
        private val cptSectMap = ArrayList<Triple<String, String, Int>>() // type ("chapter", "section"), name, pagenum in zero-based index
        private var tocPage: BTeXPage? = null

        init {
            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<OpenTag>() != null }.forEach {
//                println("Tag opener: ${it.name}")
                elemOpeners[it.name] = it
            }

            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<CloseTag>() != null }.forEach {
//                println("Tag closer: ${it.name}")
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
            val tag = qName
            if (tagStack.isEmpty() && tag.lowercase() != "btexdoc") throw BTeXParsingException("Document is not BTeX")
            val theTag = tag.uppercase()

            if (tagStack.isNotEmpty() && tagStack.any { textTags.contains(it) } && textTags.contains(theTag))
                throw IllegalStateException("Text tag '$theTag' used inside of text tags (tag stack is ${tagStack.joinToString()})")
            if (tagStack.isNotEmpty() && !textTags.contains(tagStack.last()) && textDecorTags.contains(theTag))
                throw IllegalStateException("Text decoration tag '$theTag' used outside of a text tag (tag stack is ${tagStack.joinToString()})")

            if (lastTagAtDepth[tagStack.size] != "P") pTagCntAtDepth[tagStack.size] = 0
            if (theTag == "P") pTagCntAtDepth[tagStack.size] += 1
            lastTagAtDepth[tagStack.size] = theTag

            tagStack.add(theTag)

            val attribs = HashMap<String, String>().also {
                it.putAll((0 until attributes.length).map { attributes.getQName(it) to attributes.getValue(it) })
            }


            elemOpeners["processElem$theTag"].let {
                if (it == null)
                    System.err.println("Unknown tag: $theTag")
                else {
                    try {
                        it.call(this, this, doc, theTag, uri, attribs, pTagCntAtDepth[tagStack.size])
                    }
                    catch (e: Throwable) {
                        throw BTeXParsingException("processElem$theTag"+"\n"+e.stackTraceToString())
                    }
                }
            }

//            printdbg("Start element \t($theTag)")
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            lastTagAtDepth[tagStack.size] = "xxx"

            val popped = tagStack.removeLast()

            val theTag = qName.uppercase()

            elemClosers["closeElem$theTag"].let {
                try {
                    it?.call(this, this, doc, theTag, uri, pTagCntAtDepth[tagStack.size])
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

            if (str.isNotEmpty()) {
//                printdbg("Characters \t\"$str\"")
                paragraphBuffer.append(str)
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

        private lateinit var testFont: TerrarumSansBitmap

        private fun getFont() = when (cover) {
            "typewriter" -> TODO()
            else -> App.fontGame ?: let {
                if (!::testFont.isInitialized) testFont = TerrarumSansBitmap(App.FONT_DIR)
                testFont
            }
        }

        private val hexColRegexRGBshort = Regex("#[0-9a-fA-F]{3,3}")
        private val hexColRegexRGB = Regex("#[0-9a-fA-F]{6,6}")

        private fun getSpanColour(): Color = if (spanColour == null) DEFAULT_FONTCOL
        else if (spanColour!!.matches(hexColRegexRGB)) {
            val rs = spanColour!!.substring(1,3)
            val gs = spanColour!!.substring(3,5)
            val bs = spanColour!!.substring(5,7)

            val r = rs.toInt(16) / 255f
            val g = gs.toInt(16) / 255f
            val b = bs.toInt(16) / 255f

            Color(r, g, b, 1f)
        }
        else if (spanColour!!.matches(hexColRegexRGBshort)) {
            val rs = spanColour!!.substring(1,2)
            val gs = spanColour!!.substring(2,3)
            val bs = spanColour!!.substring(3,4)

            val r = rs.toInt(16) / 15f
            val g = gs.toInt(16) / 15f
            val b = bs.toInt(16) / 15f

            Color(r, g, b, 1f)
        }
        else
            spanColourMap.getOrDefault(spanColour, DEFAULT_FONTCOL)

        // list of CSS named colours (list supports up to CSS Colors Level 4)
        private val spanColourMap = hashMapOf(
            "black" to Color(0x000000ff.toInt()),
            "silver" to Color(0xc0c0c0ff.toInt()),
            "gray" to Color(0x808080ff.toInt()),
            "white" to Color(0xffffffff.toInt()),
            "maroon" to Color(0x800000ff.toInt()),
            "red" to Color(0xff0000ff.toInt()),
            "purple" to Color(0x800080ff.toInt()),
            "fuchsia" to Color(0xff00ffff.toInt()),
            "green" to Color(0x008000ff.toInt()),
            "lime" to Color(0x00ff00ff.toInt()),
            "olive" to Color(0x808000ff.toInt()),
            "yellow" to Color(0xffff00ff.toInt()),
            "navy" to Color(0x000080ff.toInt()),
            "blue" to Color(0x0000ffff.toInt()),
            "teal" to Color(0x008080ff.toInt()),
            "aqua" to Color(0x00ffffff.toInt()),
            "aliceblue" to Color(0xf0f8ffff.toInt()),
            "antiquewhite" to Color(0xfaebd7ff.toInt()),
            "aqua" to Color(0x00ffffff.toInt()),
            "aquamarine" to Color(0x7fffd4ff.toInt()),
            "azure" to Color(0xf0ffffff.toInt()),
            "beige" to Color(0xf5f5dcff.toInt()),
            "bisque" to Color(0xffe4c4ff.toInt()),
            "black" to Color(0x000000ff.toInt()),
            "blanchedalmond" to Color(0xffebcdff.toInt()),
            "blue" to Color(0x0000ffff.toInt()),
            "blueviolet" to Color(0x8a2be2ff.toInt()),
            "brown" to Color(0xa52a2aff.toInt()),
            "burlywood" to Color(0xdeb887ff.toInt()),
            "cadetblue" to Color(0x5f9ea0ff.toInt()),
            "chartreuse" to Color(0x7fff00ff.toInt()),
            "chocolate" to Color(0xd2691eff.toInt()),
            "coral" to Color(0xff7f50ff.toInt()),
            "cornflowerblue" to Color(0x6495edff.toInt()),
            "cornsilk" to Color(0xfff8dcff.toInt()),
            "crimson" to Color(0xdc143cff.toInt()),
            "cyan" to Color(0x00ffffff.toInt()),
            "darkblue" to Color(0x00008bff.toInt()),
            "darkcyan" to Color(0x008b8bff.toInt()),
            "darkgoldenrod" to Color(0xb8860bff.toInt()),
            "darkgray" to Color(0xa9a9a9ff.toInt()),
            "darkgreen" to Color(0x006400ff.toInt()),
            "darkgrey" to Color(0xa9a9a9ff.toInt()),
            "darkkhaki" to Color(0xbdb76bff.toInt()),
            "darkmagenta" to Color(0x8b008bff.toInt()),
            "darkolivegreen" to Color(0x556b2fff.toInt()),
            "darkorange" to Color(0xff8c00ff.toInt()),
            "darkorchid" to Color(0x9932ccff.toInt()),
            "darkred" to Color(0x8b0000ff.toInt()),
            "darksalmon" to Color(0xe9967aff.toInt()),
            "darkseagreen" to Color(0x8fbc8fff.toInt()),
            "darkslateblue" to Color(0x483d8bff.toInt()),
            "darkslategray" to Color(0x2f4f4fff.toInt()),
            "darkslategrey" to Color(0x2f4f4fff.toInt()),
            "darkturquoise" to Color(0x00ced1ff.toInt()),
            "darkviolet" to Color(0x9400d3ff.toInt()),
            "deeppink" to Color(0xff1493ff.toInt()),
            "deepskyblue" to Color(0x00bfffff.toInt()),
            "dimgray" to Color(0x696969ff.toInt()),
            "dimgrey" to Color(0x696969ff.toInt()),
            "dodgerblue" to Color(0x1e90ffff.toInt()),
            "firebrick" to Color(0xb22222ff.toInt()),
            "floralwhite" to Color(0xfffaf0ff.toInt()),
            "forestgreen" to Color(0x228b22ff.toInt()),
            "fuchsia" to Color(0xff00ffff.toInt()),
            "gainsboro" to Color(0xdcdcdcff.toInt()),
            "ghostwhite" to Color(0xf8f8ffff.toInt()),
            "gold" to Color(0xffd700ff.toInt()),
            "goldenrod" to Color(0xdaa520ff.toInt()),
            "gray" to Color(0x808080ff.toInt()),
            "green" to Color(0x008000ff.toInt()),
            "greenyellow" to Color(0xadff2fff.toInt()),
            "grey" to Color(0x808080ff.toInt()),
            "honeydew" to Color(0xf0fff0ff.toInt()),
            "hotpink" to Color(0xff69b4ff.toInt()),
            "indianred" to Color(0xcd5c5cff.toInt()),
            "indigo" to Color(0x4b0082ff.toInt()),
            "ivory" to Color(0xfffff0ff.toInt()),
            "khaki" to Color(0xf0e68cff.toInt()),
            "lavender" to Color(0xe6e6faff.toInt()),
            "lavenderblush" to Color(0xfff0f5ff.toInt()),
            "lawngreen" to Color(0x7cfc00ff.toInt()),
            "lemonchiffon" to Color(0xfffacdff.toInt()),
            "lightblue" to Color(0xadd8e6ff.toInt()),
            "lightcoral" to Color(0xf08080ff.toInt()),
            "lightcyan" to Color(0xe0ffffff.toInt()),
            "lightgoldenrodyellow" to Color(0xfafad2ff.toInt()),
            "lightgray" to Color(0xd3d3d3ff.toInt()),
            "lightgreen" to Color(0x90ee90ff.toInt()),
            "lightgrey" to Color(0xd3d3d3ff.toInt()),
            "lightpink" to Color(0xffb6c1ff.toInt()),
            "lightsalmon" to Color(0xffa07aff.toInt()),
            "lightseagreen" to Color(0x20b2aaff.toInt()),
            "lightskyblue" to Color(0x87cefaff.toInt()),
            "lightslategray" to Color(0x778899ff.toInt()),
            "lightslategrey" to Color(0x778899ff.toInt()),
            "lightsteelblue" to Color(0xb0c4deff.toInt()),
            "lightyellow" to Color(0xffffe0ff.toInt()),
            "lime" to Color(0x00ff00ff.toInt()),
            "limegreen" to Color(0x32cd32ff.toInt()),
            "linen" to Color(0xfaf0e6ff.toInt()),
            "magenta" to Color(0xff00ffff.toInt()),
            "maroon" to Color(0x800000ff.toInt()),
            "mediumaquamarine" to Color(0x66cdaaff.toInt()),
            "mediumblue" to Color(0x0000cdff.toInt()),
            "mediumorchid" to Color(0xba55d3ff.toInt()),
            "mediumpurple" to Color(0x9370dbff.toInt()),
            "mediumseagreen" to Color(0x3cb371ff.toInt()),
            "mediumslateblue" to Color(0x7b68eeff.toInt()),
            "mediumspringgreen" to Color(0x00fa9aff.toInt()),
            "mediumturquoise" to Color(0x48d1ccff.toInt()),
            "mediumvioletred" to Color(0xc71585ff.toInt()),
            "midnightblue" to Color(0x191970ff.toInt()),
            "mintcream" to Color(0xf5fffaff.toInt()),
            "mistyrose" to Color(0xffe4e1ff.toInt()),
            "moccasin" to Color(0xffe4b5ff.toInt()),
            "navajowhite" to Color(0xffdeadff.toInt()),
            "navy" to Color(0x000080ff.toInt()),
            "oldlace" to Color(0xfdf5e6ff.toInt()),
            "olive" to Color(0x808000ff.toInt()),
            "olivedrab" to Color(0x6b8e23ff.toInt()),
            "orange" to Color(0xffa500ff.toInt()),
            "orangered" to Color(0xff4500ff.toInt()),
            "orchid" to Color(0xda70d6ff.toInt()),
            "palegoldenrod" to Color(0xeee8aaff.toInt()),
            "palegreen" to Color(0x98fb98ff.toInt()),
            "paleturquoise" to Color(0xafeeeeff.toInt()),
            "palevioletred" to Color(0xdb7093ff.toInt()),
            "papayawhip" to Color(0xffefd5ff.toInt()),
            "peachpuff" to Color(0xffdab9ff.toInt()),
            "peru" to Color(0xcd853fff.toInt()),
            "pink" to Color(0xffc0cbff.toInt()),
            "plum" to Color(0xdda0ddff.toInt()),
            "powderblue" to Color(0xb0e0e6ff.toInt()),
            "purple" to Color(0x800080ff.toInt()),
            "rebeccapurple" to Color(0x663399ff.toInt()),
            "red" to Color(0xff0000ff.toInt()),
            "rosybrown" to Color(0xbc8f8fff.toInt()),
            "royalblue" to Color(0x4169e1ff.toInt()),
            "saddlebrown" to Color(0x8b4513ff.toInt()),
            "salmon" to Color(0xfa8072ff.toInt()),
            "sandybrown" to Color(0xf4a460ff.toInt()),
            "seagreen" to Color(0x2e8b57ff.toInt()),
            "seashell" to Color(0xfff5eeff.toInt()),
            "sienna" to Color(0xa0522dff.toInt()),
            "silver" to Color(0xc0c0c0ff.toInt()),
            "skyblue" to Color(0x87ceebff.toInt()),
            "slateblue" to Color(0x6a5acdff.toInt()),
            "slategray" to Color(0x708090ff.toInt()),
            "slategrey" to Color(0x708090ff.toInt()),
            "snow" to Color(0xfffafaff.toInt()),
            "springgreen" to Color(0x00ff7fff.toInt()),
            "steelblue" to Color(0x4682b4ff.toInt()),
            "tan" to Color(0xd2b48cff.toInt()),
            "teal" to Color(0x008080ff.toInt()),
            "thistle" to Color(0xd8bfd8ff.toInt()),
            "tomato" to Color(0xff6347ff.toInt()),
            "transparent" to Color(0),
            "turquoise" to Color(0x40e0d0ff.toInt()),
            "violet" to Color(0xee82eeff.toInt()),
            "wheat" to Color(0xf5deb3ff.toInt()),
            "white" to Color(0xffffffff.toInt()),
            "whitesmoke" to Color(0xf5f5f5ff.toInt()),
            "yellow" to Color(0xffff00ff.toInt()),
            "yellowgreen" to Color(0x9acd32ff.toInt())
        )

        private val pageWidthMap = hashMapOf(
            "standard" to 480,
            "examination" to 640,
        )
        private val pageHeightMap = hashMapOf(
            "standard" to 25,
            "examination" to 18,
        )

        private val ccEmph = TerrarumSansBitmap.toColorCode(0xfd44)
        private val ccItemName = TerrarumSansBitmap.toColorCode(0xf37d)
        private val ccTargetName = TerrarumSansBitmap.toColorCode(0xf3c4)
        private val ccReset = TerrarumSansBitmap.toColorCode(0)


        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
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

                doc.textWidth = pageWidth
                doc.textHeight = pageHeight
            }

            handler.btexOpened = true

            printdbg("BTeX document: def=${handler.def}, cover=${handler.cover}, inner=${handler.inner}, papersize=${handler.papersize}, dim=${handler.pageWidth}x${handler.pageHeight} (${handler.pageLines} lines)")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemPAIR(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            if (tagStack.size == 3 && tagStack.getOrNull(1) == "blocklut") {
                blockLut[attribs["key"]!!] = attribs["value"]!!
            }
            else {
                throw BTeXParsingException("<pair> used outside of <blocklut>")
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemEMPH(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.append(ccEmph)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.append(ccItemName)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.append(ccTargetName)
        }

        @CloseTag
        fun closeElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, theTag, uri, siblingIndex)
        @CloseTag
        fun closeElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, theTag, uri, siblingIndex)
        @CloseTag
        fun closeElemEMPH(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            handler.paragraphBuffer.append(ccReset)
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEX(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.append("BTeX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemCOVER(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            val hue = (attribs["hue"]?.toFloatOrNull() ?: 28f) * DEG_TO_RAD
            val coverCol = OKLch(hue, 0.05f, 0.36f)
            val (r, g, b) = coverCol.tosRGB()
            doc.addNewPage(Color(r, g, b, 1f))
            handler.spanColour = "white"
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTOCPAGE(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            doc.addNewPage()
            typesetChapterHeading("Table of Contents", handler, 16)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEXPAGE(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            doc.addNewPage()
            typesetChapterHeading("Index", handler, 16)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFCONTENTS(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            tocPage = doc.currentPageObj

            handler.paragraphBuffer.clear()
            typesetParagraphs("// TODO", handler)
            handler.paragraphBuffer.clear()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.clear()

            // prepare contents
            val par = ArrayList<String>()
            val pageWidth = doc.textWidth

            indexMap.keys.toList().sorted().forEach { key ->
                val pageNum = indexMap[key]!!.plus(1).toString()
                val pageNumWidth = getFont().getWidth(pageNum)

                println("pageWidth=$pageWidth, pageNum=$pageNum, pageNumWidth=$pageNumWidth")

                typesetParagraphs(key, handler).let {
                    it.last().let { call ->
                        call.extraDrawFun = { batch, x, y ->
                            val font = getFont()
                            val dotGap = 10

                            var dotCursor = (x + call.width + dotGap/2).div(dotGap).ceilToFloat() * dotGap
                            while (dotCursor < x + pageWidth - pageNumWidth - dotGap/2) {
                                font.draw(batch, "·", dotCursor, y)
                                dotCursor += dotGap
                            }

                            font.draw(batch, pageNum, x + pageWidth - pageNumWidth.toFloat(), y)
                        }
                    }
                }
            }
        }

        @CloseTag
        fun closeElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            doc.addNewPage()
        }

        @CloseTag
        fun closeElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            // if tocPage != null, estimate TOC page size, renumber indexMap and cptSectMap if needed, then typeset the toc
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEX(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            attribs["id"]?.let {
                indexMap[it] = doc.currentPage
            }
        }







        @OpenTag // reflective access is impossible with 'private'
        fun processElemBR(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            handler.paragraphBuffer.append("\n")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemNEWPAGE(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
            doc.addNewPage()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemFULLPAGEBOX(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            doc.currentPageObj.let { page ->
                val yStart = page.drawCalls.minOf { it.posY }
                val yEnd = page.drawCalls.maxOf { it.posY + it.lineCount * doc.lineHeightInPx }
                val pageHeight = doc.textHeight

                val newYpos = (pageHeight - (yEnd - yStart)) / 2
                val yDelta = newYpos - yStart

                val xStart = page.drawCalls.minOf { it.posX }
                val xEnd = page.drawCalls.maxOf { it.posX + it.width }
                val pageWidth = doc.textWidth

                val newXpos = (pageWidth - (xEnd - xStart)) / 2
                val xDelta = newXpos - xStart

                page.drawCalls.forEach {
                    it.posX += xDelta
                    it.posY += yDelta
                }

            }

            doc.addNewPage()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemP(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, attribs: HashMap<String, String>, siblingIndex: Int) {
        }

        @CloseTag
        fun closeElemCOVER(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            handler.spanColour = null
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemTITLE(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) = closeElemP(handler, doc, theTag, uri, siblingIndex)
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemAUTHOR(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) = closeElemP(handler, doc, theTag, uri, siblingIndex)
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemEDITION(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) = closeElemP(handler, doc, theTag, uri, siblingIndex)
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            // insert new page for second+ chapters
            if (siblingIndex > 0) doc.addNewPage()

            val thePar = handler.paragraphBuffer.toString().trim()
            typesetChapterHeading(thePar, handler, 16)

            cptSectMap.add(Triple("chapter", thePar, doc.currentPage))

            handler.paragraphBuffer.clear()
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSECTION(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            // if current line is the last line, proceed to the next page
            if (doc.currentLine == doc.pageLines - 1) doc.addNewPage()

            val thePar = handler.paragraphBuffer.toString().trim()
            typesetSectionHeading(thePar, handler, 8)

            cptSectMap.add(Triple("section", thePar, doc.currentPage))

            handler.paragraphBuffer.clear()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemP(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            val thePar = (if (siblingIndex > 1) "\u3000" else "") + handler.paragraphBuffer.toString().trim() // indent the strictly non-first pars
            typesetParagraphs(thePar, handler)
            handler.paragraphBuffer.clear()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSPAN(handler: BTeXHandler, doc: BTeXDocument, theTag: String, uri: String, siblingIndex: Int) {
            spanColour = null
        }


        private fun typesetChapterHeading(thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            typesetParagraphs("\n"+thePar, handler, width - indent).also {
                // add indents and adjust text y pos
                it.forEach {
                    it.posX += indent
                    it.posY -= doc.lineHeightInPx / 2
                }
                // add ornamental column on the left
                it.forEach {
                    it.extraDrawFun = { batch, x, y ->
                        Toolkit.fillArea(batch, x - (indent - 2), y + doc.lineHeightInPx, 6f, (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx.toFloat())
                    }
                }
            }
        }

        private fun typesetSectionHeading(thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            typesetParagraphs("\n"+thePar, handler, width - indent).also {
                // add indents and adjust text y pos
                it.forEach {
                    it.posX += indent
                    it.posY -= doc.lineHeightInPx / 2
                }
            }
        }

        private fun typesetParagraphs(thePar: String, handler: BTeXHandler, width: Int = doc.textWidth): List<BTeXDrawCall> {
            val font = getFont()
            val slugs = MovableType(font, thePar, width)

            val drawCalls = ArrayList<BTeXDrawCall>()

            var remainder = doc.pageLines - doc.currentLine
            var slugHeight = slugs.height
            var linesOut = 0

//            printdbg("Page: ${doc.currentPage+1}, Line: ${doc.currentLine}")

            if (slugHeight > remainder) {
                val subset = linesOut to linesOut + remainder

                val drawCall = BTeXDrawCall(
                    0,
                    doc.currentLine * doc.lineHeightInPx,
                    handler.currentTheme,
                    handler.getSpanColour(),
                    MovableTypeDrawCall(slugs, subset.first, subset.second)
                )

                doc.appendDrawCall(drawCall); drawCalls.add(drawCall)

                linesOut += remainder
                slugHeight -= remainder

                doc.addNewPage()
            }

            while (slugHeight > 0) {
                remainder = minOf(slugHeight, doc.pageLines)

                val subset = linesOut to linesOut + remainder

                val drawCall = BTeXDrawCall(
                    0,
                    doc.currentLine * doc.lineHeightInPx,
                    handler.currentTheme,
                    handler.getSpanColour(),
                    MovableTypeDrawCall(slugs, subset.first, subset.second)
                )

                doc.appendDrawCall(drawCall); drawCalls.add(drawCall)

                linesOut += remainder
                slugHeight -= remainder

                if (remainder == doc.pageLines) {
                    doc.addNewPage()
                }
            }

            // if typesetting the paragraph leaves the first line of new page empty, move the "row cursor" back up
            if (doc.currentLine == 1 && doc.currentPageObj.isEmpty()) doc.currentLine = 0 // '\n' adds empty draw call to the page, which makes isEmpty() to return false

            return drawCalls
        }


        companion object {
            private const val ZWSP = 0x200B
            private const val SHY = 0xAD
            private const val NBSP = 0xA0
            private const val GLUE_POSITIVE_ONE = 0xFFFF0
            private const val GLUE_POSITIVE_SIXTEEN = 0xFFFFF
            private const val GLUE_NEGATIVE_ONE = 0xFFFE0
            private const val GLUE_NEGATIVE_SIXTEEN = 0xFFFEF

            fun glueToString(glue: Int): String {
                val tokens = CodepointSequence()

                if (glue == 0)
                    tokens.add(ZWSP)
                else if (glue.absoluteValue <= 16)
                    if (glue > 0)
                        tokens.add(GLUE_POSITIVE_ONE + (glue - 1))
                    else
                        tokens.add(GLUE_NEGATIVE_ONE + (glue.absoluteValue - 1))
                else {
                    val fullGlues = glue.absoluteValue / 16
                    val smallGlues = glue.absoluteValue % 16
                    if (glue > 0)
                        tokens.addAll(
                            List(fullGlues) { GLUE_POSITIVE_SIXTEEN } +
                                    listOf(GLUE_POSITIVE_ONE + (smallGlues - 1))
                        )
                    else
                        tokens.addAll(
                            List(fullGlues) { GLUE_NEGATIVE_SIXTEEN } +
                                    listOf(GLUE_NEGATIVE_ONE + (smallGlues - 1))
                        )
                }

                return tokens.toUTF8Bytes().toString(Charsets.UTF_8)
            }
        }

    }


    private annotation class OpenTag
    private annotation class CloseTag

}


class BTeXParsingException(s: String) : RuntimeException(s) {

}
