package net.torvald.terrarum.gameparticles

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.App
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * The texture must be manually discarded.
 *
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

// pickaxe sparks must use different create- function
fun createRandomBlockParticle(tileNum: Int, position: Vector2, velocityMult: Vector2, tx: Int, ty: Int, tw: Int, th: Int): ParticleBase {
    val velocity = Vector2(
        (Math.random() + Math.random()) * velocityMult.x,
        -velocityMult.y
    ) // triangular distribution with mean of 1.0 * velocityMult

    val atlasX = tileNum % BlocksDrawer.weatherTerrains[1].horizontalCount
    val atlasY = tileNum / BlocksDrawer.weatherTerrains[1].horizontalCount
    // take base texture
    val texBody = BlocksDrawer.weatherTerrains[1].get(atlasX, atlasY)
    val texGlow = BlocksDrawer.tilesGlow.get(atlasX, atlasY)

    // take random square part
    val texRegionBody = TextureRegion(texBody.texture, texBody.regionX + tx, texBody.regionY + ty, tw, th)
    val texRegionGlow = TextureRegion(texGlow.texture, texGlow.regionX + tx, texGlow.regionY + ty, tw, th)

    return ParticleVanishingTexture(texRegionBody, position.x, position.y).also {
        it.glow = texRegionGlow
        it.velocity.set(velocity)
        it.isNoSubjectToGrav = false
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
            val oldColour = batch.color.cpy()
            batch.color = drawColour
            lines.forEachIndexed { index, line ->
                drawBodyInGoodPosition(hitbox.startX.toFloat(), hitbox.startY.toFloat() + TinyAlphNum.H * index) { x, y ->
                    TinyAlphNum.draw(batch, line, x, y )
                }
            }
            batch.color = oldColour
        }
    }
}

/**
 * @param tex image
 * @param x x-coord of the particle's initial spawn position, bottom-centre
 * @param y y-coord of the particle's initial spawn position, bottom-centre
 */
open class ParticleVanishingSprite(val sprite: TextureRegionPack, val delay: Float, val loop: Boolean, x: Double, y: Double, noCollision: Boolean = true, startFrame: Int = 0, val row: Int = 0) : ParticleBase(Actor.RenderOrder.OVERLAY, false, noCollision, 2f) {

    private var frame = startFrame % sprite.horizontalCount
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

            if (frame == sprite.horizontalCount - 1 && loop)
                frame = 0
            else if (frame != sprite.horizontalCount - 1)
                frame += 1
        }
        frameAdvanceCounter += delta
    }

    override fun drawBody(batch: SpriteBatch) {
        if (!flagDespawn) {
            val oldColour = batch.color.cpy()
            batch.color = drawColour
            drawBodyInGoodPosition(hitbox.startX.toFloat(), hitbox.startY.toFloat()) { x, y ->
                batch.draw(sprite.get(frame, row), x, y)
            }
            batch.color = oldColour
        }
    }
}