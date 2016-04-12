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
     * In milliseconds
     */
    var openCloseTime: Int

    fun update(gc: GameContainer, delta: Int)

    fun render(gc: GameContainer, g: Graphics)

    fun processInput(input: Input)

    fun doOpening(gc: GameContainer, delta: Int)

    fun doClosing(gc: GameContainer, delta: Int)

    fun endOpening(gc: GameContainer, delta: Int)

    fun endClosing(gc: GameContainer, delta: Int)
}