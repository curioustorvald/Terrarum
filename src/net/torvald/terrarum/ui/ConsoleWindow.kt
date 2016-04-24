package net.torvald.terrarum.ui

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.CommandInterpreter
import net.torvald.terrarum.gamecontroller.Key
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
class ConsoleWindow : UICanvas, UITypable {

    internal var UIColour = Color(0xCC000000.toInt())

    private var commandInputPool: StringBuilder? = null
    private var prevCommand: String? = null

    private var inputCursorPos: Int = 0

    private val MESSAGES_MAX = 5000
    private var messages = Array(MESSAGES_MAX, {""})
    private var messageDisplayPos: Int = 0
    private var messagesCount: Int = 0

    private val LINE_HEIGHT = 20
    private val MESSAGES_DISPLAY_COUNT = 9

    override var width: Int = Terrarum.WIDTH
    override var height: Int = 200

    override var openCloseTime: Int = 0

    private var drawOffX: Float = 0f
    private var drawOffY: Float = -height.toFloat()
    private var openingTimeCounter = 0

    init {
        reset()
    }

    override fun update(gc: GameContainer, delta: Int) {

    }

    override fun render(gc: GameContainer, g: Graphics) {
        // background
        g.color = UIColour
        g.fillRect(drawOffX, drawOffY, width.toFloat(), height.toFloat())
        g.fillRect(drawOffX, drawOffY, width.toFloat(), LINE_HEIGHT.toFloat())

        val input = commandInputPool!!.toString()
        val inputDrawWidth = g.font.getWidth(input)
        val inputDrawHeight = g.font.lineHeight

        // text and cursor
        g.color = Color.white
        g.drawString(input, 1f + drawOffX, drawOffY)
        g.fillRect(inputDrawWidth.toFloat() + drawOffX + 1, drawOffY, 2f, inputDrawHeight.toFloat())

        // messages
        for (i in 0..MESSAGES_DISPLAY_COUNT - 1) {
            val message = messages[messageDisplayPos + i]
            g.drawString(message, 1f + drawOffX, (LINE_HEIGHT * (i + 1)).toFloat() + drawOffY)
        }
    }


    override fun keyPressed(key: Int, c: Char) {
        // execute
        if (key == Key.RET && commandInputPool!!.length > 0) {
            prevCommand = commandInputPool!!.toString()
            executeCommand()
            commandInputPool = StringBuilder()
        }
        else if (key == Key.BKSP && commandInputPool!!.length > 0) {
            commandInputPool!!.deleteCharAt(commandInputPool!!.length - 1)
        }
        else if (key >= 2 && key <= 13
                 || key >= 16 && key <= 27
                 || key >= 30 && key <= 40
                 || key >= 44 && key <= 53
                 || commandInputPool!!.length > 0 && key == 57) {
            commandInputPool!!.append(c)
            inputCursorPos += 1
        }
        else if (key == Key.UP) {
            commandInputPool = StringBuilder()
            commandInputPool!!.append(prevCommand)
        }
        else if (key == Key.PGUP) {
            setDisplayPos(-MESSAGES_DISPLAY_COUNT + 1)
        }
        else if (key == Key.PGDN) {
            setDisplayPos(MESSAGES_DISPLAY_COUNT - 1)
        }// scroll down
        // scroll up
        // prev command
        // get input
        // backspace
    }

    override fun keyReleased(key: Int, c: Char) {

    }

    override fun processInput(input: Input) {

    }

    private fun executeCommand() {
        CommandInterpreter.execute(commandInputPool!!.toString())
    }

    fun sendMessage(msg: String) {
        messages[messagesCount] = msg
        messagesCount += 1
        if (messagesCount > MESSAGES_DISPLAY_COUNT) {
            messageDisplayPos = messagesCount - MESSAGES_DISPLAY_COUNT
        }
    }

    private fun setDisplayPos(change: Int) {
        val lowLim = 0
        val highLim = if (messagesCount <= MESSAGES_DISPLAY_COUNT)
            0
        else
            messagesCount - MESSAGES_DISPLAY_COUNT
        val newVal = messageDisplayPos + change

        if (newVal < lowLim) {
            messageDisplayPos = lowLim
        }
        else if (newVal > highLim) {
            messageDisplayPos = highLim
        }
        else {
            messageDisplayPos = newVal
        }

    }

    fun reset() {
        messages = Array(MESSAGES_MAX, {""})
        messageDisplayPos = 0
        messagesCount = 0
        inputCursorPos = 0
        prevCommand = ""
        commandInputPool = StringBuilder()

        if (Terrarum.game.auth.b()) sendMessage(Lang["DEV_MESSAGE_CONSOLE_CODEX"])
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        /*openingTimeCounter += delta
        drawOffY = MovementInterpolator.fastPullOut(openingTimeCounter.toFloat() / openCloseTime.toFloat(),
                -height.toFloat(), 0f
        )*/
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        /*openingTimeCounter += delta
        drawOffY = MovementInterpolator.fastPullOut(openingTimeCounter.toFloat() / openCloseTime.toFloat(),
                0f, -height.toFloat()
        )*/
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        drawOffY = 0f
        openingTimeCounter = 0
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        drawOffY = -height.toFloat()
        openingTimeCounter = 0
    }
}
