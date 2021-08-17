package net.torvald.terrarum.gameparticles

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * @param tex image
 * @param x x-coord of the particle's initial spawn position, bottom-centre
 * @param y y-coord of the particle's initial spawn position, bottom-centre
 */
open class ParticleVanishingTexture(val tex: TextureRegion, x: Double, y: Double, noCollision: Boolean = true) : ParticleBase(Actor.RenderOrder.OVERLAY, false, noCollision, 2f) {

    init {
        velocity.set(0.0, -1.0)
        hitbox.setDimension(tex.regionWidth.toDouble(), tex.regionHeight.toDouble())
        hitbox.setPositionFromPointed(x, y)

        body = tex

        isNoSubjectToGrav = true
    }

    override fun update(delta: Float) {
        super.update(delta)

        drawColour.a = (lifetimeMax - lifetimeCounter) / lifetimeMax
    }
}

/**
 * @param text a text
 * @param x x-coord of the particle's initial spawn position, bottom-centre
 * @param y y-coord of the particle's initial spawn position, bottom-centre
 */
class ParticleVanishingText(val text: String, x: Double, y: Double, noCollision: Boolean = true) : ParticleBase(Actor.RenderOrder.OVERLAY, false, noCollision, 2f) {

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

/**
 * @param tex image
 * @param x x-coord of the particle's initial spawn position, bottom-centre
 * @param y y-coord of the particle's initial spawn position, bottom-centre
 */
open class ParticleVanishingSprite(val sprite: TextureRegionPack, val delay: Float, x: Double, y: Double, val start: Int = 0, noCollision: Boolean = true) : ParticleBase(Actor.RenderOrder.OVERLAY, false, noCollision, 2f) {

    private var row = 0
    private var frame = start % sprite.horizontalCount
    private var frameAdvanceCounter = 0f

    init {
        velocity.set(0.0, -1.0)
        hitbox.setDimension(sprite.tileW.toDouble(), sprite.tileH.toDouble())
        hitbox.setPositionFromPointed(x, y)

        isNoSubjectToGrav = true
    }

    override fun update(delta: Float) {
        super.update(delta)

        drawColour.a = (lifetimeMax - lifetimeCounter) / lifetimeMax

        if (frameAdvanceCounter >= delay) {
            frameAdvanceCounter -= delay
            frame = (frame + 1) % sprite.horizontalCount
        }
        frameAdvanceCounter += delta
    }

    override fun drawBody(batch: SpriteBatch) {
        if (!flagDespawn) {
            batch.color = drawColour
            batch.draw(sprite.get(frame, row), hitbox.startX.toFloat(), hitbox.startY.toFloat())
        }
    }
}