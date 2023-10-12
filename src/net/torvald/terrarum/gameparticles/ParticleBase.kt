package net.torvald.terrarum.gameparticles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import org.dyn4j.geometry.Vector2

/**
 * Actors with static sprites and very simple physics
 *
 * Created by minjaesong on 2017-01-20.
 */
open class ParticleBase(renderOrder: Actor.RenderOrder, var despawnUponCollision: Boolean, var noCollision: Boolean = true, maxLifeTime: Second? = null) : Runnable {

    /** Will NOT actually delete from the CircularArray */
    @Volatile var flagDespawn = false

    override fun run() = update(App.UPDATE_RATE)

    var isNoSubjectToGrav = false
    var dragCoefficient = 40.0

    val lifetimeMax = maxLifeTime ?: 5f
    var lifetimeCounter = 0f
        protected set

    val velocity = Vector2(0.0, 0.0)
    val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)

    open lateinit var body: TextureRegion // you might want to use SpriteAnimation
    open var glow: TextureRegion? = null

    val drawColour = Color(1f, 1f, 1f, 1f)

    init {

    }

    open fun update(delta: Float) {
        if (!flagDespawn) {
            lifetimeCounter += delta
            if (velocity.isZero ||
                // simple stuck check
                BlockCodex[(INGAME.world).getTileFromTerrain(
                        hitbox.centeredX.div(TerrarumAppConfiguration.TILE_SIZE).floorToInt(),
                        hitbox.startY.div(TerrarumAppConfiguration.TILE_SIZE).floorToInt()
                )].isSolid ||
                BlockCodex[(INGAME.world).getTileFromTerrain(
                        hitbox.centeredX.div(TerrarumAppConfiguration.TILE_SIZE).floorToInt(),
                        hitbox.endY.div(TerrarumAppConfiguration.TILE_SIZE).floorToInt()
                )].isSolid) {


                if (despawnUponCollision && lifetimeCounter >= 0.1f) flagDespawn = true
                if (!noCollision && lifetimeCounter >= 0.1f) velocity.y = 0.0
            }

            if (lifetimeCounter >= lifetimeMax) {
                flagDespawn = true
            }

            // gravity, winds, etc. (external forces)
            if (!isNoSubjectToGrav) {
                velocity.plusAssign((INGAME.world).gravitation / dragCoefficient)
            }


            // combine external forces
            hitbox.translate(velocity)
        }
    }

    open fun drawBody(batch: SpriteBatch) {
        if (!flagDespawn) {
            batch.color = drawColour
            drawBodyInGoodPosition(hitbox.startX.toFloat(), hitbox.startY.toFloat()) { x, y ->
                batch.draw(body, x, y, hitbox.width.toFloat(), hitbox.height.toFloat())
            }
        }
    }

    open fun drawGlow(batch: SpriteBatch) {
        if (!flagDespawn && glow != null) {
            batch.color = drawColour
            drawBodyInGoodPosition(hitbox.startX.toFloat(), hitbox.startY.toFloat()) { x, y ->
                batch.draw(glow, x, y, hitbox.width.toFloat(), hitbox.height.toFloat())
            }
        }
    }

    open fun dispose() {

    }
}