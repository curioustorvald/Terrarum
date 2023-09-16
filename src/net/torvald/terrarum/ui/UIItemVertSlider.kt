package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-09-17.
 */
class UIItemVertSlider(
    parentUI: UICanvas,
    initialX: Int, initialY: Int,
    private var initialValue: Double,
    val min: Double,
    val max: Double,
    override val height: Int,
    val handleHeight: Int = 12,
    private val backgroundTexture: TextureRegion? = null,
    private val disposeTexture: Boolean = false
) : UIItem(parentUI, initialX, initialY) {

    override val width = 16
    private var mouseOnHandle = false

    private val handleTravelDist = height - handleHeight
    private var handlePos = (initialValue / max).times(handleTravelDist).coerceIn(0.0, handleTravelDist.toDouble())

    var value: Double = initialValue; private set
    var selectionChangeListener: (Double) -> Unit = {}

    init {
        printdbg(this, "slider max=$max")
    }

    override fun update(delta: Float) {
        super.update(delta)

        mouseOnHandle = itemRelativeMouseY in handlePos.roundToInt() until handlePos.roundToInt() + handleHeight && itemRelativeMouseX in 0 until width

        // update handle position and value
        if (mouseUp && Terrarum.mouseDown || mouseLatched) {
            mouseLatched = true
            handlePos = (itemRelativeMouseY - handleHeight/2.0).coerceIn(0.0, handleTravelDist.toDouble())
            value = interpolateLinear(handlePos / handleTravelDist, min, max)
            selectionChangeListener(value)
        }

        if (!Terrarum.mouseDown) {
            mouseLatched = false
        }
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
            Toolkit.fillArea(batch, posX, posY + handlePos.roundToInt(), width, handleHeight)
        },
        // handle border
        { batch: SpriteBatch ->
            batch.color = handleCol
            Toolkit.drawBoxBorder(batch, posX - 1, posY + handlePos.roundToInt() - 1, width + 2, handleHeight + 2)
        },
    )

    private val renderOrderMouseUp = arrayOf(0,2,3,1).map { renderJobs[it] }

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)
        renderOrderMouseUp.forEach { it(batch) }

        super.render(batch, camera)
    }

    override fun dispose() {
        if (disposeTexture) backgroundTexture?.texture?.tryDispose()
    }

    fun scrolledForce(amountX: Float, amountY: Float): Boolean {
        val move = Math.round(amountY)
        val newValue = (value + move).coerceIn(min, max)

        handlePos = interpolateLinear(newValue / max, 0.0, handleTravelDist.toDouble())
        value = newValue
        selectionChangeListener(value)

        return true
    }
}