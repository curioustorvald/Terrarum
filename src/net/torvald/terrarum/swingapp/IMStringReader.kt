package net.torvald.terrarum.swingapp

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.langpack.Lang
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.*
import javax.swing.*


/**
 * Multilingual string input
 *
 * This kind of hack was often used in retro games that does not support multilingual input per se,
 * so it would give you a pop-up with text field to get the string input from user, using resources
 * provided by the OS.
 *
 * This hack is still alive, for example: "Princess Maker 2 Refine" on Steam, when installed in
 * Chinese/Japanese/Korean Language. Although the game was released in 2016 via Steam (hence the "Refine"),
 * the original game were released on 1995.
 *
 * Although admittedly, Korean input does not require this hack, you can just write the Input Method
 * out of Java/Kotlin as the language does not need conversion (jp. Henkan) exists in Chinese and Japanese.
 *
 * Created by SKYHi14 on 2017-02-05.
 */
class IMStringReader(feedInput: (String) -> Unit, message: String? = null) : JFrame() {

    private val inputArea = JTextField()
    private val buttonOkay = JButton(Lang["MENU_LABEL_OK"])
    private val buttonCancel = JButton(Lang["MENU_LABEL_CANCEL"])

    private val labelTitle = message ?: "Enter some text"


    var userInput: String = ""//null
        private set


    init {
        this.title = labelTitle
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

        Terrarum.pause()

        buttonOkay.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent?) { }
            override fun mouseClicked(e: MouseEvent?) { }
            override fun mouseReleased(e: MouseEvent?) { }
            override fun mouseExited(e: MouseEvent?) { }
            override fun mousePressed(e: MouseEvent?) {
                userInput = inputArea.text
                isVisible = false
                Terrarum.resume()

                feedInput(userInput)

                dispose()
            }
        })

        buttonCancel.addMouseListener(object : MouseListener {
            override fun mouseEntered(e: MouseEvent?) { }
            override fun mouseClicked(e: MouseEvent?) { }
            override fun mouseReleased(e: MouseEvent?) { }
            override fun mouseExited(e: MouseEvent?) { }
            override fun mousePressed(e: MouseEvent?) {
                userInput = ""//null
                isVisible = false
                Terrarum.resume()

                dispose()
            }
        })

        this.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) { }
            override fun keyReleased(e: KeyEvent?) { }
            override fun keyPressed(e: KeyEvent?) {
                userInput = inputArea.text
                isVisible = false
                Terrarum.resume()

                feedInput(userInput)

                dispose()
            }
        })

        val buttonsArea = JPanel()
        buttonsArea.layout = FlowLayout()
        buttonsArea.add(buttonOkay)
        buttonsArea.add(buttonCancel)

        this.layout = BorderLayout(2, 2)
        this.add(JLabel(labelTitle), BorderLayout.PAGE_START)
        this.add(inputArea, BorderLayout.CENTER)
        this.add(buttonsArea, BorderLayout.PAGE_END)
        this.isVisible = true
        this.setSize(240, 118)
    }

}