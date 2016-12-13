package net.torvald.terrarum.gameactors

import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input

/**
 * Actors that has movement controlled by Keyboard or AI
 *
 * Created by minjaesong on 15-12-31.
 */
interface Controllable {

    fun processInput(gc: GameContainer, delta: Int, input: Input)

    fun keyPressed(key: Int, c: Char)

}