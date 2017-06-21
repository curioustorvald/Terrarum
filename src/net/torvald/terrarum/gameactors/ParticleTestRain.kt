package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.ModMgr

/**
 * Created by minjaesong on 2017-01-20.
 */
class ParticleTestRain(posX: Double, posY: Double) : ParticleBase(Actor.RenderOrder.BEHIND, 6f) {

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