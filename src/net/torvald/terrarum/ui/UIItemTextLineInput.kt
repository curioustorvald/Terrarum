package net.torvald.terrarum.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.ibm.icu.text.Normalizer2
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gamecontroller.*
import net.torvald.terrarum.utils.Clipboard
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.unicode.toJavaString
import kotlin.math.min
import kotlin.streams.toList

data class InputLenCap(val count: Int, val unit: CharLenUnit) {
    enum class CharLenUnit {
        UTF8_BYTES, UTF16_CHARS, CODEPOINTS
    }

    fun exceeds(codepoints: CodepointSequence, extra: List<Int> = CodepointSequence()): Boolean {
        return when (unit) {
            CharLenUnit.CODEPOINTS -> (codepoints.size + extra.size) > count
            CharLenUnit.UTF16_CHARS -> {
                var cnt = 0
                listOf(codepoints,extra).forEach { it.forEach {
                    cnt += 1 + (it > 65535).toInt()
                } }
                cnt > count
            }
            CharLenUnit.UTF8_BYTES -> {
                var cnt = 0
                listOf(codepoints,extra).forEach { it.forEach {
                    cnt += if (it > 65535) 4 else if (it > 2047) 3 else if (it > 127) 2 else 1
                } }
                cnt > count
            }
        }
    }
}

/**
 * UIItemTextLineInput does not require any GDX's input event handlers, but it does require InputStrober
 * to be running and `inputStrobed()` of the parentUI is calling the same method on this.
 *
 * Protip: if there are multiple TextLineInputs on a same UI, draw bottom one first, otherwise the IME's
 * candidate window will be hidden by the bottom UIItem if they overlaps.
 *
 * Note: mouseoverUpdateLatch must be latched to make buttons work
 *
 * @param width width of the text input where the text gets drawn, not the entire item
 * @param height height of the text input where the text gets drawn, not the entire item
 *
 * Created by minjaesong on 2021-10-20.
 */
