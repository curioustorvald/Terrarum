package net.torvald.terrarum.worlddrawer

import com.jme3.math.FastMath
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.floorInt
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameworld.GameWorld
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2016-12-30.
 */
object WorldCamera {
    private val TILE_SIZE = CreateTileAtlas.TILE_SIZE

    var x: Int = 0 // left position
        private set
    var y: Int = 0 // top position
        private set
    var xEnd: Int = 0 // right position
        private set
    var yEnd: Int = 0 // bottom position
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

    private val nullVec = Vector2(0.0, 0.0)

    fun update(world: GameWorld, player: ActorWithBody?) {
        if (player == null) return

        width = FastMath.ceil(AppLoader.screenW / (Terrarum.ingame?.screenZoom ?: 1f)) // div, not mul
        height = FastMath.ceil(AppLoader.screenH / (Terrarum.ingame?.screenZoom ?: 1f))

        // TOP-LEFT position of camera border

        // some hacky equation to position player at the dead centre
        // implementing the "lag behind" camera the right way
        val pVecSum = Vector2(0.0, 0.0)//player.externalV + (player.controllerV ?: nullVec)

        x = ((player.hitbox.centeredX - pVecSum.x).toFloat() - (width / 2)).floorInt() // X only: ROUNDWORLD implementation


        y = (FastMath.clamp(
                (player.hitbox.centeredY - pVecSum.y).toFloat() - height / 2,
                TILE_SIZE.toFloat(),
                world.height * TILE_SIZE - height - TILE_SIZE.toFloat()
        )).floorInt().clampCameraY(world)

        xEnd = x + width
        yEnd = y + height
    }

    private fun Int.clampCameraY(world: GameWorld): Int {
        return if (this < 0)
            0
        else if (this > world.height.times(TILE_SIZE) - AppLoader.screenH)
            world.height.times(TILE_SIZE) - AppLoader.screenH
        else
            this
    }
}

