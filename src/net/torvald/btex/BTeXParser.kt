package net.torvald.btex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath.DEG_TO_RAD
import net.torvald.colourutil.OKLch
import net.torvald.colourutil.tosRGB
import net.torvald.terrarum.*
import net.torvald.terrarum.btex.*
import net.torvald.terrarum.btex.BTeXDocument.Companion.DEFAULT_ORNAMENTS_COL
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarumsansbitmap.MovableType
import net.torvald.terrarumsansbitmap.TypesettingStrategy
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.unicode.CURRENCY
import net.torvald.unicode.toUTF8Bytes
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.*
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.SAXParserFactory
import kotlin.math.roundToInt
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation


/**
 * Created by minjaesong on 2023-10-28.
 */
object BTeXParser {

    internal val textTags = hashSetOf("P", "CALLOUT", "TITLE", "AUTHOR", "EDITION", "CHAPTER", "SECTION", "LI")
    internal val textDecorTags = hashSetOf("SPAN", "CODE")

    operator fun invoke(file: FileHandle, varMap: Map<String, String>, progressIndicator: AtomicInteger) = invoke(file.file(), varMap, progressIndicator)

    operator fun invoke(file: File, varMap: Map<String, String>, progressIndicator: AtomicInteger): Pair<BTeXDocument, BTeXHandler> {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newInstance().let {
            it.isNamespaceAware = true
            it.isValidating = true
            it.newSAXParser()
        }
        doc.inputXML = file.readText(Common.CHARSET)
        val stream = FileInputStream(file)
        val handler = BTeXHandler(doc, varMap, progressIndicator)
        parser.parse(stream, handler)
        return doc to handler
    }

    operator fun invoke(string: String, varMap: Map<String, String>, progressIndicator: AtomicInteger): Pair<BTeXDocument, BTeXHandler> {
        val doc = BTeXDocument()
        val parser = SAXParserFactory.newInstance().let {
            it.isNamespaceAware = true
            it.isValidating = true
            it.newSAXParser()
        }
        doc.inputXML = string
        val handler = BTeXHandler(doc, varMap, progressIndicator)
        parser.parse(InputSource(StringReader(string)), handler)
        return doc to handler
    }

    class BTeXHandler(val doc: BTeXDocument, val varMap: Map<String, String>, val progressIndicator: AtomicInteger) : DefaultHandler() {
        private var cover = ""
        private var inner = ""
        private var papersize = ""
        private var def = ""

        private var btexOpened = false

        private val blockLut = HashMap<String, ItemID>()

        private val tagStack = ArrayList<String>() // index zero should be "btex"
        private var tagHistory = ArrayList<String>()

        private var currentHrefId: String? = null // any Unicode string that is not empty
        private var oldHrefTarget: String? = null


        private val elemOpeners: HashMap<String, KFunction<*>> = HashMap()
        private val elemClosers: HashMap<String, KFunction<*>> = HashMap()

        private val paragraphBuffer = StringBuilder()
        private var currentAlign = "justify"

        fun clearParBuffer() {
            paragraphBuffer.clear()
            currentAlign = "justify"
        }

        private val objDict = HashMap<String, (DrawCallWrapper) -> BatchDrawCall>()
        private val hrefDict = HashMap<String, String>()
        private val objWidthDict = HashMap<String, Int>()

        private var lastTagAtDepth = Array(24) { "" }
        private var tagCntAtDepth: HashMap<String, IntArray> = HashMap<String, IntArray>().also { map ->
            siblingAwareTags.forEach { tag ->
                map[tag] = IntArray(24)
            }
        }

        private data class CptSect(val type: String, var alt: String?, var style: String, var start: Int? = null)
        private data class CptSectInfo(val type: String, var name: String, var pagenum: Int,
                                       var partNum: String?, var cptNum: String?, var sectNum: String?)

        private val cptSectStack = ArrayList<CptSect>()

        private val cptSectMap = ArrayList<CptSectInfo>()
        private var tocPage: Int? = null

        private var hasCover = false
        private var coverCol: Color? = null

        private val macrodefs = hashMapOf(
            "thepart" to "Part %1\$s",
            "parttype" to "I",
            "thechapter" to "%1\$s",
            "chaptertype" to "1",
            "chapteronnewpage" to "0",
            "resetchapterafterpart" to "0",
            "parindent" to "16"
        )

        private fun invokeMacro(name: String, vararg args: String): String {
            val name = name.lowercase()
            val template = macrodefs[name] ?: throw IllegalArgumentException("No macro '$name' exists")
            return String.format(template, *args)
        }


        private fun Int.toListNumStr(style: String) = when (style) {
            "I" -> this.toRomanNum()
            "i" -> this.toRomanNum().lowercase()
            "1" -> this.toString()
            "A" -> this.toAlphabetic()
            "a" -> this.toAlphabetic().lowercase()
            else -> throw IllegalArgumentException("Unknown list number style: $style")
        }

        private fun StringBuilder.appendObjectPlaceholder(id: String) {
            (objWidthDict[id] ?: throw NullPointerException("No OBJ with id '$id' exists")).let {
                this.append(objectMarkerWithWidth(id, it))
            }
        }

        init {
            if (!fontInit) {
                throw RuntimeException("Font not initialised: call BTeXParser.BTeXHandler.preloadFonts() WITHIN OpenGL-context thread to initialise the fonts.")
            }

            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<OpenTag>() != null }.forEach {
//                println("Tag opener: ${it.name}")
                elemOpeners[it.name] = it
            }

            BTeXHandler::class.declaredFunctions.filter { it.findAnnotation<CloseTag>() != null }.forEach {
//                println("Tag closer: ${it.name}")
                elemClosers[it.name] = it
            }

            objWidthDict["TAG@BTEX"] = 32
            objDict["TAG@BTEX"] = { text: DrawCallWrapper ->
                object : BatchDrawCall(32, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}B", x + ( 0 + 0*interchar)*scale, y + 0*scale)
                        font.draw(batch, "${ccDefault}T", x + ( 8 + 1*interchar)*scale, y + 0*scale)
                        font.draw(batch, "${ccDefault}E", x + (15 + 2*interchar)*scale, y + 4*scale)
                        font.draw(batch, "${ccDefault}X", x + (23 + 3*interchar)*scale, y + 0*scale)
                    }
                    override fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.drawToPixmap(pixmap, "${ccDefault}B", x + ( 0 + 0*interchar)*scale, y + 0*scale)
                        font.drawToPixmap(pixmap, "${ccDefault}T", x + ( 8 + 1*interchar)*scale, y + 0*scale)
                        font.drawToPixmap(pixmap, "${ccDefault}E", x + (15 + 2*interchar)*scale, y + 4*scale)
                        font.drawToPixmap(pixmap, "${ccDefault}X", x + (23 + 3*interchar)*scale, y + 0*scale)
                    }
                }
            }

            objWidthDict["TAG@LATEX"] = 36
            objDict["TAG@LATEX"] = { text: DrawCallWrapper ->
                object : BatchDrawCall(36, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}L", x + (0 + 0 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}ᴀ", x + (4 + 0 * interchar) * scale, y + -4 * scale)
                        font.draw(batch, "${ccDefault}T", x + (12 + 1 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}E", x + (19 + 2 * interchar) * scale, y + 4 * scale)
                        font.draw(batch, "${ccDefault}X", x + (27 + 3 * interchar) * scale, y + 0 * scale)
                    }
                    override fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.drawToPixmap(pixmap, "${ccDefault}L", x + (0 + 0 * interchar) * scale, y + 0 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}ᴀ", x + (4 + 0 * interchar) * scale, y + -4 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}T", x + (12 + 1 * interchar) * scale, y + 0 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}E", x + (19 + 2 * interchar) * scale, y + 4 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}X", x + (27 + 3 * interchar) * scale, y + 0 * scale)
                    }
                }
            }

            objWidthDict["TAG@TEX"] = 24
            objDict["TAG@TEX"] = { text: DrawCallWrapper ->
                object : BatchDrawCall(24, 0, text) {
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.draw(batch, "${ccDefault}T", x + (0 + 1 * interchar) * scale, y + 0 * scale)
                        font.draw(batch, "${ccDefault}E", x + (7 + 2 * interchar) * scale, y + 4 * scale)
                        font.draw(batch, "${ccDefault}X", x + (15 + 3 * interchar) * scale, y + 0 * scale)
                    }
                    override fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int, font: TerrarumSansBitmap?) {
                        val scale = font!!.scale
                        val interchar = font.interchar
                        font.drawToPixmap(pixmap, "${ccDefault}T", x + (0 + 1 * interchar) * scale, y + 0 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}E", x + (7 + 2 * interchar) * scale, y + 4 * scale)
                        font.drawToPixmap(pixmap, "${ccDefault}X", x + (15 + 3 * interchar) * scale, y + 0 * scale)
                    }
                }
            }
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


            siblingAwareTags.forEach { tag ->
                if (lastTagAtDepth[tagStack.size] != tag) tagCntAtDepth[tag]!![tagStack.size] = 0
                if (theTag == tag) tagCntAtDepth[tag]!![tagStack.size] += 1
            }
            lastTagAtDepth[tagStack.size] = theTag


            tagStack.add(theTag)
            tagHistory.add(theTag)


            val attribs = HashMap<String, String>().also {
                it.putAll((0 until attributes.length).map { attributes.getQName(it) to attributes.getValue(it) })
            }


            elemOpeners["processElem$theTag"].let {
                if (it == null)
//                    System.err.println("Unknown tag: $theTag")
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
                    val siblingIndex = tagCntAtDepth[theTag]?.get(tagStack.size) ?: -1
                    it?.call(this, this, doc, uri, siblingIndex)
                }
                catch (e: Throwable) {
                    throw BTeXParsingException(e.stackTraceToString())
                }
            }

