package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.ccE
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.console.CommandInterpreter
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.unicode.EMDASH
import net.torvald.util.CircularArray


/**
 * Don't let the debug console have the ```toggleKeyLiteral```, this must open even when the game is paused
 *
 * Created by minjaesong on 2015-12-31.
 */
class ConsoleWindow : UICanvas() {

    internal var UIColour = Color(0x404080_80.toInt())

    private var inputCursorPos: Int = 0
    private val MESSAGES_MAX = 1000
    private val COMMAND_HISTORY_MAX = 100
    private var messages = CircularArray<String>(MESSAGES_MAX, true)
    private var messageDisplayPos: Int = 0
    private var messagesCount: Int = 0

//    private var commandInputPool: StringBuilder? = null
    private var commandHistory = CircularArray<String>(COMMAND_HISTORY_MAX, true)

    private val LINE_HEIGHT = 20
    private val MESSAGES_DISPLAY_COUNT = 11

    private val inputToMsgboxGap = 3

    override var width: Int = App.scr.width
    override var height: Int = LINE_HEIGHT * (MESSAGES_DISPLAY_COUNT + 1) + inputToMsgboxGap

    override var openCloseTime = 0f

    private var drawOffX: Float = 0f
    private var drawOffY: Float = -height.toFloat()
    private var openingTimeCounter = 0f

    private var historyIndex = -1

    private var iMadeTheGameToPause = false

    private val textinput = UIItemTextLineInput(this, 0, 0, this.width, keyFilter = { e ->
        !e.keycodes.contains(Input.Keys.GRAVE)
    })

    private var clickLatched = false

    init {
        reset()
        addUIitem(textinput)
        textinput.isEnabled = false
    }

    private val lb = ArrayList<String>()

    override fun updateUI(delta: Float) {
        Terrarum.ingame?.let {
            if (Authenticator.b()) {
                lb.clear()

                val actorsUnderCursor = it.getActorsAt(Terrarum.mouseX, Terrarum.mouseY)
                actorsUnderCursor.filter { it.referenceID < 2147483647 }.forEach { // filter out the BlockMarkerActor
                    lb.add("${it.referenceID} (${it.actorValue[AVKey.NAME] ?: "\u03AF-${it.javaClass.simpleName}"})")
                }

                it.setTooltipMessage(if (lb.size > 0) lb.joinToString("\n") else null)

                // click to enter the actor's reference ID
                if (lb.size > 0 && !clickLatched && Gdx.input.isButtonPressed(App.getConfigInt("config_mouseprimary"))) {
                    clickLatched = true
                    textinput.appendText(lb.first().substringBefore(' '))
                }
            }
            else {
                it.setTooltipMessage(null)
            }
        }

        uiItems.forEach { it.update(delta) }


        if (!Gdx.input.isButtonPressed(App.getConfigInt("config_mouseprimary"))) {
            clickLatched = false
        }

        textinput.isEnabled = (isOpened && !isClosing)
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        // background
        batch.color = UIColour
        Toolkit.fillArea(batch, drawOffX, drawOffY, width.toFloat(), height.toFloat())
        Toolkit.fillArea(batch, drawOffX, drawOffY, width.toFloat(), LINE_HEIGHT.toFloat())

//        val input = commandInputPool!!.toString()
//        val inputDrawWidth = App.fontGame.getWidth(input)
//        val inputDrawHeight = App.fontGame.lineHeight
//
//         text and cursor
//        batch.color = Color.WHITE
//        App.fontGame.draw(batch, input, 1f + drawOffX, drawOffY)
//
//        batch.color = Color(0x7f7f7f_ff)
//        Toolkit.fillArea(batch, inputDrawWidth.toFloat() + drawOffX + 1, drawOffY, 2f, inputDrawHeight)
//        batch.color = Color.WHITE
//        Toolkit.fillArea(batch, inputDrawWidth.toFloat() + drawOffX + 1, drawOffY, 1f, inputDrawHeight - 1)


        // messages
        batch.color = Color.WHITE

        for (i in 0 until MESSAGES_DISPLAY_COUNT) {
            val message = messages[messageDisplayPos + i] ?: ""
            App.fontGame.draw(batch, message, 1f + drawOffX, (LINE_HEIGHT * (MESSAGES_DISPLAY_COUNT - i)).toFloat() + drawOffY + inputToMsgboxGap)
        }

        uiItems.forEach { it.render(batch, camera) }
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        uiItems.forEach { it.inputStrobed(e) }
    }

