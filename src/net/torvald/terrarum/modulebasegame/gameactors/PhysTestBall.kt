package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser

/**
 * Created by minjaesong on 2016-03-05.
 */
class PhysTestBall : ActorWithBody(RenderOrder.MIDDLE, PhysProperties.PHYSICS_OBJECT()) {

    private var color = Color.GOLD

    init {
        setHitboxDimension(16, 16, 0, 0)
        avBaseMass = 10.0
        density = 200.0

        color = RoguelikeRandomiser.composeColourFrom(RoguelikeRandomiser.POTION_PRIMARY_COLSET)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        /*Terrarum.inShapeRenderer {
            it.color = color
            it.circle(
                    hitbox.startX.toFloat() - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() + (INGAME.world).width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )

            it.circle(
                    hitbox.startX.toFloat() - (INGAME.world).width * TILE_SIZE - 1f,
                    hitbox.startY.toFloat() - 1f,
                    hitbox.width.toFloat()
            )
        }*/

        //println(moveDelta)
    }
}