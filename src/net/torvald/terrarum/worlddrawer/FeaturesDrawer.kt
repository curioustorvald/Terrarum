package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.colourutil.ColourTemp
import net.torvald.terrarum.*
import net.torvald.terrarum.blockstats.TileSurvey
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.TheGameWorld
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2015-12-31.
 */
object FeaturesDrawer {

    /** World change is managed by IngameRenderer.setWorld() */
    internal var world: GameWorld = TheGameWorld.makeNullWorld()

    //const val TILE_SIZE = TILE_SIZE

    private val ENV_COLTEMP_LOWEST = 5500
    private val ENV_COLTEMP_HIGHEST = 7500

    val ENV_COLTEMP_NOON = 6500 // 6500 == sRGB White; do not touch!

    var colTemp: Int = 0
        private set

    init {
        TileSurvey.submitProposal(
            TileSurvey.SurveyProposal(
                "basegame.FeaturesDrawer.coldTiles", 73, 49, 2, 2
            ) { world, x, y -> BlockCodex[world.getTileFromTerrain(x, y)].hasTag("COLD").toInt().toFloat() }
        )
        TileSurvey.submitProposal(
            TileSurvey.SurveyProposal(
                "basegame.FeaturesDrawer.warmTiles", 73, 49, 2, 2
            ) { world, x, y -> BlockCodex[world.getTileFromTerrain(x, y)].hasTag("WARM").toInt().toFloat() }
        )
    }

    fun update(delta: Float) {
    }

    /**
     * A colour filter used to provide effect that makes whole screen look warmer/cooler,
     * usually targeted for the environmental temperature (desert/winterland), hence the name.
     */
    fun drawEnvOverlay(batch: SpriteBatch) {
        val onscreen_tiles_cap = 0.3
        val onscreen_cold_tiles = (TileSurvey.getRatio("basegame.FeaturesDrawer.coldTiles") ?: 0.0).coerceAtMost(onscreen_tiles_cap)
        val onscreen_warm_tiles = (TileSurvey.getRatio("basegame.FeaturesDrawer.warmTiles") ?: 0.0).coerceAtMost(onscreen_tiles_cap)

        val colTemp_cold = colTempLinearFunc((onscreen_cold_tiles / onscreen_tiles_cap).toFloat())
        val colTemp_warm = colTempLinearFunc(-(onscreen_warm_tiles / onscreen_tiles_cap).toFloat())
        colTemp = colTemp_warm + colTemp_cold - ENV_COLTEMP_NOON
        val zoom = Terrarum.ingame?.screenZoom ?: 1f

        blendMul(batch)

        batch.color = ColourTemp(colTemp)
        Toolkit.fillArea(batch, 0, 0,
                (App.scr.width * if (zoom < 1) 1f / zoom else zoom).roundToInt(),
                (App.scr.height * if (zoom < 1) 1f / zoom else zoom).roundToInt()
        )

        blendNormalStraightAlpha(batch)
    }

    /**

     * @param x [-1 , 1], 0 for 6500K (median of ENV_COLTEMP_HIGHEST and ENV_COLTEMP_LOWEST)
     * *
     * @return
     */
    private fun colTempLinearFunc(x: Float): Int {
        val colTempMedian = (ENV_COLTEMP_HIGHEST + ENV_COLTEMP_LOWEST) / 2

        return ((ENV_COLTEMP_HIGHEST - ENV_COLTEMP_LOWEST) / 2 * x.coerceIn(-1f, 1f) + colTempMedian).roundToInt()
    }
}