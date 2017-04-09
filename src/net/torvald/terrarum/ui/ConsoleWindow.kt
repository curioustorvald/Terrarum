package net.torvald.terrarum.ui

import net.torvald.gadgets.HistoryArray
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.console.CommandInterpreter
import net.torvald.terrarum.gamecontroller.Key
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
class ConsoleWindow : UICanvas, KeyboardControlled {

    internal var UIColour = Color(0x80404080.toInt())

    private var inputCursorPos: Int = 0
    private val MESSAGES_MAX = 5000
    private val COMMAND_HISTORY_MAX = 100
    private var messages = Array(MESSAGES_MAX, {""})
    private var messageDisplayPos: Int = 0
    private var messagesCount: Int = 0

    private var commandInputPool: StringBuilder? = null
    private var commandHistory = HistoryArray<String>(COMMAND_HISTORY_MAX)

    private val LINE_HEIGHT = 20
    private val MESSAGES_DISPLAY_COUNT = 11

    override var width: Int = Terrarum.WIDTH
    override var height: Int = LINE_HEIGHT * (MESSAGES_DISPLAY_COUNT + 1)

    override var openCloseTime: Int = 0

    private var drawOffX: Float = 0f
    private var drawOffY: Float = -height.toFloat()
    private var openingTimeCounter = 0

    override var handler: UIHandler? = null

    private var historyIndex = -1

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

        g.color = Color(0x7f7f7f)
        g.fillRect(inputDrawWidth.toFloat() + drawOffX + 1, drawOffY, 2f, inputDrawHeight.toFloat())
        g.color = Color.white
        g.fillRect(inputDrawWidth.toFloat() + drawOffX + 1, drawOffY, 1f, inputDrawHeight.toFloat() - 1)


        // messages
        for (i in 0..MESSAGES_DISPLAY_COUNT - 1) {
            val message = messages[messageDisplayPos + i]
            g.drawString(message, 1f + drawOffX, (LINE_HEIGHT * (i + 1)).toFloat() + drawOffY)
        }
    }


    override fun keyPressed(key: Int, c: Char) {
        // history
        if (key == Key.UP && historyIndex < commandHistory.history.size)
            historyIndex++
        else if (key == Key.DOWN && historyIndex >= 0)
            historyIndex--
        else if (key != Key.UP && key != Key.DOWN)
            historyIndex = -1

        // execute
        if (key == Key.RETURN && commandInputPool!!.isNotEmpty()) {
            commandHistory.add(commandInputPool!!.toString())
            executeCommand()
            commandInputPool = StringBuilder()
        }
        // erase last letter
        else if (key == Key.BACKSPACE && commandInputPool!!.isNotEmpty()) {
            commandInputPool!!.deleteCharAt(commandInputPool!!.length - 1)
        }
        // append acceptable letter
        else if (key >= 2 && key <= 13
                 || key >= 16 && key <= 27
                 || key >= 30 && key <= 40
                 || key >= 44 && key <= 53
                 || commandInputPool!!.length > 0 && key == 57) {
            commandInputPool!!.append(c)
            inputCursorPos += 1
        }
        // scroll
        else if (key == Key.UP || key == Key.DOWN) {
            // create new stringbuilder
            commandInputPool = StringBuilder()
            if (historyIndex >= 0) // just leave blank if index is -1
                commandInputPool!!.append(commandHistory[historyIndex] ?: "")
        }
        // delete input
        else if (key == Key.DELETE) {
            commandInputPool = StringBuilder()
        }
        // message scroll up
        else if (key == Key.PGUP) {
            setDisplayPos(-MESSAGES_DISPLAY_COUNT + 1)
        }
        // message scroll down
        else if (key == Key.PGDN) {
            setDisplayPos(MESSAGES_DISPLAY_COUNT - 1)
        }
    }

    override fun keyReleased(key: Int, c: Char) {

    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {

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
        commandHistory = HistoryArray<String>(COMMAND_HISTORY_MAX)
        commandInputPool = StringBuilder()

        if (Authenticator.b()) {
            sendMessage("${Terrarum.NAME} ${Terrarum.VERSION_STRING}")
            sendMessage(Lang["DEV_MESSAGE_CONSOLE_CODEX"])
        }
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
