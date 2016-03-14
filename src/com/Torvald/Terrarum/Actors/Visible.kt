package com.Torvald.Terrarum.Actors

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-14.
 */
interface Visible {
    fun drawBody(gc: GameContainer, g: Graphics)

    fun updateBodySprite(gc: GameContainer, delta: Int)
}
