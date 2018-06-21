package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameactors.ActorWithPhysics.Companion.SI_TO_GAME_ACC
import net.torvald.terrarum.worlddrawer.FeaturesDrawer.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.modulebasegame.Ingame
import org.dyn4j.geometry.Vector2

/**
 * Actors with static sprites and very simple physics
 *
 * Created by minjaesong on 2017-01-20.
 */
open class ParticleBase(renderOrder: Actor.RenderOrder, val despawnUponCollision: Boolean, maxLifeTime: Second? = null) : Runnable {

    /** Will NOT actually delete from the CircularArray */
    @Volatile var flagDespawn = false

    override fun run() = update(Terrarum.deltaTime)

    var isNoSubjectToGrav = false
    var dragCoefficient = 3.0

    private val lifetimeMax = maxLifeTime ?: 5f
    private var lifetimeCounter = 0f

    open val velocity = Vector2(0.0, 0.0)
    open val hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)

    open lateinit var body: TextureRegion // you might want to use SpriteAnimation
    open var glow: TextureRegion? = null

    init {

    }

    fun update(delta: Float) {
        if (!flagDespawn) {
            lifetimeCounter += delta
            if (despawnUponCollision) {
                if (velocity.isZero ||
                    // simple stuck check
                    BlockCodex[(Terrarum.ingame!! as Ingame).world.getTileFromTerrain(
                            hitbox.canonicalX.div(TILE_SIZE).floorInt(),
                            hitbox.canonicalY.div(TILE_SIZE).floorInt()
                    ) ?: Block.STONE].isSolid) {
                    flagDespawn = true
                }
            }

            if (lifetimeCounter >= lifetimeMax) {
                flagDespawn = true
            }

            // gravity, winds, etc. (external forces)
            if (!isNoSubjectToGrav) {
                velocity += (Terrarum.ingame!! as Ingame).world.gravitation / dragCoefficient * SI_TO_GAME_ACC
            }


            // combine external forces
            hitbox.translate(velocity)
        }
    }

    fun drawBody(batch: SpriteBatch) {
        if (!flagDespawn) {
            batch.draw(body, hitbox.startX.toFloat(), hitbox.startY.toFloat(), hitbox.width.toFloat(), hitbox.height.toFloat())
        }
    }

    fun drawGlow(batch: SpriteBatch) {
        if (!flagDespawn && glow != null) {
            batch.draw(glow, hitbox.startX.toFloat(), hitbox.startY.toFloat(), hitbox.width.toFloat(), hitbox.height.toFloat())
        }
    }
}