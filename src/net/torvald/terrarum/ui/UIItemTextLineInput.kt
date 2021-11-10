package net.torvald.terrarum.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gamecontroller.*
import net.torvald.terrarum.utils.Clipboard
import net.torvald.terrarumsansbitmap.gdx.CodepointSequence
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.toJavaString
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
        val enablePasteButton: Boolean = true,
        val enableIMEButton: Boolean = true
) : UIItem(parentUI, initialX, initialY) {

    init {
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24

    private val buttonsShown = enableIMEButton.toInt() + enablePasteButton.toInt()

    companion object {
        val TEXTINPUT_COL_TEXT = Color.WHITE
        val TEXTINPUT_COL_TEXT_NOMORE = Color(0xFF8888FF.toInt())
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
            width - 2 * UI_TEXT_MARGIN - buttonsShown * (WIDTH_ONEBUTTON + 3),
            height - 2 * UI_TEXT_MARGIN,
            true
    )

    var isActive: Boolean = true

    var cursorX = 0
    var cursorDrawScroll = 0
    var cursorDrawX = 0 // pixelwise point
    var cursorBlinkCounter = 0f
    var cursorOn = true

    private val textbuf = CodepointSequence()

    private var fboUpdateLatch = true

    private var currentPlaceholderText = ArrayList<Int>(placeholder().toCodePoints()) // the placeholder text may change every time you call it


    private val btn1PosX = posX + width - 2*WIDTH_ONEBUTTON - 3
    private val btn2PosX = posX + width - WIDTH_ONEBUTTON

    private val mouseUpOnTextArea: Boolean
        get() = relativeMouseX in 0 until fbo.width + 2* UI_TEXT_MARGIN && relativeMouseY in 0 until height
    private val mouseUpOnButton1
        get() = buttonsShown > 1 && relativeMouseX in btn1PosX - posX until btn1PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height
    private val mouseUpOnButton2
        get() = buttonsShown > 0 && relativeMouseX in btn2PosX - posX until btn2PosX - posX + WIDTH_ONEBUTTON && relativeMouseY in 0 until height

    private var imeOn = false
    private var candidates: List<CodepointSequence> = listOf()

    private val candidatesBackCol = TEXTINPUT_COL_BACKGROUND.cpy().mul(1f,1f,1f,1.5f)
    private val candidateNumberStrWidth = App.fontGame.getWidth("8. ")

    private fun getIME(): TerrarumIME? {
        if (!imeOn) return null

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

    private fun tryCursorBack() {
        if (cursorDrawX > fbo.width) {
            val d = cursorDrawX - fbo.width
            cursorDrawScroll = d
        }
    }
    private fun tryCursorForward() {
        if (cursorDrawX - cursorDrawScroll < 0) {
            cursorDrawScroll -= cursorDrawX
        }
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        val oldActive = isActive

        // process keypresses
        if (isActive) {

            val (eventType, char, headkey, repeatCount, keycodes) = e

            try {
                if (eventType == InputStrober.KEY_DOWN || eventType == InputStrober.KEY_CHANGE) {
                    fboUpdateLatch = true
                    forceLitCursor()
                    val ime = getIME()
                    val lowLayer = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

                    if (keycodes.contains(App.getConfigInt("control_key_toggleime")) && repeatCount == 1) {
                        toggleIME()
                    }
                    else if (keycodes.contains(Input.Keys.V) && keycodes.containsSome(Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT)) {
                        endComposing()
                        paste(Clipboard.fetch().substringBefore('\n').substringBefore('\t').toCodePoints())
                    }
                    else if (keycodes.contains(Input.Keys.C) && (keycodes.containsSome(Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT))) {
                        endComposing()
                        copyToClipboard()
                    }
                    else if (keycodes.contains(Input.Keys.BACKSPACE) || (keycodes.contains(Input.Keys.CAPS_LOCK) && lowLayer.capsMode == TerrarumKeyCapsMode.BACK)) {
                        if (ime != null && ime.composing()) {
                            candidates = ime.backspace().map { CodepointSequence(it.toCodePoints()) }
                        }
                        else if (cursorX <= 0) {
                            cursorX = 0
                            cursorDrawX = 0
                            cursorDrawScroll = 0
                        }
                        else {
                            endComposing()
                            if (cursorX > 0) {
                                while (true) {
                                    cursorX -= 1
                                    val oldCode = textbuf.removeAt(cursorX)
                                    // continue deleting hangul pieces because of the font...
                                    if (cursorX == 0 || (oldCode !in 0x115F..0x11FF && oldCode !in 0xD7B0..0xD7FF)) break
                                }

                                cursorDrawX = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                                tryCursorForward()
                            }
                        }
                    }
                    else if (keycodes.contains(Input.Keys.LEFT)) {
                        endComposing()

                        if (cursorX > 0) {
                            cursorX -= 1
                            cursorDrawX = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                            tryCursorForward()
                            if (cursorX <= 0) {
                                cursorX = 0
                                cursorDrawX = 0
                                cursorDrawScroll = 0
                            }
                        }
                    }
                    else if (keycodes.contains(Input.Keys.RIGHT)) {
                        endComposing()

                        if (cursorX < textbuf.size) {
                            cursorX += 1
                            cursorDrawX = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))
                            tryCursorBack()
                        }
                    }
                    // accept:
                    // - literal "<"
                    // - keysymbol that does not start with "<" (not always has length of 1 because UTF-16)
                    else if (char != null && char.length > 0 && char[0].code >= 32 && (char == "<" || !char.startsWith("<"))) {
                        val shiftin = keycodes.containsSome(Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT)
                        val altgrin = keycodes.contains(Input.Keys.ALT_RIGHT) || keycodes.containsAll(Input.Keys.ALT_LEFT, Input.Keys.CONTROL_LEFT)

                        val codepoints = if (ime != null) {
                            val newStatus = ime.acceptChar(headkey, shiftin, altgrin, char)
                            candidates = newStatus.first.map { CodepointSequence(it.toCodePoints()) }

                            newStatus.second.toCodePoints()
                        }
                        else char.toCodePoints()

//                    println("textinput codepoints: ${codepoints.map { it.toString(16) }.joinToString()}")

                        if (!maxLen.exceeds(textbuf, codepoints)) {
                            textbuf.addAll(cursorX, codepoints)

                            cursorX += codepoints.size
                            cursorDrawX = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))

                            tryCursorBack()
                        }
                    }
                    else if (keycodes.containsSome(Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER)) {
                        endComposing()
                    }

                    // don't put innards of tryCursorBack/Forward here -- you absolutely don't want that behaviour
                }
            }
            catch (e: NullPointerException) {
                e.printStackTrace()
            }

            if (textbuf.size == 0) {
                currentPlaceholderText = CodepointSequence(placeholder().toCodePoints())
            }
        }
        else if (oldActive) { // just became deactivated
            endComposing()
        }
    }

    override fun update(delta: Float) {
        super.update(delta)
        val mouseDown = Terrarum.mouseDown

        if (mouseDown) {
            isActive = mouseUp
        }

        if (App.getConfigString("inputmethod") == "none") imeOn = false

        if (isActive) {
            cursorBlinkCounter += delta

            while (cursorBlinkCounter >= CURSOR_BLINK_TIME) {
                cursorBlinkCounter -= CURSOR_BLINK_TIME
                cursorOn = !cursorOn
            }
        }

        if (mouseDown && !mouseLatched && (enablePasteButton && enableIMEButton && mouseUpOnButton1 || enableIMEButton && !enablePasteButton && mouseUpOnButton2)) {
            toggleIME()
            mouseLatched = true
        }
        else if (mouseDown && !mouseLatched && (enablePasteButton && enableIMEButton && mouseUpOnButton2 || enablePasteButton && !enableIMEButton && mouseUpOnButton2)) {
            endComposing()
            paste(Clipboard.fetch().substringBefore('\n').substringBefore('\t').toCodePoints())
            mouseLatched = true
        }

        if (!mouseDown) mouseLatched = false
    }

    private fun String.toCodePoints() = this.codePoints().toList().filter { it > 0 }

    private fun endComposing() {
        getIME()?.let {
            val s = it.endCompose()
            paste(s.toCodePoints())
        }
        fboUpdateLatch = true
        candidates = listOf()
//        resetIME() // not needed; IME will reset itself
    }

    private fun toggleIME() {
        endComposing()

        if (App.getConfigString("inputmethod") == "none") {
            imeOn = false
            return
        }

        imeOn = !imeOn
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

        textbuf.addAll(cursorX, actuallyInserted)

        cursorX += actuallyInserted.size
        cursorDrawX = App.fontGame.getWidth(CodepointSequence(textbuf.subList(0, cursorX)))

        tryCursorBack()

        fboUpdateLatch = true
    }

    private fun copyToClipboard() {
        Clipboard.copy(textbufToString())
    }

    private fun textbufToString(): String {
        return textbuf.toJavaString()
    }

    override fun render(batch: SpriteBatch, camera: Camera) {

        batch.end()

        if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                App.fontGameFBO.draw(it, if (textbuf.isEmpty()) currentPlaceholderText else textbuf, -1f*cursorDrawScroll, 0f)
            } }
        }

        batch.begin()

        val mouseDown = Terrarum.mouseDown

        // text area cell back
        batch.color = TEXTINPUT_COL_BACKGROUND
        Toolkit.fillArea(batch, posX, posY, fbo.width + 2 * UI_TEXT_MARGIN, height)
        // rightmost button cell back
        if (buttonsShown > 0)
            Toolkit.fillArea(batch, btn2PosX, posY, WIDTH_ONEBUTTON, height)
        if (buttonsShown > 1)
            Toolkit.fillArea(batch, btn1PosX, posY, WIDTH_ONEBUTTON, height)

        // text area border (base)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        if (buttonsShown > 0)
            Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        if (buttonsShown > 1)
            Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)

        // text area border (pop-up for isActive)
        if (isActive) {
            batch.color = Toolkit.Theme.COL_HIGHLIGHT
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        }

        // button border
        if (mouseUpOnButton2) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, btn2PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnButton1) {
            batch.color = if (mouseDown) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, btn1PosX - 1, posY - 1, WIDTH_ONEBUTTON + 2, height + 2)
        }
        else if (mouseUpOnTextArea && !isActive) {
            batch.color = Toolkit.Theme.COL_ACTIVE
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, fbo.width + 2 * UI_TEXT_MARGIN+ 2, height + 2)
        }


        // draw text
        batch.color = if (textbuf.isEmpty()) TEXTINPUT_COL_TEXT_DISABLED else TEXTINPUT_COL_TEXT
        batch.draw(fbo.colorBufferTexture, posX + 2f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())

        // draw text cursor
        val cursorXOnScreen = posX - cursorDrawScroll + cursorDrawX + 2
        if (isActive && cursorOn) {
            val baseCol = if (maxLen.exceeds(textbuf, listOf(32))) TEXTINPUT_COL_TEXT_NOMORE else TEXTINPUT_COL_TEXT

            batch.color = baseCol.cpy().mul(0.5f,0.5f,0.5f,1f)
            Toolkit.fillArea(batch, cursorXOnScreen, posY, 2, 24)

            batch.color = baseCol
            Toolkit.fillArea(batch, cursorXOnScreen, posY, 1, 23)
        }

        // draw icon
        if (enablePasteButton && enableIMEButton) {
            // IME button
            batch.color = if (mouseUpOnButton1 && mouseDown || imeOn) Toolkit.Theme.COL_ACTIVE else if (mouseUpOnButton1) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(7,2), btn1PosX + 2f, posY + 2f)
            // paste button
            batch.color = if (mouseUpOnButton2 && mouseDown) Toolkit.Theme.COL_ACTIVE else if (mouseUpOnButton2) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(8,2), btn2PosX + 2f, posY + 2f)
        }
        else if (!enableIMEButton && enablePasteButton) {
            // paste button
            batch.color = if (mouseUpOnButton2 && mouseDown) Toolkit.Theme.COL_ACTIVE else if (mouseUpOnButton2) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(8,2), btn2PosX + 2f, posY + 2f)
        }
        else if (!enablePasteButton && enableIMEButton) {
            // IME button
            batch.color = if (mouseUpOnButton1 && mouseDown || imeOn) Toolkit.Theme.COL_ACTIVE else if (mouseUpOnButton1) Toolkit.Theme.COL_HIGHLIGHT else Toolkit.Theme.COL_INACTIVE
            batch.draw(labels.get(7,2), btn2PosX + 2f, posY + 2f)
        }

        // state of the candidates are concurrently changing, so we buffer them
        val localCandidates = ArrayList<CodepointSequence>(); candidates.forEach { localCandidates.add(it) }

        // draw candidates view
        if (localCandidates.isNotEmpty()) {

            val textWidths = localCandidates.map { App.fontGame.getWidth(CodepointSequence(it)) }
            val candidatesMax = getIME()!!.config.candidates.toInt()
            val candidatesCount = minOf(candidatesMax, localCandidates.size)
            val isOnecolumn = (candidatesCount <= 3)
            val halfcount = if (isOnecolumn) candidatesCount else FastMath.ceil(candidatesCount / 2f)
            val candidateWinH = App.fontGame.lineHeight.toInt() * halfcount
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
                batch.color = Toolkit.Theme.COL_ACTIVE
                Toolkit.drawBoxBorder(batch, candidatePosX - 1, candidatePosY - 1, candidateWinW + 6, candidateWinH + 2)

                // candidate texts
                for (i in 0 until candidatesCount) {
                    val candidateNum = listOf(i+48,46,32)
                    App.fontGame.draw(batch, CodepointSequence(candidateNum + localCandidates[i]),
                            candidatePosX + (i / halfcount) * (longestCandidateW + 3) + 2,
                            candidatePosY + (i % halfcount) * 20
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
                batch.color = Toolkit.Theme.COL_ACTIVE
                Toolkit.drawBoxBorder(batch, candidatePosX - 1, candidatePosY - 1, candidateWinW + 2, candidateWinH + 2)

                val previewTextWidth = textWidths[0]
                App.fontGame.draw(batch, localCandidates[0], candidatePosX + (candidateWinW - previewTextWidth) / 2, candidatePosY)
            }
        }

        batch.color = Color.WHITE
        super.render(batch, camera)
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
        textbuf.addAll(s.toCodePoints())
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

