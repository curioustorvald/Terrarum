package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithSprite.Companion.SI_TO_GAME_ACC
import net.torvald.terrarum.mapdrawer.FeaturesDrawer.TILE_SIZE
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TileCodex
import org.dyn4j.geometry.Vector2
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Actors with static sprites and very simple physics
 *
 * Created by SKYHi14 on 2017-01-20.
 */
open class ParticleBase(renderOrder: ActorOrder, maxLifeTime: Int? = null) : ActorVisible(renderOrder), Projectile {

    override var actorValue = ActorValue()
    override @Volatile var flagDespawn = false

    override fun run() {
        TODO("not implemented")
    }

    var isNoSubjectToGrav = false
    var dragCoefficient = 3.0

    private val lifetimeMax = maxLifeTime ?: 5000
    private var lifetimeCounter = 0

    open val velocity = Vector2(0.0, 0.0)

    open lateinit var image: Image

    init {

    }

    override fun update(gc: GameContainer, delta: Int) {
        lifetimeCounter += delta
        if (velocity.isZero || lifetimeCounter >= lifetimeMax ||
            // simple stuck check
            TileCodex[Terrarum.ingame.world.getTileFromTerrain(
                    hitbox.pointedX.div(TILE_SIZE).floorInt(),
                    hitbox.pointedY.div(TILE_SIZE).floorInt()
            ) ?: Tile.STONE].isSolid) {
            flagDespawn = true
        }

        // gravity, winds, etc. (external forces)
        if (!isNoSubjectToGrav) {
            velocity += Terrarum.ingame.world.gravitation / dragCoefficient * SI_TO_GAME_ACC
        }


        // combine external forces
        hitbox.translate(velocity)
    }

    override fun drawBody(g: Graphics) {
        g.drawImage(image, hitbox.centeredX.toFloat(), hitbox.centeredY.toFloat())
    }

    override fun drawGlow(g: Graphics) {
    }
}