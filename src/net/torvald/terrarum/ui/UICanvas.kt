package net.torvald.terrarum.ui

import net.torvald.point.Point2d
import net.torvald.terrarum.Millisec
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.roundInt
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 15-12-31.
 */
interface UICanvas {

    var width: Int
    var height: Int

    /**
     * Usage: (in StateInGame:) uiHandlerField.ui.handler = uiHandlerField
     */
    var handler: UIHandler?

    /**
     * In milliseconds
     *
     * Timer itself is implemented in the handler.
     */
    var openCloseTime: Millisec

    fun update(gc: GameContainer, delta: Int)

    fun render(gc: GameContainer, g: Graphics)

    fun processInput(gc: GameContainer, delta: Int, input: Input)

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    fun doOpening(gc: GameContainer, delta: Int)

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    fun doClosing(gc: GameContainer, delta: Int)

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    fun endOpening(gc: GameContainer, delta: Int)

    /**
     * Do not modify handler!!.openCloseCounter here.
     */
    fun endClosing(gc: GameContainer, delta: Int)

    companion object {
        const val OPENCLOSE_GENERIC = 200

        fun doOpeningFade(handler: UIHandler?, openCloseTime: Int) {
            handler!!.opacity = handler.openCloseCounter.toFloat() / openCloseTime
        }
        fun doClosingFade(handler: UIHandler?, openCloseTime: Int) {
            handler!!.opacity = (openCloseTime - handler.openCloseCounter.toFloat()) / openCloseTime
        }
        fun endOpeningFade(handler: UIHandler?) {
            handler!!.opacity = 1f
        }
        fun endClosingFade(handler: UIHandler?) {
            handler!!.opacity = 0f
        }


        fun doOpeningPopOut(handler: UIHandler?, openCloseTime: Int, position: Position) {
            when (position) {
                Position.LEFT -> handler!!.posX = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        -handler.UI.width.toFloat(),
                        0f
                ).roundInt()
                Position.TOP -> handler!!.posY = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        -handler.UI.height.toFloat(),
                        0f
                ).roundInt()
                Position.RIGHT -> handler!!.posX = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        Terrarum.WIDTH.toFloat(),
                        Terrarum.WIDTH - handler.UI.width.toFloat()
                ).roundInt()
                Position.BOTTOM -> handler!!.posY = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        Terrarum.HEIGHT.toFloat(),
                        Terrarum.HEIGHT - handler.UI.height.toFloat()
                ).roundInt()
            }
        }
        fun doClosingPopOut(handler: UIHandler?, openCloseTime: Int, position: Position) {
            when (position) {
                Position.LEFT -> handler!!.posX = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        0f,
                        -handler.UI.width.toFloat()
                ).roundInt()
                Position.TOP -> handler!!.posY = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        0f,
                        -handler.UI.height.toFloat()
                ).roundInt()
                Position.RIGHT -> handler!!.posX = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        Terrarum.WIDTH - handler.UI.width.toFloat(),
                        Terrarum.WIDTH.toFloat()
                ).roundInt()
                Position.BOTTOM -> handler!!.posY = Movement.fastPullOut(
                        handler.openCloseCounter.toFloat() / openCloseTime,
                        Terrarum.HEIGHT - handler.UI.height.toFloat(),
                        Terrarum.HEIGHT.toFloat()
                ).roundInt()
            }
        }
        fun endOpeningPopOut(handler: UIHandler?, position: Position) {
            when (position) {
                Position.LEFT -> handler!!.posX = 0
                Position.TOP -> handler!!.posY = 0
                Position.RIGHT -> handler!!.posX = Terrarum.WIDTH - handler.UI.width
                Position.BOTTOM -> handler!!.posY = Terrarum.HEIGHT - handler.UI.height
            }
        }
        fun endClosingPopOut(handler: UIHandler?, position: Position) {
            when (position) {
                Position.LEFT -> handler!!.posX = -handler.UI.width
                Position.TOP -> handler!!.posY = -handler.UI.height
                Position.RIGHT -> handler!!.posX = Terrarum.WIDTH
                Position.BOTTOM -> handler!!.posY = Terrarum.HEIGHT
            }
        }

        // TODO add blackboard take in/out (sinusoidal)

        enum class Position {
            LEFT, RIGHT, TOP, BOTTOM
        }
    }
}
