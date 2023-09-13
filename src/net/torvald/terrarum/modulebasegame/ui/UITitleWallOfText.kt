package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CreditSingleton
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextArea
import java.util.TreeMap

open class UITitleWallOfText(private val text: List<String>) : UICanvas() {


    private val textAreaHMargin = 48
    override var width = 600
    override var height = App.scr.height - textAreaHMargin * 2
    private val textArea = UIItemTextArea(this,
            (App.scr.width - width) / 2, textAreaHMargin,
            width, height
    )


    init {
        uiItems.add(textArea)

        textArea.setWallOfText(text)
    }

    override fun updateUI(delta: Float) {
        textArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE
        textArea.render(batch, camera)

        //AppLoader.printdbg(this, "Rendering texts of length ${text.size}")
    }

    override fun dispose() {
    }
}

class UITitleCredits(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.credit)
class UITitleGPL3(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.gpl3)
//class UISystemInfo(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.systeminfo)

class UISystemInfo(val remoCon: UIRemoCon) : UICanvas() {
    override var width = Toolkit.drawWidth
    override var height = App.scr.height - 4 * App.scr.tvSafeGraphicsHeight

    private val v = ArrayList<Pair<String, String>>()
    private val vlen = HashMap<String, Int>()

    private val tb1w: Int
    private val tb2w: Int
    private val tb1x: Int
    private val tb2x: Int
    private val tby = (App.scr.height - height) / 2
    private val gap = 10

    private var uptime: Long = 0L

    init {
        v.add("${App.GAME_NAME}" to App.getVERSION_STRING())
        v.add("JRE" to System.getProperty("java.version"))
        v.add("Gdx" to com.badlogic.gdx.Version.VERSION)
        v.add("OS" to "${App.OSName} ${App.OSVersion}")
        v.add("Processor" to App.processor)
        v.add("Architecture" to App.systemArch)
        v.add("CPUID" to App.processorVendor.let { if (it == "null" || it == null) "n/a" else it })
        v.add("OpenGL" to "${Gdx.graphics.glVersion.majorVersion}.${Gdx.graphics.glVersion.minorVersion}.${Gdx.graphics.glVersion.releaseVersion}")
        v.add("GL Vendor" to Gdx.graphics.glVersion.vendorString)
        v.add("GL Renderer" to Gdx.graphics.glVersion.rendererString)
        v.add("BogoFlops" to "${App.bogoflops}")
        v.add("Uptime" to "00h00m00s")

        v.forEach { (k, v) ->
            vlen[k] = App.fontGame.getWidth(k)
            vlen[v] = App.fontGame.getWidth(v)
        }

        tb1w = v.map { it.first }.maxOf { vlen[it]!! }
        tb2w = v.map { it.second }.maxOf { vlen[it]!! }

        tb1x = (width - tb1w - tb2w - gap) / 2
        tb2x = tb1x + tb1w + gap
    }

    override fun updateUI(delta: Float) {
        uptime = App.getTIME_T() - App.startupTime
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        var i = 0

        v.forEach { (k, v0) ->
            val y = tby + 24 * i
            val v = if (k == "Uptime") "${uptime / 3600}h${(uptime % 3600) / 60}m${uptime % 60}s" else v0

            batch.color = Toolkit.Theme.COL_LIST_DEFAULT
            App.fontGame.draw(batch, k, tb1x + tb1w - vlen[k]!!, y)
            batch.color = Toolkit.Theme.COL_MOUSE_UP
            App.fontGame.draw(batch, v, tb2x, y)

            i++
        }
    }

    override fun dispose() {
    }

}