//            printdbg("  End element \t($popped)")
        }

        private val CODE_TAG_MARGIN = 2

        private val CODEMODE_BEGIN = "${spacingBlockToString(CODE_TAG_MARGIN)}$ccCode${TerrarumSansBitmap.charsetOverrideCodestyle}"
        private val CODEMODE_END = "${TerrarumSansBitmap.charsetOverrideDefault}$ccDefault${spacingBlockToString(CODE_TAG_MARGIN)}"

        private val HREF_BEGIN = "$ccHref"
        private val HREF_END = "${OBJ.codepointToString()}${OBJ_END.codepointToString()}$ccDefault"

        private val BUCKS_BEGIN = "$ccBucks"
        private val BUCKS_END = "$ccDefault"

        private val EMPH_BEGIN = "$ccEmph"
        private val EMPH_END = "$ccDefault"
        private val ITEMNAME_BEGIN = "$ccItemName"
        private val TARGETNAME_BEGIN = "$ccTargetName"

        private val SPAN_END = "$ccDefault${TerrarumSansBitmap.charsetOverrideDefault}"

        private fun CharArray.toSurrogatedString(): String = if (this.size == 1) "${this[0]}" else "${this[0]}${this[1]}"
        private fun Int.codepointToString() = Character.toChars(this).toSurrogatedString()


        private fun Color.toCC(): String {
            return TerrarumSansBitmap.toColorCode(
                this.r.times(15f).roundToInt(),
                this.g.times(15f).roundToInt(),
                this.b.times(15f).roundToInt()
            )
        }

        private val REGEX_WHITESPACES = Regex("\\s+")

        override fun characters(ch: CharArray, start: Int, length: Int) {
            var str =
                String(ch.sliceArray(start until start + length)).replace('\n', ' ').replace(Regex(" +"), " ")//.trim()

            if (str.isNotEmpty()) {

//                printdbg(str)

                // rising/falling edge of the hrefId
                if (currentHrefId != oldHrefTarget) {
                    // rising edge
                    if (currentHrefId != null) {
//                        printdbg("Href IN($currentHrefId) \t\"$str\"")

                        // put OBJ on every word, separated by whitespaces
                        // transform the word such that:
                        //      word1 word2 -> [OBJ:XXX]word1 [OBJ:YYY]word2
                        str = str.trim().split(" ").map {
                            val wordWidth = getFont().getWidth(it)
                            val btexObjName = "HREF@${makeRandomObjName()}"

                            hrefDict[btexObjName] = currentHrefId!!

                            objectMarkerWithWidth(btexObjName, 0) + it
                        }.joinToString(" ")

                    }
                    // falling edge
                    else {
//                        printdbg("Href OUT(null) \t\"$str\"")
                    }
                }
                // hrefId held high
                else if (currentHrefId != null) {
//                    printdbg("Href($currentHrefId) \t\"$str\"")
                }
                else {
//                    printdbg("String \t\"$str\"")
                }



                oldHrefTarget = currentHrefId
                paragraphBuffer.append(str)
            }
        }

        private fun getFont() = when (cover) {
            "typewriter" -> TODO()
            else -> testFont
        }

        private fun getPartTitleFont(): TerrarumSansBitmap {
            return partTitleFont
        }

        private fun getTitleFont(): TerrarumSansBitmap {
            return titleFont
        }

        private fun getSubtitleFont(): TerrarumSansBitmap {
            return subtitleFont
        }

        private val hexColRegexRGBshort = Regex("#[0-9a-fA-F]{3,3}")
        private val hexColRegexRGB = Regex("#[0-9a-fA-F]{6,6}")

        private fun getSpanColour(spanColour: String): Color =
        if (spanColour.matches(hexColRegexRGB)) {
            val rs = spanColour.substring(1,3)
            val gs = spanColour.substring(3,5)
            val bs = spanColour.substring(5,7)

            val r = rs.toInt(16) / 255f
            val g = gs.toInt(16) / 255f
            val b = bs.toInt(16) / 255f

            Color(r, g, b, 1f)
        }
        else if (spanColour.matches(hexColRegexRGBshort)) {
            val rs = spanColour.substring(1,2)
            val gs = spanColour.substring(2,3)
            val bs = spanColour.substring(3,4)

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
            "standard" to 508,
            "examination" to 640,
        )
        private val pageHeightMap = hashMapOf(
            "standard" to 24,
            "examination" to 18,
        )

        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            if (btexOpened) {
                throw BTeXParsingException("BTEXDOC tag has already opened")
            }

            if (attribs.containsKey("def"))
                def = attribs["def"]!!
            else {
                cover = attribs["cover"] ?: "printout"
                inner = attribs["inner"] ?: "standard"
                papersize = attribs["papersize"] ?: "standard"

                //change the "default values" of the document

                doc.textWidth = pageWidthMap[papersize]!!
                doc.pageLines = pageHeightMap[papersize]!!
            }

            btexOpened = true
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemPAIR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            if (tagStack.size == 3 && tagStack.getOrNull(1) == "BLOCKLUT") {
                blockLut[attribs["key"]!!] = attribs["value"]!!
            }
            else if (tagStack.size == 3 && tagStack.getOrNull(1) == "MACRODEF") {
                val key = attribs["key"]!!
                val value = attribs["value"]!!
                macrodefs[key.lowercase()] = value
            }
            else {
                throw BTeXParsingException("<pair> used outside of <blocklut> or <macrodef>")
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemSUBTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemAUTHOR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemEDITION(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemEMPH(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(EMPH_BEGIN)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(ITEMNAME_BEGIN)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(TARGETNAME_BEGIN)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemSPAN(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            val col = attribs["colour"] ?: attribs["color"]

            if (col != null)
                paragraphBuffer.append(getSpanColour(col).toCC())

            if (attribs["class"] == "code")
                paragraphBuffer.append(TerrarumSansBitmap.charsetOverrideCodestyle)
        }

        @CloseTag
        fun closeElemTARGETNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, uri, siblingIndex)
        @CloseTag
        fun closeElemITEMNAME(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) = closeElemEMPH(handler, doc, uri, siblingIndex)
        @CloseTag
        fun closeElemEMPH(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            paragraphBuffer.append(EMPH_END)
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemBTEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.appendObjectPlaceholder("TAG@BTEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemLATEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.appendObjectPlaceholder("TAG@LATEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.appendObjectPlaceholder("TAG@TEX")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemCODE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(CODEMODE_BEGIN)
        }
        @CloseTag
        fun closeElemCODE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            paragraphBuffer.append(CODEMODE_END)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemA(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(HREF_BEGIN)
            currentHrefId = attribs["href"]
            if (currentHrefId != null && currentHrefId!!.isBlank())
                throw IllegalStateException("Hyperlink target cannot be empty or blank")
        }
        @CloseTag
        fun closeElemA(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            paragraphBuffer.append(HREF_END)



            currentHrefId = null
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemBUCKS(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(BUCKS_BEGIN)
            paragraphBuffer.append("$ccBucks$CURRENCY\u00A0")
        }
        @CloseTag
        fun closeElemBUCKS(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            paragraphBuffer.append(BUCKS_END)
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemCOVER(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            hasCover = true
            val hue = (attribs["hue"]?.toFloatOrNull() ?: 28f) * DEG_TO_RAD
            val coverColLCH = OKLch(hue, 0.05f, 0.36f)
            val (r, g, b) = coverColLCH.tosRGB()
            coverCol = Color(r, g, b, 1f)
            doc.addNewPage(progressIndicator, coverCol!!)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTOCPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage(progressIndicator) // toc: openright
            val header = attribs["title"] ?: "Table of Contents"
            typesetChapterHeading(null, header, handler, HEADING_INDENTATION)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEXPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            if (doc.currentPageObj.isNotEmpty()) doc.addNewPage(progressIndicator)
            val header = attribs["title"] ?: "Index"
            typesetChapterHeading(null, header, handler, HEADING_INDENTATION)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFCONTENTS(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            tocPage = doc.currentPage
            clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            // prepare contents
            val pageWidth = doc.textWidth

            doc.indexTable.keys.toList().sorted().forEach { key ->
                typesetTOCline("", key, doc.indexTable[key]!!, handler, isIndex = true)
            }
        }

        @CloseTag
        fun closeElemTABLEOFINDICES(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage(progressIndicator)
        }

        @CloseTag
        fun closeElemMANUSCRIPT(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if tocPage != null, estimate TOC page size, renumber doc.indexTable and cptSectMap if needed, then typeset the toc
            if (tocPage != null) {
                // estimate the number of TOC pages
                // TOC page always takes up a full paper, therefore tocSizeInPages is always multiple of 2
                var tocSizeInPages = ((cptSectMap.size + 2f) / doc.pageLines).ceilToInt()
                if (tocSizeInPages == 0) tocSizeInPages = 2
                if (tocSizeInPages % 2 == 1) tocSizeInPages += 1

//                printdbg("TOC number of entries: ${cptSectMap.size}, estimated page count: $tocSizeInPages")

                // renumber things
                if (tocSizeInPages > 1) {
                    val pageDelta = tocSizeInPages - 1
                    doc.indexTable.keys.forEach {
                        doc.indexTable[it] = doc.indexTable[it]!! + pageDelta
                    }

                    cptSectMap.forEach { it.pagenum += pageDelta }

                    // insert new pages
                    repeat(pageDelta) {
                        doc.addNewPageAt(progressIndicator, tocPage!! + 1)
                    }
                }

                var currentTOCpage = tocPage!!
                cptSectMap.forEach { (type, name, pg, part, cpt, sect) ->
                    val indent = if (type == "subsection") 2*HEADING_INDENTATION else if (type == "section") HEADING_INDENTATION else 0
                    val heading = if (part == null && cpt == null && sect == null)
                        ""
                    else if (part != null && cpt == null && sect == null)
                        "${invokeMacro("thepart", part)}${spacingBlockToString(HEADING_NUM_TITLE_GAP)}"
                    else if (cpt != null && sect == null)
                        "${invokeMacro("thechapter", cpt)}${spacingBlockToString(HEADING_NUM_TITLE_GAP)}"
                    else
                        "$cpt.$sect${spacingBlockToString(HEADING_NUM_TITLE_GAP)}"

                    typesetTOCline("$heading", name, pg, handler, indent, currentTOCpage)

                    if (doc.linesPrintedOnPage[currentTOCpage] >= doc.pageLines) {
                        currentTOCpage += 1
                    }
                }
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemINDEX(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            attribs["id"]?.let {
                doc.indexTable[it] = doc.currentPage + 1
            }
        }

        private fun tagVimpl(attribs: HashMap<String, String>): String {
            val hasID = (attribs["id"] != null)
            val hasFROMGAME = (attribs["fromgame"] != null)

            if (hasID && hasFROMGAME) {
                throw IllegalStateException("Use only one of following attributes: id, fromgame")
            }
            else if (hasID) {
                val it = attribs["id"]!!
                val value = varMap[it] ?: throw NullPointerException("No variable definition of '$it'")
                return value
            }
            else if (hasFROMGAME) {
                val it = attribs["fromgame"]!!
                val value = Lang.get(it, true)
                return value
            }
            else {
                throw IllegalStateException("One of following attribute required: id, fromgame")
            }
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemV(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append(tagVimpl(attribs))
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVEUN(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            vjosa(tagVimpl(attribs), "는", "은")
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemVNEUN(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            processElemVEUN(handler, doc, uri, attribs)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVI(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            vjosa(tagVimpl(attribs), "가", "이")
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemVGA(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            processElemVI(handler, doc, uri, attribs)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVEUL(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            vjosa(tagVimpl(attribs), "를", "을")
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemVREUL(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            processElemVEUL(handler, doc, uri, attribs)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVWA(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            vjosa(tagVimpl(attribs), "와", "과")
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemVGWA(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            processElemVWA(handler, doc, uri, attribs)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemVRO(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            vjosa(tagVimpl(attribs), "로", "으로")
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemVEURO(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            processElemVRO(handler, doc, uri, attribs)
        }

        @OpenTag // reflective access is impossible with 'private'
        private fun vjosa(str: String, josa1: String, josa2: String) {
            val value = Lang.getHangulJosa(str, josa1, josa2)
            paragraphBuffer.append(value)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemIMG(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            val heightInLines = attribs["height"]!!.toInt()
            val imgHeight = doc.lineHeightInPx * heightInLines - 6
            val btexObjName = "IMG@${makeRandomObjName()}"
            val img = attribs["src"]
            val fromgame = attribs["fromgame"]
            val itemimg = attribs["itemimg"]
            val caption = attribs["caption"]

            if (listOf(img, fromgame, itemimg).count { it != null } != 1) {
                throw IllegalArgumentException()
            }

            // image overflowing?
            if (doc.pageLines - doc.linesPrintedOnPage.last() < heightInLines)
                doc.addNewPage(progressIndicator)

            val tempFile = FileHandle.tempFile("btex_$btexObjName")
            try {

                val inputPixmap = if (img != null) {
                    if (img.startsWith("file://")) {
    //                        printdbg("Using local file ${img.substring(7)}")
                        Pixmap(Gdx.files.absolute(img.substring(7)))
                    }
                    else {
    //                        printdbg("Downloading image $img")
                        BufferedInputStream(URL(img).openStream()).use { `in` ->
                            FileOutputStream(tempFile.file()).use { fileOutputStream ->
                                val dataBuffer = ByteArray(1024)
                                var bytesRead: Int
                                while ((`in`.read(dataBuffer, 0, 1024).also { bytesRead = it }) != -1) {
                                    fileOutputStream.write(dataBuffer, 0, bytesRead)
                                }
                            }
                        }
                        Pixmap(tempFile).also { App.disposables.add(it) }
                    }
                }
                else if (fromgame != null) {
                    val moduleName = fromgame.substringBefore(':')
                    val modulePath = fromgame.substringAfter(':')
                    Pixmap(ModMgr.getGdxFile(moduleName, modulePath))
                }
                else if (itemimg != null) {
                    ItemCodex.getItemImagePixmap(itemimg)!!
                }
                else throw InternalError()

                val imgWidth = (imgHeight.toFloat() / inputPixmap.height * inputPixmap.width).roundToInt()

                if (imgWidth > doc.textWidth)
                    throw RuntimeException("Image width ($imgWidth) is larger than the text width (${doc.textWidth})")

                val drawCallObj = { parentText: DrawCallWrapper -> object : BatchDrawCall(imgWidth, (heightInLines - 1).coerceAtLeast(0), parentText) {
                    private lateinit var inputTexture: Texture
                    override fun draw(doc: BTeXDocument, batch: SpriteBatch, x: Float, y: Float, font: TerrarumSansBitmap?) {
                        if (!::inputTexture.isInitialized) {
                            inputTexture = Texture(inputPixmap).also { App.disposables.add(it) }
                        }
                        val destX = (x + (doc.pageDimensionWidth - imgWidth) / 2)
                        val destY = y + 1
                        batch.draw(inputTexture, destX, destY, imgWidth.toFloat(), imgHeight.toFloat())
                    }
                    override fun drawToPixmap(doc: BTeXDocument, pixmap: Pixmap, x: Int, y: Int, font: TerrarumSansBitmap?) {
                        val destX = x
                        val destY = y + 1
                        pixmap.drawPixmap(inputPixmap, 0, 0, inputPixmap.width, inputPixmap.height, destX, destY, imgWidth, imgHeight)
                    }
                } }

                objDict[btexObjName] = { text: DrawCallWrapper -> drawCallObj(text) }
                objWidthDict[btexObjName] = imgWidth

                typesetParagraphs(objectMarkerWithWidth(btexObjName, imgWidth), handler, align = "center")
                if (caption != null)
                    typesetParagraphs("$ccDefault$caption", handler, align = "center")
            }
            catch (e: IOException) { }
            catch (e: GdxRuntimeException) { }
            finally {
                tempFile.delete()
            }
        }







        @OpenTag // reflective access is impossible with 'private'
        fun processElemBR(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            paragraphBuffer.append("\n")
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemNEWPAGE(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            doc.addNewPage(progressIndicator)
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

            doc.addNewPage(progressIndicator)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemP(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            currentAlign = attribs["align"] ?: "justify"

            if (attribs["class"] == "code")
                paragraphBuffer.append(TerrarumSansBitmap.charsetOverrideCodestyle)
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemCALLOUT(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            currentAlign = attribs["align"] ?: "justify"

            if (attribs["class"] == "code")
                paragraphBuffer.append(TerrarumSansBitmap.charsetOverrideCodestyle)
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemCALLOUT(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if this P is a very first P without chapters, leave two lines before typesetting
            val penultTag = tagHistory.getOrNull(tagHistory.lastIndex - 1)
            val thePar = paragraphBuffer.toString().trim()
            if (penultTag == "MANUSCRIPT")
                doc.linesPrintedOnPage[doc.currentPage] += 2


            typesetParagraphs(
                "$ccDefault$thePar",
                handler,
                width = doc.textWidth - 2*MARGIN_PARBOX_H,
                height = doc.pageLines - 1,
                align = currentAlign
            ).let {
                it.moveObjectsAround(MARGIN_PARBOX_H, doc.lineHeightInPx / 2)

                // add boxes
                it.first.forEach {
                    it.extraDrawFun = { batch, x, y ->
                        val width = doc.textWidth - 2 * MARGIN_PARBOX_H.toFloat()
                        val height = it.lineCount * doc.lineHeightInPx.toFloat()

                        if (height > 0) {
                            val oldcol = batch.color.cpy()
                            batch.color = Color(0xccccccff.toInt())
                            Toolkit.fillArea(
                                batch,
                                x - MARGIN_PARBOX_H,
                                y - MARGIN_PARBOX_V,
                                width + 2 * MARGIN_PARBOX_H,
                                height + 2 * MARGIN_PARBOX_V
                            )
                            batch.color = Color(0x999999ff.toInt())
                            Toolkit.drawBoxBorder(
                                batch,
                                x - MARGIN_PARBOX_H,
                                y - MARGIN_PARBOX_V,
                                width + 2 * MARGIN_PARBOX_H,
                                height + 2 * MARGIN_PARBOX_V
                            )
                            batch.color = oldcol
                        }
                    }
                    it.extraPixmapDrawFun = { pixmap, x, y ->
                        val width = doc.textWidth - 2 * MARGIN_PARBOX_H
                        val height = it.lineCount * doc.lineHeightInPx

                        if (height > 0) {
                            pixmap.setColor(Color(0xccccccff.toInt()))
                            pixmap.fillRectangle(
                                x - MARGIN_PARBOX_H,
                                y - MARGIN_PARBOX_V,
                                width + 2 * MARGIN_PARBOX_H,
                                height + 2 * MARGIN_PARBOX_V
                            )
                            pixmap.setColor(Color(0x999999ff.toInt()))
                            Toolkit.drawBoxBorderToPixmap(
                                pixmap,
                                x - MARGIN_PARBOX_H,
                                y - MARGIN_PARBOX_V,
                                width + 2 * MARGIN_PARBOX_H,
                                height + 2 * MARGIN_PARBOX_V
                            )
                        }
                    }
                }
            }

            insertOneEmptyLineOrAddNewPage()

            clearParBuffer()
        }

        @OpenTag // reflective access is impossible with 'private'
        fun processElemUL(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemOL(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()
        }

        @CloseTag
        fun closeElemUL(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            insertOneEmptyLineOrAddNewPage()
        }
        @CloseTag
        fun closeElemOL(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            insertOneEmptyLineOrAddNewPage()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemLI(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if this P is a very first P without chapters, leave two lines before typesetting
            val mode = tagStack.last()
            val thePar = paragraphBuffer.toString().trim()

            if (mode != "UL" && mode != "OL") throw IllegalStateException("Unknown mode for LI: $mode")

            val heading = if (mode == "UL")
                "•${spacingBlockToString(9)}"
            else {
                "$siblingIndex.${spacingBlockToString(9)}"
            }

            typesetParagraphs(
                "$ccDefault$heading$thePar",
                handler,
                width = doc.textWidth - MARGIN_LIST_LEFT, // only make indent on the left, not tightening the box
                height = doc.pageLines - 1,
                align = currentAlign
            ).moveObjectsAround(MARGIN_LIST_LEFT, doc.lineHeightInPx / 2)

            clearParBuffer()
        }

        @CloseTag
        fun closeElemANONBREAK(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            typesetParagraphs("${ccDefault}――――――――――――", handler, align = "left").also {it.first.first().let {
                it.posX += (doc.textWidth - it.width) / 2
            } }
            clearParBuffer()
        }

        @CloseTag
        fun closeElemCOVER(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            if (hasCover) {
                doc.addNewPage(progressIndicator)
            }
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = paragraphBuffer.toString().trim()
            typesetBookTitle(thePar, handler)
            clearParBuffer()

            doc.theTitle = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSUBTITLE(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = paragraphBuffer.toString().trim()
            typesetBookEdition(thePar, handler)
            clearParBuffer()

            doc.theSubtitle = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemAUTHOR(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = paragraphBuffer.toString().trim()
            typesetBookAuthor(thePar, handler)
            clearParBuffer()

            doc.theAuthor = thePar.replace("\n", " ")
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemEDITION(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val thePar = paragraphBuffer.toString().trim()
            typesetBookEdition(thePar, handler)
            clearParBuffer()

            doc.theEdition = thePar.replace("\n", " ")
        }


        @OpenTag // reflective access is impossible with 'private'
        fun processElemPART(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("part", attribs["alt"], attribs["type"] ?: macrodefs["parttype"]!!, attribs["start"]?.toInt()))
            else
                cptSectStack.add(CptSect("part-hidden", attribs["alt"], attribs["type"] ?: macrodefs["parttype"]!!, attribs["start"]?.toInt()))
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("chapter", attribs["alt"], attribs["type"] ?: macrodefs["chaptertype"]!!, attribs["start"]?.toInt()))
            else
                cptSectStack.add(CptSect("chapter-hidden", attribs["alt"], attribs["type"] ?: macrodefs["chaptertype"]!!, attribs["start"]?.toInt()))
        }
        @OpenTag // reflective access is impossible with 'private'
        fun processElemSECTION(handler: BTeXHandler, doc: BTeXDocument, uri: String, attribs: HashMap<String, String>) {
            clearParBuffer()

            if (attribs["hide"] == null)
                cptSectStack.add(CptSect("section", attribs["alt"], attribs["type"] ?: "1", attribs["start"]?.toInt()))
            else
                cptSectStack.add(CptSect("section-hidden", attribs["alt"], attribs["type"] ?: "1", attribs["start"]?.toInt()))
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemPART(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if the last page is not empty, create new one
            if (doc.currentPageObj.isNotEmpty()) doc.addNewPage(progressIndicator)

            val partOrder = cptSectMap.count { it.type.startsWith("part") } + 1
            val thePar = paragraphBuffer.toString().trim()

            val cptSectInfo = cptSectStack.removeLast()
            val partNumStr = partOrder.toListNumStr(cptSectInfo.style)

            typesetPartHeading(invokeMacro("thepart", partNumStr), thePar, handler)
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("part", cptSectInfo.alt ?: thePar, doc.currentPage, partNumStr, null, null))

            clearParBuffer()
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemCHAPTER(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            val partOrder = cptSectMap.count { it.type.startsWith("part") }
            val cptOrder = if (macrodefs["resetchapterafterpart"] != "0") {
                var cnt = cptSectMap.size - 1
                var sectOrder = 0
                while (cnt >= 0) {
                    if (cptSectMap[cnt].type.startsWith("chapter")) {
                        sectOrder += 1
                    }
                    else break

                    cnt -= 1
                }
                sectOrder + 1
            }
            else
                cptSectMap.count { it.type.startsWith("chapter") } + 1

            val thePar = paragraphBuffer.toString().trim()

            val cptSectInfo = cptSectStack.removeLast()
            val partNumStr = partOrder.toListNumStr(cptSectStack.findLast { it.type.startsWith("part") }?.type ?: "1")
            val cptNumStr = cptOrder.toListNumStr(cptSectInfo.style)
            var cptSibling = 1 // alternative for siblingIndex as the value is always 1 here

            var cnt = cptSectMap.size - 1
            while (cnt >= 0) {
                if (cptSectMap[cnt].type.startsWith("part"))
                    break
                else
                    cptSibling += 1

                cnt -= 1
            }


            // if current line is the last line, proceed to the next page
            if (doc.linesPrintedOnPage.last() >= doc.pageLines - 2) doc.addNewPage(progressIndicator)
            // if defined by the macro, proceed to the next page
            if (macrodefs["chapteronnewpage"] != "0" && cptSibling > 1)
                doc.addNewPage(progressIndicator)


            typesetChapterHeading(invokeMacro("thechapter", cptNumStr), thePar, handler, 16)
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("chapter", cptSectInfo.alt ?: thePar, doc.currentPage, partNumStr, cptNumStr, null))

            clearParBuffer()
        }
        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSECTION(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if current line is the last line, proceed to the next page
            if (doc.linesPrintedOnPage.last() >= doc.pageLines - 1) doc.addNewPage(progressIndicator)

            val partOrder = cptSectMap.count { it.type.startsWith("part") }
            val cptOrder = if (macrodefs["resetchapterafterpart"] != "0") {
                var cnt = cptSectMap.size - 1
                var sectOrder = 0
                while (cnt >= 0) {
                    if (cptSectMap[cnt].type.startsWith("chapter")) {
                        sectOrder += 1
                    }
                    else break

                    cnt -= 1
                }
                sectOrder + 1
            }
            else
                cptSectMap.count { it.type.startsWith("chapter") } + 1

            var sectOrder = 1

            var cnt = cptSectMap.size - 1
            while (cnt >= 0) {
                if (cptSectMap[cnt].type.startsWith("section")) {
                    sectOrder += 1
                }
                else break

                cnt -= 1
            }


            val thePar = paragraphBuffer.toString().trim()

            val cptSectInfo = cptSectStack.removeLast()
            val partNumStr = partOrder.toListNumStr(cptSectStack.findLast { it.type.startsWith("part") }?.type ?: "1")
            val cptNumStr = cptOrder.toListNumStr(cptSectStack.findLast { it.type.startsWith("chapter") }?.type ?: "1")
            val sectNumStr = sectOrder.toListNumStr(cptSectInfo.style)

            typesetSectionHeading("$cptNumStr.$sectNumStr", thePar, handler, 8)
            if (!cptSectInfo.type.endsWith("-hidden"))
                cptSectMap.add(CptSectInfo("section", cptSectInfo.alt ?: thePar, doc.currentPage, partNumStr, cptNumStr, sectNumStr))

            clearParBuffer()
        }



        @CloseTag // reflective access is impossible with 'private'
        fun closeElemP(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // if this P is a very first P without chapters, leave two lines before typesetting
            val penultTag = tagHistory.getOrNull(tagHistory.lastIndex - 1)
            val thePar = paragraphBuffer.toString().trim()

            val indentSize = macrodefs["parindent"]!!.toInt()
            val indent = if (indentSize > 0) spacingBlockToString(indentSize) else ""

            val text =
                // DON't indent on centering context
                if (tagStack.contains("CENTER") || tagStack.contains("FULLPAGEBOX")) thePar
                // indent the second+ pars (or don't indent first par after cpt/sect, anonbreak and br)
                else if (siblingIndex > 1 && penultTag != "ANONBREAK" && penultTag != "BR") "$indent$thePar"
                // if the very first tag within the MANUSCRIPT is par (i.e. no chapter), create a "virtual" chapter
                else if (penultTag == "MANUSCRIPT") "\n\n$thePar"
                // else, print the text normally
                else thePar

            typesetParagraphs("$ccDefault$text", handler, align = currentAlign)

            clearParBuffer()
        }

        @CloseTag // reflective access is impossible with 'private'
        fun closeElemSPAN(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            paragraphBuffer.append(SPAN_END)
        }

        @CloseTag
        fun closeElemBTEXDOC(handler: BTeXHandler, doc: BTeXDocument, uri: String, siblingIndex: Int) {
            // make sure the last pair ends with paper and end-cover
            doc.endOfPageStart = doc.currentPage + 1
            if (doc.pages.size % 2 == 1) doc.addNewPage(progressIndicator)
            doc.addNewPage(progressIndicator)
        }




        private fun typesetBookTitle(thePar: String, handler: BTeXHandler) {
            val label = "\n${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar
            typesetParagraphs(getTitleFont(), label, handler, doc.textWidth - 2*MARGIN_TITLE_TEXTS, align = "left").first.also {
                val addedLines = it.sumOf { it.lineCount }
                doc.linesPrintedOnPage[doc.currentPage] += addedLines

                it.forEach {
                    it.posX += MARGIN_TITLE_TEXTS
                }
            }
        }

        private fun typesetBookAuthor(thePar: String, handler: BTeXHandler) {
            typesetParagraphs(
                getSubtitleFont(),
                "\n\n${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar,
                handler,
                doc.textWidth - 2*MARGIN_TITLE_TEXTS,
                align = "left"
            ).also {
                it.first.last().extraDrawFun = { batch, x, y ->
                    val px = x
                    val py = y + doc.lineHeightInPx - 1
                    val pw = doc.textWidth - 2f * MARGIN_TITLE_TEXTS
                    batch.color = Color(1f,1f,1f,.5f)
                    Toolkit.fillArea(batch, px, py, pw+1, 2f)
                    batch.color = Color.WHITE
                    Toolkit.fillArea(batch, px, py, pw, 1f)
                }
                it.first.last().extraPixmapDrawFun = { pixmap, x, y ->
                    val px = x
                    val py = y + doc.lineHeightInPx - 1
                    val pw = doc.textWidth - 2 * MARGIN_TITLE_TEXTS
                    pixmap.setColor(Color(1f,1f,1f,.5f))
                    pixmap.fillRectangle(px, py, pw+1, 2)
                    pixmap.setColor(Color.WHITE)
                    pixmap.fillRectangle(px, py, pw, 1)
                }

                it.moveObjectsAround(MARGIN_TITLE_TEXTS, 0)
            }
        }

        private fun typesetBookEdition(thePar: String, handler: BTeXHandler) {
            typesetParagraphs(
                getSubtitleFont(),
                "${TerrarumSansBitmap.toColorCode(15, 15, 15)}" + thePar,
                handler,
                doc.textWidth - 2*MARGIN_TITLE_TEXTS,
                align = "left"
            ).moveObjectsAround(MARGIN_TITLE_TEXTS, 0)
        }

        private fun typesetPartHeading(num: String, thePar: String, handler: BTeXHandler, indent: Int = HEADING_INDENTATION, width: Int = doc.textWidth) {
            typesetParagraphs("${ccDefault}⁃ $num ⁃", handler, align = "left")
            typesetParagraphs(" ", handler, align = "left")
            typesetParagraphs(getPartTitleFont(), "$ccDefault$thePar", handler, align = "left")

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
            }

            // make sure page after the part always openright
            if (doc.currentPage % 2 == 1)
                doc.addNewPage(progressIndicator)

            doc.addNewPage(progressIndicator)
        }

        private fun typesetChapterHeading(num: String?, thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            val header = if (num == null) thePar else "$num${spacingBlockToString(9)}$thePar"
            typesetParagraphs(getPartTitleFont(), "\n$ccDefault$header", handler, width - indent, align = "left").also {
                // add indents and adjust text y pos
                it.moveObjectsAround(indent, -doc.lineHeightInPx / 2)


                // add ornamental column on the left
                it.first.forEach {
                    it.extraDrawFun = { batch, x, y ->
                        val yoff = -2
                        val oldCol = batch.color.cpy()
                        batch.color = DEFAULT_ORNAMENTS_COL.cpy().also { it.a *= bodyTextShadowAlpha }
                        Toolkit.fillArea(batch,
                            x - (indent - 2),
                            y + doc.lineHeightInPx + yoff,
                            7f,
                            1 + (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx.toFloat()
                        )
                        batch.color = DEFAULT_ORNAMENTS_COL
                        Toolkit.fillArea(batch,
                            x - (indent - 2),
                            y + doc.lineHeightInPx + yoff,
                            6f,
                            (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx.toFloat()
                        )
                        batch.color = oldCol
                    }
                    it.extraPixmapDrawFun = { pixmap, x, y ->
                        val yoff = -2
                        pixmap.setColor(DEFAULT_ORNAMENTS_COL.cpy().also { it.a *= bodyTextShadowAlpha })
                        pixmap.fillRectangle(
                            x - (indent - 2),
                            y + doc.lineHeightInPx + yoff,
                            7,
                            1 + (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx
                        )
                        pixmap.setColor(DEFAULT_ORNAMENTS_COL)
                        pixmap.fillRectangle(
                            x - (indent - 2),
                            y + doc.lineHeightInPx + yoff,
                            6,
                            (it.lineCount - 1).coerceAtLeast(1) * doc.lineHeightInPx
                        )
                    }
                }
            }
        }

        private fun typesetSectionHeading(num: String, thePar: String, handler: BTeXHandler, indent: Int = 16, width: Int = doc.textWidth) {
            typesetParagraphs("\n$ccDefault$num${spacingBlockToString(9)}$thePar", handler, width - indent, align = "left").also {
                // add indents and adjust text y pos
                it.moveObjectsAround(indent, -doc.lineHeightInPx / 2)
            }
        }

        private fun typesetParagraphs(
            thePar: String,
            handler: BTeXHandler,
            width: Int = doc.textWidth,
            height: Int = doc.pageLines,
            startingPage: Int = doc.currentPage,
            align: String
        ): Pair<List<DrawCallWrapper>, List<BTeXClickable>> {
            return typesetParagraphs(getFont(), thePar, handler, width, height, startingPage, align)
        }

        private fun typesetParagraphs(
            font: TerrarumSansBitmap,
            thePar: String,
            handler: BTeXHandler,
            width: Int = doc.textWidth,
            height: Int = doc.pageLines,
            startingPage: Int = doc.currentPage,
            align: String
        ): Pair<List<DrawCallWrapper>, List<BTeXClickable>> {
            val strat = when (align) {
                "left" -> TypesettingStrategy.RAGGED_RIGHT
                "right" -> TypesettingStrategy.RAGGED_LEFT
                "center", "centre" -> TypesettingStrategy.CENTRED
                "justify" -> TypesettingStrategy.JUSTIFIED
                else -> throw IllegalArgumentException("Unknown text alignment")
            }
            val slugs = MovableType(font, thePar, width, strat)
            var pageNum = startingPage

            val drawCalls = ArrayList<DrawCallWrapper>()
            val clickables = ArrayList<BTeXClickable>()

            var remainder = height - doc.linesPrintedOnPage.last()
            var slugHeight = slugs.height
            var linesOut = 0

//            printdbg("Page: ${doc.currentPage+1}, Line: ${doc.currentLine}")

            if (remainder <= 0) {
                doc.addNewPage(progressIndicator); pageNum += 1
            }
            else if (slugHeight > remainder) {
                val subset = linesOut to remainder
                val posYline = doc.linesPrintedOnPage[pageNum]

//                println("typeset par slugHeight=$slugHeight, remainder=$remainder, linesOut=$linesOut")

                val textDrawCalls = textToDrawCall(doc.pages[pageNum], posYline, slugs, subset.first, subset.second)
                val objectDrawCalls = parseAndGetObjDrawCalls(textDrawCalls[0], font, doc.pages[pageNum], posYline, slugs, subset.first, subset.second)
                (textDrawCalls + objectDrawCalls).let {
                    it.forEach {
                        doc.appendDrawCall(doc.pages[pageNum], it); drawCalls.add(it)
                    }
                }
                // >>> HREF code here!! <<<
                val hrefs = parseAndGetHref(textDrawCalls[0], font, handler, posYline, slugs, subset.first, subset.second)
                var objectIsSplit = false
                hrefs.forEach { (hrefObj, objSeq) ->
                    // search for:
                    // ..... [OBJ:RSETNFAOON]word setaf
                    // get width of "word"
                    val searchStrs = slugs.typesettedSlugs.subList(subset.first, subset.first + subset.second)
                    searchStrs.forEach { str ->
//                        printdbg("1HREF searchStr: ${str.toReadable()}")
//                        printdbg("1HREF object: ${objSeq.toReadable()} (id=${hrefDict[objSeq.toReadable()]})")

                        val indexOfSequence = str.indexOfSequence(objSeq)

                        val theIndex = if (objectIsSplit) -(objSeq.size) else indexOfSequence

                        theIndex?.let { index -> // we never know which line the object appears
                            val wordOffset = index + objSeq.size // must be index of starting NUL
                            // target word is on the current line
                            if (wordOffset < str.size) {
                                var wordEnd = wordOffset + 1 // will be right on the ending NUL
                                // find ending OBJ
                                while (!(wordEnd >= str.size || str[wordEnd] == OBJ)) {
                                    wordEnd++
                                }
                                val substr = CodepointSequence(str.subList(wordOffset + 1, wordEnd))

//                                printdbg("1HREF word: ${substr.toReadable()}")
//                                printdbg("1HREF hrefObj: ${hrefObj}")

                                val hrefX = if (objectIsSplit) 0 else hrefObj.x
                                var hrefY = hrefObj.y; if (objectIsSplit) hrefY += doc.lineHeightInPx

                                val clickable = BTeXClickable(hrefX, hrefY, getFont().getWidth(substr), doc.lineHeightInPx, false) { viewer ->
                                    viewer.getPageOfIndex(hrefObj.hrefTarget)
                                }
                                doc.appendClickable(doc.pages[pageNum], clickable); clickables.add(clickable)

                                objectIsSplit = false
                            }
                            // target word is on the next line (probably)
                            else {
//                                printdbg("1HREF object was cut off by the linebreak")
                                objectIsSplit = true
                            }
                        }

//                        printdbg("  ")
                    }
                }
                // >>> HREF code ends here!! <<<

                linesOut += remainder
                slugHeight -= remainder

                doc.addNewPage(progressIndicator); pageNum += 1
            }

            while (slugHeight > 0) {
                remainder = minOf(slugHeight, height)

                val subset = linesOut to remainder
                val posYline = doc.linesPrintedOnPage[pageNum]

                val textDrawCalls = textToDrawCall(doc.pages[pageNum], posYline, slugs, subset.first, subset.second)
                val objectDrawCalls = parseAndGetObjDrawCalls(textDrawCalls[0], font, doc.pages[pageNum], posYline, slugs, subset.first, subset.second)
                (textDrawCalls + objectDrawCalls).let {
                    it.forEach {
                        doc.appendDrawCall(doc.pages[pageNum], it); drawCalls.add(it)
                    }
                }
                // >>> HREF code here!! <<<
                val hrefs = parseAndGetHref(textDrawCalls[0], font, handler, posYline, slugs, subset.first, subset.second)
                var objectIsSplit = false
                hrefs.forEach { (hrefObj, objSeq) ->
                    // search for:
                    // ..... [OBJ:RSETNFAOON]word setaf
                    // get width of "word"
                    val searchStrs = slugs.typesettedSlugs.subList(subset.first, subset.first + subset.second)
                    searchStrs.forEach { str ->
//                        printdbg("2HREF searchStr: ${str.toReadable()}")
//                        printdbg("2HREF object: ${objSeq.toReadable()} (id=${hrefDict[objSeq.toReadable()]})")

                        val indexOfSequence = str.indexOfSequence(objSeq)

                        val theIndex = if (objectIsSplit) -(objSeq.size) else indexOfSequence

                        theIndex?.let { index -> // we never know which line the object appears
                            val wordOffset = index + objSeq.size // must be index of starting NUL
                            // target word is on the current line
                            if (wordOffset < str.size) {
                                var wordEnd = wordOffset + 1 // will be right on the ending NUL
                                // find ending OBJ
                                while (!(wordEnd >= str.size || str[wordEnd] == OBJ)) {
                                    wordEnd++
                                }
                                // if searching finished without finding OBJ, mark it
                                val objectIsSplit2 = (wordEnd >= str.size)

                                // retrieve the actual word
                                val substr = CodepointSequence(str.subList(wordOffset + 1, wordEnd))

//                                printdbg("2HREF word: ${substr.toReadable()}")
//                                printdbg("2HREF hrefObj: ${hrefObj}")

                                val hrefX = if (objectIsSplit) 0 else hrefObj.x
                                var hrefY = hrefObj.y; if (objectIsSplit) hrefY += doc.lineHeightInPx

                                val clickable = BTeXClickable(hrefX, hrefY, getFont().getWidth(substr), doc.lineHeightInPx) { viewer ->
                                    viewer.getPageOfIndex(hrefObj.hrefTarget)
                                }
                                doc.appendClickable(doc.pages[pageNum], clickable); clickables.add(clickable)

                                objectIsSplit = objectIsSplit2
                            }
                            // target word is on the next line (probably)
                            else {
//                                printdbg("2HREF object was cut off by the linebreak")
                                objectIsSplit = true
                            }
                        }

//                        printdbg("  ")
                    }
                }
                // >>> HREF code ends here!! <<<

                linesOut += remainder
                slugHeight -= remainder

                if (remainder == height) {
                    doc.addNewPage(progressIndicator); pageNum += 1
                }
            }

            // if typesetting the paragraph leaves the first line of new page empty, move the "row cursor" back up
            if (doc.linesPrintedOnPage[pageNum] == 1 && doc.pages[pageNum].isEmpty()) doc.linesPrintedOnPage[pageNum] = 0 // '\n' adds empty draw call to the page, which makes isEmpty() to return false

            return drawCalls to clickables
        }

        private fun textToDrawCall(page: BTeXPage, posYline: Int, slugs: MovableType, lineStart: Int, lineCount: Int): List<DrawCallWrapper> {
            return listOf(
                DrawCallWrapper(
                    doc, page, 0, posYline * doc.lineHeightInPx,
                    TypesetDrawCall(slugs, lineStart, lineCount)
                )
            )
        }

        private fun parseAndGetObjDrawCalls(
            textDrawCall: DrawCallWrapper,
            font: TerrarumSansBitmap,
            page: BTeXPage,
            posYline: Int,
            slugs: MovableType,
            lineStart: Int,
            lineCount: Int
        ): List<DrawCallWrapper> {
            val out = ArrayList<DrawCallWrapper>()

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

                    if (idbuf.isNotBlank() && !idbuf.startsWith("HREF@")) {
                        out.add(DrawCallWrapper(
                            doc, page, x, y,
                            cmd = objDict[idbuf.toString()]?.invoke(textDrawCall)
                                ?: throw NullPointerException("No OBJ with id '$idbuf' exists"),
                            font = font,
                        ))
                    }
                }
            }

            return out
        }

        private fun parseAndGetHref(
            textDrawCall: DrawCallWrapper,
            font: TerrarumSansBitmap,
            handler: BTeXHandler,
            posYline: Int,
            slugs: MovableType,
            lineStart: Int,
            lineCount: Int
        ): List<Pair<_HrefObject, CodepointSequence>> {
            val out = ArrayList<Pair<_HrefObject, CodepointSequence>>()

            slugs.typesettedSlugs.subList(lineStart, lineStart + lineCount).forEachIndexed { lineNumCnt, line ->
                line.mapIndexed { i, c -> i to c }.filter { it.second == OBJ }.map { it.first }.forEach { xIndex ->
                    val x = font.getWidthNormalised(CodepointSequence(line.subList(0, xIndex)))
                    val y = (posYline + lineNumCnt) * doc.lineHeightInPx

                    // get OBJ id
                    val idbuf = StringBuilder()
                    val cpseq = CodepointSequence()

                    var c = xIndex + 1
                    while (true) {
                        val codepoint = line[c]
                        if (codepoint == 0xFFF9F) break
                        idbuf.append(codepointToObjIdChar(codepoint))
                        cpseq.add(codepoint)
                        c += 1
                    }

                    if (idbuf.isNotBlank() && idbuf.startsWith("HREF@")) {
                        val id = hrefDict[idbuf.toString()]!!
                        out.add(_HrefObject(x, y, id) to cpseq)
                    }
                }
            }

            return out
        }

        private fun typesetTOCline(heading: String, name: String, pageNumInt: Int, handler: BTeXHandler, indentation: Int = 0, pageToWrite: Int? = null, isIndex: Boolean = false) {
            val pageNum = if (isIndex)
                pageNumInt.toString()
            else
                pageNumInt.plus(1).toString()

            val pageNumWidth = getFont().getWidth(pageNum)
            val typeWidth = doc.textWidth - indentation
            val dotGap = 10
            val dotPosEnd = typeWidth - pageNumWidth - dotGap*1.5f

            typesetParagraphs("$ccDefault$heading$name", handler, typeWidth - pageNumWidth - dotGap, startingPage = pageToWrite ?: doc.currentPage, align = "justify").let {
                it.moveObjectsAround(indentation, 0)

                it.first.last { it.text != null }.let { call ->
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
                    call.extraPixmapDrawFun = { pixmap, x, y ->
                        val font = getFont()
                        val y = y + (call.lineCount - 1).coerceAtLeast(0) * doc.lineHeightInPx

                        val textWidth = if (call.text is TypesetDrawCall) {
                            font.getWidthNormalised(call.text.movableType.typesettedSlugs.last())
                        }
                        else call.width

                        var dotCursor = (x.toFloat() + textWidth).div(dotGap).ceilToInt() * dotGap
                        while (dotCursor < x + dotPosEnd) {
                            font.drawToPixmap(pixmap, "$ccDefault·", dotCursor + dotGap/2, y)
                            dotCursor += dotGap
                        }

                        font.drawToPixmap(pixmap, "$ccDefault$pageNum", x + typeWidth - pageNumWidth, y)
                    }

                    // create and add hyperlinks to the page
                    if (call.text?.getText() != null) {
                        val boxh = call.text.getText().size * doc.lineHeightInPx
                        val boxw = doc.textWidth
                        val boxx = call.posX - indentation
                        val boxy = call.posY
                        val target = pageNumInt
                        val thePage = call.pageObject

                        thePage.appendClickable(BTeXClickable(boxx, boxy, boxw, boxh, false) {
                            target
                        })
                    }

                }
            }
        }

        private fun insertOneEmptyLineOrAddNewPage() {
            if (doc.linesPrintedOnPage[doc.currentPage] < doc.pageLines)
                doc.linesPrintedOnPage[doc.currentPage] += 1
            else
                doc.addNewPage(progressIndicator)
        }

        private data class _HrefObject(val x: Int, val y: Int, val hrefTarget: String)

        companion object {
            init {
                App.disposables.add(object : Disposable {
                    override fun dispose() {
                        testFont.dispose()
                        partTitleFont.dispose()
                        titleFont.dispose()
                        subtitleFont.dispose()
                    }
                })
            }

            private val siblingAwareTags = arrayOf(
                "PART","CHAPTER","SECTION","SUBSECTION","P","I","LI"
            )

            private val bodyTextShadowAlpha = 0.36f

            private var fontInit = false
            private lateinit var testFont: TerrarumSansBitmap
            private lateinit var partTitleFont: TerrarumSansBitmap
            private lateinit var titleFont: TerrarumSansBitmap
            private lateinit var subtitleFont: TerrarumSansBitmap

            fun preloadFonts() {
                testFont = TerrarumSansBitmap(App.FONT_DIR, shadowAlpha = bodyTextShadowAlpha, textCacheSize = 4096)
                partTitleFont = TerrarumSansBitmap(App.FONT_DIR, shadowAlpha = bodyTextShadowAlpha).also {
                    it.interchar = 1
                }
                titleFont = TerrarumSansBitmap(App.FONT_DIR).also {
                    it.interchar = 1
                    it.scale = 2
                }
                subtitleFont = TerrarumSansBitmap(App.FONT_DIR).also {
                    it.interchar = 1
                }
                fontInit = true
            }

            private const val MARGIN_PARBOX_V = 4
            private const val MARGIN_PARBOX_H = 12
            private const val MARGIN_TITLE_TEXTS = 8
            private const val MARGIN_LIST_LEFT = 6
            private const val HEADING_INDENTATION = 16
            private const val HEADING_NUM_TITLE_GAP = 9

            val ccDefault = TerrarumSansBitmap.toColorCode(0,0,0)
            val ccBucks = TerrarumSansBitmap.toColorCode(10,0,6)
            val ccCode = TerrarumSansBitmap.toColorCode(12,0,7)
            val ccHref = TerrarumSansBitmap.toColorCode(0,3,11)
            val ccEmph = TerrarumSansBitmap.toColorCode(0xfc11)
            val ccItemName = TerrarumSansBitmap.toColorCode(0xf03b)
            val ccTargetName = TerrarumSansBitmap.toColorCode(0xf170)

            private const val NUL = 0
            private const val ZWSP = 0x200B
            private const val SHY = 0xAD
            private const val NBSP = 0xA0
            private const val OBJ = 0xFFFC
            private const val OBJ_END = 0xFFF9F
            private const val SPACING_BLOCK_ONE = 0xFFFD0
            private const val SPACING_BLOCK_SIXTEEN = 0xFFFDF

            fun spacingBlockToString(glue: Int): String {
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

                if (width != 0)
                    return "\uFFFC" + idstr.toUTF8Bytes().toString(Charsets.UTF_8) + spacingBlockToString(width)
                else
                    return "\uFFFC" + idstr.toUTF8Bytes().toString(Charsets.UTF_8)
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

            fun Int.toAlphabetic(): String = when (this) {
                in 27..2147483647 -> (this / 26).toAlphabetic() + (this % 26).toAlphabetic()
                in 1..26 -> "${Char(this + 0x40)}"
                0 -> ""
                else -> throw IllegalArgumentException("Number is negative: $this")
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

            private fun makeRandomObjName() = (0 until 16).joinToString("") { "${hashStrMap.random()}" }

            // KMP algorithm
            internal fun CodepointSequence.indexOfSequence(pattern: CodepointSequence): Int? {
                if (pattern.isEmpty())
                    throw IllegalArgumentException("Pattern is empty")
                if (this.isEmpty())
                    throw IllegalArgumentException("String is empty")

                // pattern cannot exist because the string is shorter than the pattern
                if (this.size < pattern.size)
                    return null

                // next[i] stores the index of the next best partial match
                val next = IntArray(pattern.size + 1)
                for (i in 1 until pattern.size) {
                    var j = next[i]

                    while (j > 0 && this[j] != this[i]) {
                        j = next[j]
                    }

                    if (j > 0 || this[j] == this[i]) {
                        next[i + 1] = j + 1
                    }
                }

                var i = 0
                var j = 0
                while (i < this.size) {
                    if (j < pattern.size && this[i] == pattern[j]) {
                        if (++j == pattern.size) {
                            return i - j + 1
                        }
                    }
                    else if (j > 0) {
                        j = next[j]
                        i-- // since i will be incremented in the next iteration
                    }
                    i++
                }

                return null
            }
        }
    }


    private annotation class OpenTag
    private annotation class CloseTag
}

private fun Pair<List<DrawCallWrapper>, List<BTeXClickable>>.moveObjectsAround(x: Int, y: Int) {
    this.first.forEachIndexed { index, it ->
        it.posX += x
        it.deltaX += x
        it.posY += y
        it.deltaY += y
    }
    this.second.forEachIndexed { index, it ->
        it.posX += x
        it.deltaX += x
        it.posY += y
        it.deltaY += y
    }
}


class BTeXParsingException(s: String) : RuntimeException(s) {

}
