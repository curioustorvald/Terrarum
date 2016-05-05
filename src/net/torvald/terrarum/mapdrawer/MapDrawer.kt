package net.torvald.terrarum.mapdrawer

import net.torvald.terrarum.gamemap.GameMap
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.TileNameCode
import net.torvald.terrarum.tilestats.TileStats
import com.jme3.math.FastMath
import org.newdawn.slick.*

/**
 * Created by minjaesong on 15-12-31.
 */
object MapDrawer {
    const val TILE_SIZE = 16

    private var envOverlayColourmap: Image = Image("./res/graphics/colourmap/black_body_col_1000_40000_K.png")

    private val ENV_COLTEMP_LOWEST = 5500
    private val ENV_COLTEMP_HIGHEST = 7500

    val ENV_COLTEMP_NOON = 6500 // 6500 == sRGB White == untouched!

    private var colTemp: Int = 0

    private val TILES_COLD = intArrayOf(
              TileNameCode.ICE_MAGICAL
            , TileNameCode.ICE_FRAGILE
            , TileNameCode.ICE_NATURAL
            , TileNameCode.SNOW)

    private val TILES_WARM = intArrayOf(
              TileNameCode.SAND_DESERT
            , TileNameCode.SAND_RED)

    @JvmStatic
    fun update(gc: GameContainer, delta_t: Int) {
    }

    @JvmStatic
    fun render(gc: GameContainer, g: Graphics) {
    }

    @JvmStatic
    fun drawEnvOverlay(g: Graphics) {
        val onscreen_tiles_max = FastMath.ceil(Terrarum.HEIGHT * Terrarum.WIDTH / FastMath.sqr(TILE_SIZE.toFloat())) * 2
        val onscreen_tiles_cap = onscreen_tiles_max / 4f
        val onscreen_cold_tiles = TileStats.getCount(*TILES_COLD).toFloat()
        val onscreen_warm_tiles = TileStats.getCount(*TILES_WARM).toFloat()

        val colTemp_cold = colTempLinearFunc(onscreen_cold_tiles / onscreen_tiles_cap)
        val colTemp_warm = colTempLinearFunc(-(onscreen_warm_tiles / onscreen_tiles_cap))
        colTemp = colTemp_warm + colTemp_cold - ENV_COLTEMP_NOON
        val zoom = Terrarum.game.screenZoom

        g.color = getColourFromMap(colTemp)
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

    fun getColourFromMap(K: Int): Color {
        return envOverlayColourmap.getColor(colTempToImagePos(K), 0)
    }

    private fun colTempToImagePos(K: Int): Int {
        if (K < 1000 || K >= 40000) throw IllegalArgumentException("K: out of range. ($K)")
        return (K - 1000) / 10
    }

    @JvmStatic
    fun getColTemp(): Int {
        return colTemp
    }
}