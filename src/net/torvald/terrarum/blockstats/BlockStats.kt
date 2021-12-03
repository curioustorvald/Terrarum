package net.torvald.terrarum.blockstats

import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.worlddrawer.BlocksDrawer

/**
 * Created by minjaesong on 2016-02-01.
 */
object BlockStats {

    private val tilestat = HashMap<ItemID, Int>()
    
    /**
     * Update tile stats from tiles on screen
     */
    fun update() {
        tilestat.clear()

        // Get stats on no-zoomed screen area. In other words, will behave as if screen zoom were 1.0
        // no matter how the screen is zoomed.
        val map = (INGAME.world)
        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
        if (player == null) return

        val renderWidth = FastMath.ceil(App.scr.wf)
        val renderHeight = FastMath.ceil(App.scr.hf)

        val noZoomCameraX = Math.round(FastMath.clamp(
                player.hitbox.centeredX.toFloat() - renderWidth / 2, TILE_SIZEF, map.width * TILE_SIZE - renderWidth - TILE_SIZEF))
        val noZoomCameraY = Math.round(FastMath.clamp(
                player.hitbox.centeredY.toFloat() - renderHeight / 2, TILE_SIZEF, map.width * TILE_SIZE - renderHeight - TILE_SIZEF))

        val for_x_start = noZoomCameraX / TILE_SIZE
        val for_y_start = noZoomCameraY / TILE_SIZE
        val for_y_end = BlocksDrawer.clampHTile(for_y_start + (renderHeight / TILE_SIZE) + 2)
        val for_x_end = BlocksDrawer.clampWTile(for_x_start + (renderWidth / TILE_SIZE) + 2)

        for (y in for_y_start..for_y_end - 1) {
            for (x in for_x_start..for_x_end - 1) {
                val tileWall = map.getTileFromWall(x, y)
                val tileTerrain = map.getTileFromTerrain(x, y)
                tilestat[tileWall] = 1 + (tilestat[tileWall] ?: 0)
                tilestat[tileTerrain] = 1 + (tilestat[tileTerrain] ?: 0)
            }
        }
    }

    fun getCount(vararg tiles: ItemID): Int {
        return tiles.fold(0) { acc, key -> acc + (tilestat[key] ?: 0) }
    }

}
