package com.torvald.terrarum.gameactors

import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-14.
 */
class PhysTestBall : ActorWithBody {
    constructor(): super() {
        setHitboxDimension(16, 16, 0, 0)
        isVisible = true
        mass = 10f
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {
        g.color = Color.orange
        g.fillOval(
                hitbox!!.posX,
                hitbox!!.posY,
                hitbox!!.width,
                hitbox!!.height)
    }
}