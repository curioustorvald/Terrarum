package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarum.gameactors.floor
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.round

/**
 * Created by minjaesong on 2016-12-30.
 */
object WorldCamera {
    private val world: GameWorld? = TerrarumGDX.ingame?.world
    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE

    var x: Int = 0
        private set
    var y: Int = 0
        private set
    var gdxCamX: Float = 0f
        private set
    var gdxCamY: Float = 0f
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
        if (TerrarumGDX.ingame != null) {

            val player = TerrarumGDX.ingame!!.player

            width = FastMath.ceil(TerrarumGDX.WIDTH / TerrarumGDX.ingame!!.screenZoom) // div, not mul
            height = FastMath.ceil(TerrarumGDX.HEIGHT / TerrarumGDX.ingame!!.screenZoom)

            // position - (WH / 2)
            x = (// X only: ROUNDWORLD implementation
                    (player?.hitbox?.centeredX?.toFloat() ?: 0f) - width / 2).roundInt()
            y = (FastMath.clamp(
                    (player?.hitbox?.centeredY?.toFloat() ?: 0f) - height / 2,
                    TILE_SIZE.toFloat(),
                    world!!.height * TILE_SIZE - height - TILE_SIZE.toFloat()
            )).roundInt()


            gdxCamX = x + (width / 2f).round()
            gdxCamY = y + (height / 2f).round()
        }
    }
}