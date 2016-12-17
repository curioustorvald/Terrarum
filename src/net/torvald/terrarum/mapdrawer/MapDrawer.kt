package net.torvald.terrarum.mapdrawer

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tilestats.TileStats
import com.jme3.math.FastMath
import net.torvald.colourutil.ColourTemp
import org.newdawn.slick.*

/**
 * Created by minjaesong on 15-12-31.
 */
object MapDrawer {
    const val TILE_SIZE = 16

    private val ENV_COLTEMP_LOWEST = 5500
    private val ENV_COLTEMP_HIGHEST = 7500

    val ENV_COLTEMP_NOON = 6500 // 6500 == sRGB White == untouched!

    var colTemp: Int = 0
        private set

    private val TILES_COLD = intArrayOf(
              Tile.ICE_MAGICAL
            , Tile.ICE_FRAGILE
            , Tile.ICE_NATURAL
            , Tile.SNOW)

    private val TILES_WARM = intArrayOf(
              Tile.SAND_DESERT
            , Tile.SAND_RED)

    fun update(gc: GameContainer, delta_t: Int) {
    }

    fun render(gc: GameContainer, g: Graphics) {
    }

    fun drawEnvOverlay(g: Graphics) {
        val onscreen_tiles_max = FastMath.ceil(Terrarum.HEIGHT * Terrarum.WIDTH / FastMath.sqr(TILE_SIZE.toFloat())) * 2
        val onscreen_tiles_cap = onscreen_tiles_max / 4f
        val onscreen_cold_tiles = TileStats.getCount(*TILES_COLD).toFloat()
        val onscreen_warm_tiles = TileStats.getCount(*TILES_WARM).toFloat()

        val colTemp_cold = colTempLinearFunc(onscreen_cold_tiles / onscreen_tiles_cap)
        val colTemp_warm = colTempLinearFunc(-(onscreen_warm_tiles / onscreen_tiles_cap))
        colTemp = colTemp_warm + colTemp_cold - ENV_COLTEMP_NOON
        val zoom = Terrarum.ingame.screenZoom

        g.color = ColourTemp(colTemp)
        //g.color = getColourFromMap(3022)
        g.fillRect(
                MapCamera.cameraX * zoom,
                MapCamera.cameraY * zoom,
                Terrarum.WIDTH * if (zoom < 1) 1f / zoom else zoom,
                Terrarum.HEIGHT * if (zoom < 1) 1f / zoom else zoom
        )
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