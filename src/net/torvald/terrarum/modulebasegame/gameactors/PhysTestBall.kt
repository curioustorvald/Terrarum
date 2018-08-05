package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser

/**
 * Created by minjaesong on 2016-03-05.
 */
class PhysTestBall(world: GameWorld) : ActorWBMovable(world, RenderOrder.MIDDLE, immobileBody = true) {

    private var color = Color.GOLD

    init {
        setHitboxDimension(16, 16, 0, 0)
        avBaseMass = 10.0
        density = 200.0

        color = RoguelikeRandomiser.composeColourFrom(RoguelikeRandomiser.POTION_PRIMARY_COLSET)
    }

    override fun drawBody(batch: SpriteBatch) {
        Terrarum.inShapeRenderer {
            it.color = color
            it.circle(
                    hitbox.startX.toFloat() - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() + (Terrarum.ingame!!.world).width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() - (Terrarum.ingame!!.world).width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )
        }

        //println(moveDelta)
    }
}