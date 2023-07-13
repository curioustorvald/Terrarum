package net.torvald.terrarum.ui

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.modulebasegame.ui.NullUI
import net.torvald.terrarum.tryDispose
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Three virtual slide panels for horizontal sliding transition.
 *
 * The left/centre/right designation is arbitrary: you can have "left" as a centre (main) and the "centre" as a right (aux)
 *
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
    private val uisOnLeft: List<UICanvas>,
    private val uisOnCentre: List<UICanvas>,
    private val uisOnRight: List<UICanvas> = emptyList(),
) : UIItemTransitionContainer(parent, initialX, initialY, width, height, 0.12f, currentPosition, emptyList()) {

    fun getOffX(index: Int) = -((currentPosition - index) * width / 2f).roundToInt()
    fun getOpacity(index: Int) = 1f - (currentPosition - index).absoluteValue.coerceIn(0f, 1f)

    private var leftUI: UICanvas = uisOnLeft.firstOrNull() ?: NullUI
    private var centreUI: UICanvas = uisOnCentre.firstOrNull() ?: NullUI
    private var rightUI: UICanvas = uisOnRight.firstOrNull() ?: NullUI

    override val uis: List<UICanvas>; get() = listOf(leftUI, centreUI, rightUI)

    fun setLeftUIto(index: Int) {
        leftUI = uisOnLeft[index]
    }
    fun setCentreUIto(index: Int) {
        centreUI = uisOnCentre[index]
    }
    fun setRightUIto(index: Int) {
        rightUI = uisOnRight[index]
    }

    init {
        // re-position the uis according to the initial choice of currentPosition
        uisOnLeft.forEachIndexed { index, it ->
            it.posX = getOffX(0)
            it.initialX = getOffX(0)
            it.posY = 0
            it.initialY = 0
            it.opacity = getOpacity(0)
        }
        uisOnCentre.forEachIndexed { index, it ->
            it.posX = getOffX(1)
            it.initialX = getOffX(1)
            it.posY = 0
            it.initialY = 0
            it.opacity = getOpacity(1)
        }
        uisOnRight.forEachIndexed { index, it ->
            it.posX = getOffX(2)
            it.initialX = getOffX(2)
            it.posY = 0
            it.initialY = 0
            it.opacity = getOpacity(2)
        }
    }

    override fun onTransition(currentPosition: Float, uis: List<UICanvas>) {
        uis.forEachIndexed { index, it ->
            it.posX = getOffX(index)
            it.posY = it.initialY
            it.opacity = getOpacity(index)
        }
        INGAME.setTooltipMessage(null)
    }

    override fun dispose() {
        uisOnLeft.forEach { it.tryDispose() }
        uisOnCentre.forEach { it.tryDispose() }
        uisOnRight.forEach { it.tryDispose() }
    }

}