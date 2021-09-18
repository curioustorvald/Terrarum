package net.torvald.terrarum

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

    val textArea = JTextArea()

    private val outputStream = object : OutputStream() {
        override fun write(p0: Int) {
            textArea.append("${p0.toChar()}")
        }
    }

    private val printStream = object : PrintStream(outputStream) {}

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


        textArea.font = Font("Monospaced", PLAIN, 14)
        textArea.isVisible = true
        textArea.isEditable = false
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


        e.printStackTrace(printStream)
        e.printStackTrace()
    }

}