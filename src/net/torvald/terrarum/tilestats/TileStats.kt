package net.torvald.terrarum.tilestats

import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarum.gamemap.GameWorld
import net.torvald.terrarum.gamemap.MapLayer
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import com.jme3.math.FastMath

import java.util.Arrays

/**
 * Created by minjaesong on 16-02-01.
 */
object TileStats {

    private val tilestat = ShortArray(GameWorld.TILES_SUPPORTED)

    private val TSIZE = MapDrawer.TILE_SIZE

    /**
     * Update tile stats from tiles on screen
     */
    fun update() {
        Arrays.fill(tilestat, 0.toShort())

        // Get stats on no-zoomed screen area. In other words, will behave as if screen zoom were 1.0
        // no matter how the screen is zoomed.
        val map = Terrarum.ingame.world
        val player = Terrarum.ingame.player

        val renderWidth = FastMath.ceil(Terrarum.WIDTH.toFloat())
        val renderHeight = FastMath.ceil(Terrarum.HEIGHT.toFloat())

        val noZoomCameraX = Math.round(FastMath.clamp(
                player.hitbox.centeredX.toFloat() - renderWidth / 2, TSIZE.toFloat(), map.width * TSIZE - renderWidth - TSIZE.toFloat()))
        val noZoomCameraY = Math.round(FastMath.clamp(
                player.hitbox.centeredY.toFloat() - renderHeight / 2, TSIZE.toFloat(), map.width * TSIZE - renderHeight - TSIZE.toFloat()))

        val for_x_start = MapCamera.div16(noZoomCameraX)
        val for_y_start = MapCamera.div16(noZoomCameraY)
        val for_y_end = MapCamera.clampHTile(for_y_start + MapCamera.div16(renderHeight) + 2)
        val for_x_end = MapCamera.clampWTile(for_x_start + MapCamera.div16(renderWidth) + 2)

        for (y in for_y_start..for_y_end - 1) {
            for (x in for_x_start..for_x_end - 1) {
                val tileWall = map.getTileFromWall(x, y)
                val tileTerrain = map.getTileFromTerrain(x, y)
                ++tilestat[tileWall ?: 0]
                ++tilestat[tileTerrain ?: 0]
            }
        }
    }

    fun getCount(vararg tile: Byte): Int {
        var sum = 0
        for (i in tile.indices) {
            val newArgs = java.lang.Byte.toUnsignedInt(tile[i])
            sum += java.lang.Short.toUnsignedInt(tilestat[newArgs])
        }

        return sum
    }

    fun getCount(vararg tile: Int): Int {
        var sum = 0
        for (i in tile.indices) {
            sum += java.lang.Short.toUnsignedInt(tilestat[tile[i]])
        }
        return sum
    }

    /**

     * @return copy of the stat data
     */
    val statCopy: ShortArray
        get() = Arrays.copyOf(tilestat, MapLayer.RANGE)

}
