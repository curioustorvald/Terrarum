package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.btex.BTeXParser
import net.torvald.terrarum.btex.BTeXDocument


/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXTest : ApplicationAdapter() {

    val csiR = "\u001B[31m"
    val csiG = "\u001B[32m"
    val csi0 = "\u001B[m"

    val tex = """<btexdoc cover="hardcover" inner="standard" papersize="standard">
<cover>
    <title>The Way to Mastery of Lorem Ipsum<br />Or, How To Write and Publish a Book</title>
    <author>Terran Publishing</author>
    <edition>Test Edition</edition>
</cover>

<toc><tableofcontents /></toc>

<manuscript>
    
    
    
    <chapter>What Is a Book</chapter>

    <p>This example book is designed to give you the example of the Book Language.</p>

    <section>What Really Is a Book</section>

    <p>A book is a collection of texts printed in a special way that allows them to be read easily, with
        enumerable pages and insertion of other helpful resources, such as illustrations and hyperlinks.</p>

    <newpage />

    <fullpagebox>
        <p><span colour="grey">
            this page is intentionally left blank
        </span></p>
    </fullpagebox>

    
    
    
    <chapter>Writing Book Using Pen and Papers</chapter>

    <p><index id="pen and paper" />If you open a book on a writing table, you will be welcomed with a
        toolbar used to put other book elements, such as chapters, sections.</p>

    
    
    
    <chapter>Writing Book Using Typewriter</chapter>

    <p><index id="typewriter" />Typewriters can only write single style of font, therefore chapters and
        sections are not available.</p>

    
    
    
    <chapter>Writing Book using Computer</chapter>

    <p>Writing book using a computer requires a use of the Book Typesetting Engine Extended, or <btex /></p>

    <section>Full Control of the Shape</section>

    <p><index id="btex language" />With <btex /> you can fully control how your publishing would look like,
        from a pile of papers that look like they have been typed out using typewriter, a pile of papers but a
        fully-featured printouts that have illustrations in it, to a fully-featured hardcover book.</p>

    <p><index id="cover" />This style is controlled using the <code>cover</code> attribute on the root tag,
        with following values: <code>typewriter</code>, <code>printout</code>, <code>hardcover</code></p>

    <p>Typewriter and Printout are considered not bound and readers will only see one page at a time,
        while Hardcover is considered bound and two pages are presented to the readers.</p>

</manuscript>

<indexpage><tableofindices /></indexpage>
</btexdoc>

"""

    private lateinit var document: BTeXDocument

    override fun create() {
        document = BTeXParser.invoke(tex)
    }



}

fun main() {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(1280, 720)
    appConfig.setForegroundFPS(60)
    appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)

    Lwjgl3Application(BTeXTest(), appConfig)
}