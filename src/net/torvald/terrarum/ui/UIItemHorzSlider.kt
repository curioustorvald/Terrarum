package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticCursorHovered
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticNudge
import net.torvald.terrarum.ui.UIItemAccessibilityUtil.playHapticPushedDown
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * For stepped values use UIItemHorzSliderStep (has different design tho:
 * ```
 * [-]--|--|--[]--|--[+]
 * ```
 *
 *
 *
 * Created by minjaesong on 2023-07-14.
 */

class UIItemHorzSlider(
    parentUI: UICanvas,
    initialX: Int, initialY: Int,
    private var initialValue: Double,
    val min: Double,
    val max: Double,
    override val width: Int,
    initialHandleWidth: Int = 12,
    private val backgroundTexture: TextureRegion? = null,
    private val disposeTexture: Boolean = false
) : UIItem(parentUI, initialX, initialY) {

    override var suppressHaptic = false

    var handleWidth: Int = initialHandleWidth
        set(value) {
            // reset the scroll status
            handlePos = 0.0
            this.value = 0.0
            field = value.coerceIn(12, width)

            handleTravelDist = width - field
        }

    override val height = 24
    private var mouseOnHandle = false

    private var handleTravelDist = width - handleWidth
    private var handlePos = (initialValue / max).times(handleTravelDist).coerceIn(0.0, handleTravelDist.toDouble())

    var value: Double = initialValue; private set
    var selectionChangeListener: (Double) -> Unit = {}

    private var mouseLatched = false // trust me this one needs its own binary latch

    private var oldValue = initialValue

    override fun update(delta: Float) {
        super.update(delta)

        mouseOnHandle = itemRelativeMouseX in handlePos.roundToInt() until handlePos.roundToInt() + handleWidth && itemRelativeMouseY in 0 until height

        // update handle position and value
        if (mouseUp && Terrarum.mouseDown || mouseLatched) {
            mouseLatched = true
            handlePos = (itemRelativeMouseX - handleWidth/2.0).coerceIn(0.0, handleTravelDist.toDouble())
            value = interpolateLinear(handlePos / handleTravelDist, min, max)
            selectionChangeListener(value)
            if (oldValue != value && (value == min || value == max)) {
                playHapticNudge()
            }
        }

        if (!Terrarum.mouseDown) {
            mouseLatched = false
        }

        suppressHaptic = mouseLatched

        oldValue = value
    }

    val troughBorderCol: Color; get() = if (mouseUp || mouseLatched) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
    val handleCol: Color; get() = if (mouseOnHandle && mousePushed || mouseLatched) Toolkit.Theme.COL_SELECTED
    else if (mouseOnHandle) Toolkit.Theme.COL_MOUSE_UP else Color.WHITE


    private val renderJobs = arrayOf(
        // trough fill
        { batch: SpriteBatch ->
            if (backgroundTexture != null) {
                batch.color = Color.WHITE
                batch.draw(backgroundTexture, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
            }
            else {
                batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
                Toolkit.fillArea(batch, posX, posY, width, height)
            }
        },
        // trough border
        { batch: SpriteBatch ->
            batch.color = troughBorderCol
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        },
        // handle fill
        { batch: SpriteBatch ->
            batch.color = handleCol.cpy().mul(Color.LIGHT_GRAY)
            Toolkit.fillArea(batch, posX + handlePos.roundToInt(), posY, handleWidth, height)
        },
        // handle border
        { batch: SpriteBatch ->
            batch.color = handleCol
            Toolkit.drawBoxBorder(batch, posX + handlePos.roundToInt() - 1, posY - 1, handleWidth + 2, height + 2)
        },
    )

    private val renderOrderMouseUp = arrayOf(0,2,3,1).map { renderJobs[it] }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)
        renderOrderMouseUp.forEach { it(batch) }

        super.render(frameDelta, batch, camera)
    }

    override fun dispose() {
        if (disposeTexture) backgroundTexture?.texture?.tryDispose()
    }

    fun scrolledForce(amountX: Float, amountY: Float): Boolean {
        val scroll = if (amountY == 0f) amountX else if (amountX == 0f) amountY else (amountX + amountY) / 2f

        val move = Math.round(scroll)
        val newValue = (value + move).coerceIn(min, max)

        handlePos = interpolateLinear(newValue / max, 0.0, handleTravelDist.toDouble())
        value = newValue
        selectionChangeListener(value)

        return true
    }
}