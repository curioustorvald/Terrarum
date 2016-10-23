package net.torvald.terrarum.gameactors

import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-10-23.
 */
class AnyPlayer(val actor: HistoricalFigure) {

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