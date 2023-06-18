package net.torvald.terrarum.ui

import com.jme3.math.FastMath
import net.torvald.terrarum.INGAME
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * @param width size of the canvas where transition occurs
 * @param height size of the canvas where transition occurs
 */
class UIItemHorizontalFadeSlide(
        parent: UICanvas,
        initialX: Int,
        initialY: Int,
        width: Int,
        height: Int,
        //transitionLength: Float,
        currentPosition: Float,
        vararg uis: UICanvas
) : UIItemTransitionContainer(parent, initialX, initialY, width, height, 0.12f, currentPosition, uis) {

    fun getOffX(index: Int) = ((currentPosition - index) * width / 2f).roundToInt()
    fun getOpacity(index: Int) = 1f - (currentPosition - index).absoluteValue.coerceIn(0f, 1f)


    init {
        // re-position the uis according to the initial choice of currentPosition
        uis.forEachIndexed { index, it ->
            it.posX = 0 + getOffX(index)
            it.initialX = 0 + getOffX(index)
            it.posY = 0
            it.initialY = 0
            it.opacity = getOpacity(index)
        }
    }

    override fun onTransition(currentPosition: Float, uis: Array<out UICanvas>) {
        uis.forEachIndexed { index, it ->
            it.posX = -getOffX(index)
            it.posY = it.initialY
            it.opacity = getOpacity(index)
        }
        INGAME.setTooltipMessage(null)
    }
}