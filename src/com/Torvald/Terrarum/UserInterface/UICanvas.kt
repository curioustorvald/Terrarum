package com.Torvald.Terrarum.UserInterface

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-03-14.
 */
interface UICanvas {

    var width: Int?
    var height: Int?

    fun update(gc: GameContainer, delta_t: Int)

    fun render(gc: GameContainer, g: Graphics)

    fun processInput(input: Input)

}