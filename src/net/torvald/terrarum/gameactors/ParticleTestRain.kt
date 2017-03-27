package net.torvald.terrarum.gameactors

import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 2017-01-20.
 */
class ParticleTestRain(posX: Double, posY: Double) : ParticleBase(Actor.RenderOrder.BEHIND, 6000) {

    init {
        body = Image("./assets/graphics/weathers/raindrop.tga")
        val w = body.width.toDouble()
        val h = body.height.toDouble()
        hitbox.setFromWidthHeight(
                posX - w.times(0.5),
                posY - h.times(0.5),
                w, h
        )

        velocity.y = 10.0
    }

}