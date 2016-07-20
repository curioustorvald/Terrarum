package net.torvald.terrarum.gameactors

import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Input

/**
 * Actors that has movement controlled by Keyboard or AI
 *
 * Created by minjaesong on 16-03-14.
 */
interface Controllable {

    fun processInput(input: Input)

    fun keyPressed(key: Int, c: Char)

}