package net.torvald.terrarum.gameactors

import org.dyn4j.geometry.Vector2
import org.newdawn.slick.Image

/**
 * Created by SKYHi14 on 2017-01-20.
 */
class ParticleTestRain(posX: Double, posY: Double) : ParticleBase(ActorOrder.BEHIND, 6000) {

    init {
        image = Image("./assets/graphics/weathers/raindrop.tga")
        val w = image.width.toDouble()
        val h = image.height.toDouble()
        hitbox.setFromWidthHeight(
                posX - w.times(0.5),
                posY - h.times(0.5),
                w, h
        )

        velocity.y = 16.0
    }

}