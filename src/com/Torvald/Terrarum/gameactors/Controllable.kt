package com.torvald.terrarum.gameactors

import org.newdawn.slick.Input

/**
 * Created by minjaesong on 16-03-14.
 */
interface Controllable {

    fun processInput(input: Input)

    fun keyPressed(key: Int, c: Char)

}