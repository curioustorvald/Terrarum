package net.torvald.terrarum.modulebasegame.gameparticles

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameparticles.ParticleBase

/**
 * Created by minjaesong on 2017-01-20.
 */
class ParticleTestRain(posX: Double, posY: Double) : ParticleBase(Actor.RenderOrder.BEHIND, true, false, 6f) {

    init {
        body = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "weathers/raindrop.tga")))
        val w = body.regionWidth.toDouble()
        val h = body.regionHeight.toDouble()
        hitbox.setFromWidthHeight(
                posX - w.times(0.5),
                posY - h.times(0.5),
                w, h
        )

        velocity.y = 10.0
    }

}