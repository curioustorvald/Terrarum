package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.colourutil.ColourTemp
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blendMul
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockstats.BlockStats
import net.torvald.terrarum.fillRect
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.TILE_SIZE

/**
 * Created by minjaesong on 2015-12-31.
 */
object FeaturesDrawer {
    internal var world: GameWorld = GameWorld.makeNullWorld()

    //const val TILE_SIZE = CreateTileAtlas.TILE_SIZE

    private val ENV_COLTEMP_LOWEST = 5500
    private val ENV_COLTEMP_HIGHEST = 7500

    val ENV_COLTEMP_NOON = 6500 // 6500 == sRGB White; do not touch!

    var colTemp: Int = 0
        private set

    private val TILES_COLD = intArrayOf(
              Block.ICE_MAGICAL
            , Block.ICE_FRAGILE
            , Block.ICE_NATURAL
            , Block.SNOW)

    private val TILES_WARM = intArrayOf(
              Block.SAND_DESERT
            , Block.SAND_RED)

    fun update(delta: Float) {
    }

    /**
     * A colour filter used to provide effect that makes whole screen look warmer/cooler,
     * usually targeted for the environmental temperature (desert/winterland), hence the name.
     */
    fun drawEnvOverlay(batch: SpriteBatch) {
        val onscreen_tiles_max = FastMath.ceil(Terrarum.HEIGHT * Terrarum.WIDTH / FastMath.sqr (TILE_SIZE.toFloat())) * 2
        val onscreen_tiles_cap = onscreen_tiles_max / 4f
        val onscreen_cold_tiles = BlockStats.getCount(*TILES_COLD).toFloat()
        val onscreen_warm_tiles = BlockStats.getCount(*TILES_WARM).toFloat()

        val colTemp_cold = colTempLinearFunc(onscreen_cold_tiles / onscreen_tiles_cap)
        val colTemp_warm = colTempLinearFunc(-(onscreen_warm_tiles / onscreen_tiles_cap))
        colTemp = colTemp_warm + colTemp_cold - ENV_COLTEMP_NOON
        val zoom = Terrarum.ingame?.screenZoom ?: 1f

        blendMul(batch)

        batch.color = ColourTemp(colTemp)
        batch.fillRect(0f, 0f,
                Terrarum.WIDTH * if (zoom < 1) 1f / zoom else zoom,
                Terrarum.HEIGHT * if (zoom < 1) 1f / zoom else zoom
        )

        blendNormal(batch)
    }

    /**

     * @param x [-1 , 1], 0 for 6500K (median of ENV_COLTEMP_HIGHEST and ENV_COLTEMP_LOWEST)
     * *
     * @return
     */
    private fun colTempLinearFunc(x: Float): Int {
        val colTempMedian = (ENV_COLTEMP_HIGHEST + ENV_COLTEMP_LOWEST) / 2

        return Math.round((ENV_COLTEMP_HIGHEST - ENV_COLTEMP_LOWEST) / 2 * FastMath.clamp(x, -1f, 1f) + colTempMedian)
    }
}