    override fun keyDown(key: Int): Boolean {
        try {
            val textOnBuffer = textinput.getText()

            // history
            if (key == Input.Keys.UP && historyIndex < commandHistory.elemCount)
                historyIndex++
            else if (key == Input.Keys.DOWN && historyIndex >= 0)
                historyIndex--
            else if (key != Input.Keys.UP && key != Input.Keys.DOWN)
                historyIndex = -1

            // execute
            if (key == Input.Keys.ENTER && textOnBuffer.isNotEmpty()) {
                commandHistory.appendHead(textOnBuffer)
                executeCommand(textOnBuffer)
                textinput.clearText()
            }
            // scroll
            else if (key == Input.Keys.UP || key == Input.Keys.DOWN) {
                // create new stringbuilder
                textinput.clearText()
                if (historyIndex >= 0) // just leave blank if index is -1
                    textinput.setText(commandHistory[historyIndex] ?: "")
            }
            // delete input
//        else if (key == Input.Keys.BACKSPACE) {
//            commandInputPool = StringBuilder()
//        }
            // message scroll up
            else if (key == Input.Keys.PAGE_UP) {
                setDisplayPos(-MESSAGES_DISPLAY_COUNT + 1)
            }
            // message scroll down
            else if (key == Input.Keys.PAGE_DOWN) {
                setDisplayPos(MESSAGES_DISPLAY_COUNT - 1)
            }
        }
        catch (e: ConcurrentModificationException) {
            System.err.println("[ConsoleWindow.keyDown] ConcurrentModificationException with key ${Input.Keys.toString(key)}")
        }


        return true
    }

    val acceptedChars = "1234567890-=qwfpgjluy;[]\\arstdhneio'zxcvbkm,./!@#$%^&*()_+QWFPGJLUY:{}|ARSTDHNEIO\"ZXCVBKM<>? ".toSet()

    /*override fun keyTyped(character: Char): Boolean {
        if (character in acceptedChars) {
            commandInputPool!!.append(character)
            inputCursorPos += 1

            return true
        }
        else {
            return false
        }
    }*/

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    private fun executeCommand(s: String) {
        CommandInterpreter.execute(s)
    }

    fun sendMessage(msg: String) {
        messages.appendHead(msg)
        messagesCount += 1

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
        messages = CircularArray<String>(MESSAGES_MAX, true)
        messageDisplayPos = 0
        messagesCount = 0
        inputCursorPos = 0
        commandHistory = CircularArray<String>(COMMAND_HISTORY_MAX, true)
        textinput.clearText()

        if (Authenticator.b()) {
            sendMessage("$ccE${TerrarumAppConfiguration.GAME_NAME} ${App.getVERSION_STRING()} $EMDASH ${TerrarumAppConfiguration.COPYRIGHT_DATE_NAME}")
            sendMessage("$ccE${TerrarumAppConfiguration.COPYRIGHT_LICENSE}")
            sendMessage(Lang["DEV_MESSAGE_CONSOLE_CODEX"])
        }
    }

    override fun doOpening(delta: Float) {
        Terrarum.ingame?.let {
//            printdbg(this, "Game was paused beforehand: ${it.paused}")
            if (!it.paused) {
                iMadeTheGameToPause = true
                it.pause()
            }
            else {
                iMadeTheGameToPause = false
            }
//            printdbg(this, "I made the game to pause: $iMadeTheGameToPause")
        }
        /*openingTimeCounter += delta
        drawOffY = MovementInterpolator.fastPullOut(openingTimeCounter.toFloat() / openCloseTime.toFloat(),
                -height.toFloat(), 0f
        )*/
    }

    override fun doClosing(delta: Float) {
        /*openingTimeCounter += delta
        drawOffY = MovementInterpolator.fastPullOut(openingTimeCounter.toFloat() / openCloseTime.toFloat(),
                0f, -height.toFloat()
        )*/
        textinput.isEnabled = false
        textinput.mouseoverUpdateLatch = false
    }

    override fun endOpening(delta: Float) {
        drawOffY = 0f
        openingTimeCounter = 0f
        textinput.isEnabled = true
        textinput.mouseoverUpdateLatch = true
    }

    override fun endClosing(delta: Float) {
//        printdbg(this, "Close -- I made the game to pause: $iMadeTheGameToPause")
        if (iMadeTheGameToPause) {
            Terrarum.ingame?.resume()
//            printdbg(this, "Close -- resume game")
        }
        iMadeTheGameToPause = false
        Terrarum.ingame?.setTooltipMessage(null)
        drawOffY = -height.toFloat()
        openingTimeCounter = 0f
    }

    override fun dispose() {
        uiItems.forEach { it.dispose() }
    }
}
