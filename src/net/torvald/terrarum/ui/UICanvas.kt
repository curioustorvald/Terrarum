package net.torvald.terrarum.ui

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
    var openCloseTime: Int

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
}

const val OPENCLOSE_GENERIC = 200