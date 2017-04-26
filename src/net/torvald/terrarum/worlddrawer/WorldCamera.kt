package net.torvald.terrarum.worlddrawer

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-12-30.
 */
object WorldCamera {
    private val world: GameWorld? = Terrarum.ingame?.world
    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE

    var x: Int = 0
        private set
    var y: Int = 0
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    val xCentre: Int
        get() = x + width.ushr(1)
    val yCentre: Int
        get() = y + height.ushr(1)

    fun update() {
        if (Terrarum.ingame != null) {

            val player = Terrarum.ingame!!.player

            width = FastMath.ceil(Terrarum.WIDTH / Terrarum.ingame!!.screenZoom) // div, not mul
            height = FastMath.ceil(Terrarum.HEIGHT / Terrarum.ingame!!.screenZoom)

            // position - (WH / 2)
            x = Math.round(// X only: ROUNDWORLD implementation
                    (player?.hitbox?.centeredX?.toFloat() ?: 0f) - width / 2)
            y = Math.round(FastMath.clamp(
                    (player?.hitbox?.centeredY?.toFloat() ?: 0f) - height / 2,
                    TILE_SIZE.toFloat(),
                    world!!.height * TILE_SIZE - height - TILE_SIZE.toFloat()
            ))

        }
    }
}