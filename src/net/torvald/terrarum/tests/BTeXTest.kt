package net.torvald.terrarum.tests

import org.xml.sax.Attributes
import org.xml.sax.HandlerBase
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory


/**
 * Created by minjaesong on 2023-10-28.
 */
fun main() {
    val csiR = "\u001B[31m"
    val csiG = "\u001B[32m"
    val csi0 = "\u001B[m"

    val tex = """<btex cover="hardcover" inner="standard" papersize="standard">
<cover>
    <title>The Way to Mastery of Lorem Ipsum<br />Or, How To Write and Publish a Book</title>
    <author>Terran Publishing</author>
    <edition>Test Edition</edition>
</cover>

<toc><tableofcontents /></toc>

<manuscript>

    <chapter>What Is a Book</chapter>

    <p>This example book is designed to give you the exampe of the Book Language.</p>

    <section>What Really Is a Book</section>

    <p>A book is a collection of texts printed in a special way that allows them to be read easily, with
    enumerable pages and insertion of other helpful resources, such as illustrations and hyperlinks.</p>

    <newpage />

    <fullpagebox>
        <span colour="grey">
            <p>this page is intentionally left blank</p>
        </span>
    </fullpagebox>

    <chapter>Writing Book Using Pen and Papers</chapter>

    <p>If you open a book on a writing table, you will be welcomed with a toolbar used to put other book
    elements, such as chapters, sections.</p>

    <chapter>Writing Book Using Typewriter</chapter>

    <p>Typewriters can only write single style of font, therefore chapters and sections are not available.</p>

    <chapter>Writing Book using Computer</chapter>

    <p>Writing book using a computer requires a use of the Book Typesetting Engine Extended, or <BTeX /></p>

    <section>Full Control of the Shape</section>

    <p>With <BTeX /> you can fully control how your publishing would look like, from a pile of papers that
    look like they have been typed out using typewriter, a pile of papers but a fully-featured printouts that
    have illustrations in it, to a fully-featured hardcover book.</p>

    <p>This style is controlled using the <code>cover</code> attribute on the root tag, with following
    values: <code>typewriter</code>, <code>printout</code>, <code>hardcover</code></p>

    <p>Typewriter and Printout are considered not bound and readers will only see one page at a time,
    while Hardcover is considered bound and two pages are presented to the readers.</p>

</manuscript>
</btex>


"""

    val parser = SAXParserFactory.newDefaultInstance().newSAXParser()
    val stream: InputStream = ByteArrayInputStream(tex.encodeToByteArray())
    val hb = object : DefaultHandler() {
        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            println(" $csiG$qName$csi0 ${(0 until attributes.length).map { "${attributes.getQName(it)}=${attributes.getValue(it)}" }}")
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            println("$csiG/$qName$csi0")
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            val str = String(ch.sliceArray(start until start+length)).replace('\n',' ').replace(Regex(" +"), " ").trim()
            if (str.isNotBlank()) {
                println("$str|")
            }
        }



    }
    parser.parse(stream, hb)
}