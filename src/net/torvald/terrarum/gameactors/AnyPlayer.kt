package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ActorHumanoid
import org.newdawn.slick.Input

/**
 * A wrapper to support instant player changing (or possessing other NPCs maybe)
 *
 * Created by minjaesong on 16-10-23.
 */
class AnyPlayer(val actor: ActorHumanoid) {

    init {
        if (actor !is Controllable)
            throw IllegalArgumentException("Player must be 'Controllable'!")
    }

    fun processInput(input: Input) {
        (actor as Controllable).processInput(input)
    }

    fun keyPressed(key: Int, c: Char) {
        (actor as Controllable).keyPressed(key, c)
    }

}