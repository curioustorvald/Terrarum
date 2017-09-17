package net.torvald.terrarum.worlddrawer

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.round

/**
 * Created by minjaesong on 2016-12-30.
 */
object WorldCamera {
    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE

    var x: Int = 0 // left position
        private set
    var y: Int = 0 // top position
        private set
    inline val gdxCamX: Float // centre position
        get() = xCentre.toFloat()
    inline val gdxCamY: Float// centre position
        get() = yCentre.toFloat()
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    inline val xCentre: Int
        get() = x + width.ushr(1)
    inline val yCentre: Int
        get() = y + height.ushr(1)

    fun update(world: GameWorld, player: ActorWithPhysics) {



        // FIXME player is stucked to the left (titlescreen AND ingame)



        width = FastMath.ceil(Terrarum.WIDTH / (Terrarum.ingame?.screenZoom ?: 1f)) // div, not mul
        height = FastMath.ceil(Terrarum.HEIGHT / (Terrarum.ingame?.screenZoom ?: 1f))

        // TOP-LEFT position of camera border
        x = player.hitbox.centeredX.toFloat().minus(width / 2).floorInt() // X only: ROUNDWORLD implementation
        y = (FastMath.clamp(
                player.hitbox.centeredY.toFloat() - height / 2,
                TILE_SIZE.toFloat(),
                world.height * TILE_SIZE - height - TILE_SIZE.toFloat()
        )).floorInt().clampCameraY(world)


    }

    private fun Int.clampCameraY(world: GameWorld): Int {
        return if (this < 0)
            0
        else if (this > world.height.times(TILE_SIZE) - Terrarum.HEIGHT)
            world.height.times(TILE_SIZE) - Terrarum.HEIGHT
        else
            this
    }
}

