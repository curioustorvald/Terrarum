package net.torvald.terrarum.worlddrawer

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.floor
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
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
    var gdxCamX: Float = 0f // centre position
        private set
    var gdxCamY: Float = 0f // centre position
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    val xCentre: Int
        get() = x + width.ushr(1)
    val yCentre: Int
        get() = y + height.ushr(1)

    fun update(world: GameWorld, player: ActorWithBody) {
        width = FastMath.ceil(Terrarum.WIDTH / (Terrarum.ingame?.screenZoom ?: 1f)) // div, not mul
        height = FastMath.ceil(Terrarum.HEIGHT / (Terrarum.ingame?.screenZoom ?: 1f))

        // position - (WH / 2)
        x = player.hitbox.startX.toFloat().floorInt() // X only: ROUNDWORLD implementation
        y = (FastMath.clamp(
                player.hitbox.centeredY.toFloat() - height / 2,
                TILE_SIZE.toFloat(),
                world.height * TILE_SIZE - height - TILE_SIZE.toFloat()
        )).floorInt().clampCameraY(world)



        gdxCamX = x + (width / 2f).floor()
        gdxCamY = y + (height / 2f).floor()
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

