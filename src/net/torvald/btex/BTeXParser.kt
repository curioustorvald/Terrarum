package net.torvald.btex

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath.DEG_TO_RAD
import net.torvald.colourutil.OKLch
import net.torvald.colourutil.tosRGB
import net.torvald.terrarum.App
import net.torvald.terrarum.btex.BTeXBatchDrawCall
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.btex.BTeXDocument.Companion.DEFAULT_ORNAMENTS_COL
import net.torvald.terrarum.btex.BTeXDrawCall
import net.torvald.terrarum.btex.TypesetDrawCall
import net.torvald.terrarum.ceilToFloat
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.toHex
import net.torvald.terrarum.tryDispose
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.MovableType
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.unicode.CURRENCY
import net.torvald.unicode.toUTF8Bytes
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import kotlin.math.roundToInt
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

/**
 * Created by minjaesong on 2023-10-28.
 */
object BTeXParser {

    internal val textTags = hashSetOf("P", "PBOX", "TITLE", "AUTHOR", "EDITION", "CHAPTER", "SECTION", "LI")
    internal val textDecorTags = hashSetOf("SPAN", "CODE")

    operator fun invoke(file: FileHandle, varMap: Map<String, String>) = invoke(file.file(), varMap)

    operator fun invoke(file: File, varMap: Map<String, String>): Pair<BTeXDocument, BTeXHandler> {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newInstance().let {
            it.isNamespaceAware = true
            it.isValidating = true
            it.newSAXParser()
        }
        val stream = FileInputStream(file)
        val handler = BTeXHandler(doc, varMap)
        parser.parse(stream, handler)
        return doc to handler
    }

    operator fun invoke(string: String, varMap: Map<String, String>): Pair<BTeXDocument, BTeXHandler> {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newInstance().let {
            it.isNamespaceAware = true
            it.isValidating = true
            it.newSAXParser()
        }
        val handler = BTeXHandler(doc, varMap)
        parser.parse(InputSource(StringReader(string)), handler)
        return doc to handler
    }

    class BTeXHandler(val doc: BTeXDocument, val varMap: Map<String, String>) : DefaultHandler() {
        private var cover = ""
        private var inner = ""
        private var papersize = ""
        private var def = ""

        private var btexOpened = false

//        private var pageWidth = doc.textWidth
//        private var pageLines = doc.pageLines
//        private var pageHeight = doc.textHeight

        private val blockLut = HashMap<String, ItemID>()

        private val tagStack = ArrayList<String>() // index zero should be "btex"
        private var tagHistory = ArrayList<String>()

        private var currentTheme = ""
        private var spanColour: String? = null
        private var codeMode: Boolean = false
        private var hrefMode: Boolean = false
        private var bucksMode: Boolean = false


        private val elemOpeners: HashMap<String, KFunction<*>> = HashMap()
        private val elemClosers: HashMap<String, KFunction<*>> = HashMap()

        private val paragraphBuffer = StringBuilder()

        fun clearParBuffer() {
            paragraphBuffer.clear()
        }

        private val objDict = HashMap<String, (BTeXDrawCall) -> BTeXBatchDrawCall>()
        private val objWidthDict = HashMap<String, Int>()

        private var lastTagAtDepth = Array(24) { "" }
        private var pTagCntAtDepth = IntArray(24)

        private data class CptSect(val type: String, var alt: String?)
        private data class CptSectInfo(val type: String, var name: String, var pagenum: Int,
                                       var partNum: Int?, var cptNum: Int?, var sectNum: Int?)

        private val cptSectStack = ArrayList<CptSect>()

        private val indexMap = HashMap<String, Int>() // id to pagenum
        private val cptSectMap = ArrayList<CptSectInfo>()
        private var tocPage: Int? = null

        private var hasCover = false
        private var coverCol: Color? = null

        private lateinit var testFont: TerrarumSansBitmap
        private lateinit var titleFont: TerrarumSansBitmap
        private lateinit var subtitleFont: TerrarumSansBitmap

        private val bodyTextShadowAlpha = 0.36f



        private fun StringBuilder.appendObjectPlaceholder(id: String) {
            (objWidthDict[id] ?: throw NullPointerException("No OBJ with id '$id' exists")).let {
                this.append(objectMarkerWithWidth(id, it))
            }
        }

        init {
            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<OpenTag>() != null }.forEach {
//                println("Tag opener: ${it.name}")
                elemOpeners[it.name] = it
            }

            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<CloseTag>() != null }.forEach {
//                println("Tag closer: ${it.name}")
                elemClosers[it.name] = it
            }

