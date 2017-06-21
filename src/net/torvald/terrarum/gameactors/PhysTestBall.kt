package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.worldgenerator.RoguelikeRandomiser

/**
 * Created by minjaesong on 16-03-05.
 */
class PhysTestBall : ActorWithPhysics(Actor.RenderOrder.MIDDLE, immobileBody = true) {

    private var color = Color.GOLD

    init {
        setHitboxDimension(16, 16, 0, 0)
        avBaseMass = 10.0
        density = 200.0

        color = RoguelikeRandomiser.composeColourFrom(RoguelikeRandomiser.POTION_PRIMARY_COLSET)
    }

    override fun drawBody(batch: SpriteBatch) {
        TerrarumGDX.inShapeRenderer {
            it.color = color
            it.circle(
                    hitbox.startX.toFloat() - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() + TerrarumGDX.ingame!!.world.width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() - TerrarumGDX.ingame!!.world.width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )
        }

        //println(moveDelta)
    }
}