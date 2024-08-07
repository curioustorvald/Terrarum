package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticCursorHovered
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticPushedDown
import kotlin.math.absoluteValue

/**
 * Internal properties, namely initialValue, min, max, step; have the type of [Double] regardless of their input type.
 *
 * Created by minjaesong on 2021-10-23.
 */

class UIItemSpinner(
    parentUI: UICanvas,
    initialX: Int, initialY: Int,
    private var initialValue: Number,
    var min: Number,
    var max: Number,
    var step: Number,
    override val width: Int,
    private val drawBorder: Boolean = true,
    private val numberToTextFunction: (Number) -> String = { "$it" }
) : UIItem(parentUI, initialX, initialY) {

    override var suppressHaptic = false

    // to alleviate floating point errors adding up as the spinner is being used
    private val values = DoubleArray(1 + ((max.toDouble() - min.toDouble()).div(step.toDouble())).ceilToInt()) {
//        printdbg(this, "$min..$max step $step; index [$it] = ${min.toDouble() + (step.toDouble() * it)}")
        min.toDouble() + (step.toDouble() * it)
    }

    init {
//        println("valueType=${valueType.canonicalName} for UI ${parentUI.javaClass.canonicalName}")

        resetToInitial()
    }

    private val valueType = initialValue.javaClass // should be java.lang.Double or java.lang.Integer

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24
    private val buttonW = 30

    private val fboWidth = width - 2*buttonW - 6
    private val fboHeight = height - 4
//    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width - 2*buttonW - 6, height - 4, false)

    var value = initialValue.toDouble().coerceIn(min.toDouble(), max.toDouble()) as Number; private set
    var fboUpdateLatch = true

    private var mouseOnButton = 0 // 0: nothing, 1: left, 2: right

    var selectionChangeListener: (Number) -> Unit = {}
    private var currentIndex = values.indexOfFirst { it == initialValue.toDouble() }

    fun resetToInitial() {
        // if the initial value cannot be derived from the settings, round to nearest
        if (values.indexOfFirst { it == initialValue.toDouble() } < 0) {
//            throw IllegalArgumentException("Initial value $initialValue cannot be derived from given ($min..$max step $step) settings")
            val mind = min.toDouble()
            val maxd = max.toDouble()
            val stepd = step.toDouble()
            val id = initialValue.toDouble()

            val intermediate = (0..(maxd - mind).div(stepd).ceilToInt()).map {
                it to ((mind + stepd * it) - id).absoluteValue
            }.minByOrNull { it.second }!!.first * stepd + mind
            initialValue = when (initialValue.javaClass.simpleName) {
                "Integer" -> intermediate.toInt()
                else -> intermediate
            }
        }
        fboUpdateLatch = true
    }

    fun resetToSmallest() {
        currentIndex = 0
        value = values[currentIndex]
        fboUpdateLatch = true
    }

    fun changeValueBy(diff: Int) {
        currentIndex = (currentIndex + diff).coerceIn(values.indices)
        value = values[currentIndex]
        fboUpdateLatch = true
    }

    private var oldMouseOnButton = 0

    override fun update(delta: Float) {
        super.update(delta)

        mouseOnButton =
                if (itemRelativeMouseX in 0 until buttonW && itemRelativeMouseY in 0 until height)
                    1
                else if (itemRelativeMouseX in width - buttonW until width && itemRelativeMouseY in 0 until height)
                    2
                else if (itemRelativeMouseX in buttonW + 3 until width - buttonW - 3 && itemRelativeMouseY in 0 until height)
                    3
                else
                    0

        if (mouseOnButton in 1..2) mouseLatch.latch {
            changeValueBy((mouseOnButton * 2) - 3)
            fboUpdateLatch = true
            selectionChangeListener(value)
//            playHapticPushedDown() // changeValueBy() will play the sound
        }

        if (mouseOnButton > 0 && mouseOnButton != oldMouseOnButton) {
            playHapticCursorHovered()
        }

        oldMouseOnButton = mouseOnButton
    }

    private var textCache = ""
    private var textCacheLen = 0

    override fun show() {
        fboUpdateLatch = true
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {

        batch.end()

        if (fboUpdateLatch) {
//            printdbg(this, "FBO Latched")
            fboUpdateLatch = false
            textCache = numberToTextFunction(value.toDouble() + 0.0000000000000002)
            textCacheLen = App.fontGame.getWidth(textCache)
            /*fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                val t = numberToTextFunction(value)
                val tw = App.fontGame.getWidth(t)
                App.fontGameFBO.draw(it, t, (fbo.width - tw) / 2, 0)
            } }*/
        }

        batch.begin()

        if (drawBorder) {
            batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
            // left button cell back
            Toolkit.fillArea(batch, posX, posY, buttonW, height)
            // text area cell back
            Toolkit.fillArea(batch, posX + buttonW + 3, posY, width - 2*buttonW - 6, height)
            // right button cell back
            Toolkit.fillArea(batch, posX + width - buttonW, posY, buttonW, height)

            // text area border
            if (mouseOnButton != 3) {
                batch.color = Toolkit.Theme.COL_INACTIVE
                Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
            }

            // left button border
            batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_SELECTED
            else if (mouseOnButton == 1) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, buttonW + 2, height + 2)

            // right button border
            batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_SELECTED
            else if (mouseOnButton == 2) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX + width - buttonW - 1, posY - 1, buttonW + 2, height + 2)

            // text area border (again)
            if (mouseOnButton == 3) {
                batch.color = Toolkit.Theme.COL_MOUSE_UP
                Toolkit.drawBoxBorder(batch, posX + buttonW + 2, posY - 1, width - 2*buttonW - 4, height + 2)
            }
        }

        // left button icon
        batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseOnButton == 1) Toolkit.Theme.COL_MOUSE_UP else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(labels.get(9,2), posX + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // right button icon
        batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseOnButton == 2) Toolkit.Theme.COL_MOUSE_UP else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(labels.get(10,2), posX + width - buttonW + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // draw text
        batch.color = UIItemTextLineInput.TEXTINPUT_COL_TEXT
//        batch.draw(fbo.colorBufferTexture, posX + buttonW + 3f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())
        App.fontGame.draw(batch, textCache, posX + buttonW + 3f + (fboWidth - textCacheLen).div(2), posY.toFloat())

        super.render(frameDelta, batch, camera)

//        batch.color = Color.WHITE
//        App.fontSmallNumbers.draw(batch, "${valueType.simpleName}", posX.toFloat(), posY.toFloat()) // draws "Integer" or "Double"
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (mouseUp) {
            if (amountX <= -1 || amountY <= -1)
                changeValueBy(-1)
            else if (amountX >= 1 || amountY >= 1)
                changeValueBy(1)

            selectionChangeListener(value)
            fboUpdateLatch = true
            playHapticPushedDown()
            return true
        }
        else {
            return false
        }
    }

    override fun dispose() {
//        fbo.dispose()
    }
}
