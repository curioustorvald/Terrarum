package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.btex.BTeXParser
import net.torvald.terrarum.FlippingSpriteBatch
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.inUse


/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXTest : ApplicationAdapter() {

    val csiR = "\u001B[31m"
    val csiG = "\u001B[32m"
    val csi0 = "\u001B[m"

    val tex = """<btexdoc cover="hardcover" inner="standard" papersize="standard">
<cover hue="160">
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
        enumerable pages and insertion of other helpful resources, such as illustrations and <a href="btex language">hyperlinks</a>.</p>

    <newpage />

    <fullpagebox>
        <p><span colour="grey">
            this page is intentionally left blank
        </span></p>
    </fullpagebox>




    <chapter>Writing Book Using Pen and Papers</chapter>

    <p><index id="pen and paper" />If you open a book on a writing table, you will be welcomed with a
        toolbar used to put other book elements, such as chapters and sections.</p>




    <chapter>Writing Book Using a Typewriter</chapter>

    <p><index id="typewriter" />Typewriters can only write in a single style of font, chapters and
        sections are not available.</p>




    <chapter>Writing Book Using a Computer</chapter>

    <p>Writing book using a computer requires the use of the Book Typesetting Engine Extended, or <btex />.</p>

    <section>Full Control of the Shape</section>

    <p><index id="btex language" />With <btex /> you can fully control how your publishing would look like,
        from a pile of papers that look like they have been typed out using typewriter, a pile of printouts 
        that have pictures in it, to a true hardcover book.</p>

    <p><index id="cover" />This style is controlled using the <code>cover</code> attribute on the root tag,
        with following values: <code>typewriter</code>, <code>printout</code> and <code>hardcover</code>.</p>

    <p>Typewriter and Printout are considered not-bound and readers will only see one page at a time,
        while Hardcover is considered bound and two pages are presented to the readers.</p>

</manuscript>

<indexpage><tableofindices /></indexpage>
</btexdoc>



"""

    private lateinit var document: BTeXDocument
    private lateinit var batch: FlippingSpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var bg: TextureRegion

    override fun create() {
        batch = FlippingSpriteBatch(1000)
        camera = OrthographicCamera(1280f, 720f)
        camera.setToOrtho(true) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined

        bg = TextureRegion(Texture(Gdx.files.internal("test_assets/real_bg_with_guides.png")))

        document = BTeXParser.invoke(tex)
    }

    private var scroll = 0

    val pageGap = 6

    override fun render() {
        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        val drawX = (1280 - (pageGap + document.pageWidth*2)) / 2
        val drawY = 24

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(bg, 0f, 0f)

            if (scroll - 1 in document.pageIndices)
                document.render(0f, batch, scroll - 1, drawX, drawY)
            if (scroll in document.pageIndices)
                document.render(0f, batch, scroll, drawX + (6 + document.pageWidth), drawY)
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
            scroll = (scroll - 2).coerceAtLeast(0)
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
            scroll = (scroll + 2).coerceAtMost(document.pageIndices.endInclusive.toFloat().div(2f).ceilToInt().times(2))
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