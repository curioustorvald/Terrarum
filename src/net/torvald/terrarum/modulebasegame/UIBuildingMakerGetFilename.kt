package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_BACKGROUNDCOL
import net.torvald.terrarum.ui.UINSMenu.Companion.LINE_HEIGHT
import kotlin.math.roundToInt

class UIBuildingMakerGetFilename : UICanvas() {

    var confirmCallback: (String) -> Unit = {}
    var title = "Export"
    var text = listOf("")

    var labelDo = "OK"
    var labelDont = "Cancel"

    override var width = 280
    override var height = 160

    private val textWidth = width - LINE_HEIGHT
    private val buttonWidth = 101
    private val buttonGap = (width - buttonWidth*2) / 3

    override var openCloseTime = OPENCLOSE_GENERIC

    val textInput = UIItemTextLineInput(
        this,
        (width - textWidth) / 2,
        LINE_HEIGHT * (text.size + 2),
        textWidth,
        { "The Yucky Panopticon" },
        InputLenCap(250, InputLenCap.CharLenUnit.UTF8_BYTES)
    )

    val buttonOk = UIItemTextButton(
        this,
        { labelDo },
        buttonGap,
        height - LINE_HEIGHT - LINE_HEIGHT/2,
        buttonWidth
    ).also {
        it.clickOnceListener = { _, _ ->

            textInput.getTextOrPlaceholder().let { name ->
                if (name.isNotBlank())
                    confirmCallback(name.trim())
            }

            this.setAsClose()
        }
    }

    val buttonCancel = UIItemTextButton(
        this,
        { labelDont },
        width - buttonWidth - buttonGap,
        height - LINE_HEIGHT - LINE_HEIGHT/2,
        buttonWidth
    ).also {
        it.clickOnceListener = { _, _ ->
            reset()
            this.setAsClose()
        }
    }

    fun reset() {
        textInput.clearText()
    }

    init {
        addUIitem(textInput)
        addUIitem(buttonOk)
        addUIitem(buttonCancel)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }


    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)

        // draw border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, -1, -1 - LINE_HEIGHT, width + 2, height + LINE_HEIGHT + 2)

        // draw title bar
        batch.color = UINSMenu.DEFAULT_TITLEBACKCOL
        Toolkit.fillArea(batch, 0, 0 - LINE_HEIGHT, width, LINE_HEIGHT)

        batch.color = UINSMenu.DEFAULT_TITLETEXTCOL
        App.fontGame.draw(batch, title, UINSMenu.TEXT_OFFSETX + 0, UINSMenu.TEXT_OFFSETY + 0 - LINE_HEIGHT)

        // draw the back
        batch.color = DEFAULT_BACKGROUNDCOL
        Toolkit.fillArea(batch, 0, 0, width, height)


        // draw the list
        batch.color = Color.WHITE
        val textWidth: Int = text.maxOf { App.fontGame.getWidth(it) }

        text.forEachIndexed { index, str ->
            App.fontGame.draw(batch, str, 0 + LINE_HEIGHT / 2, 0 + LINE_HEIGHT / 2 + LINE_HEIGHT * index)
        }

        uiItems.forEach { it.render(batch, camera) }
    }

    private var dragOriginX = 0 // relative mousepos
    private var dragOriginY = 0 // relative mousepos
    private var dragForReal = false

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (mouseInScreen(screenX, screenY)) {
            if (dragForReal) {
                handler.setPosition(
                    (screenX / App.scr.magn - dragOriginX).roundToInt(),
                    (screenY / App.scr.magn - dragOriginY).roundToInt()
                )
            }
        }

        uiItems.forEach { it.touchDragged(screenX, screenY, pointer) }

        return true
    }

    fun mouseOnTitleBar() =
        relativeMouseX in 0 until width && relativeMouseY in -LINE_HEIGHT until 0

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseOnTitleBar()) {
            dragOriginX = relativeMouseX
            dragOriginY = relativeMouseY
            dragForReal = true
        }
        else {
            dragForReal = false
        }

        uiItems.forEach { it.touchDown(screenX, screenY, pointer, button) }

        return true
    }

    override fun dispose() {
        uiItems.forEach { it.tryDispose() }
    }
}