            objWidthDict["TAG@BTEX"] = 32
            objDict["TAG@BTEX"] = { text: BTeXDrawCall ->
                object : BTeXBatchDrawCall(32, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}B", x + ( 0 + 0*interchar)*scale, y + 0*scale)
                        font.draw(batch, "${ccDefault}T", x + ( 8 + 1*interchar)*scale, y + 0*scale)
                        font.draw(batch, "${ccDefault}E", x + (15 + 2*interchar)*scale, y + 4*scale)
                        font.draw(batch, "${ccDefault}X", x + (23 + 3*interchar)*scale, y + 0*scale)
                    }
                }
            }

            objWidthDict["TAG@LATEX"] = 36
            objDict["TAG@LATEX"] = { text: BTeXDrawCall ->
                object : BTeXBatchDrawCall(36, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}L", x + (0 + 0 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}ᴀ", x + (4 + 0 * interchar) * scale, y + -4 * scale)
                        font.draw(batch, "${ccDefault}T", x + (12 + 1 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}E", x + (19 + 2 * interchar) * scale, y + 4 * scale)
                        font.draw(batch, "${ccDefault}X", x + (27 + 3 * interchar) * scale, y + 0 * scale)
                    }
                }
            }

            objWidthDict["TAG@TEX"] = 24
            objDict["TAG@TEX"] = { text: BTeXDrawCall ->
                object : BTeXBatchDrawCall(24, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}T", x + (0 + 1 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}E", x + (7 + 2 * interchar) * scale, y + 4 * scale)
                        font.draw(batch, "${ccDefault}X", x + (15 + 3 * interchar) * scale, y + 0 * scale)
                    }
                }
            }
        }

        /*private fun getOrPutCodeTagRef(width: Int): ((BTeXDrawCall) -> BTeXBatchDrawCall)? {
            val tagname = "TAG@CODE-$width"
            if (!objDict.contains(tagname)) {
                objWidthDict[tagname] = 0
                objDict[tagname] = { text: BTeXDrawCall ->
                    object : BTeXBatchDrawCall(0, 0, text) {
                        override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                            val oldcol = batch.color.cpy()
                            batch.color = Color(0xccccccff.toInt())
                            Toolkit.fillArea(batch, x - 2, y, width + 4f, doc.lineHeightInPx.toFloat())
                            batch.color = Color(0x999999ff.toInt())
                            Toolkit.drawBoxBorder(batch, x - 2, y, width + 4f, doc.lineHeightInPx.toFloat())
                            batch.color = oldcol
                        }
                    }
                }
            }
            return objDict[tagname]
        }*/

        fun dispose() {
            if (::testFont.isInitialized) testFont.tryDispose()
            if (::titleFont.isInitialized) titleFont.tryDispose()
            if (::subtitleFont.isInitialized) subtitleFont.tryDispose()
        }

        override fun warning(e: SAXParseException) {
            e.printStackTrace()
        }

        override fun error(e: SAXParseException) {
            throw e
        }

        override fun fatalError(e: SAXParseException) {
            throw e
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
            val theTag = tag.uppercase()
            if (tagStack.isEmpty() && theTag != "BTEXDOC") throw BTeXParsingException("Document is not BTeX")

            if (tagStack.isNotEmpty() && tagStack.any { textTags.contains(it) } && textTags.contains(theTag))
                throw IllegalStateException("Text tag '$theTag' used inside of text tags (tag stack is ${tagStack.joinToString()}, $theTag)")
            if (tagStack.isNotEmpty() && !textTags.contains(tagStack.last()) && textDecorTags.contains(theTag))
                throw IllegalStateException("Text decoration tag '$theTag' used outside of a text tag (tag stack is ${tagStack.joinToString()}, $theTag)")

            if (lastTagAtDepth[tagStack.size] != "P") pTagCntAtDepth[tagStack.size] = 0
            if (theTag == "P") pTagCntAtDepth[tagStack.size] += 1
            lastTagAtDepth[tagStack.size] = theTag

            tagStack.add(theTag)
            tagHistory.add(theTag)


            val attribs = HashMap<String, String>().also {
                it.putAll((0 until attributes.length).map { attributes.getQName(it) to attributes.getValue(it) })
            }


            elemOpeners["processElem$theTag"].let {
                if (it == null)
                    System.err.println("Unknown tag: $theTag")
                else {
                    try {
                        it.call(this, this, doc, uri, attribs)
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
                    it?.call(this, this, doc, uri, pTagCntAtDepth[tagStack.size])
                }
                catch (e: Throwable) {
                    throw BTeXParsingException(e.stackTraceToString())
                }
            }

//            printdbg("  End element \t($popped)")
        }

        private var oldSpanColour: String? = null
        private var oldCodeMode = false
        private var oldHrefMode = false
        private var oldBucksMode = false
        private val CODE_TAG_MARGIN = 2

        override fun characters(ch: CharArray, start: Int, length: Int) {
            val str =
                String(ch.sliceArray(start until start + length)).replace('\n', ' ').replace(Regex(" +"), " ")//.trim()

            if (str.isNotEmpty()) {
//                printdbg("Characters [col:${spanColour}] \t\"$str\"")
                // process span request
                if (spanColour != oldSpanColour || spanColour != null) {

                    val spanGdxCol = getSpanColourOrNull()

                    if (spanGdxCol == null) {
                        paragraphBuffer.append(ccDefault)
                    }
                    else {
                        paragraphBuffer.append(TerrarumSansBitmap.toColorCode(
                            spanGdxCol.r.times(15f).roundToInt(),
                            spanGdxCol.g.times(15f).roundToInt(),
                            spanGdxCol.b.times(15f).roundToInt()
                        ))
                    }
                }

                // process code request
                if (codeMode != oldCodeMode || codeMode) {
                    if (!codeMode) {
                        paragraphBuffer.append(TerrarumSansBitmap.charsetOverrideDefault)
                        paragraphBuffer.append(ccDefault)
                        paragraphBuffer.append(glueToString(CODE_TAG_MARGIN))
                    }
                    else {
//                        println("CODE tag for str '$str'")
//                        val w = getFont().getWidth(str) + 2*CODE_TAG_MARGIN
//                        getOrPutCodeTagRef(w)
//                        paragraphBuffer.appendObjectPlaceholder("TAG@CODE-${w}")
                        paragraphBuffer.append(glueToString(CODE_TAG_MARGIN))
                        paragraphBuffer.append(ccCode)
                        paragraphBuffer.append(TerrarumSansBitmap.charsetOverrideCodestyle)
                    }
                }

                // process href request
                if (hrefMode != oldHrefMode || hrefMode) {
                    if (!hrefMode) {
                        paragraphBuffer.append(ccDefault)
                    }
                    else {
                        paragraphBuffer.append(ccHref)
                    }
                }

                // process bucks request
                if (bucksMode != oldBucksMode || bucksMode) {
                    if (!bucksMode) {
                        paragraphBuffer.append(ccDefault)

                    }
                    else {
                        paragraphBuffer.append(ccBucks)
                        val str2 = str.replace(' ', '\u00A0')

                    }
                }


                paragraphBuffer.append(str)

                oldSpanColour = spanColour
                oldCodeMode = codeMode
                oldHrefMode = hrefMode
                oldBucksMode = bucksMode
            }
        }

        /*private fun Color?.toInternalColourCodeStr(): String {
            return if (this == null) "\u000E"
            else {
                val rgba = this.toRGBA()
                val r = rgba.ushr(24).and(255) + 0xF800
                val g = rgba.ushr(16).and(255) + 0xF800
                val b = rgba.ushr(8).and(255) + 0xF800

                return "\u000F" + Char(r) + Char(g) + Char(b)
            }
        }*/

        private fun getFont() = when (cover) {
            "typewriter" -> TODO()
            else -> {
                if (!::testFont.isInitialized) testFont = TerrarumSansBitmap(App.FONT_DIR, shadowAlpha = bodyTextShadowAlpha, textCacheSize = 4096)
                testFont
            }
        }

        private fun getTitleFont(): TerrarumSansBitmap {
            if (!::titleFont.isInitialized) titleFont = TerrarumSansBitmap(App.FONT_DIR).also {
                it.interchar = 1
                it.scale = 2
            }
            return titleFont
        }

        private fun getSubtitleFont(): TerrarumSansBitmap {
            if (!::subtitleFont.isInitialized) subtitleFont = TerrarumSansBitmap(App.FONT_DIR).also {
                it.interchar = 1
            }
            return subtitleFont
        }

        private val hexColRegexRGBshort = Regex("#[0-9a-fA-F]{3,3}")
        private val hexColRegexRGB = Regex("#[0-9a-fA-F]{6,6}")

        private fun getSpanColourOrNull() = if (spanColour == null) null else getSpanColour()

        private fun getSpanColour(): Color = if (spanColour == null) Color.BLACK
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
            spanColourMap.getOrDefault(spanColour, Color.BLACK)

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
            "standard" to 512,
            "examination" to 640,
        )
        private val pageHeightMap = hashMapOf(
            "standard" to 24,
            "examination" to 18,
        )

        private val ccEmph = "#C11"// TerrarumSansBitmap.toColorCode(0xfd44)
        private val ccItemName = "#03B" // TerrarumSansBitmap.toColorCode(0xf37d)
        private val ccTargetName = "#170" // TerrarumSansBitmap.toColorCode(0xf3c4)

        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
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

                doc.textWidth = pageWidthMap[papersize]!!
                doc.pageLines = pageHeightMap[papersize]!!
            }

            handler.btexOpened = true
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemPAIR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            if (tagStack.size == 3 && tagStack.getOrNull(1) == "blocklut") {
                blockLut[attribs["key"]!!] = attribs["value"]!!
            }
            else {
                throw BTeXParsingException("<pair> used outside of <blocklut>")
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemSUBTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemAUTHOR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemEDITION(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemEMPH(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.spanColour = ccEmph
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.spanColour = ccItemName
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.spanColour = ccTargetName
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemSPAN(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.spanColour = attribs["colour"] ?: attribs["color"]
        }

        @CloseTag
        fun closeElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, uri, siblingIndex)
        @CloseTag
        fun closeElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, uri, siblingIndex)
        @CloseTag
        fun closeElemEMPH(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            handler.spanColour = null
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.paragraphBuffer.appendObjectPlaceholder("TAG@BTEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemLATEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.paragraphBuffer.appendObjectPlaceholder("TAG@LATEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.paragraphBuffer.appendObjectPlaceholder("TAG@TEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemCODE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.codeMode = true
        }
        @CloseTag
        fun closeElemCODE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            handler.codeMode = false
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemA(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.hrefMode = true
        }
        @CloseTag
        fun closeElemA(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            handler.hrefMode = false
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemBUCKS(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.bucksMode = true
            handler.paragraphBuffer.append("$CURRENCY\u00A0")
        }
        @CloseTag
        fun closeElemBUCKS(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            handler.bucksMode = false
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemCOVER(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            hasCover = true
            val hue = (attribs["hue"]?.toFloatOrNull() ?: 28f) * DEG_TO_RAD
            val coverColLCH = OKLch(hue, 0.05f, 0.36f)
            val (r, g, b) = coverColLCH.tosRGB()
            coverCol = Color(r, g, b, 1f)
            doc.addNewPage(coverCol!!)
//            handler.spanColour = "white"
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTOCPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage() // toc: openright
            val header = attribs["title"] ?: "Table of Contents"
            typesetChapterHeading(null, header, handler, 16)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEXPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            if (doc.currentPageObj.isNotEmpty()) doc.addNewPage()
            val header = attribs["title"] ?: "Index"
            typesetChapterHeading(null, header, handler, 16)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFCONTENTS(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            tocPage = doc.currentPage
            handler.clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()

            // prepare contents
            val pageWidth = doc.textWidth

            indexMap.keys.toList().sorted().forEach { key ->
                typesetTOCline("", key, indexMap[key]!!, handler)
            }
        }

        @CloseTag
        fun closeElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage()
        }

        @CloseTag
        fun closeElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // setup pages such that:
            // toc: openright (done on processElemTOCPAGE)
            // first chapter: openright
            // second+ chapter: openany
            if (cptSectMap.isNotEmpty() && cptSectMap.first().pagenum % 2 == 1) {
                doc.addNewPageAt(cptSectMap.first().pagenum)
                cptSectMap.forEach { it.pagenum += 1 }
            }
            else if (cptSectMap.isNotEmpty()) {
                doc.addNewPageAt(cptSectMap.first().pagenum)
                doc.addNewPageAt(cptSectMap.first().pagenum)
                cptSectMap.forEach { it.pagenum += 2 }
            }

            // if tocPage != null, estimate TOC page size, renumber indexMap and cptSectMap if needed, then typeset the toc
            if (tocPage != null) {
                // estimate the number of TOC pages
                val tocSizeInPages = (cptSectMap.size + 2) / doc.pageLines

                // renumber things
                if (tocSizeInPages > 1) {
                    val pageDelta = tocSizeInPages - 1
                    indexMap.keys.forEach {
                        indexMap[it] = indexMap[it]!! + pageDelta
                    }

                    cptSectMap.forEach { it.pagenum += pageDelta }

                    // insert new pages
                    repeat(pageDelta) {
                        doc.addNewPageAt(tocPage!! + 1)
                    }
                }

                cptSectMap.forEach { (type, name, pg, part, cpt, sect) ->
                    val indent = if (type == "subsection") 32 else if (type == "section") 16 else 0
                    val heading = if (part == null && cpt == null && sect == null)
                        ""
                    else if (part != null && cpt == null && sect == null)
                        "Part ${part.toRomanNum()}${glueToString(9)}"
                    else
                        listOfNotNull(cpt, sect).joinToString(".") + "${glueToString(9)}" +
                                (if (cpt != null && cpt < 10) "${glueToString(9)}" else "")

                    typesetTOCline("$heading", name, pg, handler, indent, tocPage)
                }
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            attribs["id"]?.let {
                indexMap[it] = doc.currentPage + 1
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVAR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            attribs["id"]?.let {
                val value = varMap[it] ?: throw NullPointerException("No variable definition of '$it'")
                handler.paragraphBuffer.append(value)
            }
        }







        @OpenTag // reflective access is impossible with 'private'
        fun processElemBR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.paragraphBuffer.append("\n")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemNEWPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemFULLPAGEBOX(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            doc.currentPageObj.let { page ->
                page.drawCalls.forEach {
                    val yStart = it.posY
                    val yEnd = it.posY + it.lineCount * doc.lineHeightInPx
                    val pageHeight = doc.textHeight

                    val newYpos = (pageHeight - (yEnd - yStart)) / 2
                    val yDelta = newYpos - yStart

                    val xStart = it.posX
                    val xEnd = it.posX + it.width
                    val pageWidth = doc.textWidth

                    val newXpos = (pageWidth - (xEnd - xStart)) / 2
                    val xDelta = newXpos - xStart

                    it.posX += xDelta
                    it.posY += yDelta
                }
            }

            doc.addNewPage()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemP(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()
        }

        @CloseTag
        fun closeElemANONBREAK(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            typesetParagraphs("${ccDefault}――――――――――――", handler).also {it.first().let {
                it.posX += (doc.textWidth - it.width) / 2
            } }
            handler.clearParBuffer()
        }

        @CloseTag
        fun closeElemCOVER(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            handler.spanColour = null

            if (hasCover) {
                doc.addNewPage()
            }
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = handler.paragraphBuffer.toString().trim()
            typesetBookTitle(thePar, handler)
            handler.clearParBuffer()

            doc.theTitle = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSUBTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = handler.paragraphBuffer.toString().trim()
            typesetBookEdition(thePar, handler)
            handler.clearParBuffer()

            doc.theSubtitle = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemAUTHOR(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = handler.paragraphBuffer.toString().trim()
            typesetBookAuthor(thePar, handler)
            handler.clearParBuffer()

            doc.theAuthor = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemEDITION(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = handler.paragraphBuffer.toString().trim()
            typesetBookEdition(thePar, handler)
            handler.clearParBuffer()

            doc.theEdition = thePar.replace("\n", " ")
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemPART(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("part", attribs["alt"]))
            else
                cptSectStack.add(CptSect("part-hidden", attribs["alt"]))
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("chapter", attribs["alt"]))
            else
                cptSectStack.add(CptSect("chapter-hidden", attribs["alt"]))
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemSECTION(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            handler.clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("section", attribs["alt"]))
            else
                cptSectStack.add(CptSect("section-hidden", attribs["alt"]))
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemPART(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if the last page is not empty, create new oen
            if (doc.currentPageObj.isNotEmpty()) doc.addNewPage()

            val partOrder = cptSectMap.count { it.type.startsWith("part") } + 1
            val thePar = handler.paragraphBuffer.toString().trim()
            typesetPartHeading(partOrder, thePar, handler)

            val cptSectInfo = cptSectStack.removeLast()
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("part", cptSectInfo.alt ?: thePar, doc.currentPage, partOrder, null, null))

            handler.clearParBuffer()
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if current line is the last line, proceed to the next page
            if (doc.linesPrintedOnPage.last() >= doc.pageLines - 2) doc.addNewPage()

            val partOrder = cptSectMap.count { it.type.startsWith("part") }
            val cptOrder = cptSectMap.count { it.type.startsWith("chapter") } + 1

            val thePar = handler.paragraphBuffer.toString().trim()
            typesetChapterHeading(cptOrder, thePar, handler, 16)

            val cptSectInfo = cptSectStack.removeLast()
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("chapter", cptSectInfo.alt ?: thePar, doc.currentPage, partOrder, cptOrder, null))

            handler.clearParBuffer()
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSECTION(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if current line is the last line, proceed to the next page
            if (doc.linesPrintedOnPage.last() >= doc.pageLines - 1) doc.addNewPage()

            val partOrder = cptSectMap.count { it.type.startsWith("part") }
            val cptOrder = cptSectMap.count { it.type.startsWith("chapter") }
            var sectOrder = 1

            var cnt = cptSectMap.size - 1
            while (cnt >= 0) {
                if (cptSectMap[cnt].type.startsWith("section")) {
                    cnt += 1
                    sectOrder += 1
                }
                else break
            }


            val thePar = handler.paragraphBuffer.toString().trim()
            typesetSectionHeading(cptOrder, sectOrder, thePar, handler, 8)

            val cptSectInfo = cptSectStack.removeLast()
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("section", cptSectInfo.alt ?: thePar, doc.currentPage, partOrder, cptOrder, sectOrder))

            handler.clearParBuffer()
        }



        @CloseTag // reflective access is impossible with 'private'
        fun closeElemP(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if this P is a very first P without chapters, leave two lines before typesetting
            val penultTag = tagHistory.getOrNull(tagHistory.lastIndex - 1)
            val thePar = handler.paragraphBuffer.toString().trim()

            val text =
                // DON't indent on centering context
                if (tagStack.contains("CENTER") || tagStack.contains("FULLPAGEBOX")) thePar
                // indent the second+ pars (or don't indent first par after cpt/sect, anonbreak and br)
                else if (siblingIndex > 1 && penultTag != "ANONBREAK" && penultTag != "BR") "\uDBBF\uDFDF$thePar"
                // if the very first tag within the MANUSCRIPT is par (i.e. no chapter), create a "virtual" chapter
                else if (penultTag == "MANUSCRIPT") "\n\n$thePar"
                // else, print the text normally
                else thePar

            typesetParagraphs(ccDefault + text, handler)

            handler.clearParBuffer()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSPAN(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            spanColour = null
        }

        @CloseTag
        fun closeElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // make sure the last pair ends with paper and end-cover
            doc.endOfPageStart = doc.currentPage + 1
            if (doc.pages.size % 2 == 1) doc.addNewPage()
            doc.addNewPage()
        }




        private fun typesetBookTitle(thePar: String, handler: BTeXHandler) {
            val label = "\n${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar
            typesetParagraphs(getTitleFont(), label, handler, doc.textWidth - 16).also {
                val addedLines = it.sumOf { it.lineCount }
                doc.linesPrintedOnPage[doc.currentPage] += addedLines

                it.forEach {
                    it.posX += 8
                }
            }
        }

        private fun typesetBookAuthor(thePar: String, handler: BTeXHandler) {
            typesetParagraphs(getSubtitleFont(), "\n\n${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar, handler, doc.textWidth - 16).also {
                it.last().extraDrawFun = { batch, x, y ->
                    val px = x
                    val py = y + 23
                    val pw = doc.textWidth - 16f
                    batch.color = Color(1f,1f,1f,.5f)
                    Toolkit.fillArea(batch, px, py, pw+1, 2f)
                    batch.color = Color.WHITE
                    Toolkit.fillArea(batch, px, py, pw, 1f)
                }

                it.forEach {
                    it.posX += 8
                }
            }
        }

        private fun typesetBookEdition(thePar: String, handler: BTeXHandler) {
            typesetParagraphs(getSubtitleFont(), "${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar, handler, doc.textWidth - 16).also {
                it.forEach {
                    it.posX += 8
                }
            }
        }

        private fun typesetPartHeading(num: Int, thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            typesetParagraphs("${ccDefault}⁃ Part ${num.toRomanNum()} ⁃", handler)
            typesetParagraphs(" ", handler)
//            typesetParagraphs(getTitleFont(), "$ccDefault$thePar", handler)
            typesetParagraphs(getSubtitleFont(), "$ccDefault$thePar", handler)

            // get global yDelta
            doc.currentPageObj.let { page ->
                val yStart = page.drawCalls.minOf { it.posY }
                val yEnd = page.drawCalls.maxOf { it.posY + it.lineCount * doc.lineHeightInPx }
                val pageHeight = doc.textHeight

                val newYpos = (pageHeight - (yEnd - yStart)) / 2
                val yDelta = newYpos - yStart


                page.drawCalls.forEach {
                    val text = it.text?.getText()
                    val batchCall = it.cmd
                    /*if (text != null) {
                        println("Part draw call (${text.size} lines, pos: ${it.posX}, ${it.posY}, width: ${it.width}):" +
                                "\n${text.joinToString("\n") { it.toReadable() }}")
                    }
                    else if (batchCall != null) {
                        println("Part draw call (batch, pos: ${it.posX}, ${it.posY}, width: ${it.width})")
                    }
                    else {
                        println("wtf?")
                    }*/

                    // set posX
                    //// if the batchcall has parent text, use parent's delta value to move things around
                    if (batchCall != null && batchCall.parentText != null) {
                        it.posX += it.cmd.parentText!!.deltaX
                        it.deltaX += it.cmd.parentText!!.deltaX
                    }
                    else {
                        // get individual xDelta
                        val xDelta = (doc.textWidth - it.width) / 2

                        // apply the movement
                        it.posX += xDelta
                        it.deltaX += xDelta
                    }

                    // set posY
                    it.posY += yDelta
                    it.deltaY += yDelta
                }

                println()
            }

            /*doc.currentPageObj.let { page ->
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

            }*/


            if (doc.currentPage % 2 == 0)
                doc.addNewPage()

            doc.addNewPage()
        }

        private fun typesetChapterHeading(chapNum: Int?, thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            val header = if (chapNum == null) thePar else "$chapNum${glueToString(9)}$thePar"
            typesetParagraphs("\n$ccDefault$header", handler, width - indent).also {
                // add indents and adjust text y pos
                it.forEach {
                    it.posX += indent
                    it.posY -= doc.lineHeightInPx / 2
                }
                // add ornamental column on the left
                it.forEach {
                    it.extraDrawFun = { batch, x, y ->
                        val oldCol = batch.color.cpy()
                        batch.color = DEFAULT_ORNAMENTS_COL.cpy().also { it.a *= bodyTextShadowAlpha }
                        Toolkit.fillArea(batch, x - (indent - 2), y + doc.lineHeightInPx, 7f, 1 + (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx.toFloat())
                        batch.color = DEFAULT_ORNAMENTS_COL
                        Toolkit.fillArea(batch, x - (indent - 2), y + doc.lineHeightInPx, 6f, (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx.toFloat())
                        batch.color = oldCol
                    }
                }
            }
        }

        private fun typesetSectionHeading(chapNum: Int, sectNum: Int, thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            typesetParagraphs("\n$ccDefault$chapNum.$sectNum${glueToString(9)}$thePar", handler, width - indent).also {
                // add indents and adjust text y pos
                it.forEach {
                    it.posX += indent
                    it.posY -= doc.lineHeightInPx / 2
                }
            }
        }

        private fun typesetParagraphs(thePar: String, handler: BTeXHandler, width: Int = doc.textWidth, startingPage: Int = doc.currentPage): List<BTeXDrawCall> {
            return typesetParagraphs(getFont(), thePar, handler, width, startingPage)
        }

        private fun typesetParagraphs(font: TerrarumSansBitmap, thePar: String, handler: BTeXHandler, width: Int = doc.textWidth, startingPage: Int = doc.currentPage): List<BTeXDrawCall> {
            val slugs = MovableType(font, thePar, width)
            var pageNum = startingPage

            val drawCalls = ArrayList<BTeXDrawCall>()

            var remainder = doc.pageLines - doc.linesPrintedOnPage.last()
            var slugHeight = slugs.height
            var linesOut = 0

//            printdbg("Page: ${doc.currentPage+1}, Line: ${doc.currentLine}")

            if (slugHeight > remainder) {
                val subset = linesOut to remainder
                val posYline = doc.linesPrintedOnPage[pageNum]

                val textDrawCalls = textToDrawCall(handler, posYline, slugs, subset.first, subset.second)
                val objectDrawCalls = parseAndGetObjDrawCalls(textDrawCalls[0], font, handler, posYline, slugs, subset.first, subset.second)
                (textDrawCalls + objectDrawCalls).let {
                    it.forEach {
                        doc.appendDrawCall(doc.pages[pageNum], it); drawCalls.add(it)
                    }
                }

                linesOut += remainder
                slugHeight -= remainder

                doc.addNewPage(); pageNum += 1
            }

            while (slugHeight > 0) {
                remainder = minOf(slugHeight, doc.pageLines)

                val subset = linesOut to remainder
                val posYline = doc.linesPrintedOnPage[pageNum]

                val textDrawCalls = textToDrawCall(handler, posYline, slugs, subset.first, subset.second)
                val objectDrawCalls = parseAndGetObjDrawCalls(textDrawCalls[0], font, handler, posYline, slugs, subset.first, subset.second)
                (textDrawCalls + objectDrawCalls).let {
                    it.forEach {
                        doc.appendDrawCall(doc.pages[pageNum], it); drawCalls.add(it)
                    }
                }

                linesOut += remainder
                slugHeight -= remainder

                if (remainder == doc.pageLines) {
                    doc.addNewPage(); pageNum += 1
                }
            }

            // if typesetting the paragraph leaves the first line of new page empty, move the "row cursor" back up
            if (doc.linesPrintedOnPage[pageNum] == 1 && doc.pages[pageNum].isEmpty()) doc.linesPrintedOnPage[pageNum] = 0 // '\n' adds empty draw call to the page, which makes isEmpty() to return false

            return drawCalls
        }

        private fun textToDrawCall(handler: BTeXHandler, posYline: Int, slugs: MovableType, lineStart: Int, lineCount: Int): List<BTeXDrawCall> {
            return listOf(
                BTeXDrawCall(
                    doc,
                    0,
                    posYline * doc.lineHeightInPx,
                    handler.currentTheme,
                    TypesetDrawCall(slugs, lineStart, lineCount)
                )
            )
        }

        private fun parseAndGetObjDrawCalls(textDrawCall: BTeXDrawCall, font: TerrarumSansBitmap, handler: BTeXHandler, posYline: Int, slugs: MovableType, lineStart: Int, lineCount: Int): List<BTeXDrawCall> {
            val out = ArrayList<BTeXDrawCall>()

            slugs.typesettedSlugs.subList(lineStart, lineStart + lineCount).forEachIndexed { lineNumCnt, line ->
                line.mapIndexed { i, c -> i to c }.filter { it.second == OBJ }.map { it.first }.forEach { xIndex ->
                    val x = font.getWidthNormalised(CodepointSequence(line.subList(0, xIndex)))
                    val y = (posYline + lineNumCnt) * doc.lineHeightInPx

                    // get OBJ id
                    val idbuf = StringBuilder()

                    var c = xIndex + 1
                    while (true) {
                        val codepoint = line[c]
                        if (codepoint == 0xFFF9F) break
                        idbuf.append(codepointToObjIdChar(codepoint))
                        c += 1
                    }

                    val extraDrawCall = BTeXDrawCall(
                        doc,
                        x,
                        y,
                        handler.currentTheme,
                        cmd = objDict[idbuf.toString()]?.invoke(textDrawCall) ?: throw NullPointerException("No OBJ with id '$idbuf' exists"),
                        font = font,
                    )

                    out.add(extraDrawCall)
                }
            }

            return out
        }

        private fun CodepointSequence.toReadable() = this.joinToString("") {
            if (it in 0x00..0x1f)
                "${(0x2400 + it).toChar()}"
            else if (it == 0x20)
                "\u2423"
            else if (it == NBSP)
                "{NBSP}"
            else if (it == SHY)
                "{SHY}"
            else if (it == ZWSP)
                "{ZWSP}"
            else if (it >= 0xF0000)
                it.toHex() + " "
            else
                Character.toString(it.toChar())
        }

        private fun typesetTOCline(heading: String, name: String, pageNum: Int, handler: BTeXHandler, indentation: Int = 0, pageToWrite: Int? = null) {
            val pageNum = pageNum.plus(1).toString()
            val pageNumWidth = getFont().getWidth(pageNum)
            val typeWidth = doc.textWidth - indentation
            val dotGap = 10
            val dotPosEnd = typeWidth - pageNumWidth - dotGap*1.5f

            typesetParagraphs("$ccDefault$heading$name", handler, typeWidth, pageToWrite ?: doc.currentPage).let {
                it.forEach {
                    it.posX += indentation

//                    println("pos: (${it.posX}, ${it.posY})\tTOC: $name")
                }

                it.last { it.text != null }.let { call ->
                    call.extraDrawFun = { batch, x, y ->
                        val oldCol = batch.color.cpy()

                        batch.color = Color.BLACK

                        val font = getFont()
                        val y = y + (call.lineCount - 1).coerceAtLeast(0) * doc.lineHeightInPx

                        val textWidth = if (call.text is TypesetDrawCall) {
                            font.getWidthNormalised(call.text.movableType.typesettedSlugs.last())
                        }
                        else call.width

                        var dotCursor = (x + textWidth).div(dotGap).ceilToFloat() * dotGap
                        while (dotCursor < x + dotPosEnd) {
                            font.draw(batch, "·", dotCursor + dotGap/2, y)
                            dotCursor += dotGap
                        }

                        font.draw(batch, pageNum, x + typeWidth - pageNumWidth.toFloat(), y)

                        batch.color = oldCol



//                        println("pos: ($x, $y)\tTOC: $name -- dot start: ${(x + textWidth).div(dotGap).ceilToFloat() * dotGap}, dot end: $dotCursor, typeWidth=$typeWidth, pageNumWidth=$pageNumWidth")
                    }
                }
            }
        }


        companion object {
            val ccDefault = TerrarumSansBitmap.toColorCode(0,0,0)
            val ccBucks = TerrarumSansBitmap.toColorCode(4,0,0)
            val ccCode = TerrarumSansBitmap.toColorCode(0,5,7)
            val ccHref = TerrarumSansBitmap.toColorCode(0,3,11)

            private const val ZWSP = 0x200B
            private const val SHY = 0xAD
            private const val NBSP = 0xA0
            private const val OBJ = 0xFFFC
            private const val SPACING_BLOCK_ONE = 0xFFFD0
            private const val SPACING_BLOCK_SIXTEEN = 0xFFFDF

            fun glueToString(glue: Int): String {
                val tokens = CodepointSequence()

                if (glue < 0)
                    throw IllegalArgumentException("Space is less than zero ($glue)")
                else if (glue == 0)
                    tokens.add(ZWSP)
                else if (glue in 1..16)
                    tokens.add(SPACING_BLOCK_ONE + (glue - 1))
                else {
                    val fullGlues = glue / 16
                    val smallGlues = glue % 16
                    if (smallGlues > 0) {
                        tokens.addAll(
                            List(fullGlues) { SPACING_BLOCK_SIXTEEN } +
                                    listOf(SPACING_BLOCK_ONE + (smallGlues - 1))
                        )
                    }
                    else {
                        tokens.addAll(
                            List(fullGlues) { SPACING_BLOCK_SIXTEEN }
                        )
                    }
                }

                return tokens.toUTF8Bytes().toString(Charsets.UTF_8)
            }

            fun objectMarkerWithWidth(id: String, width: Int): String {
                val idstr = CodepointSequence()

                id.forEach {
                    idstr.add(when (it) {
                        '@' -> 0xFFF80
                        '-' -> 0xFFF7D
                        in '0'..'9' -> 0xFFF70 + (it.code - 0x30)
                        in 'A'..'Z' -> 0xFFF81 + (it.code - 0x41)
                        else -> throw IllegalArgumentException("Non-object ID char: $it")
                    })
                }

                idstr.add(0xFFF9F)

                return "\uFFFC" + idstr.toUTF8Bytes().toString(Charsets.UTF_8) + glueToString(width)
            }

            fun Int.toRomanNum(): String = when (this) {
                in 1000..3999 -> "M" + (this - 1000).toRomanNum()
                in 900 until 1000 -> "CM" + (this - 900).toRomanNum()
                in 500 until 900 -> "D" + (this - 500).toRomanNum()
                in 400 until 500 -> "CD" + (this - 400).toRomanNum()
                in 100 until 400 -> "C" + (this - 100).toRomanNum()
                in 90 until 100 -> "XC" + (this - 90).toRomanNum()
                in 50 until 90 -> "L" + (this - 50).toRomanNum()
                in 40 until 50 -> "XL" + (this - 40).toRomanNum()
                in 10 until 40 -> "X" + (this - 10).toRomanNum()
                9 -> "IX"
                in 5 until 9 -> "V" + (this - 5).toRomanNum()
                4 -> "IV"
                in 1 until 4 -> "I" + (this - 1).toRomanNum()
                0 -> ""
                else -> throw IllegalArgumentException("Number out of range: $this")
            }

            private fun codepointToObjIdChar(c: Int): Char {
                return when (c) {
                    in 0xFFF70..0xFFF79 -> (0x30 + (c - 0xFFF70)).toChar()
                    in 0xFFF81..0xFFF9A -> (0x41 + (c - 0xFFF81)).toChar()
                    0xFFF7D -> '-'
                    0xFFF80 -> '@'
                    else -> throw IllegalArgumentException("Non-object ID char: $c")
                }
            }
        }
    }


    private annotation class OpenTag
    private annotation class CloseTag
}



class BTeXParsingException(s: String) : RuntimeException(s) {

}
