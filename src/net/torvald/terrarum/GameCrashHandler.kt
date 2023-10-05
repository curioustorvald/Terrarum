package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Font.BOLD
import java.awt.Font.PLAIN
import java.io.OutputStream
import java.io.PrintStream
import javax.swing.*

/**
 * Created by minjaesong on 2021-09-18.
 */
class GameCrashHandler(e: Throwable) : JFrame() {

    val textArea = JTextPane()

    val htmlSB = StringBuilder()

    private val outputStream = object : OutputStream() {
        override fun write(p0: Int) {
            htmlSB.append((p0 and 255).toChar())
        }
    }

    private val css = """
body {
    font-size: 12px;
    font-family: sans-serif;
    margin: 3px;
    background-color: #fdfdfd;
}

h3 {
    font-size: 16px;
    font-weight: 700;
    color: #444;
    margin: 20px 10px 12px 10px;
}

h4 {
    font-size: 14px;
    font-weight: 500;
    color: #444;
    margin: 0 12px 6px 12px;
}

li {
    margin: 0;
}

p {
    margin: 3px 12px;
}

pre {
    font-faminy: monospaced;
    font-size: 11px;
    color: #801;
    border: 1px solid #801;
    border-radius: 3px;
    padding: 3px 6px;
}

small {
    font-size: 9px;
}

emph {
    font-style: italic;
    color: #777;
}
    """

    private val printStream = object : PrintStream(outputStream) {
        override fun println(x: String?) {
            super.print(x)
        }
    }

    private fun moduleMetaToText(m: ModMgr.ModuleMetadata?) = if (m == null)
        "<emph>metadata not available or the mod failed to load</emph>"
    else
        "author: ${m.author}, version: ${m.version}, release date: ${m.releaseDate}, dependencies: ${m.dependencies.joinToString("/")}"

    init {
        val border = JPanel()
        border.layout = BorderLayout(18,18)
        border.isVisible = true
        border.background = Color(63,79,93)


        val title = JLabel("Help! A blackhole ate my game!")
        title.font = Font("SansSerif", BOLD, 18)
        title.foreground = Color.WHITE
        title.isVisible = true
        title.horizontalAlignment = SwingConstants.CENTER
        border.add(title, BorderLayout.NORTH)


        textArea.isVisible = true
        textArea.isEditable = false
        textArea.contentType = "text/html"
        border.add(JScrollPane(textArea), BorderLayout.CENTER)


        this.layout = BorderLayout(32,32)
        this.size = Dimension(1280, 720)
        this.isVisible = true
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.contentPane.background = Color(63,79,93)
        this.add(JLabel(), BorderLayout.EAST)
        this.add(JLabel(), BorderLayout.WEST)
        this.add(JLabel(), BorderLayout.SOUTH)
        this.add(JLabel(), BorderLayout.NORTH)
        this.add(border, BorderLayout.CENTER)
        this.title = TerrarumAppConfiguration.GAME_NAME

        val uptime = App.getTIME_T() - App.startupTime

        // print out device info
        printStream.println("<h3>System Info</h3>")
        printStream.println("<ul>")
        printStream.println("<li>Game name: ${TerrarumAppConfiguration.GAME_NAME}</li>")
        printStream.println("<li>Engine version: ${App.getVERSION_STRING()}</li>")
        printStream.println("<li>JRE version: ${System.getProperty("java.version")}</li>")
        printStream.println("<li>Gdx version: ${com.badlogic.gdx.Version.VERSION}</li>")
        printStream.println("<li>LWJGL version: ${org.lwjgl.Version.VERSION_MAJOR}.${org.lwjgl.Version.VERSION_MINOR}.${org.lwjgl.Version.VERSION_REVISION}</li>")
        printStream.println("<li>Uptime: ${uptime / 3600}h${(uptime % 3600) / 60}m${uptime % 60}s</li>")
        printStream.println("<li>BogoFlops: ${App.bogoflops}</li>")
        printStream.println("<li>OS Name: ${App.OSName}</li>")
        printStream.println("<li>OS Version: ${App.OSVersion}</li>")
        printStream.println("<li>System architecture: ${App.systemArch}</li>")
        printStream.println("<li>Processor: ${App.processor} x${Runtime.getRuntime().availableProcessors()} (${App.processorVendor})</li>")
        printStream.println("</ul>")

        printStream.println("<h3>OpenGL Info</h3>")

        try {
            printStream.println("<ul><li>${Gdx.graphics.glVersion.debugVersionString.replace("\n", "</li><li>")}</li></ul>")
        }
        catch (e: NullPointerException) {
            printStream.println("<p><emph>GL not initialised</emph></p>")
        }

        printStream.println("<h3>Module Info</h3>")
        printStream.println("<h4>Load Order</h4>")
        printStream.println("<ol>${ModMgr.loadOrder.joinToString(separator = "") { "<li>" +
                   "$it&ensp;<small>(" +
                   "${moduleMetaToText(ModMgr.moduleInfo[it] ?: ModMgr.moduleInfoErrored[it])}" +
                   ")</small></li>" }
                }</ol>")


        ModMgr.errorLogs.let {
            if (it.size > 0) {
                printStream.println("<h4>Module Errors</h4>")
                System.err.println("== Module Errors ==")
                it.forEach {
                    printStream.println("<p>From Module <strong>${it.moduleName}</strong> (${it.type.toHTML()}):</p>")
                    printStream.println("<pre>")
                    it.cause?.printStackTrace(printStream)
                    printStream.println("</pre>")
                    it.cause?.printStackTrace(System.err)
                }
            }
        }

        printStream.println("<h3>The Error Info</h3>")
        System.err.println("== The Error Info ==")

        printStream.println("<pre>")
        e.printStackTrace(printStream)
        printStream.println("</pre>")
        e.printStackTrace(System.err)



        textArea.text = "<html><style type=\"text/css\">$css</style><body>$htmlSB</body></html>"
    }

    private fun ModMgr.LoadErrorType.toHTML() = when(this) {
        ModMgr.LoadErrorType.YOUR_FAULT -> "caused by the module"
        ModMgr.LoadErrorType.MY_FAULT -> "caused by the game"
        ModMgr.LoadErrorType.NOT_EVEN_THERE -> "dependency not satisfied"
    }

}
