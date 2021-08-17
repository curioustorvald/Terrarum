package net.torvald.terrarum.gameparticles

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.imagefont.TinyAlphNum

/**
 * @param tex image
 * @param x x-coord of the particle's initial spawn position, bottom-centre
 * @param y y-coord of the particle's initial spawn position, bottom-centre
 */
open class ParticleVanishingTexture(val tex: TextureRegion, x: Double, y: Double) : ParticleBase(Actor.RenderOrder.OVERLAY, false, 2f) {

    init {
        velocity.set(0.0, -1.0)
        hitbox.setDimension(2.0, 2.0)
        hitbox.setPositionFromPointed(x, y)

        body = tex

        isNoSubjectToGrav = true
    }

    override fun update(delta: Float) {
        super.update(delta)

        drawColour.a = (lifetimeMax - lifetimeCounter) / lifetimeMax
    }
}

class ParticleVanishingText(val text: String, x: Double, y: Double) : ParticleBase(Actor.RenderOrder.OVERLAY, false, 2f) {

    private val lines = text.split('\n')

    init {
        velocity.set(0.0, -1.0)

        hitbox.setDimension(lines.maxOf { TinyAlphNum.getWidth(it) }.toDouble(), lines.size * TinyAlphNum.H.toDouble())
        hitbox.setPositionFromPointed(x, y)

        isNoSubjectToGrav = true
    }

    override fun update(delta: Float) {
        super.update(delta)
        drawColour.a = (lifetimeMax - lifetimeCounter) / lifetimeMax
    }

    override fun drawBody(batch: SpriteBatch) {
        if (!flagDespawn) {
            batch.color = drawColour
            lines.forEachIndexed { index, line ->
                TinyAlphNum.draw(batch, line, hitbox.startX.toFloat(), hitbox.startY.toFloat() + TinyAlphNum.H * index)
            }
        }
    }
}