class UIItemTextLineInput(
        parentUI: UICanvas,
        initialX: Int, initialY: Int,
        override val width: Int,
        var placeholder: () -> String = { "" },
        val maxLen: InputLenCap = InputLenCap(1000, InputLenCap.CharLenUnit.CODEPOINTS),
//        val enablePasteButton: Boolean = true,
//        val enableIMEButton: Boolean = true
        var keyFilter: (TerrarumKeyboardEvent) -> Boolean = { true },
        val alignment: UIItemTextButton.Companion.Alignment = UIItemTextButton.Companion.Alignment.LEFT,
        val defaultValue: (() -> String?)? = null
) : UIItem(parentUI, initialX, initialY) {

    init {
    }

    var onKeyDown: (TerrarumKeyboardEvent) -> Unit = {}

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24

//    private val buttonsShown = enableIMEButton.toInt() + enablePasteButton.toInt()

    companion object {
        val TEXTINPUT_COL_TEXT = Color.WHITE
        val TEXTINPUT_COL_TEXT_NOMORE = Toolkit.Theme.COL_RED
        val TEXTINPUT_COL_TEXT_DISABLED = Toolkit.Theme.COL_DISABLED
        val TEXTINPUT_COL_BACKGROUND = Toolkit.Theme.COL_CELL_FILL
        val TEXTINPUT_COL_BACKGROUND2 = Toolkit.Theme.COL_CELL_FILL.cpy()
        const val CURSOR_BLINK_TIME = 1f / 3f

        private const val UI_TEXT_MARGIN = 2
        private const val WIDTH_ONEBUTTON = 24

        init {
            TEXTINPUT_COL_BACKGROUND2.a = 1f - (1f - TEXTINPUT_COL_BACKGROUND2.a) * (1f - TEXTINPUT_COL_BACKGROUND2.a)
        }
    }

    private val fbo = FrameBuffer(
            Pixmap.Format.RGBA8888,
            width - 2 * UI_TEXT_MARGIN - 2 * (WIDTH_ONEBUTTON + 3),
            height - 2 * UI_TEXT_MARGIN,
            false
    )

    override var isEnabled: Boolean = false // keep it false by default!
        set(value) {
            if (field && !value) endComposing(true)
            field = value
        }

    var cursorX = 0
    var cursorDrawScroll = 0
    var cursorDrawX = 0 // pixelwise point. Cursor's position on screen relative to the fbo's position (should not be affected by cursorDrawScroll)
    private var oldTextLenPx = 0
    private var currentTextLenPx = 0
    var cursorBlinkCounter = 0f
    var cursorOn = true

    private val textbuf = CodepointSequence()

    private var fboUpdateLatch = true

    private var currentPlaceholderText = ArrayList<Int>(placeholder().toCodePoints()) // the placeholder text may change every time you call it


    private val btn1PosX; get() = posX
    private val btn2PosX; get() = posX + width - WIDTH_ONEBUTTON
    private val inputPosX; get() = posX + WIDTH_ONEBUTTON + 3

    var mouseoverUpdateLatch = true // keep it true by default!
        set(value) {
            field = value
            if (!value) {
                mouseLatched = false
                fboUpdateLatch = false
                isEnabled = false
                cursorOn = false
                cursorBlinkCounter = 0f
            }
        }

    private val mouseUpOnTextArea: Boolean
        get() = mouseoverUpdateLatch && itemRelativeMouseX in 0 until fbo.width + 2 * UI_TEXT_MARGIN && itemRelativeMouseY in 0 until height
    private val mouseUpOnIMEButton
        get() = mouseoverUpdateLatch && itemRelativeMouseX in btn1PosX - posX until btn1PosX - posX + WIDTH_ONEBUTTON && itemRelativeMouseY in 0 until height
    private val mouseUpOnPasteButton
        get() = mouseoverUpdateLatch && itemRelativeMouseX in btn2PosX - posX until btn2PosX - posX + WIDTH_ONEBUTTON && itemRelativeMouseY in 0 until height

    private var imeOn = false
    private var candidates: List<CodepointSequence> = listOf()

    private val candidatesBackCol = TEXTINPUT_COL_BACKGROUND.cpy().mul(1f,1f,1f,1.5f)
    private val candidateNumberStrWidth = App.fontGame.getWidth("8. ")

    private var textStatus = 0 // 0: normal, 1: invalid, 2: disabled

    private var textColours = arrayOf(
        Color.WHITE,
        Toolkit.Theme.COL_RED,
        Color(0x888888ff.toInt())
    )

    fun markAsNormal() {
        textStatus = 0
    }
    fun markAsInvalid() {
        textStatus = 1
    }
    fun markAsDisabled() {
        textStatus = 2
    }

    /** Event fired whenever a character is entered or pasted from clipboard */
    var textCommitListener: (String) -> Unit = {}

    private fun getIME(ignoreOnOff: Boolean = false): TerrarumIME? {
        if (!imeOn && !ignoreOnOff) return null

        val selectedIME = App.getConfigString("inputmethod")

        if (selectedIME == "none") return null
        try {
            return IME.getHighLayerByName(selectedIME)
        }
        catch (e: NullPointerException) {
            return null
        }
    }

    private fun forceLitCursor() {
        cursorBlinkCounter = 0f
        cursorOn = true
    }

    private fun moveCursorBack(delta: Int) {
        cursorDrawX -= delta
        if (cursorDrawX < 0) {
            val stride = -cursorDrawX + min(256, fbo.width * 40 / 100) // + lookbehind term
            cursorDrawX += stride
            cursorDrawScroll -= stride
        }
        // make sure to not scroll past the line head
        if (cursorDrawScroll < 0) {
            cursorDrawX += cursorDrawScroll
            cursorDrawScroll = 0
        }
    }

    private fun moveCursorForward(delta: Int) {
        cursorDrawX += delta
        if (cursorDrawX > fbo.width) {
            val stride = cursorDrawX - fbo.width
            cursorDrawX -= stride // -cursorDrawX + width
            cursorDrawScroll += stride
        }
    }

    /*private fun moveCursorToEnd(stride: Int) {
        try {
            cursorX += stride
            currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
            moveCursorForward(currentTextLenPx - oldTextLenPx)
            oldTextLenPx = currentTextLenPx
        }
        catch (e: Throwable) {}
    }*/

    private fun Int.toCharInfo() = "U+${this.toString(16).uppercase()} '${String(intArrayOf(this), 0, if (this > 65535) 2 else 1)}'"

    private fun inputBackspaceOnce(dbgprn: Int = 0) {
        if (cursorX > 0) {

            while (cursorX > 0) {
                cursorX -= 1
                val charDeleted = textbuf.removeAt(cursorX)
//                printdbg(this, "$dbgprn)charDeleted=${charDeleted.toCharInfo()}")

                if (charDeleted !in 0x1160..0x11FF) break
            }

            currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
            moveCursorBack(oldTextLenPx - currentTextLenPx)
            oldTextLenPx = currentTextLenPx
        }
    }

    /**
     * Only makes sense when the placeholder returns randomised texts
     */
    fun refreshPlaceholder() {
        currentPlaceholderText = ArrayList<Int>(placeholder().toCodePoints())
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        val oldActive = isEnabled

        // process keypresses
        if (isEnabled) {

            val (eventType, char, headkey, repeatCount, keycodes) = e

            if (keyFilter(e)) {
                try {
                    if (eventType == InputStrober.KEY_DOWN || eventType == InputStrober.KEY_CHANGE) {
                        fboUpdateLatch = true
                        forceLitCursor()
                        val ime = getIME()
                        val lowLayer = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

                        if (keycodes.contains(Input.Keys.V) && keycodes.containsSome(Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT)) {
                            endComposing()
                            paste(Clipboard.fetch().substringBefore('\n').substringBefore('\t').toCodePoints())
                        }
                        else if (keycodes.contains(Input.Keys.C) && (keycodes.containsSome(Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT))) {
                            endComposing()
                            copyToClipboard()
                        }
                        else if (keycodes.contains(Input.Keys.BACKSPACE) || (keycodes.contains(Input.Keys.CAPS_LOCK) && lowLayer.capsMode == TerrarumKeyCapsMode.BACK)) {

    //                        printdbg(this, "BACKSPACE hit; ime.composing=${ime?.composing?.invoke()}; buflen=${textbuf.size}")

                            if (ime != null && ime.composing()) {
                                if (ime.config.mode == TerrarumIMEMode.CANDIDATES) {
                                    candidates = ime.backspace().map { CodepointSequence(it.toCodePoints()) }
                                }
                                else if (ime.config.mode == TerrarumIMEMode.REWRITE) {
                                    candidates = listOf()
                                    val op = ime.backspace()
                                    if (textbuf.isNotEmpty()) {
                                        inputBackspaceOnce(1)
                                    }

                                    if (op.size > 0) {
                                        val codepoints = op[0].toCodePoints()
                                        if (!maxLen.exceeds(textbuf, codepoints)) {
                                            textbuf.addAll(cursorX, codepoints)
                                            cursorX += codepoints.size
                                            currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                                            moveCursorForward(currentTextLenPx - oldTextLenPx)
                                            oldTextLenPx = currentTextLenPx
                                        }
                                    }
                                }
                            }
                            else if (cursorX <= 0) {
                                cursorX = 0
                                cursorDrawX = 0
                                cursorDrawScroll = 0
                            }
                            else {
                                endComposing()
                                inputBackspaceOnce(2)
                            }
                        }
                        else if (keycodes.contains(Input.Keys.LEFT)) {
                            endComposing()

                            if (cursorX > 0) {
                                cursorX -= 1
                                currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                                moveCursorBack(oldTextLenPx - currentTextLenPx)
                                oldTextLenPx = currentTextLenPx
                            }
                        }
                        else if (keycodes.contains(Input.Keys.RIGHT)) {
                            endComposing()

                            if (cursorX < textbuf.size) {
                                cursorX += 1
                                currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                                moveCursorForward(currentTextLenPx - oldTextLenPx)
                                oldTextLenPx = currentTextLenPx
                            }
                        }
                        else if (keycodes.containsSome(Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER)) {
                            endComposing()

    //                        println("END COMPOSING!!")
                        }
                        // accept:
                        // - literal "<"
                        // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                        else if (char != null && char.length > 0 && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                            val shiftin = keycodes.containsSome(Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT)
                            val altgrin = keycodes.contains(Input.Keys.ALT_RIGHT) || keycodes.containsAll(Input.Keys.ALT_LEFT, Input.Keys.CONTROL_LEFT)

                            val codepoints = if (ime != null) {
                                if (ime.config.mode == TerrarumIMEMode.CANDIDATES) {
                                    val newStatus = ime.acceptChar(headkey, shiftin, altgrin, char)
                                    candidates = newStatus.first.map { CodepointSequence(it.toCodePoints()) }

                                    newStatus.second.toCodePoints()
                                }
                                else if (ime.config.mode == TerrarumIMEMode.REWRITE) {
                                    candidates = listOf()
                                    val op = ime.acceptChar(headkey, shiftin, altgrin, char)

    //                                printdbg(this, "delcount: ${op.first[0].toInt()}, rewrite: '${op.second}'")

                                    repeat(op.first[0].toInt()) {
                                        if (textbuf.isNotEmpty()) {
    //                                        printdbg(this, "<del 1>")
                                            inputBackspaceOnce(3)
                                        }
                                    }

                                    op.second.toCodePoints()
                                }
                                else throw IllegalArgumentException("Unknown IME Operation mode: ${ime.config.mode}")
                            }
                            else char.toCodePoints()

    //                        printdbg(this, "textinput codepoints: ${codepoints.map { it.toString(16) }.joinToString()}")

                            if (!maxLen.exceeds(textbuf, codepoints)) {
                                textbuf.addAll(cursorX, codepoints)
                                cursorX += codepoints.size
                                currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                                moveCursorForward(currentTextLenPx - oldTextLenPx)
                                oldTextLenPx = currentTextLenPx
                            }
                        }

                        // don't put innards of tryCursorBack/Forward here -- you absolutely don't want that behaviour
                    }
                }
                catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
                catch (e: NullPointerException) {
                    e.printStackTrace()
                }

                if (textbuf.size == 0) {
                    currentPlaceholderText = CodepointSequence(placeholder().toCodePoints())
                }

                onKeyDown(e)
            }
        }
        else if (oldActive) { // just became deactivated
            endComposing()
        }

        fboUpdateLatch = true

    }

    override fun show() {
        defaultValue?.let {
            setText(it() ?: "")
        }
        fboUpdateLatch = true
    }

    override fun hide() {
        super.hide()
        if (this.isEnabled) {
            this.isEnabled = false
            TerrarumGlobalState.HAS_KEYBOARD_INPUT_FOCUS.unset()
        }
    }

    override fun update(delta: Float) {
        if (mouseoverUpdateLatch) {
            super.update(delta)
            val mouseDown = Terrarum.mouseDown

            if (mouseDown) {
                val oldEnabled = isEnabled
                isEnabled = mouseUp

                if (oldEnabled && !isEnabled) TerrarumGlobalState.HAS_KEYBOARD_INPUT_FOCUS.unset()
                if (!oldEnabled && isEnabled) TerrarumGlobalState.HAS_KEYBOARD_INPUT_FOCUS.set()
            }

            if (App.getConfigString("inputmethod") == "none") imeOn = false

            if (isEnabled) {
                cursorBlinkCounter += delta

                while (cursorBlinkCounter >= CURSOR_BLINK_TIME) {
                    cursorBlinkCounter -= CURSOR_BLINK_TIME
                    cursorOn = !cursorOn
                }
            }

            if (mouseDown && !mouseLatched && mouseUpOnIMEButton) {
                toggleIME()
                mouseLatched = true
            }
            else if (mouseDown && !mouseLatched && mouseUpOnPasteButton) {
                endComposing()
                paste(Clipboard.fetch().substringBefore('\n').substringBefore('\t').toCodePoints())
                mouseLatched = true
            }

            if (!mouseDown) mouseLatched = false

            imeOn = KeyToggler.isOn(ControlPresets.getKey("control_key_toggleime"))
        }
    }

    private fun String.toCodePoints() = (this as java.lang.CharSequence).codePoints().toList().filter { it > 0 }.toList()

    private fun endComposing(force: Boolean = false) {
        getIME(force)?.let {
            val s = it.endCompose()
            if (s.isNotEmpty()) {
                if (it.config.mode == TerrarumIMEMode.REWRITE) {
                    inputBackspaceOnce(9)
                }
                paste(s.toCodePoints())
            }
        }
        fboUpdateLatch = true
        candidates = listOf()
//        resetIME() // not needed; IME will reset itself
    }

    private fun toggleIME() {
        endComposing()

        imeOn = !imeOn
        KeyToggler.forceSet(ControlPresets.getKey("control_key_toggleime"), imeOn)
    }

    private fun resetIME() {
        getIME()?.reset?.invoke()
        candidates = listOf()
    }

    private fun paste(codepoints: List<Int>) {
        val actuallyInserted = arrayListOf(0)

        for (c in codepoints) {
            if (maxLen.exceeds(textbuf, actuallyInserted)) break
            actuallyInserted.add(c)
        }

        actuallyInserted.removeAt(0)

        textbuf.addAll(actuallyInserted)
        cursorX += codepoints.size
        currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
        moveCursorForward(currentTextLenPx - oldTextLenPx)
        oldTextLenPx = currentTextLenPx

        fboUpdateLatch = true
    }

    private fun copyToClipboard() {
        Clipboard.copy(textbufToString())
    }

    private fun textbufToString(): String {
        return textbuf.toJavaString().toUnicodeNFC()
    }

    private fun String.toUnicodeNFC() = Normalizer2.getNFCInstance().normalize(this)

    private var textDrawOffset = 0

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {

        val posXDelta = posX - oldPosX


        val ime = getIME(true)

        batch.end()


        // FIXME any subsequent UIItems after this function are not moved even if their parent UI is moving
        if (true || fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

                it.color = textColours[textStatus]

                val text = if (textbuf.isEmpty()) currentPlaceholderText else textbuf
                val tw = App.fontGameFBO.getWidth(text)
                textDrawOffset = if (alignment == UIItemTextButton.Companion.Alignment.CENTRE)
                        ((fbo.width - tw) / 2).coerceAtLeast(0)
                else
                    0
                // TODO support alignment-right

                App.fontGameFBO.draw(it, text, -1f*cursorDrawScroll + textDrawOffset, -2f)
            } }
            textCommitListener(getTextOrPlaceholder())
        }

        batch.begin()

        val mouseDown = Terrarum.mouseDown

        // text area cell back
        batch.color = TEXTINPUT_COL_BACKGROUND
        Toolkit.fillArea(batch, inputPosX, posY, fbo.width + 2 * UI_TEXT_MARGIN, height)
        // button cell back
        Toolkit.fillArea(batch, btn2PosX, posY, WIDTH_ONEBUTTON, height)
        Toolkit.fillArea(batch, btn1PosX, posY, WIDTH_ONEBUTTON, height)

        // text area border (base)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2) // this is a full border, not a text area
        Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)

        // text area border (pop-up for isActive)
        if (isEnabled) {
            batch.color = Toolkit.Theme.COL_SELECTED
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2) // this is a full border, not a text area
        }

        // button border
        if (mouseUpOnPasteButton) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnIMEButton) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnTextArea && !isEnabled) {
            batch.color = Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, inputPosX - 1, posY - 1, fbo.width + 2 * UI_TEXT_MARGIN+ 2, height + 2)
        }


        // draw text
        batch.color = if (textbuf.isEmpty()) TEXTINPUT_COL_TEXT_DISABLED else TEXTINPUT_COL_TEXT
        batch.draw(fbo.colorBufferTexture, inputPosX + 2f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())

        // draw text cursor
        val cursorXOnScreen = inputPosX + cursorDrawX + 2 + textDrawOffset
        if (isEnabled && cursorOn) {
            val baseCol = if (maxLen.exceeds(textbuf, listOf(32))) TEXTINPUT_COL_TEXT_NOMORE else TEXTINPUT_COL_TEXT

            batch.color = baseCol.cpy().mul(0.5f,0.5f,0.5f,1f)
            Toolkit.fillArea(batch, cursorXOnScreen, posY, 2, 24)

            batch.color = baseCol
            Toolkit.fillArea(batch, cursorXOnScreen, posY, 1, 23)
        }

        val imeButton = IME.icons[ime?.config?.lang] ?: labels.get(7, 2)

        // draw icon
        // IME button
        batch.color = if (mouseUpOnIMEButton && mouseDown || imeOn) Toolkit.Theme.COL_MOUSE_UP else if (mouseUpOnIMEButton) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_INACTIVE
        batch.draw(imeButton, btn1PosX + 2f, posY + 2f)
        // paste button
        batch.color = if (mouseUpOnPasteButton && mouseDown) Toolkit.Theme.COL_MOUSE_UP else if (mouseUpOnPasteButton) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_INACTIVE
        batch.draw(labels.get(8,2), btn2PosX + 2f, posY + 2f)

        // state of the candidates are concurrently changing, so we buffer them
        val localCandidates = ArrayList<CodepointSequence>()
        candidates.forEach { localCandidates.add(it) }

        // draw candidates view
        if (localCandidates.isNotEmpty() && ime != null) {

            val textWidths = localCandidates.map { App.fontGame.getWidth(CodepointSequence(it)) }
            val candidatesMax = ime.config.candidates.toInt()
            val candidatesCount = min(candidatesMax, localCandidates.size)
            val isOnecolumn = (candidatesCount <= 3)
            val halfcount = if (isOnecolumn) candidatesCount else FastMath.ceil(candidatesCount / 2f)
            val candidateWinH = halfcount * 20 // using hard-coded 20 instead of the actual font height of 24
            val candidatePosX = cursorXOnScreen + 4
            val candidatePosY = posY + 2

            // candidate view text
            if (candidatesMax > 1) {
                val longestCandidateW = textWidths.maxOrNull()!! + candidateNumberStrWidth
                val candidateWinW = if (isOnecolumn) longestCandidateW else 2*longestCandidateW + 3

                // candidate view background
                batch.color = candidatesBackCol
                Toolkit.fillArea(batch, candidatePosX, candidatePosY, candidateWinW + 4, candidateWinH)
                // candidate view border
                batch.color = Toolkit.Theme.COL_MOUSE_UP
                Toolkit.drawBoxBorder(batch, candidatePosX - 1, candidatePosY - 1, candidateWinW + 6, candidateWinH + 2)

                // candidate texts
                for (i in 0 until candidatesCount) {
                    val candidateNum = listOf(i+48,46,32)
                    App.fontGame.draw(batch, CodepointSequence(candidateNum + localCandidates[i]),
                            candidatePosX + (i / halfcount) * (longestCandidateW + 3) + 2,
                            candidatePosY + (i % halfcount) * 20 - 2 // using hard-coded 20 instead of the actual font height of 24
                    )
                }

                // candidate view splitter
                if (!isOnecolumn) {
                    batch.color = batch.color.cpy().mul(0.65f,0.65f,0.65f,1f)
                    Toolkit.fillArea(batch, candidatePosX + longestCandidateW + 2, candidatePosY, 1, candidateWinH)
                }
            }
            else {
                val candidateWinW = textWidths.maxOrNull()!!.coerceAtLeast(6)

                // candidate view background
                batch.color = candidatesBackCol
                Toolkit.fillArea(batch, candidatePosX, candidatePosY, candidateWinW, candidateWinH)
                // candidate view border
                batch.color = Toolkit.Theme.COL_MOUSE_UP
                Toolkit.drawBoxBorder(batch, candidatePosX - 1, candidatePosY - 1, candidateWinW + 2, candidateWinH + 2)

                val previewTextWidth = textWidths[0]
                App.fontGame.draw(batch, localCandidates[0], candidatePosX + (candidateWinW - previewTextWidth) / 2, candidatePosY - 2)
            }
        }

        batch.color = Color.WHITE
        super.render(batch, camera)


        oldPosX = posX
    }

    fun getText() = textbufToString()
    fun getTextOrPlaceholder(): String = if (textbuf.isEmpty()) currentPlaceholderText.toJavaString() else getText()
    fun clearText() {
        resetIME()
        textbuf.clear()
        cursorX = 0
        cursorDrawScroll = 0
        cursorDrawX = 0
    }
    fun setText(s: String) {
        clearText()
        appendText(s)
    }
    fun appendText(s: String) {
        val codepoints = s.toCodePoints()
        textbuf.addAll(codepoints)
        cursorX += codepoints.size
        currentTextLenPx = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
        moveCursorForward(currentTextLenPx - oldTextLenPx)
        oldTextLenPx = currentTextLenPx
    }
    override fun dispose() {
        fbo.dispose()
    }

    /*private fun CodepointSequence.toJavaString(): String {
        val sb = StringBuilder()
        this.forEach {
            sb.append(Character.toChars(it))
        }
        return sb.toString()
    }*/

}

