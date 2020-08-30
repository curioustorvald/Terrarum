package net.torvald.terrarum.ui

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
) : UIItemTransitionContainer(parent, initialX, initialY, width, height, 0.212f, currentPosition, uis) {

    fun getOffX(index: Int) = ((currentPosition - index) * width).roundToInt()
    fun getOpacity(index: Int) = (currentPosition - index).coerceIn(0f, 1f)

    init {
        // re-position the uis according to the initial choice of currentPosition
        uis.forEachIndexed { index, it ->
            it.posX = posX + getOffX(index)
            it.initialX = posX + getOffX(index)
            it.posY = posY
            it.initialY = posY
            it.opacity = getOpacity(index)
        }
    }

    override fun onTransition(currentPosition: Float, uis: Array<out UICanvas>) {
        uis.forEachIndexed { index, it ->
            it.posX = it.initialX + getOffX(index)
            it.posY = posY
            it.opacity = getOpacity(index)
        }
    }
}