package com.Torvald.Terrarum.Actors

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-15.
 */
class DroppedItem constructor() : ActorWithBody() {

    init {
        isVisible = true
    }

    override fun update(gc: GameContainer, delta_t: Int) {

    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        drawBody(gc, g)
    }
}